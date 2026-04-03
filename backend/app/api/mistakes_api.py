"""错题模块 - API 路由"""
from fastapi import APIRouter, HTTPException
from typing import List

from app.mistakes.schema import (
    MistakeRecord,
    AddMistakeRequest,
    AnalyzeMistakeResponse,
    GetPracticeResponse,
)
from app.mistakes.service import MistakesService


router = APIRouter()
mistakes_service = MistakesService()


@router.post("/mistakes/add", response_model=MistakeRecord, summary="添加错题")
async def add_mistake(request: AddMistakeRequest):
    """
    添加一道错题记录
    
    需要提供：
    - 题目内容
    - 学生答案
    - 正确答案
    - 相关知识点（可选）
    """
    return mistakes_service.add_mistake(request)


@router.post("/mistakes/{mistake_id}/analyze", response_model=AnalyzeMistakeResponse, summary="分析错题")
async def analyze_mistake(mistake_id: str):
    """
    分析错题并生成针对性练习
    
    返回：
    - 错误根因分析
    - 知识漏洞诊断
    - 学习建议
    - 3 道针对性练习题
    """
    try:
        return mistakes_service.analyze_mistake(mistake_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"分析失败：{str(e)}")


@router.get("/mistakes/{mistake_id}/practice", response_model=GetPracticeResponse, summary="获取练习题")
async def get_practice_problems(mistake_id: str):
    """
    获取针对某道错题的练习题
    
    返回：
    - 3 道针对性练习题
    - 学习建议
    """
    try:
        return mistakes_service.get_practice_problems(mistake_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取练习失败：{str(e)}")


@router.get("/mistakes/list", response_model=List[MistakeRecord], summary="获取错题列表")
async def list_mistakes(limit: int = 20):
    """获取错题本中的错题列表"""
    return mistakes_service.list_mistakes(limit)


@router.get("/mistakes/{mistake_id}", response_model=MistakeRecord, summary="获取错题详情")
async def get_mistake(mistake_id: str):
    """获取单道错题的详细信息"""
    mistake = mistakes_service.get_mistake(mistake_id)
    if not mistake:
        raise HTTPException(status_code=404, detail="错题不存在")
    return mistake
