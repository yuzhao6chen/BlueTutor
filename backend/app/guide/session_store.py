import json
import logging
from datetime import datetime
from pathlib import Path

from .phase2_dialogue.session_state import SessionState

logger = logging.getLogger(__name__)

_SESSIONS_DIR = Path(__file__).parent / "data" / "sessions"
_INDEX_FILE = _SESSIONS_DIR / "index.json"


def _ensure_dir() -> None:
    """确保存储目录存在"""
    _SESSIONS_DIR.mkdir(parents=True, exist_ok=True)


def _read_index() -> dict:
    """读取 index.json，若不存在则返回空字典"""
    if not _INDEX_FILE.exists():
        return {}
    try:
        with open(_INDEX_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError) as e:
        logger.error("读取 index.json 失败：%s", e)
        return {}


def _write_index(index: dict) -> None:
    """将 index 字典写入 index.json"""
    try:
        with open(_INDEX_FILE, "w", encoding="utf-8") as f:
            json.dump(index, f, ensure_ascii=False, indent=2)
    except OSError as e:
        logger.error("写入 index.json 失败：%s", e)
        raise


def add_to_index(session_id: str, raw_problem: str, created_at: str) -> None:
    """
    向 index.json 追加一条新会话的元信息。

    Args:
        session_id: 会话唯一标识
        raw_problem: 原始题目文本
        created_at: 创建时间（ISO 格式字符串）
    """
    _ensure_dir()
    index = _read_index()
    index[session_id] = {
        "session_id": session_id,
        "raw_problem": raw_problem,
        "created_at": created_at,
    }
    _write_index(index)
    logger.debug("已添加会话索引：%s", session_id)


def list_sessions() -> list[dict]:
    """
    返回所有历史会话的元信息列表，按创建时间倒序排列。

    Returns:
        元信息字典列表，每项包含 session_id、raw_problem、created_at
    """
    index = _read_index()
    sessions = list(index.values())
    sessions.sort(key=lambda x: x["created_at"], reverse=True)
    return sessions


def save_session(session_id: str, state: SessionState) -> None:
    """
    将 SessionState 序列化并写入对应的 JSON 文件（覆盖写）。

    Args:
        session_id: 会话唯一标识
        state: 当前会话状态
    """
    _ensure_dir()
    file_path = _SESSIONS_DIR / f"{session_id}.json"
    data = {"session_id": session_id, **state.to_dict()}
    try:
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        logger.debug("会话已持久化：%s", session_id)
    except OSError as e:
        logger.error("会话持久化失败 [%s]：%s", session_id, e)
        raise


def load_session(session_id: str) -> SessionState | None:
    """
    从 JSON 文件读取并还原 SessionState。

    Args:
        session_id: 会话唯一标识

    Returns:
        还原的 SessionState 实例，若文件不存在或读取失败则返回 None
    """
    file_path = _SESSIONS_DIR / f"{session_id}.json"
    if not file_path.exists():
        logger.warning("会话文件不存在：%s", session_id)
        return None
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        logger.debug("会话已从文件恢复：%s", session_id)
        return SessionState.from_dict(data)
    except (OSError, KeyError, ValueError) as e:
        logger.error("会话文件读取失败 [%s]：%s", session_id, e)
        return None
