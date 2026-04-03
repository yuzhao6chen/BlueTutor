"""预习模块 - Service (业务逻辑层)"""
import uuid
from datetime import datetime
from typing import List, Optional

from app.preview.schema import (
    PreviewRequest, 
    PreviewResponse, 
    KnowledgePoint, 
    PreviewQuestion,
    PreviewHistory
)
from app.preview.agent import PreviewAgent


# 模拟的预习历史记录存储
_preview_history_db: List[PreviewHistory] = []


class PreviewService:
    """预习服务"""
    
    def __init__(self):
        self.agent = PreviewAgent()
    
    def get_knowledge_points(self, grade_level: Optional[int] = None) -> List[KnowledgePoint]:
        """获取知识点列表"""
        from app.preview.prompt import KNOWLEDGE_POINTS_DB
        
        result = []
        for kp_id, kp_data in KNOWLEDGE_POINTS_DB.items():
            if grade_level is None or kp_data["grade_level"] == grade_level:
                result.append(KnowledgePoint(id=kp_id, **kp_data))
        return result
    
    def generate_preview_content(self, request: PreviewRequest) -> PreviewResponse:
        """生成预习内容"""
        # 获取知识点信息
        knowledge_point_data = self.agent.get_knowledge_point(request.knowledge_point_id)
        knowledge_point = KnowledgePoint(
            id=knowledge_point_data["id"],
            name=knowledge_point_data["name"],
            description=knowledge_point_data["description"],
            grade_level=knowledge_point_data["grade_level"],
            subject=knowledge_point_data.get("subject", "math")
        )
        
        # 生成预习材料
        preview_data = self.agent.generate_preview(
            knowledge_point=knowledge_point_data,
            grade_level=request.student_grade,
            learning_style=request.learning_style
        )
        
        # 构建响应
        questions = [
            PreviewQuestion(
                question=q.get("question", "") if isinstance(q, dict) else q.question,
                hint=q.get("hint") if isinstance(q, dict) else q.hint,
                difficulty=q.get("difficulty", 2) if isinstance(q, dict) else q.difficulty
            )
            for q in preview_data["questions"]
        ]
        
        response = PreviewResponse(
            knowledge_point=knowledge_point,
            introduction=preview_data["introduction"],
            questions=questions,
            visual_aids=preview_data.get("visual_aids", []),
            estimated_time=preview_data.get("estimated_time", 15)
        )
        
        # 保存历史记录（模拟）
        history = PreviewHistory(
            id=str(uuid.uuid4()),
            knowledge_point_id=request.knowledge_point_id,
            student_id="demo_student",
            completed=False,
            created_at=datetime.now()
        )
        _preview_history_db.append(history)
        
        return response
    
    def get_preview_history(self, student_id: str = "demo_student") -> List[PreviewHistory]:
        """获取预习历史"""
        return [h for h in _preview_history_db if h.student_id == student_id]
    
    def complete_preview(self, history_id: str, score: Optional[int] = None):
        """标记预习完成"""
        for history in _preview_history_db:
            if history.id == history_id:
                history.completed = True
                history.score = score
                return True
        return False
