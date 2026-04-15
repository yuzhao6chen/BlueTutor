from __future__ import annotations

import logging
from typing import Any

from fastapi import APIRouter
from fastapi.responses import JSONResponse

from ..guide.schema import (
    CreateSessionRequest,
    CreateSessionResponse,
    CreateSessionData,
    RunTurnRequest,
    RunTurnResponse,
    RunTurnData,
)
from ..guide.session import TutorSessionError
from ..guide.session_manager import (
    create_session,
    generate_report,
    get_session_detail,
    get_thinking_tree,
    run_turn,
)
from ..guide.session_store import list_sessions

logger = logging.getLogger(__name__)

guide_router = APIRouter(prefix="/api/guide", tags=["guide"])


# ── 路由 ───────────────────────────────────────────────────────

@guide_router.get("/sessions")
def get_sessions() -> list[dict]:
    """返回所有历史会话的元信息列表"""
    return list_sessions()


@guide_router.post("/sessions", response_model=CreateSessionResponse)
def post_create_session(body: CreateSessionRequest) -> Any:
    """创建新的讲题会话"""
    try:
        session_id = create_session(body.problem_text)
        return CreateSessionResponse(data=CreateSessionData(session_id=session_id))
    except TutorSessionError as e:
        logger.error("创建会话失败：%s", e)
        return JSONResponse(status_code=422, content={"code": 4001, "message": str(e), "data": None})


@guide_router.get("/sessions/{session_id}")
def get_session(session_id: str) -> Any:
    """获取会话的题目文本和对话历史，用于恢复聊天界面"""
    try:
        return get_session_detail(session_id)
    except KeyError:
        return JSONResponse(status_code=404, content={"code": 4041, "message": f"会话不存在：{session_id}", "data": None})


@guide_router.get("/sessions/{session_id}/thinking-tree")
def get_session_thinking_tree(session_id: str) -> Any:
    """获取会话的最新思维树"""
    try:
        return get_thinking_tree(session_id)
    except KeyError:
        return JSONResponse(status_code=404, content={"code": 4042, "message": f"会话不存在：{session_id}", "data": None})


@guide_router.post("/sessions/{session_id}/turns", response_model=RunTurnResponse)
def post_run_turn(session_id: str, body: RunTurnRequest) -> Any:
    """执行一轮对话"""
    try:
        question = run_turn(session_id, body.student_input)
        return RunTurnResponse(data=RunTurnData(question=question))
    except KeyError:
        return JSONResponse(status_code=404, content={"code": 4043, "message": f"会话不存在：{session_id}", "data": None})
    except TutorSessionError as e:
        logger.error("对话执行失败 [%s]：%s", session_id, e)
        return JSONResponse(status_code=500, content={"code": 5001, "message": str(e), "data": None})


@guide_router.post("/sessions/{session_id}/report")
def post_generate_report(session_id: str) -> Any:
    """生成讲题报告"""
    try:
        return generate_report(session_id)
    except KeyError:
        return JSONResponse(status_code=404, content={"code": 4044, "message": f"会话不存在：{session_id}", "data": None})
    except TutorSessionError as e:
        logger.error("报告生成失败 [%s]：%s", session_id, e)
        return JSONResponse(status_code=500, content={"code": 5002, "message": str(e), "data": None})


__all__ = ["guide_router"]
