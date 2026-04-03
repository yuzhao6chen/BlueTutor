"""预习模块 - Schema 定义"""
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


class KnowledgePoint(BaseModel):
    """知识点"""
    id: str
    name: str
    description: str
    grade_level: int = Field(ge=3, le=9, description="年级 (3-9)")
    subject: str = "math"


class PreviewRequest(BaseModel):
    """预习请求"""
    knowledge_point_id: str
    student_grade: int = Field(ge=3, le=9, description="学生年级")
    learning_style: Optional[str] = Field(default="visual", description="学习风格：visual/auditory/kinesthetic")


class PreviewQuestion(BaseModel):
    """引导式问题"""
    question: str
    hint: Optional[str] = None
    expected_answer: Optional[str] = None
    difficulty: int = Field(ge=1, le=5, default=2)


class PreviewResponse(BaseModel):
    """预习响应"""
    knowledge_point: KnowledgePoint
    introduction: str
    questions: List[PreviewQuestion]
    visual_aids: List[str] = []
    estimated_time: int = Field(description="预计学习时间 (分钟)")
    created_at: datetime = Field(default_factory=datetime.now)


class PreviewHistory(BaseModel):
    """预习历史"""
    id: str
    knowledge_point_id: str
    student_id: str
    completed: bool
    score: Optional[int] = None
    created_at: datetime
