from __future__ import annotations

from typing import Any

from fastapi import APIRouter, status
from fastapi.responses import JSONResponse

from ..shared.OCR import OCRAgent
from ..shared.schema import ApiResponse, OcrData, OcrRequest, OcrResponse

shared_router = APIRouter(prefix="/api/shared", tags=["shared"])

_ocr_agent: OCRAgent | None = None


def get_ocr_agent() -> OCRAgent:
    global _ocr_agent
    if _ocr_agent is None:
        _ocr_agent = OCRAgent()
    return _ocr_agent


@shared_router.get("/health")
def shared_health() -> ApiResponse[dict[str, str]]:
    return ApiResponse(data={"status": "ok", "module": "shared"})


@shared_router.post("/ocr", response_model=OcrResponse)
def ocr_recognize(request: OcrRequest) -> OcrResponse | JSONResponse:
    """
    接收 Android 端发来的图片（base64），调用视觉大模型识别题目文字。
    返回结构化题目文本，供后续 guide 或 preview 模块使用。
    """
    try:
        result = get_ocr_agent().recognize(
            image_base64=request.image_base64,
            image_type=request.image_type,
        )
        return OcrResponse(
            data=OcrData(
                image_id=result["image_id"],
                question_text=result["question_text"],
            )
        )
    except ValueError as exc:
        return _error_response(status.HTTP_400_BAD_REQUEST, 4001, str(exc))
    except RuntimeError as exc:
        return _error_response(status.HTTP_502_BAD_GATEWAY, 5001, str(exc))
    except Exception as exc:
        return _error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, 5000, f"内部错误: {exc}")


def _error_response(status_code: int, code: int, message: str) -> JSONResponse:
    payload: dict[str, Any] = {
        "code": code,
        "message": message,
        "data": None,
    }
    return JSONResponse(status_code=status_code, content=payload)


__all__ = ["shared_router", "get_ocr_agent"]
