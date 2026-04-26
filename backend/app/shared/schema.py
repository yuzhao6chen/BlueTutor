from __future__ import annotations

from typing import Any, Generic, Literal, TypeVar

from pydantic import BaseModel, Field


T = TypeVar("T")

ImageType = Literal["photo", "screenshot"]
GoalType = Literal["做题模块", "预习模块", "错题本模块", "guide", "preview", "mistakes"]
TargetModule = Literal["guide", "preview", "mistakes"]
DispatchStatus = Literal["forwarded", "handoff_required"]
DocumentParseStatus = Literal["success"]


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


class DocumentParseData(BaseModel):
    user_id: str = Field(description="Caller user identifier.")
    file_id: str = Field(description="Unique parsed file identifier.")
    file_name: str = Field(description="Original uploaded file name.")
    file_extension: str = Field(description="Normalized file extension without dot.")
    file_size_bytes: int = Field(description="Uploaded file size in bytes.")
    docmind_job_id: str = Field(description="Aliyun DocMind async job identifier.")
    status: DocumentParseStatus = Field(description="Document parse status.")
    markdown_text: str = Field(description="Markdown content extracted from the document.")
    plain_text: str = Field(description="Plain-text version extracted from markdown output.")
    cache_hit: bool = Field(default=False, description="Whether the result was returned from local cache.")
    page_count_estimate: int | None = Field(default=None, description="Estimated page count reported by DocMind.")
    paragraph_count: int | None = Field(default=None, description="Paragraph count reported by DocMind.")
    token_count: int | None = Field(default=None, description="Token/character count reported by DocMind.")
    image_count: int | None = Field(default=None, description="Image count reported by DocMind.")
    table_count: int | None = Field(default=None, description="Table count reported by DocMind.")
    llm_enhancement: bool = Field(default=True, description="Whether DocMind LLM enhancement was enabled.")
    formula_enhancement: bool = Field(default=True, description="Whether DocMind formula enhancement was enabled.")
    raw_layout_count: int = Field(default=0, description="Number of layout blocks collected from DocMind.")


class DocumentParseResponse(ApiResponse[DocumentParseData]):
    pass


class DocumentDispatchData(BaseModel):
    user_id: str = Field(description="Caller user identifier.")
    goal: GoalType = Field(description="Original dispatch goal sent by the caller.")
    target_module: TargetModule = Field(description="Normalized downstream target module.")
    dispatch_status: DispatchStatus = Field(description="Whether the request was forwarded or needs manual handoff.")
    parsed_document: DocumentParseData = Field(description="Parsed document payload.")
    downstream_endpoint: str | None = Field(default=None, description="Downstream endpoint invoked after parsing.")
    downstream_request: dict[str, Any] = Field(default_factory=dict, description="Payload sent downstream after parsing.")
    downstream_response: dict[str, Any] | None = Field(default=None, description="Response returned by the downstream module.")
    notes: str | None = Field(default=None, description="Extra integration notes for the caller.")


class DocumentDispatchResponse(ApiResponse[DocumentDispatchData]):
    pass


__all__ = [
    "ApiResponse",
    "DispatchStatus",
    "DocumentDispatchData",
    "DocumentDispatchResponse",
    "DocumentParseData",
    "DocumentParseResponse",
    "DocumentParseStatus",
    "GoalType",
    "ImageType",
    "OcrData",
    "OcrRequest",
    "OcrResponse",
    "SharedDispatchData",
    "SharedDispatchResponse",
    "TargetModule",
]
