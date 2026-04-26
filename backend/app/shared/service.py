from __future__ import annotations

from ..preview.schema import PreviewKnowledgeRequest
from ..preview.service import PreviewService
from .OCR import OCRAgent
from .document_parser import DocumentParserAgent
from .schema import (
    DocumentDispatchData,
    DocumentParseData,
    GoalType,
    OcrData,
    OcrRequest,
    SharedDispatchData,
    TargetModule,
)


_GUIDE_GOALS = {"做题模块", "guide"}
_PREVIEW_GOALS = {"预习模块", "preview"}
_MISTAKES_GOALS = {"错题本模块", "mistakes"}


class SharedService:
    def __init__(
        self,
        *,
        ocr_agent: OCRAgent | None = None,
        document_parser: DocumentParserAgent | None = None,
        preview_service: PreviewService | None = None,
    ) -> None:
        self.ocr_agent = ocr_agent or OCRAgent()
        self.document_parser = document_parser or DocumentParserAgent()
        self.preview_service = preview_service or PreviewService()

    def recognize_ocr(self, request: OcrRequest) -> OcrData:
        result = self.ocr_agent.recognize(
            image_base64=request.image_base64,
            image_type=request.image_type,
        )
        return OcrData(
            image_id=result["image_id"],
            question_text=result["question_text"],
        )

    def dispatch_ocr_result(self, request: OcrRequest) -> SharedDispatchData:
        ocr_data = self.recognize_ocr(request)
        question_text = ocr_data.question_text.strip()
        if not question_text:
            raise RuntimeError("OCR did not extract any question text.")

        target_module = self._resolve_target_module(request.goals)
        if target_module == "guide":
            return self._forward_to_guide(request=request, ocr_data=ocr_data)
        if target_module == "preview":
            return self._forward_to_preview(request=request, ocr_data=ocr_data)
        return self._build_mistakes_handoff(request=request, ocr_data=ocr_data)

    def parse_document(
        self,
        *,
        user_id: str,
        file_name: str,
        file_bytes: bytes,
        llm_enhancement: bool = True,
        formula_enhancement: bool = True,
    ) -> DocumentParseData:
        result = self.document_parser.parse_bytes(
            file_bytes=file_bytes,
            file_name=file_name,
            llm_enhancement=llm_enhancement,
            formula_enhancement=formula_enhancement,
        )
        return DocumentParseData(user_id=user_id, **result)

    def dispatch_document_result(
        self,
        *,
        user_id: str,
        goal: GoalType,
        file_name: str,
        file_bytes: bytes,
        llm_enhancement: bool = True,
        formula_enhancement: bool = True,
    ) -> DocumentDispatchData:
        parsed_document = self.parse_document(
            user_id=user_id,
            file_name=file_name,
            file_bytes=file_bytes,
            llm_enhancement=llm_enhancement,
            formula_enhancement=formula_enhancement,
        )
        target_module = self._resolve_target_module(goal)
        if target_module == "preview":
            return self._forward_document_to_preview(
                user_id=user_id,
                goal=goal,
                parsed_document=parsed_document,
            )
        return DocumentDispatchData(
            user_id=user_id,
            goal=goal,
            target_module=target_module,
            dispatch_status="handoff_required",
            parsed_document=parsed_document,
            downstream_endpoint=None,
            downstream_request={
                "user_id": user_id,
                "file_name": file_name,
                "content_text": parsed_document.plain_text,
            },
            downstream_response=None,
            notes="文档已经解析完成，但当前只为预习模块提供自动转发。",
        )

    def _resolve_target_module(self, goal: GoalType) -> TargetModule:
        if goal in _GUIDE_GOALS:
            return "guide"
        if goal in _PREVIEW_GOALS:
            return "preview"
        if goal in _MISTAKES_GOALS:
            return "mistakes"
        raise ValueError(f"Unsupported goals value: {goal}")

    def _forward_to_guide(self, *, request: OcrRequest, ocr_data: OcrData) -> SharedDispatchData:
        try:
            from ..guide.session_manager import create_session
        except ModuleNotFoundError as exc:
            raise RuntimeError(f"Guide module dependency missing: {exc.name}") from exc

        problem_text = ocr_data.question_text.strip()
        session_id = create_session(problem_text)
        return SharedDispatchData(
            user_id=request.user_id,
            image_id=ocr_data.image_id,
            question_text=problem_text,
            goal=request.goals,
            target_module="guide",
            dispatch_status="forwarded",
            downstream_endpoint="/api/guide/sessions",
            downstream_request={"problem_text": problem_text},
            downstream_response={"session_id": session_id},
            notes="OCR result was forwarded to the guide module and a new session was created.",
        )

    def _forward_to_preview(self, *, request: OcrRequest, ocr_data: OcrData) -> SharedDispatchData:
        downstream_request = PreviewKnowledgeRequest(
            user_id=request.user_id,
            content_text=ocr_data.question_text.strip(),
            source_type="screenshot_ocr",
        )
        preview_result = self.preview_service.get_knowledge_points(downstream_request)
        return SharedDispatchData(
            user_id=request.user_id,
            image_id=ocr_data.image_id,
            question_text=ocr_data.question_text.strip(),
            goal=request.goals,
            target_module="preview",
            dispatch_status="forwarded",
            downstream_endpoint="/api/preview/knowledge-points",
            downstream_request=downstream_request.model_dump(exclude_none=True),
            downstream_response=preview_result.model_dump(),
            notes="OCR result was forwarded to the preview module for knowledge-point extraction.",
        )

    def _build_mistakes_handoff(self, *, request: OcrRequest, ocr_data: OcrData) -> SharedDispatchData:
        handoff_payload = {
            "user_id": request.user_id,
            "image_id": ocr_data.image_id,
            "image_type": request.image_type,
            "question_text": ocr_data.question_text.strip(),
            "source": "ocr",
        }
        return SharedDispatchData(
            user_id=request.user_id,
            image_id=ocr_data.image_id,
            question_text=ocr_data.question_text.strip(),
            goal=request.goals,
            target_module="mistakes",
            dispatch_status="handoff_required",
            downstream_endpoint=None,
            downstream_request=handoff_payload,
            downstream_response=None,
            notes="The mistakes module is not implemented in this checkout yet. Use downstream_request as the handoff payload.",
        )

    def _forward_document_to_preview(
        self,
        *,
        user_id: str,
        goal: GoalType,
        parsed_document: DocumentParseData,
    ) -> DocumentDispatchData:
        content_text = parsed_document.markdown_text.strip() or parsed_document.plain_text.strip()
        if not content_text:
            raise RuntimeError("文档解析完成，但没有可用于预习模块的文本内容。")

        downstream_request = PreviewKnowledgeRequest(
            user_id=user_id,
            content_text=content_text,
            source_type="document_upload",
        )
        preview_result = self.preview_service.get_knowledge_points(downstream_request)
        return DocumentDispatchData(
            user_id=user_id,
            goal=goal,
            target_module="preview",
            dispatch_status="forwarded",
            parsed_document=parsed_document,
            downstream_endpoint="/api/preview/knowledge-points",
            downstream_request=downstream_request.model_dump(exclude_none=True),
            downstream_response=preview_result.model_dump(),
            notes="文档已解析并自动转发到预习模块，生成知识点摘要与知识点列表。",
        )


__all__ = ["SharedService"]
