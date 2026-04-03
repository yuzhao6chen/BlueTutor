"""预习模块 - Agent (AI 调用层)"""
import json
import re
from typing import List, Dict, Any
from openai import OpenAI

from app.shared.config import settings
from app.preview.prompt import PREVIEW_SYSTEM_PROMPT, PREVIEW_USER_PROMPT, KNOWLEDGE_POINTS_DB
from app.preview.schema import PreviewQuestion, KnowledgePoint


class PreviewAgent:
    """预习引导 Agent"""
    
    def __init__(self):
        self.client = OpenAI(
            api_key=settings.openai_api_key or "demo-key",
            base_url=settings.openai_base_url
        )
        self.model = settings.openai_model
    
    def get_knowledge_point(self, kp_id: str) -> Dict[str, Any]:
        """获取知识点信息"""
        if kp_id in KNOWLEDGE_POINTS_DB:
            return {"id": kp_id, **KNOWLEDGE_POINTS_DB[kp_id]}
        raise ValueError(f"知识点不存在：{kp_id}")
    
    def generate_preview(self, knowledge_point: Dict[str, Any], grade_level: int, 
                         learning_style: str = "visual") -> Dict[str, Any]:
        """生成预习材料"""
        
        user_prompt = PREVIEW_USER_PROMPT.format(
            knowledge_name=knowledge_point["name"],
            knowledge_description=knowledge_point["description"],
            grade_level=grade_level,
            learning_style=learning_style
        )
        
        # 如果没有配置真实的 API key，返回模拟数据
        if not settings.openai_api_key:
            return self._generate_mock_preview(knowledge_point, grade_level, learning_style)
        
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": PREVIEW_SYSTEM_PROMPT},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.7,
                max_tokens=1500
            )
            
            content = response.choices[0].message.content
            return self._parse_ai_response(content, knowledge_point)
            
        except Exception as e:
            # API 调用失败时返回模拟数据
            print(f"AI 调用失败，使用模拟数据：{e}")
            return self._generate_mock_preview(knowledge_point, grade_level, learning_style)
    
    def _generate_mock_preview(self, knowledge_point: Dict[str, Any], 
                                grade_level: int, learning_style: str) -> Dict[str, Any]:
        """生成模拟预习数据（用于演示）"""
        name = knowledge_point["name"]
        
        mock_questions = [
            PreviewQuestion(
                question=f"你知道{name}是什么吗？能用自己的话说一说吗？",
                hint="回想一下生活中见过的例子...",
                difficulty=1
            ),
            PreviewQuestion(
                question=f"如果让你解释给同学听，你会怎么介绍{name}？",
                hint="可以画图或者举例子来说明",
                difficulty=2
            ),
            PreviewQuestion(
                question=f"关于{name}，你有什么疑问或者想知道的地方？",
                hint="比如它有什么用？怎么计算？",
                difficulty=3
            ),
        ]
        
        return {
            "introduction": f"今天我们要学习的是「{name}」。这是数学中非常重要的一个概念，在生活中也有很多应用。让我们通过一些问题来一起探索吧！",
            "questions": mock_questions,
            "visual_aids": [
                f"{name}的示意图",
                "生活中的实际应用例子",
                "互动练习卡片"
            ],
            "estimated_time": 15
        }
    
    def _parse_ai_response(self, content: str, knowledge_point: Dict[str, Any]) -> Dict[str, Any]:
        """解析 AI 响应"""
        # 简化的解析逻辑，实际项目中可以使用更完善的解析
        lines = content.split('\n')
        questions = []
        visual_aids = []
        
        for line in lines:
            if '问题' in line or '?' in line or '？' in line:
                questions.append(PreviewQuestion(question=line.strip(), difficulty=2))
            if '图' in line or '演示' in line or '示例' in line:
                visual_aids.append(line.strip())
        
        return {
            "introduction": content[:200] if len(content) > 200 else content,
            "questions": questions[:5] if questions else [],
            "visual_aids": visual_aids[:3] if visual_aids else ["知识点思维导图"],
            "estimated_time": 15
        }
