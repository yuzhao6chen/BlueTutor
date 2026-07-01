from __future__ import annotations

import os


def get_first_env(*names: str) -> str | None:
    for name in names:
        value = os.getenv(name)
        if value:
            return value.strip()
    return None


def build_chat_completions_url(base_url: str) -> str:
    normalized = base_url.strip().rstrip("/")
    if normalized.endswith("/chat/completions") or normalized.endswith("/completions"):
        return normalized
    return f"{normalized}/chat/completions"


def build_provider_base_url(base_url: str) -> str:
    normalized = base_url.strip().rstrip("/")
    suffix = "/chat/completions"
    if normalized.endswith(suffix):
        return normalized[: -len(suffix)].rstrip("/")
    return normalized


__all__ = ["build_chat_completions_url", "build_provider_base_url", "get_first_env"]
