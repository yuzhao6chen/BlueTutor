from __future__ import annotations

from typing import Generic, Literal, TypeVar

from pydantic import BaseModel, Field


T = TypeVar("T")

PreviewSourceType = Literal["pdf_selection", "screenshot_ocr", "manual_input", "quick_topic"]
ChatMessageRole = Literal["user", "assistant"]


class ApiResponse(BaseModel, Generic[T]):
	code: int = Field(default=0, description="业务状态码，0 表示成功")
	message: str = Field(default="success", description="响应消息")
	data: T | None = Field(default=None, description="响应数据")


class KnowledgePointItem(BaseModel):
	id: str = Field(description="知识点唯一标识")
	title: str = Field(min_length=1, description="知识点标题")
	description: str = Field(default="", description="知识点简要说明")
	confidence: float = Field(default=0.0, ge=0.0, le=1.0, description="模型置信度")


class PreviewKnowledgeRequest(BaseModel):
	user_id: str = Field(min_length=1, description="用户标识")
	content_text: str = Field(min_length=1, description="待分析的教材或练习文本")
	source_type: PreviewSourceType = Field(
		default="manual_input",
		description="文本来源类型",
	)
	topic_id: str | None = Field(default=None, description="快捷预习专题 ID")
	topic_title: str | None = Field(default=None, description="快捷预习专题标题")
	page_hint: int | None = Field(default=None, ge=1, description="可选页码提示")
	session_id: str | None = Field(default=None, description="预习会话 ID")


class PreviewKnowledgeData(BaseModel):
	knowledge_points: list[KnowledgePointItem] = Field(
		default_factory=list,
		description="识别出的知识点列表",
	)
	summary: str = Field(default="", description="当前文本的预习摘要")


class PreviewKnowledgeResponse(ApiResponse[PreviewKnowledgeData]):
	pass


class PreviewChatMessage(BaseModel):
	role: ChatMessageRole = Field(description="对话角色")
	content: str = Field(min_length=1, description="消息内容")


class PreviewChatRequest(BaseModel):
	user_id: str = Field(min_length=1, description="用户标识")
	text: str = Field(min_length=1, description="用户当前问题")
	context_text: str = Field(min_length=1, description="当前页面或选中文本上下文")
	topic_id: str | None = Field(default=None, description="快捷预习专题 ID")
	topic_title: str | None = Field(default=None, description="快捷预习专题标题")
	selected_knowledge_points: list[str] = Field(
		default_factory=list,
		description="用户已选择的知识点标题列表",
	)
	session_id: str | None = Field(default=None, description="预习会话 ID")
	history: list[PreviewChatMessage] = Field(
		default_factory=list,
		description="可选历史对话",
	)


class PreviewChatData(BaseModel):
	reply: str = Field(min_length=1, description="AI 对话回复")
	follow_up_questions: list[str] = Field(
		default_factory=list,
		description="推荐追问问题",
	)


class PreviewChatResponse(ApiResponse[PreviewChatData]):
	pass


__all__ = [
	"ApiResponse",
	"KnowledgePointItem",
	"PreviewKnowledgeRequest",
	"PreviewKnowledgeData",
	"PreviewKnowledgeResponse",
	"PreviewChatMessage",
	"PreviewChatRequest",
	"PreviewChatData",
	"PreviewChatResponse",
	"PreviewSourceType",
	"ChatMessageRole",
]
