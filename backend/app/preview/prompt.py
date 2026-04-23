from __future__ import annotations

import json
from textwrap import dedent

from .schema import PreviewChatMessage


PREVIEW_KNOWLEDGE_OUTPUT_SCHEMA = {
	"summary": "字符串，概括这段内容主要在讲什么，不超过80字",
	"knowledge_points": [
		{
			"title": "字符串，知识点名称",
			"description": "字符串，知识点简要说明，不超过60字",
			"confidence": "0到1之间的小数",
		}
	],
}


PREVIEW_CHAT_OUTPUT_SCHEMA = {
	"reply": "字符串，面向中小学生的简洁解释",
	"follow_up_questions": [
		"字符串，适合继续追问的问题1",
		"字符串，适合继续追问的问题2",
	],
}


PREVIEW_KNOWLEDGE_SYSTEM_PROMPT = dedent(
	"""
	你是 BlueTutor 的预习指导助手，服务对象是小学三年级到初中阶段学生。
	你的任务是从教材、讲义、练习册或课堂预习材料中提取当前页面最相关的知识点。

	你的输出必须严格满足以下要求：
	1. 只输出 JSON，不要输出 markdown，不要输出解释性前后缀。
	2. 返回字段只能包含 summary 和 knowledge_points。
	3. knowledge_points 返回 3 到 6 个最相关知识点；如果文本较短，可少于 3 个，但不要编造。
	4. 每个知识点必须包含 title、description、confidence。
	5. confidence 必须是 0 到 1 之间的小数。
	6. 你的任务是“预习知识点提取”，不是做题、判错、错题生成，也不是完整解题。
	7. 如果文本信息不足，返回保守结果，不要虚构超出原文的知识点。

	输出 JSON 结构必须与下面保持一致：
	"""
).strip()


PREVIEW_CHAT_SYSTEM_PROMPT = dedent(
	"""
	你是 BlueTutor 的预习对话助手，服务对象是小学三年级到初中阶段学生。
	你的任务是围绕用户当前选中的教材内容和知识点进行预习型讲解。

	你的回复必须符合以下要求：
	1. 只输出 JSON，不要输出 markdown，不要输出解释性前后缀。
	2. 返回字段只能包含 reply 和 follow_up_questions。
	3. reply 要简洁、友好、启发式，优先解释概念、公式含义、直观理解和简单例子。
	4. 不要把回复写成做题模块，不要直接展开复杂解题步骤，除非用户明确只是在问概念相关的简单例子。
	5. 如果用户问题超出上下文，先基于上下文解释，再提醒当前页重点。
	6. follow_up_questions 返回 2 到 3 个适合继续预习的问题；如果不合适，可以返回空数组。
	7. 不要编造教材中不存在的定义、定理或结论。

	输出 JSON 结构必须与下面保持一致：
	"""
).strip()


def build_knowledge_extraction_prompt(content_text: str, topic_title: str | None = None) -> str:
	content_text = content_text.strip()
	topic_title = (topic_title or "").strip()
	output_schema = json.dumps(PREVIEW_KNOWLEDGE_OUTPUT_SCHEMA, ensure_ascii=False, indent=2)
	topic_section = f"当前专题：\n{topic_title}\n\n" if topic_title else ""
	return dedent(
		f"""
		{PREVIEW_KNOWLEDGE_SYSTEM_PROMPT}
		{output_schema}

		请分析以下预习内容，提取学生当前页面最可能需要理解的知识点。

		{topic_section}待分析文本：
		{content_text}
		"""
	).strip()


def build_preview_chat_prompt(
	text: str,
	context_text: str,
	selected_knowledge_points: list[str],
	topic_title: str | None = None,
	history: list[PreviewChatMessage] | None = None,
) -> str:
	user_question = text.strip()
	context_text = context_text.strip()
	topic_title = (topic_title or "").strip()
	knowledge_points_text = _format_selected_knowledge_points(selected_knowledge_points)
	history_text = _format_history(history or [])
	output_schema = json.dumps(PREVIEW_CHAT_OUTPUT_SCHEMA, ensure_ascii=False, indent=2)
	topic_section = f"当前专题：\n{topic_title}\n\n" if topic_title else ""

	return dedent(
		f"""
		{PREVIEW_CHAT_SYSTEM_PROMPT}
		{output_schema}

		{topic_section}当前页面内容：
		{context_text}

		用户当前选择的知识点：
		{knowledge_points_text}

		历史对话：
		{history_text}

		用户当前问题：
		{user_question}
		"""
	).strip()


def _format_selected_knowledge_points(selected_knowledge_points: list[str]) -> str:
	cleaned_points = [point.strip() for point in selected_knowledge_points if point and point.strip()]
	if not cleaned_points:
		return "未选择明确知识点，请优先结合当前页面内容进行解释。"
	return "\n".join(f"- {point}" for point in cleaned_points)


def _format_history(history: list[PreviewChatMessage]) -> str:
	if not history:
		return "无历史对话"
	return "\n".join(f"{message.role}: {message.content.strip()}" for message in history)


__all__ = [
	"PREVIEW_KNOWLEDGE_OUTPUT_SCHEMA",
	"PREVIEW_CHAT_OUTPUT_SCHEMA",
	"PREVIEW_KNOWLEDGE_SYSTEM_PROMPT",
	"PREVIEW_CHAT_SYSTEM_PROMPT",
	"build_knowledge_extraction_prompt",
	"build_preview_chat_prompt",
]
