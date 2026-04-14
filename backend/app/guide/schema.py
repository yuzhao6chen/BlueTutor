from __future__ import annotations

from typing import Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    code: int = Field(default=0, description="业务状态码，0 表示成功")
    message: str = Field(default="success", description="响应消息")
    data: T | None = Field(default=None, description="响应数据")


# ── 创建会话 ───────────────────────────────────────────────────

class CreateSessionRequest(BaseModel):
    problem_text: str = Field(min_length=1, description="学生提交的原始题目文本")


class CreateSessionData(BaseModel):
    session_id: str = Field(description="新建会话的唯一标识")


class CreateSessionResponse(ApiResponse[CreateSessionData]):
    pass


# ── 执行一轮对话 ───────────────────────────────────────────────

class RunTurnRequest(BaseModel):
    student_input: str = Field(min_length=1, description="学生的输入文本")


class RunTurnData(BaseModel):
    question: str = Field(description="老师的引导问题文本")


class RunTurnResponse(ApiResponse[RunTurnData]):
    pass


__all__ = [
    "ApiResponse",
    "CreateSessionRequest",
    "CreateSessionData",
    "CreateSessionResponse",
    "RunTurnRequest",
    "RunTurnData",
    "RunTurnResponse",
]
