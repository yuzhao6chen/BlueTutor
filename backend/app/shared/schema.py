from __future__ import annotations
from typing import Generic, Literal, TypeVar
from pydantic import BaseModel, Field

T = TypeVar("T")

ImageType = Literal["photo", "screenshot"]
GoalType = Literal["做题模块", "预习模块"]


class ApiResponse(BaseModel, Generic[T]):
    code: int = Field(default=0, description="业务状态码，0 表示成功")
    message: str = Field(default="success", description="响应消息")
    data: T | None = Field(default=None, description="响应数据")


class OcrRequest(BaseModel):
    user_id: str = Field(min_length=1, description="用户标识")
    image_type: ImageType = Field(default="photo", description="图片类型：拍照或截图")
    image_base64: str = Field(min_length=1, description="base64编码的图片，可含或不含data:前缀")
    goals: GoalType = Field(default="做题模块", description="调用来源模块")


class OcrData(BaseModel):
    image_id: str = Field(description="本次识别的唯一ID")
    question_text: str = Field(description="从图片中识别出的题目文字")


class OcrResponse(ApiResponse[OcrData]):
    pass


__all__ = [
    "ApiResponse",
    "OcrRequest",
    "OcrData",
    "OcrResponse",
    "ImageType",
    "GoalType",
]
