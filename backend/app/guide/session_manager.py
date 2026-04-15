import logging
import uuid
from datetime import datetime

from .session import SocraticTutorSession
from .session_store import add_to_index, load_session, save_session
from .phase2_dialogue.dialogue_loop import create_dialogue_graph

logger = logging.getLogger(__name__)

# 内存缓存：session_id -> SocraticTutorSession 实例
_cache: dict[str, SocraticTutorSession] = {}


def create_session(problem_text: str) -> str:
    """
    创建一个新的讲题会话。

    Args:
        problem_text: 学生提交的原始题目文本

    Returns:
        新会话的 session_id

    Raises:
        TutorSessionError: 题目解析失败时抛出
    """
    session_id = str(uuid.uuid4())
    created_at = datetime.now().isoformat()

    # 初始化会话（内部会调用 parse_problem，可能抛出 TutorSessionError）
    session = SocraticTutorSession(problem_text)

    # 写入内存缓存
    _cache[session_id] = session

    # 持久化初始状态和元信息索引
    save_session(session_id, session._state)
    add_to_index(session_id, problem_text, created_at)

    logger.info("新会话已创建：%s", session_id)
    return session_id


def get_session(session_id: str) -> SocraticTutorSession:
    """
    获取会话实例。优先从内存缓存读取，缓存未命中时从文件恢复。

    Args:
        session_id: 会话唯一标识

    Returns:
        SocraticTutorSession 实例

    Raises:
        KeyError: 会话不存在（内存和文件均无记录）时抛出
    """
    # 优先命中内存缓存
    if session_id in _cache:
        return _cache[session_id]

    # 缓存未命中，尝试从文件恢复
    logger.info("缓存未命中，尝试从文件恢复会话：%s", session_id)
    state = load_session(session_id)
    if state is None:
        raise KeyError(f"会话不存在：{session_id}")

    # 重建 SocraticTutorSession 实例并恢复状态
    session = SocraticTutorSession.__new__(SocraticTutorSession)
    session._state = state
    session._graph = create_dialogue_graph()

    _cache[session_id] = session
    logger.info("会话已从文件恢复并写入缓存：%s", session_id)
    return session


def run_turn(session_id: str, student_input: str) -> str:
    """
    执行一轮对话，并在完成后持久化最新状态。

    Args:
        session_id: 会话唯一标识
        student_input: 学生的输入文本

    Returns:
        老师的引导问题文本

    Raises:
        KeyError: 会话不存在时抛出
        TutorSessionError: 对话执行失败时抛出
    """
    session = get_session(session_id)
    question = session.run_one_turn(student_input)

    # 每轮结束后持久化
    save_session(session_id, session._state)

    return question


def generate_report(session_id: str) -> dict:
    """
    生成讲题报告。

    Args:
        session_id: 会话唯一标识

    Returns:
        结构化报告字典

    Raises:
        KeyError: 会话不存在时抛出
        TutorSessionError: 报告生成失败时抛出
    """
    session = get_session(session_id)
    return session.generate_report()


def get_session_detail(session_id: str) -> dict:
    """
    获取会话的聊天界面所需数据（题目文本 + 对话历史）。

    Raises:
        KeyError: 会话不存在时抛出
    """
    session = get_session(session_id)
    state = session._state
    return {
        "session_id": session_id,
        "raw_problem": state.raw_problem,
        "dialogue_history": state.dialogue_history,
    }


def get_thinking_tree(session_id: str) -> dict:
    """
    获取会话的最新思维树。

    Raises:
        KeyError: 会话不存在时抛出
    """
    session = get_session(session_id)
    state = session._state
    return {
        node_id: {
            "node_id": node.node_id,
            "content": node.content,
            "status": node.status.value,
            "parent_id": node.parent_id,
            "error_history": node.error_history,
            "children": node.children,
        }
        for node_id, node in state.thinking_tree.items()
    }
