from __future__ import annotations

import os
from pathlib import Path
from typing import Any

import httpx
from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_openai import ChatOpenAI

from ..shared.openai_compat import get_first_env


ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
DEFAULT_OPENAI_MODEL = "gpt-5.4"
DEFAULT_TONGYI_MODEL = "qwen-plus"

load_dotenv(ENV_PATH, override=True)

_llm_instance: Any | None = None


def _normalize_openai_base_url(base_url: str) -> str:
    normalized = base_url.strip().rstrip("/")
    if normalized.endswith("/chat/completions"):
        normalized = normalized[: -len("/chat/completions")]
    return normalized


def _build_openai_llm() -> Any:
    api_key = get_first_env("OPENAI_API_KEY", "LLM_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is missing or empty")

    model_name = (
        get_first_env("GUIDE_MODEL_NAME", "OPENAI_MODEL", "LLM_MODEL_NAME")
        or DEFAULT_OPENAI_MODEL
    )
    timeout_seconds = float(os.getenv("LLM_TIMEOUT_SECONDS", "60"))
    max_retries = int(os.getenv("LLM_MAX_RETRIES", "2"))

    kwargs: dict[str, Any] = {
        "api_key": api_key,
        "model": model_name,
        "timeout": timeout_seconds,
        "max_retries": max_retries,
        "http_client": httpx.Client(timeout=timeout_seconds, trust_env=False),
        "http_async_client": httpx.AsyncClient(timeout=timeout_seconds, trust_env=False),
    }

    base_url = get_first_env("OPENAI_BASE_URL", "LLM_BASE_URL")
    if base_url:
        kwargs["base_url"] = _normalize_openai_base_url(base_url)

    return ChatOpenAI(**kwargs)


def _build_tongyi_llm() -> Any:
    api_key = os.getenv("DASHSCOPE_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("DASHSCOPE_API_KEY is missing or empty")

    model_name = os.getenv("DASHSCOPE_MODEL", DEFAULT_TONGYI_MODEL).strip() or DEFAULT_TONGYI_MODEL
    max_retries = int(os.getenv("LLM_MAX_RETRIES", "2"))

    return ChatTongyi(
        api_key=api_key,
        model=model_name,
    ).with_retry(stop_after_attempt=max_retries + 1)


def get_llm() -> Any:
    global _llm_instance
    if _llm_instance is None:
        if get_first_env("OPENAI_API_KEY", "LLM_API_KEY"):
            _llm_instance = _build_openai_llm()
        else:
            _llm_instance = _build_tongyi_llm()
    return _llm_instance


__all__ = ["get_llm"]
