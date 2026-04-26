from __future__ import annotations

from typing import Generic, Literal, TypeVar

from pydantic import BaseModel, Field


T = TypeVar("T")

PreviewSourceType = Literal["pdf_selection", "screenshot_ocr", "manual_input", "quick_topic", "document_upload"]
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


HandoutBlockType = Literal["section_heading", "paragraph", "formula", "thinking_prompt", "note"]


class PreviewHandoutBlockItem(BaseModel):
	id: str = Field(min_length=1, description="讲义区块唯一标识")
	type: HandoutBlockType = Field(description="讲义区块类型")
	title: str = Field(default="", description="区块标题")
	text: str = Field(min_length=1, description="区块正文")
	supporting_text: str = Field(default="", description="补充说明")
	section_title: str = Field(default="", description="所属章节标题")


class PreviewGeneratedHandoutData(BaseModel):
	article_title: str = Field(min_length=1, description="讲义标题")
	article_subtitle: str = Field(default="", description="讲义副标题")
	introduction: str = Field(min_length=1, description="讲义导语")
	blocks: list[PreviewHandoutBlockItem] = Field(default_factory=list, description="讲义正文区块")
	footer_prompt: str = Field(default="", description="讲义底部提示语")


class PreviewGeneratedHandoutResponse(ApiResponse[PreviewGeneratedHandoutData]):
	pass


class PreviewUploadHandoutRequest(BaseModel):
	user_id: str = Field(min_length=1, description="用户标识")
	file_name: str = Field(min_length=1, description="原始文件名")
	parsed_markdown: str = Field(min_length=1, description="文档解析得到的 markdown 文本")
	parsed_plain_text: str = Field(min_length=1, description="文档解析得到的纯文本")


class PreviewDocumentHandoutData(BaseModel):
	file_id: str = Field(min_length=1, description="文档唯一标识")
	file_name: str = Field(min_length=1, description="原始文件名")
	file_extension: str = Field(min_length=1, description="文件扩展名")
	status: str = Field(default="success", description="处理状态")
	summary: str = Field(default="", description="预习摘要")
	knowledge_points: list[KnowledgePointItem] = Field(default_factory=list, description="知识点列表")
	handout: PreviewGeneratedHandoutData = Field(description="结构化讲义内容")
	original_markdown: str = Field(default="", description="原始解析 markdown")
	original_plain_text: str = Field(default="", description="原始解析纯文本")
	cache_hit: bool = Field(default=False, description="是否命中后端缓存")


class PreviewDocumentHandoutResponse(ApiResponse[PreviewDocumentHandoutData]):
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
	"HandoutBlockType",
	"PreviewGeneratedHandoutData",
	"PreviewGeneratedHandoutResponse",
	"PreviewHandoutBlockItem",
	"PreviewUploadHandoutRequest",
	"PreviewDocumentHandoutData",
	"PreviewDocumentHandoutResponse",
	"PreviewChatMessage",
	"PreviewChatRequest",
	"PreviewChatData",
	"PreviewChatResponse",
	"PreviewSourceType",
	"ChatMessageRole",
]
