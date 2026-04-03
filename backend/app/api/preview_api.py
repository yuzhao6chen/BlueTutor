"""预习模块 - API 路由"""
from fastapi import APIRouter, HTTPException
from typing import List, Optional

from app.preview.schema import PreviewRequest, PreviewResponse, KnowledgePoint, PreviewHistory
from app.preview.service import PreviewService


router = APIRouter()
preview_service = PreviewService()


@router.get("/preview/knowledge-points", response_model=List[KnowledgePoint], summary="获取知识点列表")
async def list_knowledge_points(grade_level: Optional[int] = None):
    """
    获取可预习的知识点列表
    
    - **grade_level**: 可选，按年级筛选 (3-9)
    """
    return preview_service.get_knowledge_points(grade_level)


@router.post("/preview/generate", response_model=PreviewResponse, summary="生成预习内容")
async def generate_preview(request: PreviewRequest):
    """
    根据知识点生成引导式预习内容
    
    返回：
    - 知识点介绍
    - 3-5 个层层递进的引导式问题
    - 可视化辅助建议
    - 预计学习时间
    """
    try:
        return preview_service.generate_preview_content(request)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"生成预习内容失败：{str(e)}")


@router.get("/preview/history", response_model=List[PreviewHistory], summary="获取预习历史")
async def get_preview_history(student_id: str = "demo_student"):
    """获取学生的预习历史记录"""
    return preview_service.get_preview_history(student_id)


@router.post("/preview/complete/{history_id}", summary="标记预习完成")
async def mark_preview_complete(history_id: str, score: Optional[int] = None):
    """
    标记某次预习为已完成
    
    - **history_id**: 预习记录 ID
    - **score**: 可选，预习得分 (0-100)
    """
    success = preview_service.complete_preview(history_id, score)
    if not success:
        raise HTTPException(status_code=404, detail="预习记录不存在")
    return {"status": "success", "message": "预习已标记为完成"}
