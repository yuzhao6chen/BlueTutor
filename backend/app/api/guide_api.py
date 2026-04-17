from __future__ import annotations

import json
import logging
from typing import Any
from fastapi.responses import StreamingResponse

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
    generate_solution,
    get_session,
    get_session_detail,
    get_thinking_tree,
    run_turn,
    run_turn_stream,
    stream_solution,
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
def get_session_info(session_id: str) -> Any:
    """获取会话的题目文本和对话历史，用于恢复聊天界面"""
    try:
        return get_session_detail(session_id)
    except KeyError:
        return JSONResponse(status_code=404,
                            content={"code": 4041, "message": f"会话不存在：{session_id}", "data": None})


@guide_router.get("/sessions/{session_id}/thinking-tree")
def get_session_thinking_tree(session_id: str) -> Any:
    """获取会话的最新思维树"""
    try:
        return get_thinking_tree(session_id)
    except KeyError:
        return JSONResponse(status_code=404,
                            content={"code": 4042, "message": f"会话不存在：{session_id}", "data": None})


@guide_router.post("/sessions/{session_id}/turns", response_model=RunTurnResponse)
def post_run_turn(session_id: str, body: RunTurnRequest) -> Any:
    """执行一轮对话"""
    try:
        question, is_solved = run_turn(session_id, body.student_input)
        return RunTurnResponse(data=RunTurnData(question=question, is_solved=is_solved))

    except KeyError:
        return JSONResponse(status_code=404,
                            content={"code": 4043, "message": f"会话不存在：{session_id}", "data": None})

    except TutorSessionError as e:
        logger.error("对话执行失败 [%s]：%s", session_id, e)
        return JSONResponse(status_code=500, content={"code": 5001, "message": str(e), "data": None})


@guide_router.post("/sessions/{session_id}/turns/stream")
def post_run_turn_stream(session_id: str, body: RunTurnRequest) -> StreamingResponse:
    """执行一轮对话（SSE 流式版本）

    事件类型：
    - token:  {"token": "文字片段"}
    - retry:  {"attempt": 1}  — Guardrail 打回，前端应清空已显示内容
    - done:   {}              — 本轮结束
    - error:  {"message": "错误描述"}
    """

    def event_generator():
        try:
            is_solved = False
            for chunk in run_turn_stream(session_id, body.student_input):
                if chunk == "__RETRY__":
                    yield f"event: retry\ndata: {{}}\n\n"
                elif chunk == "__SOLVED__":
                    is_solved = True
                else:
                    payload = json.dumps({"token": chunk}, ensure_ascii=False)
                    yield f"event: token\ndata: {payload}\n\n"
            # 本轮正常结束，携带 is_solved 信号
            done_payload = json.dumps({"is_solved": is_solved}, ensure_ascii=False)
            yield f"event: done\ndata: {done_payload}\n\n"


        except KeyError:
            payload = json.dumps({"message": f"会话不存在：{session_id}"}, ensure_ascii=False)
            yield f"event: error\ndata: {payload}\n\n"

        except Exception as e:
            logger.error("流式对话执行失败 [%s]：%s", session_id, e)
            payload = json.dumps({"message": str(e)}, ensure_ascii=False)
            yield f"event: error\ndata: {payload}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",  # 禁用 Nginx 缓冲，确保实时推送
        }
    )


@guide_router.post("/sessions/{session_id}/report")
def post_generate_report(session_id: str) -> Any:
    """生成讲题报告"""
    try:
        return generate_report(session_id)
    except KeyError:
        return JSONResponse(status_code=404,
                            content={"code": 4044, "message": f"会话不存在：{session_id}", "data": None})
    except TutorSessionError as e:
        logger.error("报告生成失败 [%s]：%s", session_id, e)
        return JSONResponse(status_code=500, content={"code": 5002, "message": str(e), "data": None})


@guide_router.get("/sessions/{session_id}/solution")
def get_solution(session_id: str) -> Any:
    """获取已生成的题解（题解未生成时返回 404）"""
    try:
        session = get_session(session_id)
        if session._state.solution is None:
            return JSONResponse(
                status_code=404,
                content={"code": 4046, "message": "题解尚未生成", "data": None}
            )
        return {"code": 200, "message": "success", "data": {"solution": session._state.solution}}
    except KeyError:
        return JSONResponse(
            status_code=404,
            content={"code": 4045, "message": f"会话不存在：{session_id}", "data": None}
        )


@guide_router.post("/sessions/{session_id}/solution")
def post_generate_solution(session_id: str) -> Any:
    """生成个性化题解（非流式）"""
    try:
        solution_text = generate_solution(session_id)
        return {"code": 200, "message": "success", "data": {"solution": solution_text}}
    except KeyError:
        return JSONResponse(status_code=404,
                            content={"code": 4045, "message": f"会话不存在：{session_id}", "data": None})
    except TutorSessionError as e:
        logger.error("题解生成失败 [%s]：%s", session_id, e)
        return JSONResponse(status_code=500, content={"code": 5003, "message": str(e), "data": None})


@guide_router.post("/sessions/{session_id}/solution/stream")
def post_stream_solution(session_id: str) -> StreamingResponse:
    """生成个性化题解（SSE 流式版本）

    事件类型：
    - token:  {"token": "JSON 片段"}  — 题解 JSON 字符串的逐 Token 推送，前端拼接后解析
    - done:   {}                      — 题解生成完毕
    - error:  {"message": "错误描述"}
    """

    def event_generator():
        try:
            for chunk in stream_solution(session_id):
                payload = json.dumps({"token": chunk}, ensure_ascii=False)
                yield f"event: token\ndata: {payload}\n\n"
            yield "event: done\ndata: {}\n\n"
        except KeyError:
            payload = json.dumps({"message": f"会话不存在：{session_id}"}, ensure_ascii=False)
            yield f"event: error\ndata: {payload}\n\n"
        except Exception as e:
            logger.error("流式题解生成失败 [%s]：%s", session_id, e)
            payload = json.dumps({"message": str(e)}, ensure_ascii=False)
            yield f"event: error\ndata: {payload}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        }
    )


__all__ = ["guide_router"]
