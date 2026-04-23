import os
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

load_dotenv(Path(__file__).with_name(".env"), override=True)

_llm_instances: dict[str, Any] = {}

# DashScope OpenAI 兼容接口地址
_DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"


def get_llm(model: str = "qwen-plus" ) -> Any:
    """
    获取指定模型的 LLM 实例（按模型名称缓存，单例模式）。
    使用 DashScope OpenAI 兼容接口，支持所有千问系列模型。
    实例带有自动重试机制，最多重试 3 次。

    Args:
        model: 模型名称，默认为 "qwen-plus"。
               可选值示例：
               - "qwen-plus"（原有默认，兼容现有调用）
               - "qwen3-max"（用于图意规划，推理能力最强）
               - "qwen3.6-flash"（用于 SVG 代码生成，速度快成本低）

    Returns:
        带重试的 ChatOpenAI 实例（通过 DashScope 兼容接口）
    """
    global _llm_instances
    if model not in _llm_instances:
        api_key = os.getenv("DASHSCOPE_API_KEY", "").strip()
        if not api_key:
            raise RuntimeError("DASHSCOPE_API_KEY is missing or empty")
        _llm_instances[model] = ChatOpenAI(
            api_key=api_key,
            base_url=_DASHSCOPE_BASE_URL,
            model=model,
        ).with_retry(stop_after_attempt=3)
    return _llm_instances[model]
