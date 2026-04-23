from __future__ import annotations

from typing import Any

from fastapi import APIRouter, status
from fastapi.responses import JSONResponse

from ..shared.schema import ApiResponse, OcrRequest, OcrResponse, SharedDispatchResponse
from ..shared.service import SharedService


shared_router = APIRouter(prefix="/api/shared", tags=["shared"])

_shared_service: SharedService | None = None


def get_shared_service() -> SharedService:
    global _shared_service
    if _shared_service is None:
        _shared_service = SharedService()
    return _shared_service


@shared_router.get("/health")
def shared_health() -> ApiResponse[dict[str, str]]:
    return ApiResponse(data={"status": "ok", "module": "shared"})


@shared_router.post("/ocr", response_model=OcrResponse)
def ocr_recognize(request: OcrRequest) -> OcrResponse | JSONResponse:
    try:
        data = get_shared_service().recognize_ocr(request)
        return OcrResponse(data=data)
    except ValueError as exc:
        return _error_response(status.HTTP_400_BAD_REQUEST, 4001, str(exc))
    except RuntimeError as exc:
        return _error_response(status.HTTP_502_BAD_GATEWAY, 5001, str(exc))
    except Exception as exc:
        return _error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, 5000, f"Internal error: {exc}")


@shared_router.post("/ocr/dispatch", response_model=SharedDispatchResponse)
def ocr_dispatch(request: OcrRequest) -> SharedDispatchResponse | JSONResponse:
    try:
        data = get_shared_service().dispatch_ocr_result(request)
        return SharedDispatchResponse(data=data)
    except ValueError as exc:
        return _error_response(status.HTTP_400_BAD_REQUEST, 4002, str(exc))
    except RuntimeError as exc:
        return _error_response(status.HTTP_502_BAD_GATEWAY, 5002, str(exc))
    except Exception as exc:
        return _error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, 5000, f"Internal error: {exc}")


def _error_response(status_code: int, code: int, message: str) -> JSONResponse:
    payload: dict[str, Any] = {
        "code": code,
        "message": message,
        "data": None,
    }
    return JSONResponse(status_code=status_code, content=payload)


__all__ = ["shared_router", "get_shared_service"]
