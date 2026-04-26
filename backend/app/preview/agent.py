from __future__ import annotations

import json
import os
import re
import socket
from pathlib import Path
from typing import Any, Iterator
from urllib.error import HTTPError, URLError
from urllib.request import ProxyHandler, Request, build_opener

from .prompt import build_handout_generation_prompt, build_knowledge_extraction_prompt, build_preview_chat_prompt
from .schema import PreviewChatMessage
from ..shared.openai_compat import build_chat_completions_url, get_first_env


DEFAULT_ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
DEFAULT_BASE_URL = "https://apiport.cc/v1"
DEFAULT_MODEL_NAME = "qwen3.6-flash"
_DIRECT_HTTP_OPENER = build_opener(ProxyHandler({}))


class PreviewAgent:
	def __init__(
		self,
		*,
		base_url: str | None = None,
		api_key: str | None = None,
		model_name: str | None = None,
		timeout_seconds: int | None = None,
		max_retries: int | None = None,
		env_path: Path | None = None,
	) -> None:
		self.env_path = env_path or DEFAULT_ENV_PATH
		_load_env_file(self.env_path)

		configured_base_url = base_url or get_first_env("OPENAI_BASE_URL", "LLM_BASE_URL") or DEFAULT_BASE_URL
		self.base_url = build_chat_completions_url(configured_base_url)
		self.api_key = api_key or get_first_env("OPENAI_API_KEY", "LLM_API_KEY")
		self.model_name = model_name or get_first_env("OPENAI_MODEL", "LLM_MODEL_NAME") or DEFAULT_MODEL_NAME
		self.timeout_seconds = timeout_seconds or int(os.getenv("LLM_TIMEOUT_SECONDS", "60"))
		self.max_retries = max_retries or int(os.getenv("LLM_MAX_RETRIES", "2"))

		if not self.api_key:
			raise ValueError("LLM_API_KEY is not configured")

	def extract_knowledge_points(self, content_text: str, topic_title: str | None = None) -> dict[str, Any]:
		prompt = build_knowledge_extraction_prompt(content_text, topic_title=topic_title)
		return self._call_llm_json(prompt)

	def chat(
		self,
		text: str,
		context_text: str,
		selected_knowledge_points: list[str],
		topic_title: str | None = None,
		history: list[PreviewChatMessage] | None = None,
	) -> dict[str, Any]:
		prompt = build_preview_chat_prompt(
			text=text,
			context_text=context_text,
			selected_knowledge_points=selected_knowledge_points,
			topic_title=topic_title,
			history=history,
		)
		return self._call_llm_json(prompt)

	def generate_handout(
		self,
		*,
		parsed_markdown: str,
		parsed_plain_text: str,
		file_name: str,
	) -> dict[str, Any]:
		prompt = build_handout_generation_prompt(
			parsed_markdown=parsed_markdown,
			parsed_plain_text=parsed_plain_text,
			file_name=file_name,
		)
		return self._call_llm_json(prompt)

	def stream_chat(
		self,
		text: str,
		context_text: str,
		selected_knowledge_points: list[str],
		topic_title: str | None = None,
		history: list[PreviewChatMessage] | None = None,
	) -> Iterator[str]:
		prompt = build_preview_chat_prompt(
			text=text,
			context_text=context_text,
			selected_knowledge_points=selected_knowledge_points,
			topic_title=topic_title,
			history=history,
		)
		return self._stream_chat_completion(prompt)

	def parse_json_response(self, raw_content: str) -> dict[str, Any]:
		return self._parse_json_response(raw_content)

	def _call_llm_json(self, prompt: str) -> dict[str, Any]:
		last_error: Exception | None = None
		retry_prompt = prompt

		for _attempt in range(self.max_retries + 1):
			raw_content = self._send_chat_completion(retry_prompt)
			try:
				parsed = self._parse_json_response(raw_content)
				if not isinstance(parsed, dict):
					raise ValueError("LLM response JSON root must be an object")
				return parsed
			except ValueError as exc:
				last_error = exc
				retry_prompt = self._build_retry_prompt(prompt)

		raise RuntimeError("LLM response was not valid JSON after retries") from last_error

	def _send_chat_completion(self, prompt: str) -> str:
		request = self._build_request(prompt, stream=False)

		with self._open_request(request) as response:
			body = response.read().decode("utf-8")

		response_json = json.loads(body)
		try:
			return response_json["choices"][0]["message"]["content"]
		except (KeyError, IndexError, TypeError) as exc:
			raise RuntimeError(f"Unexpected LLM response structure: {body}") from exc

	def _stream_chat_completion(self, prompt: str) -> Iterator[str]:
		request = self._build_request(prompt, stream=True)
		response = self._open_request(request)

		def generate() -> Iterator[str]:
			with response:
				for event_data in _iter_sse_event_data(response):
					if event_data == "[DONE]":
						break

					try:
						chunk_json = json.loads(event_data)
					except json.JSONDecodeError as exc:
						raise RuntimeError(f"Unexpected LLM stream payload: {event_data}") from exc

					content = _extract_stream_delta_content(chunk_json)
					if content:
						yield content

		return generate()

	def _build_request(self, prompt: str, *, stream: bool) -> Request:
		payload = {
			"model": self.model_name,
			"messages": [
				{
					"role": "user",
					"content": prompt,
				}
			],
			"temperature": 0.2,
			"stream": stream,
		}
		return Request(
			url=self.base_url,
			data=json.dumps(payload).encode("utf-8"),
			headers={
				"Authorization": f"Bearer {self.api_key}",
				"Content-Type": "application/json",
				"Accept": "text/event-stream" if stream else "application/json",
			},
			method="POST",
		)

	def _open_request(self, request: Request):
		try:
			return _DIRECT_HTTP_OPENER.open(request, timeout=self.timeout_seconds)
		except HTTPError as exc:
			error_body = exc.read().decode("utf-8", errors="ignore")
			raise RuntimeError(f"LLM HTTP error {exc.code}: {error_body}") from exc
		except (TimeoutError, socket.timeout) as exc:
			raise RuntimeError(f"LLM request timed out after {self.timeout_seconds}s") from exc
		except URLError as exc:
			raise RuntimeError(f"LLM connection failed: {exc.reason}") from exc

	def _parse_json_response(self, raw_content: str) -> dict[str, Any]:
		cleaned_content = self._clean_response_text(raw_content)
		try:
			return json.loads(cleaned_content)
		except json.JSONDecodeError:
			json_candidate = _extract_json_object(cleaned_content)
			if json_candidate is None:
				raise ValueError("No valid JSON object found in LLM response")
			return json.loads(json_candidate)

	def _clean_response_text(self, raw_content: str) -> str:
		content = raw_content.strip()
		if content.startswith("```"):
			content = re.sub(r"^```(?:json)?\s*", "", content, flags=re.IGNORECASE)
			content = re.sub(r"\s*```$", "", content)
		return content.strip()

	def _build_retry_prompt(self, original_prompt: str) -> str:
		return (
			f"{original_prompt}\n\n"
			"再次强调：你的输出必须是一个合法 JSON 对象，"
			"不能包含 ``` 代码块、解释文字、前缀或后缀。"
		)


def _load_env_file(env_path: Path) -> None:
	if not env_path.exists():
		return

	for line in env_path.read_text(encoding="utf-8").splitlines():
		stripped = line.strip()
		if not stripped or stripped.startswith("#") or "=" not in stripped:
			continue
		key, value = stripped.split("=", 1)
		key = key.strip()
		value = value.strip().strip('"').strip("'")
		os.environ.setdefault(key, value)


def _extract_json_object(content: str) -> str | None:
	match = re.search(r"\{.*\}", content, re.DOTALL)
	if match:
		return match.group(0)
	return None


def _iter_sse_event_data(response) -> Iterator[str]:
	data_lines: list[str] = []

	for raw_line in response:
		line = raw_line.decode("utf-8", errors="ignore").rstrip("\r\n")
		if not line.strip():
			if data_lines:
				yield "\n".join(data_lines)
				data_lines.clear()
			continue

		if line.startswith("data:"):
			data = line[5:]
			if data.startswith(" "):
				data = data[1:]
			data_lines.append(data)

	if data_lines:
		yield "\n".join(data_lines)


def _extract_stream_delta_content(chunk_json: dict[str, Any]) -> str:
	try:
		delta = chunk_json["choices"][0]["delta"]
	except (KeyError, IndexError, TypeError):
		return ""

	if not isinstance(delta, dict):
		return ""

	content = delta.get("content")
	if isinstance(content, str):
		return content

	if isinstance(content, list):
		parts: list[str] = []
		for item in content:
			if not isinstance(item, dict):
				continue
			text_value = item.get("text")
			if isinstance(text_value, str):
				parts.append(text_value)
		return "".join(parts)

	return ""


__all__ = ["PreviewAgent"]
