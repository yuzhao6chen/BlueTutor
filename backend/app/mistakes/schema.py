"""错题模块 - Schema 定义"""
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


class MistakeRecord(BaseModel):
    """错题记录"""
    id: str
    problem_content: str
    student_answer: str
    correct_answer: str
    mistake_type: str = Field(description="错误类型：审题错误/计算错误/概念错误/思路错误")
    knowledge_points: List[str] = []
    created_at: datetime = Field(default_factory=datetime.now)


class MistakeAnalysis(BaseModel):
    """错题分析"""
    mistake_id: str
    root_cause: str
    knowledge_gaps: List[str]
    suggestion: str
    similar_problems: List[str] = []


class PracticeProblem(BaseModel):
    """练习题"""
    id: str
    content: str
    difficulty: int = Field(ge=1, le=5)
    related_knowledge: str
    hint: Optional[str] = None


class AddMistakeRequest(BaseModel):
    """添加错题请求"""
    problem_content: str
    student_answer: str
    correct_answer: str
    knowledge_points: List[str] = []


class AnalyzeMistakeResponse(BaseModel):
    """错题分析响应"""
    analysis: MistakeAnalysis
    practice_problems: List[PracticeProblem]


class GetPracticeResponse(BaseModel):
    """获取练习响应"""
    mistake_id: str
    problems: List[PracticeProblem]
    study_suggestion: str
