"""错题模块 - Service (业务逻辑层)"""
import uuid
from datetime import datetime
from typing import Dict, List, Optional

from app.mistakes.schema import (
    MistakeRecord,
    MistakeAnalysis,
    PracticeProblem,
    AddMistakeRequest,
    AnalyzeMistakeResponse,
    GetPracticeResponse,
)
from app.mistakes.agent import MistakesAgent


# 模拟的错题存储
_mistakes_db: Dict[str, MistakeRecord] = {}


class MistakesService:
    """错题服务"""
    
    def __init__(self):
        self.agent = MistakesAgent()
    
    def add_mistake(self, request: AddMistakeRequest) -> MistakeRecord:
        """添加错题记录"""
        mistake_id = str(uuid.uuid4())
        
        # 简单判断错误类型
        mistake_type = "concept"
        if len(request.student_answer) < len(request.correct_answer) // 2:
            mistake_type = "reading"
        elif request.student_answer.isdigit() and request.correct_answer.isdigit():
            mistake_type = "calculation"
        
        mistake = MistakeRecord(
            id=mistake_id,
            problem_content=request.problem_content,
            student_answer=request.student_answer,
            correct_answer=request.correct_answer,
            mistake_type=mistake_type,
            knowledge_points=request.knowledge_points
        )
        
        _mistakes_db[mistake_id] = mistake
        return mistake
    
    def analyze_mistake(self, mistake_id: str) -> AnalyzeMistakeResponse:
        """分析错题并生成练习"""
        mistake = _mistakes_db.get(mistake_id)
        if not mistake:
            raise ValueError("错题记录不存在")
        
        # AI 分析错题
        analysis_data = self.agent.analyze_mistake(
            problem=mistake.problem_content,
            student_answer=mistake.student_answer,
            correct_answer=mistake.correct_answer,
            knowledge_points=mistake.knowledge_points
        )
        
        analysis = MistakeAnalysis(
            mistake_id=mistake_id,
            root_cause=analysis_data["root_cause"],
            knowledge_gaps=analysis_data["knowledge_gaps"],
            suggestion=analysis_data["suggestion"]
        )
        
        # 生成针对性练习
        practice_problems = self.agent.generate_practice_problems(
            problem=mistake.problem_content,
            knowledge_gaps=analysis.knowledge_gaps
        )
        
        analysis.similar_problems = [p.id for p in practice_problems]
        
        return AnalyzeMistakeResponse(
            analysis=analysis,
            practice_problems=practice_problems
        )
    
    def get_practice_problems(self, mistake_id: str) -> GetPracticeResponse:
        """获取错题对应的练习题"""
        mistake = _mistakes_db.get(mistake_id)
        if not mistake:
            raise ValueError("错题记录不存在")
        
        problems = self.agent.generate_practice_problems(
            problem=mistake.problem_content,
            knowledge_gaps=mistake.knowledge_points or ["相关知识"]
        )
        
        study_suggestion = f"建议先复习{', '.join(mistake.knowledge_points)}相关知识点，然后完成以下练习。"
        
        return GetPracticeResponse(
            mistake_id=mistake_id,
            problems=problems,
            study_suggestion=study_suggestion
        )
    
    def list_mistakes(self, limit: int = 20) -> List[MistakeRecord]:
        """获取错题列表"""
        return list(_mistakes_db.values())[:limit]
    
    def get_mistake(self, mistake_id: str) -> Optional[MistakeRecord]:
        """获取单道错题"""
        return _mistakes_db.get(mistake_id)
