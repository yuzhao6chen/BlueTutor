"""讲题模块 - Schema 定义"""
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


class Problem(BaseModel):
    """题目"""
    id: str
    content: str
    problem_type: str = Field(description="题目类型：路程问题/鸡兔同笼/工程问题等")
    grade_level: int = Field(ge=3, le=9, description="年级 (3-9)")
    difficulty: int = Field(ge=1, le=5, default=2)
    image_url: Optional[str] = None


class GuideStep(BaseModel):
    """引导步骤"""
    step_number: int
    question: str
    hint: Optional[str] = None
    expected_thinking: Optional[str] = None
    is_final: bool = False


class GuideSession(BaseModel):
    """讲题会话"""
    id: str
    problem_id: str
    current_step: int = 0
    steps: List[GuideStep]
    student_answers: List[str] = []
    completed: bool = False
    created_at: datetime = Field(default_factory=datetime.now)


class StartGuideRequest(BaseModel):
    """开始讲题请求"""
    problem_id: str
    student_grade: int = Field(ge=3, le=9, description="学生年级")
    student_name: Optional[str] = "同学"


class GuideResponse(BaseModel):
    """讲题响应"""
    session_id: str
    problem: Problem
    current_step: GuideStep
    total_steps: int
    encouragement: str = ""


class SubmitAnswerRequest(BaseModel):
    """提交答案请求"""
    session_id: str
    answer: str


class SubmitAnswerResponse(BaseModel):
    """提交答案响应"""
    session_id: str
    feedback: str
    next_step: Optional[GuideStep] = None
    is_completed: bool
    final_solution: Optional[str] = None
    knowledge_summary: Optional[str] = None
