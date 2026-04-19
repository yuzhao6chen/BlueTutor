from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import ProxyHandler, Request, build_opener

from .prompt import build_knowledge_extraction_prompt, build_preview_chat_prompt
from .schema import PreviewChatMessage
from ..shared.openai_compat import build_chat_completions_url, get_first_env


DEFAULT_ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
DEFAULT_BASE_URL = "https://apiport.cc/v1"
DEFAULT_MODEL_NAME = "qwen3.6-plus"
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

	def extract_knowledge_points(self, content_text: str) -> dict[str, Any]:
		prompt = build_knowledge_extraction_prompt(content_text)
		return self._call_llm_json(prompt)

	def chat(
		self,
		text: str,
		context_text: str,
		selected_knowledge_points: list[str],
		history: list[PreviewChatMessage] | None = None,
	) -> dict[str, Any]:
		prompt = build_preview_chat_prompt(
			text=text,
			context_text=context_text,
			selected_knowledge_points=selected_knowledge_points,
			history=history,
		)
		return self._call_llm_json(prompt)

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
		payload = {
			"model": self.model_name,
			"messages": [
				{
					"role": "user",
					"content": prompt,
				}
			],
			"temperature": 0.2,
			"stream": False,
		}
		request = Request(
			url=self.base_url,
			data=json.dumps(payload).encode("utf-8"),
			headers={
				"Authorization": f"Bearer {self.api_key}",
				"Content-Type": "application/json",
			},
			method="POST",
		)

		try:
			with _DIRECT_HTTP_OPENER.open(request, timeout=self.timeout_seconds) as response:
				body = response.read().decode("utf-8")
		except HTTPError as exc:
			error_body = exc.read().decode("utf-8", errors="ignore")
			raise RuntimeError(f"LLM HTTP error {exc.code}: {error_body}") from exc
		except URLError as exc:
			raise RuntimeError(f"LLM connection failed: {exc.reason}") from exc

		response_json = json.loads(body)
		try:
			return response_json["choices"][0]["message"]["content"]
		except (KeyError, IndexError, TypeError) as exc:
			raise RuntimeError(f"Unexpected LLM response structure: {body}") from exc

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


__all__ = ["PreviewAgent"]
