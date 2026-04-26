from __future__ import annotations

import json
import logging
from typing import Any

from fastapi import APIRouter, File, Form, UploadFile, status
from fastapi.responses import JSONResponse, StreamingResponse

from ..preview.schema import (
	ApiResponse,
	PreviewChatRequest,
	PreviewChatResponse,
	PreviewDocumentHandoutResponse,
	PreviewKnowledgeRequest,
	PreviewKnowledgeResponse,
	PreviewUploadHandoutRequest,
)
from ..preview.service import PreviewService
from ..shared.service import SharedService


preview_router = APIRouter(prefix="/api/preview", tags=["preview"])
_preview_service: PreviewService | None = None
_shared_service: SharedService | None = None
logger = logging.getLogger(__name__)


def get_preview_service() -> PreviewService:
	global _preview_service
	if _preview_service is None:
		_preview_service = PreviewService()
	return _preview_service


def get_shared_service() -> SharedService:
	global _shared_service
	if _shared_service is None:
		_shared_service = SharedService()
	return _shared_service


@preview_router.get("/health")
def preview_health() -> ApiResponse[dict[str, str]]:
	return ApiResponse(data={"status": "ok", "module": "preview"})


@preview_router.post("/knowledge-points", response_model=PreviewKnowledgeResponse)
def analyze_knowledge_points(request: PreviewKnowledgeRequest) -> PreviewKnowledgeResponse | JSONResponse:
	try:
		data = get_preview_service().get_knowledge_points(request)
		return PreviewKnowledgeResponse(data=data)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4001,
			message=str(exc),
		)
	except RuntimeError as exc:
		return _error_response(
			status_code=status.HTTP_502_BAD_GATEWAY,
			code=5001,
			message=str(exc),
		)
	except Exception as exc:
		return _error_response(
			status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
			code=5000,
			message=f"Internal error: {exc}",
		)


@preview_router.post("/chat", response_model=PreviewChatResponse)
def preview_chat(request: PreviewChatRequest) -> PreviewChatResponse | JSONResponse:
	try:
		data = get_preview_service().preview_chat(request)
		return PreviewChatResponse(data=data)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4002,
			message=str(exc),
		)
	except RuntimeError as exc:
		return _error_response(
			status_code=status.HTTP_502_BAD_GATEWAY,
			code=5002,
			message=str(exc),
		)
	except Exception as exc:
		return _error_response(
			status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
			code=5000,
			message=f"Internal error: {exc}",
		)


@preview_router.post("/chat/stream")
def preview_chat_stream(request: PreviewChatRequest) -> StreamingResponse:
	def event_generator():
		try:
			for event_name, payload in get_preview_service().stream_preview_chat(request):
				yield _format_sse_event(event_name, payload)
		except ValueError as exc:
			yield _format_sse_event("error", {"message": str(exc), "code": 4002})
		except RuntimeError as exc:
			yield _format_sse_event("error", {"message": str(exc), "code": 5002})
		except Exception as exc:
			logger.exception("Preview stream chat failed")
			yield _format_sse_event("error", {"message": f"Internal error: {exc}", "code": 5000})

	return StreamingResponse(
		event_generator(),
		media_type="text/event-stream",
		headers={
			"Cache-Control": "no-cache",
			"X-Accel-Buffering": "no",
		},
	)


@preview_router.post("/documents/upload-handout", response_model=PreviewDocumentHandoutResponse)
async def upload_document_handout(
	user_id: str = Form(...),
	file: UploadFile = File(...),
	llm_enhancement: bool = Form(default=True),
	formula_enhancement: bool = Form(default=True),
) -> PreviewDocumentHandoutResponse | JSONResponse:
	try:
		file_name = file.filename or "uploaded_document"
		file_bytes = await file.read()
		parsed_document = get_shared_service().parse_document(
			user_id=user_id,
			file_name=file_name,
			file_bytes=file_bytes,
			llm_enhancement=llm_enhancement,
			formula_enhancement=formula_enhancement,
		)
		data = get_preview_service().build_document_handout(
			PreviewUploadHandoutRequest(
				user_id=user_id,
				file_name=file_name,
				parsed_markdown=parsed_document.markdown_text,
				parsed_plain_text=parsed_document.plain_text,
			),
			file_id=parsed_document.file_id,
			file_extension=parsed_document.file_extension,
			parse_cache_hit=parsed_document.cache_hit,
		)
		return PreviewDocumentHandoutResponse(data=data)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4003,
			message=str(exc),
		)
	except TimeoutError as exc:
		return _error_response(
			status_code=status.HTTP_504_GATEWAY_TIMEOUT,
			code=5004,
			message=str(exc),
		)
	except RuntimeError as exc:
		return _error_response(
			status_code=status.HTTP_502_BAD_GATEWAY,
			code=5003,
			message=str(exc),
		)
	except Exception as exc:
		return _error_response(
			status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
			code=5000,
			message=f"Internal error: {exc}",
		)


def _error_response(*, status_code: int, code: int, message: str) -> JSONResponse:
	payload: dict[str, Any] = {
		"code": code,
		"message": message,
		"data": None,
	}
	return JSONResponse(status_code=status_code, content=payload)


def _format_sse_event(event_name: str, payload: dict[str, Any]) -> str:
	return f"event: {event_name}\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


__all__ = ["preview_router", "get_preview_service"]
