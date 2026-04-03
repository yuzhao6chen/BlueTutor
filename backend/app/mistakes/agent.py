"""错题模块 - Agent (AI 调用层)"""
import uuid
from typing import Dict, Any, List
from openai import OpenAI

from app.shared.config import settings
from app.mistakes.prompt import (
    MISTAKE_ANALYSIS_PROMPT, 
    MISTAKE_PRACTICE_PROMPT,
    MISTAKE_TYPES,
    PRACTICE_PROBLEMS_DB
)
from app.mistakes.schema import MistakeAnalysis, PracticeProblem


class MistakesAgent:
    """错题分析 Agent"""
    
    def __init__(self):
        self.client = OpenAI(
            api_key=settings.openai_api_key or "demo-key",
            base_url=settings.openai_base_url
        )
        self.model = settings.openai_model
    
    def analyze_mistake(self, problem: str, student_answer: str, 
                        correct_answer: str, knowledge_points: List[str]) -> Dict[str, Any]:
        """分析错题"""
        
        if not settings.openai_api_key:
            return self._mock_analyze(problem, student_answer, correct_answer, knowledge_points)
        
        try:
            user_prompt = f"""
【题目】{problem}
【学生答案】{student_answer}
【正确答案】{correct_answer}
【涉及知识点】{', '.join(knowledge_points) if knowledge_points else '未知'}

请分析这道错题。"""
            
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": MISTAKE_ANALYSIS_PROMPT},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.5,
                max_tokens=1500
            )
            
            return self._parse_analysis(response.choices[0].message.content)
            
        except Exception as e:
            print(f"AI 分析失败：{e}")
            return self._mock_analyze(problem, student_answer, correct_answer, knowledge_points)
    
    def _mock_analyze(self, problem: str, student_answer: str, 
                      correct_answer: str, knowledge_points: List[str]) -> Dict[str, Any]:
        """模拟错题分析"""
        # 简单判断错误类型
        mistake_type = "concept"
        if len(student_answer) < len(correct_answer) // 2:
            mistake_type = "reading"
        elif student_answer.isdigit() and correct_answer.isdigit():
            mistake_type = "calculation"
        
        type_info = MISTAKE_TYPES.get(mistake_type, MISTAKE_TYPES["concept"])
        
        return {
            "root_cause": f"可能是{type_info['name']}。{type_info['description']}",
            "knowledge_gaps": knowledge_points if knowledge_points else ["相关基础知识"],
            "suggestion": f"建议：{'；'.join(type_info['suggestions'][:2])}",
            "mistake_type": mistake_type,
        }
    
    def generate_practice_problems(self, problem: str, knowledge_gaps: List[str]) -> List[PracticeProblem]:
        """生成针对性练习题"""
        
        if not settings.openai_api_key:
            return self._mock_practice_problems(knowledge_gaps)
        
        try:
            user_prompt = MISTAKE_PRACTICE_PROMPT.format(
                problem_content=problem,
                student_answer="",
                correct_answer="",
                knowledge_gaps=", ".join(knowledge_gaps)
            )
            
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "你是一位数学教师，擅长出题。"},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.7,
                max_tokens=1500
            )
            
            return self._parse_problems(response.choices[0].message.content)
            
        except Exception as e:
            print(f"AI 出题失败：{e}")
            return self._mock_practice_problems(knowledge_gaps)
    
    def _mock_practice_problems(self, knowledge_gaps: List[str]) -> List[PracticeProblem]:
        """返回模拟练习题"""
        problems_data = PRACTICE_PROBLEMS_DB.get("default_practice", [])
        
        return [
            PracticeProblem(
                id=p["id"],
                content=p["content"],
                difficulty=p["difficulty"],
                related_knowledge=p["related_knowledge"],
                hint=p.get("hint")
            )
            for p in problems_data
        ]
    
    def _parse_analysis(self, content: str) -> Dict[str, Any]:
        """解析 AI 分析结果"""
        return {
            "root_cause": content[:200] if len(content) > 200 else content,
            "knowledge_gaps": ["相关知识"],
            "suggestion": "请仔细阅读分析内容",
            "mistake_type": "concept"
        }
    
    def _parse_problems(self, content: str) -> List[PracticeProblem]:
        """解析 AI 生成的题目"""
        return self._mock_practice_problems([])
