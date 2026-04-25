from __future__ import annotations

from typing import Generic, Literal, TypeVar

from pydantic import BaseModel, Field


T = TypeVar("T")
ThinkingNodeStatus = Literal["correct", "incorrect", "unknown"]
MistakeStatus = Literal["pending", "mastered"]


class ApiResponse(BaseModel, Generic[T]):
	code: int = Field(default=0, description="业务状态码，0 表示成功")
	message: str = Field(default="success", description="响应消息")
	data: T | None = Field(default=None, description="响应数据")


class MistakeProblem(BaseModel):
	raw_problem: str = Field(min_length=1, description="原始题目")
	known_conditions: list[str] = Field(default_factory=list, description="已知条件")
	goal: str = Field(default="", description="题目目标")
	answer: str = Field(default="", description="参考答案")


class MistakeThinkingNode(BaseModel):
	node_id: str = Field(min_length=1, description="思维节点 ID")
	content: str = Field(min_length=1, description="节点内容")
	status: ThinkingNodeStatus = Field(default="unknown", description="节点状态")
	parent_id: str | None = Field(default=None, description="父节点 ID")
	error_history: list[str] = Field(default_factory=list, description="节点错误历史")


class MistakeErrorProfileItem(BaseModel):
	error_type: str = Field(min_length=1, description="错误类型")
	detail: str = Field(default="", description="错误详情")


class MistakeIndependenceEvaluation(BaseModel):
	level: str = Field(default="", description="独立完成度等级")
	detail: str = Field(default="", description="独立完成度说明")


class MistakeReportPayload(BaseModel):
	problem: MistakeProblem = Field(description="讲题报告中的题目信息")
	knowledge_tags: list[str] = Field(default_factory=list, description="知识点标签")
	thinking_chain: list[MistakeThinkingNode] = Field(default_factory=list, description="思维链")
	error_profile: list[MistakeErrorProfileItem] = Field(default_factory=list, description="错因分析")
	independence_evaluation: MistakeIndependenceEvaluation = Field(description="独立完成度")
	solution: str | None = Field(default=None, description="讲题模块生成的题解")


class MistakeReportIngestRequest(BaseModel):
	user_id: str = Field(min_length=1, description="用户标识")
	report: MistakeReportPayload = Field(description="讲题模块产出的错题报告")
	source_session_id: str | None = Field(default=None, description="来源讲题会话 ID")
	report_title: str | None = Field(default=None, description="可选报告标题")


class MistakeReportStatusUpdateRequest(BaseModel):
	status: MistakeStatus = Field(description="新的巩固状态")


class MistakeTimelineItem(BaseModel):
	report_id: str = Field(description="错题报告 ID")
	title: str = Field(description="列表标题")
	status: MistakeStatus = Field(description="巩固状态")
	primary_error_type: str = Field(default="待分析", description="主要错因")
	knowledge_tags: list[str] = Field(default_factory=list, description="知识点标签")
	independence_level: str = Field(default="", description="独立完成度")
	has_solution: bool = Field(default=False, description="是否包含题解")
	solution_preview: str = Field(default="", description="题解摘要")
	created_at: str = Field(description="创建时间")


class MistakeTimelineGroup(BaseModel):
	label: str = Field(description="时间轴分组标签，如今天/昨天/更早")
	items: list[MistakeTimelineItem] = Field(default_factory=list, description="当前分组下的错题项")


class MistakeReportSummary(BaseModel):
	report_id: str = Field(description="错题报告 ID")
	user_id: str = Field(description="用户标识")
	report_title: str = Field(description="报告标题")
	source_session_id: str | None = Field(default=None, description="来源讲题会话 ID")
	status: MistakeStatus = Field(description="巩固状态")
	knowledge_tags: list[str] = Field(default_factory=list, description="知识点标签")
	primary_error_type: str = Field(default="待分析", description="主要错因")
	independence_level: str = Field(default="", description="独立完成度")
	has_solution: bool = Field(default=False, description="是否包含题解")
	solution_preview: str = Field(default="", description="题解摘要")
	created_at: str = Field(description="创建时间")
	problem_preview: str = Field(description="题目摘要")


class MistakeReportData(MistakeReportSummary):
	report: MistakeReportPayload = Field(description="完整错题报告")


class MistakeReportIngestData(BaseModel):
	report_id: str = Field(description="新写入的报告 ID")
	report_title: str = Field(description="报告标题")
	status: MistakeStatus = Field(description="巩固状态")
	primary_error_type: str = Field(default="待分析", description="主要错因")
	knowledge_tags: list[str] = Field(default_factory=list, description="知识点标签")
	has_solution: bool = Field(default=False, description="是否包含题解")
	solution_preview: str = Field(default="", description="题解摘要")
	created_at: str = Field(description="创建时间")


class MistakeReportIngestResponse(ApiResponse[MistakeReportIngestData]):
	pass


class MistakeReportListData(BaseModel):
	reports: list[MistakeReportSummary] = Field(default_factory=list, description="错题报告列表")
	timeline: list[MistakeTimelineItem] = Field(default_factory=list, description="用于前端时间轴展示的数据")
	count: int = Field(default=0, description="报告数量")


class MistakeReportListResponse(ApiResponse[MistakeReportListData]):
	pass


class MistakeReportDetailResponse(ApiResponse[MistakeReportData]):
	pass


class MistakeStatsData(BaseModel):
	total_reports: int = Field(default=0, description="错题总数")
	pending_reports: int = Field(default=0, description="待巩固数量")
	mastered_reports: int = Field(default=0, description="已巩固数量")
	mastered_error_types: int = Field(default=0, description="已突破错因类型数量")
	top_knowledge_tags: list[str] = Field(default_factory=list, description="高频知识点标签")


class MistakeStatsResponse(ApiResponse[MistakeStatsData]):
	pass


class MistakeHomeWeakTagItem(BaseModel):
	tag: str = Field(description="知识点标签")
	count: int = Field(ge=0, description="出现次数")


class MistakeHomeSummaryData(BaseModel):
	today_pending_count: int = Field(default=0, description="今日新增待巩固错题数")
	pending_review_count: int = Field(default=0, description="当前待巩固总数")
	completed_this_week_count: int = Field(default=0, description="本周已巩固数量")
	mastered_error_types_count: int = Field(default=0, description="已突破错因类型数")
	weak_knowledge_tags: list[MistakeHomeWeakTagItem] = Field(default_factory=list, description="高频薄弱知识点")
	recent_timeline: list[MistakeTimelineGroup] = Field(default_factory=list, description="首页展示用最近时间轴")


class MistakeHomeSummaryResponse(ApiResponse[MistakeHomeSummaryData]):
	pass


class MistakeTimelineData(BaseModel):
	groups: list[MistakeTimelineGroup] = Field(default_factory=list, description="按时间分组的时间轴数据")
	count: int = Field(default=0, description="时间轴项数量")


class MistakeTimelineResponse(ApiResponse[MistakeTimelineData]):
	pass


class MistakeLectureSection(BaseModel):
	title: str = Field(description="讲义区块标题")
	content: str = Field(description="讲义区块内容")
	kind: Literal["summary", "knowledge", "thinking", "error", "solution", "suggestion"] = Field(description="区块类型")


class MistakeReviewStep(BaseModel):
	step_no: int = Field(ge=1, description="复盘步骤序号")
	title: str = Field(description="复盘步骤标题")
	content: str = Field(description="复盘步骤内容")
	status: Literal["done", "focus"] = Field(description="步骤状态")


class MistakeLectureData(BaseModel):
	report_id: str = Field(description="错题报告 ID")
	report_title: str = Field(description="报告标题")
	status: MistakeStatus = Field(description="巩固状态")
	problem_text: str = Field(description="完整题目文本")
	answer: str = Field(default="", description="参考答案")
	knowledge_tags: list[str] = Field(default_factory=list, description="知识点标签")
	primary_error_type: str = Field(default="待分析", description="主要错因")
	independence_level: str = Field(default="", description="独立完成度")
	has_solution: bool = Field(default=False, description="是否包含题解")
	solution_preview: str = Field(default="", description="题解摘要")
	lecture_sections: list[MistakeLectureSection] = Field(default_factory=list, description="错题讲义页结构化区块")
	review_steps: list[MistakeReviewStep] = Field(default_factory=list, description="复盘步骤")
	key_takeaways: list[str] = Field(default_factory=list, description="复盘要点")
	created_at: str = Field(description="创建时间")


class MistakeLectureResponse(ApiResponse[MistakeLectureData]):
	pass


class MistakePlanItem(BaseModel):
	report_id: str = Field(description="???? ID")
	title: str = Field(description="?????")
	primary_error_type: str = Field(default="???", description="????")
	knowledge_tags: list[str] = Field(default_factory=list, description="?????")
	has_solution: bool = Field(default=False, description="??????")
	action: Literal["review_lecture", "redo_problem"] = Field(description="????")
	reason: str = Field(description="????")


class MistakeDailyPlanData(BaseModel):
	user_id: str | None = Field(default=None, description="????")
	generated_at: str = Field(description="????")
	today_focus: str = Field(description="??????")
	summary: str = Field(description="????")
	focus_knowledge_tags: list[str] = Field(default_factory=list, description="???????")
	items: list[MistakePlanItem] = Field(default_factory=list, description="???????")
	count: int = Field(default=0, description="?????")


class MistakeDailyPlanResponse(ApiResponse[MistakeDailyPlanData]):
	pass


class MistakeRecommendationGenerateRequest(BaseModel):
	report_id: str = Field(min_length=1, description="?????? ID")
	user_id: str | None = Field(default=None, description="??????")
	recommendation_type: Literal["similar", "variant"] = Field(default="variant", description="?????")


class MistakeAgentTrace(BaseModel):
	agent: str = Field(description="agent ??")
	status: Literal["success", "failed"] = Field(description="????")
	summary: str = Field(default="", description="????")


class MistakeRecommendationOption(BaseModel):
	id: str = Field(description="?? ID")
	text: str = Field(description="????")


class MistakeRecommendationData(BaseModel):
	recommendation_id: str = Field(description="??? ID")
	origin_report_id: str = Field(description="?????? ID")
	recommendation_type: Literal["similar", "variant"] = Field(description="?????")
	title: str = Field(description="????")
	difficulty: Literal["easy", "medium", "hard"] = Field(description="????")
	question: str = Field(description="????")
	options: list[MistakeRecommendationOption] = Field(default_factory=list, description="?????")
	correct_option_id: str = Field(default="", description="???? ID")
	answer: str = Field(default="", description="????")
	explanation: str = Field(default="", description="????")
	knowledge_tags: list[str] = Field(default_factory=list, description="?????")
	why_recommended: str = Field(default="", description="????")
	agent_traces: list[MistakeAgentTrace] = Field(default_factory=list, description="? agent ????")
	generated_at: str = Field(description="????")


class MistakeRecommendationResponse(ApiResponse[MistakeRecommendationData]):
	pass


MistakeRedoStage = Literal["understand_problem", "identify_first_step", "solve", "final_check", "completed"]
MistakeRedoInteractionMode = Literal["single_choice", "free_text"]
MistakeRedoResult = Literal["correct", "partial", "incorrect"]


class MistakeRedoOption(BaseModel):
	id: str = Field(description="?? ID")
	text: str = Field(description="????")


class MistakeRedoTurnRecord(BaseModel):
	turn_no: int = Field(ge=1, description="??")
	prompt: str = Field(description="????")
	interaction_mode: MistakeRedoInteractionMode = Field(description="????")
	student_answer: str = Field(default="", description="????")
	result: MistakeRedoResult = Field(description="????")
	feedback: str = Field(default="", description="????")
	hint: str = Field(default="", description="????")
	stage: MistakeRedoStage = Field(description="????")


class MistakeRedoSessionRequest(BaseModel):
	report_id: str = Field(min_length=1, description="?????? ID")
	user_id: str | None = Field(default=None, description="??????")


class MistakeRedoSessionTurnRequest(BaseModel):
	answer: str = Field(min_length=1, description="??????")


class MistakeRedoSessionData(BaseModel):
	session_id: str = Field(description="会话 ID")
	report_id: str = Field(description="关联错题 ID")
	user_id: str | None = Field(default=None, description="用户标识")
	report_title: str = Field(description="报告标题")
	problem_text: str = Field(description="题目文本")
	stage: MistakeRedoStage = Field(description="当前阶段")
	turn_count: int = Field(default=0, description="已回答轮数")
	current_prompt: str = Field(description="当前引导问题")
	interaction_mode: MistakeRedoInteractionMode = Field(description="交互模式")
	options: list[MistakeRedoOption] = Field(default_factory=list, description="选项列表")
	hint: str = Field(default="", description="当前提示")
	hint_level: int = Field(default=1, description="提示等级：1=轻微引导 2=中度提示 3=直接提示")
	max_hint_level: int = Field(default=3, description="最大提示等级")
	last_feedback: str = Field(default="", description="上一轮反馈")
	is_completed: bool = Field(default=False, description="是否已完成")
	can_clear_mistake: bool = Field(default=False, description="是否可标记错题已巩固")
	consecutive_correct: int = Field(default=0, description="连续答对轮数")
	required_consecutive_correct: int = Field(default=1, description="完成所需连续答对轮数")
	session_type: Literal["redo_original", "redo_recommendation"] = Field(default="redo_original", description="会话类型")
	recommendation_id: str | None = Field(default=None, description="关联推荐题 ID")
	agent_traces: list[MistakeAgentTrace] = Field(default_factory=list, description="本轮 agent 执行记录")
	history: list[MistakeRedoTurnRecord] = Field(default_factory=list, description="历史作答记录")
	created_at: str = Field(description="创建时间")
	updated_at: str = Field(description="更新时间")


class MistakeRedoSessionResponse(ApiResponse[MistakeRedoSessionData]):
	pass


class MistakeRecommendationStartRedoRequest(BaseModel):
	user_id: str | None = Field(default=None, description="用户标识")


class MistakeRecommendationSubmitRequest(BaseModel):
	answer: str = Field(min_length=1, description="学生作答")
	user_id: str | None = Field(default=None, description="用户标识")


class MistakeRecommendationSubmitData(BaseModel):
	recommendation_id: str = Field(description="推荐题 ID")
	is_correct: bool = Field(description="是否答对")
	selected_option_id: str = Field(default="", description="所选选项 ID")
	correct_option_id: str = Field(default="", description="正确选项 ID")
	feedback: str = Field(default="", description="作答反馈")
	should_create_mistake: bool = Field(default=False, description="是否应回写新错题")


class MistakeRecommendationSubmitResponse(ApiResponse[MistakeRecommendationSubmitData]):
	pass


class MistakeUserProfileWeakPoint(BaseModel):
	knowledge_tag: str = Field(description="薄弱知识点标签")
	error_types: list[str] = Field(default_factory=list, description="该知识点下的错误类型")
	frequency: int = Field(default=0, description="出现次数")
	latest_report_ids: list[str] = Field(default_factory=list, description="最近关联的报告 ID")


class MistakeUserProfileData(BaseModel):
	user_id: str = Field(description="用户标识")
	total_reports: int = Field(default=0, description="错题报告总数")
	pending_count: int = Field(default=0, description="待巩固数量")
	mastered_count: int = Field(default=0, description="已巩固数量")
	weak_knowledge_tags: list[str] = Field(default_factory=list, description="薄弱知识点列表")
	weak_points: list[MistakeUserProfileWeakPoint] = Field(default_factory=list, description="薄弱点详情")
	error_type_distribution: dict[str, int] = Field(default_factory=dict, description="错误类型分布")
	independence_summary: str = Field(default="", description="独立完成度总评")
	learning_style_hints: list[str] = Field(default_factory=list, description="学习风格建议")
	updated_at: str = Field(default="", description="最后更新时间")


class MistakeUserProfileResponse(ApiResponse[MistakeUserProfileData]):
	pass


class MistakeDialogueMessage(BaseModel):
	role: Literal["user", "assistant"] = Field(description="消息角色")
	content: str = Field(description="消息内容")
	timestamp: str = Field(default="", description="消息时间戳")


MistakeDialogueMasteryVerdict = Literal["mastered", "not_mastered", "in_progress"]


class MistakeDialogueSessionRequest(BaseModel):
	report_id: str = Field(min_length=1, description="错题报告 ID")
	user_id: str | None = Field(default=None, description="用户标识")


class MistakeDialogueSessionTurnRequest(BaseModel):
	message: str = Field(min_length=1, description="用户讲题内容")


class MistakeDialogueSessionData(BaseModel):
	session_id: str = Field(description="会话 ID")
	report_id: str = Field(description="关联错题报告 ID")
	user_id: str | None = Field(default=None, description="用户标识")
	report_title: str = Field(description="报告标题")
	problem_text: str = Field(description="题目文本")
	is_completed: bool = Field(default=False, description="对话是否结束")
	mastery_verdict: MistakeDialogueMasteryVerdict = Field(default="in_progress", description="掌握判定结果")
	mastery_detail: str = Field(default="", description="掌握判定详情")
	messages: list[MistakeDialogueMessage] = Field(default_factory=list, description="对话消息列表")
	similar_question: MistakeRecommendationData | None = Field(default=None, description="关联的相似题")
	agent_traces: list[MistakeAgentTrace] = Field(default_factory=list, description="agent 执行记录")
	created_at: str = Field(description="创建时间")
	updated_at: str = Field(description="更新时间")


class MistakeDialogueSessionResponse(ApiResponse[MistakeDialogueSessionData]):
	pass


__all__ = [
	"ApiResponse",
	"MistakeProblem",
	"MistakeThinkingNode",
	"MistakeErrorProfileItem",
	"MistakeIndependenceEvaluation",
	"MistakeReportPayload",
	"MistakeReportIngestRequest",
	"MistakeReportStatusUpdateRequest",
	"MistakeTimelineItem",
	"MistakeTimelineGroup",
	"MistakeReportSummary",
	"MistakeReportData",
	"MistakeReportIngestData",
	"MistakeReportIngestResponse",
	"MistakeReportListData",
	"MistakeReportListResponse",
	"MistakeReportDetailResponse",
	"MistakeStatsData",
	"MistakeStatsResponse",
	"MistakeHomeWeakTagItem",
	"MistakeHomeSummaryData",
	"MistakeHomeSummaryResponse",
	"MistakeTimelineData",
	"MistakeTimelineResponse",
	"MistakeLectureSection",
	"MistakeReviewStep",
	"MistakeLectureData",
	"MistakeLectureResponse",
	"MistakePlanItem",
	"MistakeDailyPlanData",
	"MistakeDailyPlanResponse",
	"MistakeRecommendationGenerateRequest",
	"MistakeAgentTrace",
	"MistakeRecommendationOption",
	"MistakeRecommendationData",
	"MistakeRecommendationResponse",
	"MistakeRecommendationStartRedoRequest",
	"MistakeRecommendationSubmitRequest",
	"MistakeRecommendationSubmitData",
	"MistakeRecommendationSubmitResponse",
	"MistakeRedoOption",
	"MistakeRedoTurnRecord",
	"MistakeRedoSessionRequest",
	"MistakeRedoSessionTurnRequest",
	"MistakeRedoSessionData",
	"MistakeRedoSessionResponse",
	"MistakeRedoStage",
	"MistakeRedoInteractionMode",
	"MistakeRedoResult",
	"MistakeStatus",
	"MistakeUserProfileWeakPoint",
	"MistakeUserProfileData",
	"MistakeUserProfileResponse",
	"MistakeDialogueMessage",
	"MistakeDialogueMasteryVerdict",
	"MistakeDialogueSessionRequest",
	"MistakeDialogueSessionTurnRequest",
	"MistakeDialogueSessionData",
	"MistakeDialogueSessionResponse",
]
