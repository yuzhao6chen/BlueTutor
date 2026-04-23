from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterator

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

	def stream_preview_chat(self, request: PreviewChatRequest) -> Iterator[tuple[str, dict[str, Any]]]:
		text = self._validate_text(request.text, field_name="text")
		context_text = self._validate_text(request.context_text, field_name="context_text")
		selected_knowledge_points = [item.strip() for item in request.selected_knowledge_points if item.strip()]

		raw_content = ""
		emitted_reply = ""

		for chunk in self.agent.stream_chat(
			text=text,
			context_text=context_text,
			selected_knowledge_points=selected_knowledge_points,
			topic_title=request.topic_title,
			history=request.history,
		):
			raw_content += chunk
			current_reply = _extract_partial_reply_text(raw_content)
			if len(current_reply) > len(emitted_reply):
				next_token = current_reply[len(emitted_reply):]
				emitted_reply = current_reply
				if next_token:
					yield ("token", {"token": next_token})

		try:
			raw_result = self.agent.parse_json_response(raw_content)
			data = self._build_chat_data(raw_result)
		except Exception as exc:
			if not emitted_reply:
				raise RuntimeError("LLM stream response missing reply field") from exc
			data = PreviewChatData(reply=emitted_reply, follow_up_questions=[])

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

		yield (
			"done",
			{
				"reply": data.reply,
				"follow_up_questions": data.follow_up_questions,
			},
		)

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


def _extract_partial_reply_text(content: str) -> str:
	key_index = content.find('"reply"')
	if key_index < 0:
		return ""

	index = key_index + len('"reply"')
	content_length = len(content)

	while index < content_length and content[index].isspace():
		index += 1
	if index >= content_length or content[index] != ":":
		return ""

	index += 1
	while index < content_length and content[index].isspace():
		index += 1
	if index >= content_length or content[index] != '"':
		return ""

	index += 1
	characters: list[str] = []
	while index < content_length:
		current = content[index]
		if current == '"':
			return "".join(characters)

		if current == "\\":
			index += 1
			if index >= content_length:
				break

			escaped = content[index]
			if escaped in {'"', "\\", "/"}:
				characters.append(escaped)
			elif escaped == "b":
				characters.append("\b")
			elif escaped == "f":
				characters.append("\f")
			elif escaped == "n":
				characters.append("\n")
			elif escaped == "r":
				characters.append("\r")
			elif escaped == "t":
				characters.append("\t")
			elif escaped == "u":
				if index + 4 >= content_length:
					break
				hex_digits = content[index + 1:index + 5]
				if len(hex_digits) < 4 or any(character not in "0123456789abcdefABCDEF" for character in hex_digits):
					break
				characters.append(chr(int(hex_digits, 16)))
				index += 4
			else:
				characters.append(escaped)
		else:
			characters.append(current)

		index += 1

	return "".join(characters)


__all__ = ["PreviewService"]
