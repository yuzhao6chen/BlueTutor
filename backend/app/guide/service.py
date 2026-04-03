"""讲题模块 - Service (业务逻辑层)"""
import uuid
from datetime import datetime
from typing import Dict, Any, List, Optional

from app.guide.schema import (
    Problem,
    GuideStep,
    GuideSession,
    StartGuideRequest,
    GuideResponse,
    SubmitAnswerRequest,
    SubmitAnswerResponse,
)
from app.guide.agent import GuideAgent


# 模拟的会话存储
_guide_sessions_db: Dict[str, GuideSession] = {}


class GuideService:
    """讲题服务"""
    
    def __init__(self):
        self.agent = GuideAgent()
    
    def list_problems(self, problem_type: Optional[str] = None, 
                      grade_level: Optional[int] = None) -> List[Problem]:
        """获取题目列表"""
        problems_data = self.agent.list_problems(problem_type, grade_level)
        return [
            Problem(
                id=p["id"],
                content=p["content"],
                problem_type=p["problem_type"],
                grade_level=p["grade_level"],
                difficulty=p.get("difficulty", 2),
                image_url=p.get("image_url")
            )
            for p in problems_data
        ]
    
    def start_guide_session(self, request: StartGuideRequest) -> GuideResponse:
        """开始讲题会话"""
        # 获取题目信息
        problem_data = self.agent.get_problem(request.problem_id)
        problem = Problem(
            id=problem_data["id"],
            content=problem_data["content"],
            problem_type=problem_data["problem_type"],
            grade_level=problem_data["grade_level"],
            difficulty=problem_data.get("difficulty", 2),
            image_url=problem_data.get("image_url")
        )
        
        # 生成引导步骤
        steps = self.agent.generate_guide_steps(problem_data, request.student_grade)
        
        if not steps:
            raise ValueError("无法生成引导步骤")
        
        # 创建会话
        session_id = str(uuid.uuid4())
        session = GuideSession(
            id=session_id,
            problem_id=request.problem_id,
            current_step=0,
            steps=steps,
            student_answers=[],
            completed=False
        )
        _guide_sessions_db[session_id] = session
        
        # 构建响应
        encouragement = f"{request.student_name}你好！我们一起来思考这道题。{self._get_encouragement(problem.problem_type)}"
        
        return GuideResponse(
            session_id=session_id,
            problem=problem,
            current_step=steps[0],
            total_steps=len(steps),
            encouragement=encouragement
        )
    
    def submit_answer(self, request: SubmitAnswerRequest) -> SubmitAnswerResponse:
        """提交答案"""
        session = _guide_sessions_db.get(request.session_id)
        if not session:
            raise ValueError("会话不存在")
        
        # 保存学生答案
        session.student_answers.append(request.answer)
        
        # 评估答案
        evaluation = self.agent.evaluate_answer(session, request.answer)
        
        is_completed = False
        next_step = None
        final_solution = None
        knowledge_summary = None
        
        # 检查是否完成
        if session.current_step >= len(session.steps) - 1:
            session.completed = True
            is_completed = True
            problem_data = self.agent.get_problem(session.problem_id)
            final_solution = "\n".join(problem_data.get("solution_steps", []))
            knowledge_summary = problem_data.get("key_knowledge", "")
        else:
            # 进入下一步
            session.current_step += 1
            next_step = session.steps[session.current_step]
        
        return SubmitAnswerResponse(
            session_id=request.session_id,
            feedback=evaluation["feedback"],
            next_step=next_step,
            is_completed=is_completed,
            final_solution=final_solution,
            knowledge_summary=knowledge_summary
        )
    
    def get_session(self, session_id: str) -> Optional[GuideSession]:
        """获取会话信息"""
        return _guide_sessions_db.get(session_id)
    
    def _get_encouragement(self, problem_type: str) -> str:
        """获取鼓励语"""
        encouragements = {
            "distance": "路程问题很有趣的，想想我们平时走路、骑车的经历！",
            "chicken_rabbit": "鸡兔同笼是经典的数学问题，用假设法就能解决！",
            "work": "工程问题关键是理解工作效率，一起加油！",
            "fraction_application": "分数应用题在生活中很常见，你一定可以的！",
        }
        return encouragements.get(problem_type, "慢慢来，我们一起思考！")
