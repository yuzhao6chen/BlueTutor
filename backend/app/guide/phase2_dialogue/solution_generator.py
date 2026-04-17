import json

from langchain_core.output_parsers import StrOutputParser

from ..llm_provider import get_llm
from .prompts import SOLUTION_GENERATOR_PROMPT
from .session_state import SessionState
from .state_tracker import _serialize_tree, _serialize_dialogue

import logging

logger = logging.getLogger(__name__)


def _serialize_solution_path(solution_path: list) -> str:
    """将解题路径节点列表序列化为可读字符串，供 LLM 理解"""
    if not solution_path:
        return "（解题路径为空）"
    lines = []
    for i, node in enumerate(solution_path):
        prefix = "起点" if node.node_id == "n0" else f"第{i}步"
        error_info = f"（曾出现错误：{'、'.join(node.error_history)}）" if node.error_history else ""
        lines.append(f"{prefix}：[{node.node_id}] {node.content} ({node.status.value}){error_info}")
    return "\n".join(lines)


def generate_solution(session_state: SessionState) -> dict:
    """
    接收完整的 SessionState，生成个性化题解。

    Args:
        session_state: 对话结束后的完整会话状态，is_solved 应为 True

    Returns:
        结构化题解字典，包含 solution_steps、personalized_review、knowledge_summary

    Raises:
        Exception: LLM 调用失败时直接抛出异常
    """
    llm = get_llm()
    chain = SOLUTION_GENERATOR_PROMPT | llm | StrOutputParser()


    solution_path = session_state.get_solution_path()

    result = chain.invoke({
        "parsed_problem": json.dumps(session_state.parsed_problem, ensure_ascii=False),
        "solution_path": _serialize_solution_path(solution_path),
        "thinking_tree": _serialize_tree(session_state.thinking_tree),
        "dialogue_history": _serialize_dialogue(session_state.dialogue_history),
    })

    result = result.strip()
    logger.info("题解生成完成，字符数：%d", len(result))

    return result


def stream_solution_generator(session_state: SessionState):
    """
    流式版本的题解生成器，逐 Token yield。

    由于题解使用 JsonOutputParser，流式输出的是原始 JSON 字符串的 Token 片段，
    前端需要等待完整 JSON 后再解析。

    Yields:
        str: 题解 JSON 字符串的每个 Token 片段
    """
    llm = get_llm()
    chain = SOLUTION_GENERATOR_PROMPT | llm | StrOutputParser()

    solution_path = session_state.get_solution_path()

    inputs = {
        "parsed_problem": json.dumps(session_state.parsed_problem, ensure_ascii=False),
        "solution_path": _serialize_solution_path(solution_path),
        "thinking_tree": _serialize_tree(session_state.thinking_tree),
        "dialogue_history": _serialize_dialogue(session_state.dialogue_history),
    }

    for chunk in chain.stream(inputs):
        yield chunk
