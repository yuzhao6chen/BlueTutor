from __future__ import annotations

import json
import os
import re
import uuid
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from .openai_compat import build_chat_completions_url, get_first_env


DEFAULT_ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
DEFAULT_BASE_URL = "https://apiport.cc/v1"
DEFAULT_MODEL_NAME = "gpt-4o"

OCR_PROMPT = (
    "你是 BlueTutor 的题目识别助手，服务对象是小学三年级到初中阶段的数学学习场景。\n"
    "请仔细识别图片中的数学题目文字，提取完整、准确的题目内容。\n\n"
    "输出要求（严格遵守）：\n"
    "1. 只输出 JSON，不要输出 markdown 代码块，不要有任何解释性文字。\n"
    "2. 返回字段只包含 question_text。\n"
    "3. question_text 是图片中完整的题目文字，保留所有数字、单位、问句、条件。\n"
    "4. 如果图片中有多道题，只提取最主要/最完整的一道。\n"
    "5. 如果图片模糊无法识别，question_text 返回空字符串。\n"
    "6. 不要补全、不要推断、不要改变原题文字。\n\n"
    "输出格式（严格）：\n"
    '{\"question_text\": \"题目文字\"}'
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


def _normalize_base64(image_base64: str) -> str:
    """
    统一处理 base64 格式：
    Android 端传来的可能带 data:image/jpeg;base64, 前缀，也可能是纯 base64。
    视觉模型需要带 data: 前缀的完整 URL。
    """
    if image_base64.startswith("data:"):
        return image_base64
    return f"data:image/jpeg;base64,{image_base64}"


class OCRAgent:
    def __init__(
        self,
        *,
        base_url: str | None = None,
        api_key: str | None = None,
        model_name: str | None = None,
        timeout_seconds: int | None = None,
        env_path: Path | None = None,
    ) -> None:
        self.env_path = env_path or DEFAULT_ENV_PATH
        _load_env_file(self.env_path)

        configured_base_url = base_url or get_first_env("OPENAI_BASE_URL", "LLM_BASE_URL") or DEFAULT_BASE_URL
        self.base_url = build_chat_completions_url(configured_base_url)
        self.api_key = api_key or get_first_env("OPENAI_API_KEY", "LLM_API_KEY")
        self.model_name = model_name or get_first_env("OPENAI_MODEL", "OCR_MODEL_NAME", "LLM_MODEL_NAME") or DEFAULT_MODEL_NAME
        self.timeout_seconds = timeout_seconds or int(os.getenv("LLM_TIMEOUT_SECONDS", "60"))

        if not self.api_key:
            raise ValueError("LLM_API_KEY is not configured. 请在 .env 文件中配置 LLM_API_KEY。")

    def recognize(self, image_base64: str, image_type: str = "photo") -> dict[str, Any]:
        """
        识别图片中的题目文字。
        返回：{"image_id": "img_xxx", "question_text": "..."}
        """
        image_id = f"img_{uuid.uuid4().hex[:8]}"
        image_url = _normalize_base64(image_base64)

        raw_content = self._send_vision_request(image_url)
        question_text = self._parse_question_text(raw_content)

        return {
            "image_id": image_id,
            "question_text": question_text,
        }

    def _send_vision_request(self, image_data_url: str) -> str:
        payload = {
            "model": self.model_name,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": image_data_url
                            },
                        },
                        {
                            "type": "text",
                            "text": OCR_PROMPT,
                        },
                    ],
                }
            ],
            "temperature": 0.1,
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
            with urlopen(request, timeout=self.timeout_seconds) as response:
                body = response.read().decode("utf-8")
        except HTTPError as exc:
            error_body = exc.read().decode("utf-8", errors="ignore")
            raise RuntimeError(f"OCR API HTTP 错误 {exc.code}: {error_body}") from exc
        except URLError as exc:
            raise RuntimeError(f"OCR API 连接失败: {exc.reason}") from exc

        response_json = json.loads(body)
        try:
            return response_json["choices"][0]["message"]["content"]
        except (KeyError, IndexError, TypeError) as exc:
            raise RuntimeError(f"OCR API 响应结构异常: {body}") from exc

    def _parse_question_text(self, raw_content: str) -> str:
        content = raw_content.strip()

        # 去掉可能的 markdown 代码块包裹
        if content.startswith("```"):
            content = re.sub(r"^```(?:json)?\s*", "", content, flags=re.IGNORECASE)
            content = re.sub(r"\s*```$", "", content)
        content = content.strip()

        # 尝试正常 JSON 解析
        try:
            parsed = json.loads(content)
            if isinstance(parsed, dict):
                return str(parsed.get("question_text", "")).strip()
        except json.JSONDecodeError:
            pass

        # 降级：正则提取 question_text 字段
        match = re.search(r'"question_text"\s*:\s*"((?:[^"\\]|\\.)*)"', content)
        if match:
            return match.group(1).strip()

        # 实在解析不了，返回空字符串，不抛异常（不能因为格式问题让整个服务崩溃）
        return ""


__all__ = ["OCRAgent"]
