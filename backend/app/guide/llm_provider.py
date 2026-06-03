import os
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

_GUIDE_DIR = Path(__file__).resolve().parent
_BACKEND_DIR = _GUIDE_DIR.parents[1]

load_dotenv(_BACKEND_DIR / ".env", override=False)
load_dotenv(_GUIDE_DIR / ".env", override=True)

_llm_instances: dict[str, Any] = {}

_DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"


def get_llm(model: str = "qwen-plus") -> Any:
    global _llm_instances

    effective_model = os.getenv("LLM_MODEL_NAME", "").strip() or model
    cache_key = effective_model

    if cache_key not in _llm_instances:
        api_key = os.getenv("DASHSCOPE_API_KEY", "").strip()
        if not api_key:
            api_key = os.getenv("LLM_API_KEY", "").strip()
        if not api_key:
            raise RuntimeError("DASHSCOPE_API_KEY/LLM_API_KEY is missing or empty")

        base_url = os.getenv("LLM_BASE_URL", "").strip() or _DASHSCOPE_BASE_URL

        timeout = int(os.getenv("LLM_TIMEOUT_SECONDS", "120"))

        _llm_instances[cache_key] = ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=effective_model,
            request_timeout=timeout,
        ).with_retry(stop_after_attempt=3)
    return _llm_instances[cache_key]
