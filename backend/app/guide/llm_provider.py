import os
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from pydantic import SecretStr

load_dotenv(Path(__file__).with_name(".env"), override=True)

_llm_instance = None


def get_llm() -> Any:
    """
    获取全局共享的 LLM 实例（单例模式）。
    实例带有自动重试机制，最多重试 3 次。

    Returns:
        带重试的 ChatTongyi 实例
    """
    print(os.getenv("DASHSCOPE_API_KEY", "").strip())  # 调试输出，确认环境变量是否正确加载
    global _llm_instance
    if _llm_instance is None:
        api_key = os.getenv("DASHSCOPE_API_KEY", "").strip()
        if not api_key:
            raise RuntimeError("DASHSCOPE_API_KEY is missing or empty")
        _llm_instance = ChatTongyi(
            api_key=api_key,
            model="qwen-plus"
        ).with_retry(stop_after_attempt=3)
    return _llm_instance
