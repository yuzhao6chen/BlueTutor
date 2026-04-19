from __future__ import annotations

from typing import Any, Generic, Literal, TypeVar

from pydantic import BaseModel, Field


T = TypeVar("T")

ImageType = Literal["photo", "screenshot"]
GoalType = Literal["做题模块", "预习模块", "错题本模块", "guide", "preview", "mistakes"]
TargetModule = Literal["guide", "preview", "mistakes"]
DispatchStatus = Literal["forwarded", "handoff_required"]


class ApiResponse(BaseModel, Generic[T]):
    code: int = Field(default=0, description="Business status code. 0 means success.")
    message: str = Field(default="success", description="Response message.")
    data: T | None = Field(default=None, description="Response payload.")


class OcrRequest(BaseModel):
    user_id: str = Field(min_length=1, description="Caller user identifier.")
    image_type: ImageType = Field(default="photo", description="Uploaded image type.")
    image_base64: str = Field(min_length=1, description="Base64-encoded image content.")
    goals: GoalType = Field(default="做题模块", description="Requested downstream module.")


class OcrData(BaseModel):
    image_id: str = Field(description="Unique OCR image identifier.")
    question_text: str = Field(description="Recognized question text extracted from the image.")


class OcrResponse(ApiResponse[OcrData]):
    pass


class SharedDispatchData(BaseModel):
    user_id: str = Field(description="Caller user identifier.")
    image_id: str = Field(description="Unique OCR image identifier.")
    question_text: str = Field(description="Recognized question text extracted from the image.")
    goal: GoalType = Field(description="Original dispatch goal sent by the caller.")
    target_module: TargetModule = Field(description="Normalized downstream target module.")
    dispatch_status: DispatchStatus = Field(description="Whether the request was forwarded or needs manual handoff.")
    downstream_endpoint: str | None = Field(
        default=None,
        description="Backend endpoint used for forwarding when available.",
    )
    downstream_request: dict[str, Any] = Field(
        default_factory=dict,
        description="Payload sent, or ready to be sent, to the downstream module.",
    )
    downstream_response: dict[str, Any] | None = Field(
        default=None,
        description="Response returned by the downstream module when forwarding succeeds.",
    )
    notes: str | None = Field(default=None, description="Extra integration notes for the caller.")


class SharedDispatchResponse(ApiResponse[SharedDispatchData]):
    pass


__all__ = [
    "ApiResponse",
    "DispatchStatus",
    "GoalType",
    "ImageType",
    "OcrData",
    "OcrRequest",
    "OcrResponse",
    "SharedDispatchData",
    "SharedDispatchResponse",
    "TargetModule",
]
