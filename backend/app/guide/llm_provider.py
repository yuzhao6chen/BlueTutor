import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi

load_dotenv()

_llm_instance = None


def get_llm() -> ChatTongyi:
    """
    获取全局共享的 LLM 实例（单例模式）。
    实例带有自动重试机制，最多重试 3 次。

    Returns:
        带重试的 ChatTongyi 实例
    """
    global _llm_instance
    if _llm_instance is None:
        _llm_instance = ChatTongyi(
            api_key=os.environ["DASHSCOPE_API_KEY"],
            model="qwen-plus"
        ).with_retry(stop_after_attempt=3)
    return _llm_instance
