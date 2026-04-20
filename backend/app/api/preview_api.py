from __future__ import annotations

from typing import Any

from fastapi import APIRouter, status
from fastapi.responses import JSONResponse

from ..preview.schema import (
	ApiResponse,
	PreviewChatRequest,
	PreviewChatResponse,
	PreviewKnowledgeRequest,
	PreviewKnowledgeResponse,
)
from ..preview.service import PreviewService


preview_router = APIRouter(prefix="/api/preview", tags=["preview"])
_preview_service: PreviewService | None = None


def get_preview_service() -> PreviewService:
	global _preview_service
	if _preview_service is None:
		_preview_service = PreviewService()
	return _preview_service


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


def _error_response(*, status_code: int, code: int, message: str) -> JSONResponse:
	payload: dict[str, Any] = {
		"code": code,
		"message": message,
		"data": None,
	}
	return JSONResponse(status_code=status_code, content=payload)


__all__ = ["preview_router", "get_preview_service"]
