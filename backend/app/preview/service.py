from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .agent import PreviewAgent
from .schema import (
	KnowledgePointItem,
	PreviewChatData,
	PreviewChatRequest,
	PreviewKnowledgeData,
	PreviewKnowledgeRequest,
)


DEFAULT_LOG_DIR = Path(__file__).resolve().parents[2] / "data" / "preview_logs"


class PreviewService:
	def __init__(
		self,
		agent: PreviewAgent | None = None,
		log_dir: Path | None = None,
	) -> None:
		self.agent = agent or PreviewAgent()
		self.log_dir = log_dir or DEFAULT_LOG_DIR

	def get_knowledge_points(self, request: PreviewKnowledgeRequest) -> PreviewKnowledgeData:
		content_text = self._validate_text(request.content_text, field_name="content_text")
		raw_result = self.agent.extract_knowledge_points(content_text)
		data = self._build_knowledge_data(raw_result)

		self._log_preview_interaction(
			file_name="knowledge_points.jsonl",
			payload={
				"user_id": request.user_id,
				"session_id": request.session_id,
				"page_hint": request.page_hint,
				"source_type": request.source_type,
				"content_text": content_text,
				"summary": data.summary,
				"knowledge_points": [item.model_dump() for item in data.knowledge_points],
			},
		)
		return data

	def preview_chat(self, request: PreviewChatRequest) -> PreviewChatData:
		text = self._validate_text(request.text, field_name="text")
		context_text = self._validate_text(request.context_text, field_name="context_text")
		selected_knowledge_points = [item.strip() for item in request.selected_knowledge_points if item.strip()]

		raw_result = self.agent.chat(
			text=text,
			context_text=context_text,
			selected_knowledge_points=selected_knowledge_points,
			history=request.history,
		)
		data = self._build_chat_data(raw_result)

		self._log_preview_interaction(
			file_name="chat.jsonl",
			payload={
				"user_id": request.user_id,
				"session_id": request.session_id,
				"context_text": context_text,
				"selected_knowledge_points": selected_knowledge_points,
				"question": text,
				"reply": data.reply,
				"follow_up_questions": data.follow_up_questions,
				"history": [message.model_dump() for message in request.history],
			},
		)
		return data

	def _build_knowledge_data(self, raw_result: dict[str, Any]) -> PreviewKnowledgeData:
		summary = self._coerce_string(raw_result.get("summary"), default="")
		knowledge_points = self._build_knowledge_points(raw_result.get("knowledge_points"))
		return PreviewKnowledgeData(knowledge_points=knowledge_points, summary=summary)

	def _build_knowledge_points(self, raw_items: Any) -> list[KnowledgePointItem]:
		if not isinstance(raw_items, list):
			return []

		knowledge_points: list[KnowledgePointItem] = []
		for index, raw_item in enumerate(raw_items, start=1):
			if not isinstance(raw_item, dict):
				continue

			title = self._coerce_string(raw_item.get("title"), default="")
			if not title:
				continue

			knowledge_points.append(
				KnowledgePointItem(
					id=self._build_knowledge_point_id(index),
					title=title,
					description=self._coerce_string(raw_item.get("description"), default=""),
					confidence=self._coerce_confidence(raw_item.get("confidence")),
				)
			)

		return knowledge_points

	def _build_chat_data(self, raw_result: dict[str, Any]) -> PreviewChatData:
		reply = self._coerce_string(raw_result.get("reply"), default="")
		if not reply:
			raise RuntimeError("LLM response missing reply field")

		follow_up_questions = raw_result.get("follow_up_questions", [])
		if not isinstance(follow_up_questions, list):
			follow_up_questions = []

		cleaned_questions = [
			str(question).strip()
			for question in follow_up_questions
			if str(question).strip()
		]

		return PreviewChatData(reply=reply, follow_up_questions=cleaned_questions[:3])

	def _build_knowledge_point_id(self, index: int) -> str:
		return f"kp_{index:03d}"

	def _log_preview_interaction(self, file_name: str, payload: dict[str, Any]) -> None:
		self.log_dir.mkdir(parents=True, exist_ok=True)
		log_path = self.log_dir / file_name
		payload_with_timestamp = {
			"timestamp": datetime.now(timezone.utc).isoformat(),
			**payload,
		}
		with log_path.open("a", encoding="utf-8") as log_file:
			log_file.write(json.dumps(payload_with_timestamp, ensure_ascii=False) + "\n")

	def _validate_text(self, value: str, *, field_name: str) -> str:
		cleaned_value = value.strip()
		if not cleaned_value:
			raise ValueError(f"{field_name} is empty")
		if len(cleaned_value) > 8000:
			raise ValueError(f"{field_name} is too long")
		return cleaned_value

	def _coerce_string(self, value: Any, *, default: str) -> str:
		if value is None:
			return default
		cleaned_value = str(value).strip()
		return cleaned_value or default

	def _coerce_confidence(self, value: Any) -> float:
		try:
			confidence = float(value)
		except (TypeError, ValueError):
			return 0.0
		return min(max(confidence, 0.0), 1.0)


__all__ = ["PreviewService"]
