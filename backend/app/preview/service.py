from __future__ import annotations

import hashlib
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
DEFAULT_CACHE_DIR = Path(__file__).resolve().parents[2] / "data" / "preview_cache"


class PreviewService:
	def __init__(
		self,
		agent: PreviewAgent | None = None,
		log_dir: Path | None = None,
		cache_dir: Path | None = None,
	) -> None:
		self.agent = agent or PreviewAgent()
		self.log_dir = log_dir or DEFAULT_LOG_DIR
		self.cache_dir = cache_dir or DEFAULT_CACHE_DIR

	def get_knowledge_points(self, request: PreviewKnowledgeRequest) -> PreviewKnowledgeData:
		content_text = self._validate_text(request.content_text, field_name="content_text")
		cache_key = self._build_knowledge_cache_key(request=request, content_text=content_text)
		data = self._load_cached_knowledge_data(cache_key)
		cache_hit = data is not None

		if data is None:
			raw_result = self.agent.extract_knowledge_points(
				content_text,
				topic_title=request.topic_title,
			)
			data = self._build_knowledge_data(raw_result)
			self._save_cached_knowledge_data(cache_key, data)

		self._log_preview_interaction(
			file_name="knowledge_points.jsonl",
			payload={
				"cache_hit": cache_hit,
				"user_id": request.user_id,
				"session_id": request.session_id,
				"page_hint": request.page_hint,
				"source_type": request.source_type,
				"topic_id": request.topic_id,
				"topic_title": request.topic_title,
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
			topic_title=request.topic_title,
			history=request.history,
		)
		data = self._build_chat_data(raw_result)

		self._log_preview_interaction(
			file_name="chat.jsonl",
			payload={
				"user_id": request.user_id,
				"session_id": request.session_id,
				"topic_id": request.topic_id,
				"topic_title": request.topic_title,
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

	def _build_knowledge_cache_key(self, *, request: PreviewKnowledgeRequest, content_text: str) -> str:
		cache_payload = {
			"model_name": getattr(self.agent, "model_name", ""),
			"source_type": request.source_type,
			"topic_id": request.topic_id,
			"topic_title": request.topic_title,
			"content_text": content_text,
		}
		serialized_payload = json.dumps(cache_payload, ensure_ascii=False, sort_keys=True)
		return hashlib.sha256(serialized_payload.encode("utf-8")).hexdigest()

	def _load_cached_knowledge_data(self, cache_key: str) -> PreviewKnowledgeData | None:
		cache_path = self.cache_dir / f"{cache_key}.json"
		if not cache_path.exists():
			return None

		try:
			payload = json.loads(cache_path.read_text(encoding="utf-8"))
			return PreviewKnowledgeData.model_validate(payload)
		except Exception:
			return None

	def _save_cached_knowledge_data(self, cache_key: str, data: PreviewKnowledgeData) -> None:
		self.cache_dir.mkdir(parents=True, exist_ok=True)
		cache_path = self.cache_dir / f"{cache_key}.json"
		try:
			cache_path.write_text(data.model_dump_json(indent=2), encoding="utf-8")
		except OSError:
			return

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
