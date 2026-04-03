"""讲题模块 - API 路由"""
from fastapi import APIRouter, HTTPException
from typing import List, Optional

from app.guide.schema import (
    Problem,
    StartGuideRequest,
    GuideResponse,
    SubmitAnswerRequest,
    SubmitAnswerResponse,
)
from app.guide.service import GuideService


router = APIRouter()
guide_service = GuideService()


@router.get("/guide/problems", response_model=List[Problem], summary="获取题目列表")
async def list_problems(
    problem_type: Optional[str] = None,
    grade_level: Optional[int] = None
):
    """
    获取可讲解的题目列表
    
    - **problem_type**: 可选，按题目类型筛选（distance/chicken_rabbit/work/fraction_application）
    - **grade_level**: 可选，按年级筛选 (3-9)
    """
    return guide_service.list_problems(problem_type, grade_level)


@router.post("/guide/start", response_model=GuideResponse, summary="开始讲题")
async def start_guide(request: StartGuideRequest):
    """
    开始一道题的引导式讲解
    
    返回：
    - 会话 ID
    - 题目信息
    - 第一个引导步骤
    - 鼓励语
    """
    try:
        return guide_service.start_guide_session(request)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"开始讲题失败：{str(e)}")


@router.post("/guide/submit", response_model=SubmitAnswerResponse, summary="提交答案")
async def submit_answer(request: SubmitAnswerRequest):
    """
    提交学生的回答
    
    返回：
    - 反馈信息
    - 下一步引导（如果有）
    - 是否完成
    - 最终解答和知识点总结（完成后返回）
    """
    try:
        return guide_service.submit_answer(request)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"提交答案失败：{str(e)}")


@router.get("/guide/session/{session_id}", summary="获取会话信息")
async def get_session(session_id: str):
    """获取讲题会话的当前状态"""
    session = guide_service.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    return {
        "session_id": session.id,
        "problem_id": session.problem_id,
        "current_step": session.current_step,
        "total_steps": len(session.steps),
        "completed": session.completed,
        "answers_count": len(session.student_answers)
    }
