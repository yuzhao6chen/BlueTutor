"""讲题模块 - Agent (AI 调用层)"""
import uuid
from typing import Dict, Any, List, Optional
from openai import OpenAI

from app.shared.config import settings
from app.guide.prompt import PROBLEMS_DB, get_guide_prompts
from app.guide.schema import GuideStep, Problem, GuideSession


class GuideAgent:
    """讲题引导 Agent"""
    
    def __init__(self):
        self.client = OpenAI(
            api_key=settings.openai_api_key or "demo-key",
            base_url=settings.openai_base_url
        )
        self.model = settings.openai_model
    
    def get_problem(self, problem_id: str) -> Dict[str, Any]:
        """获取题目信息"""
        if problem_id in PROBLEMS_DB:
            return PROBLEMS_DB[problem_id]
        raise ValueError(f"题目不存在：{problem_id}")
    
    def list_problems(self, problem_type: Optional[str] = None, 
                      grade_level: Optional[int] = None) -> List[Dict[str, Any]]:
        """获取题目列表"""
        result = []
        for prob in PROBLEMS_DB.values():
            if problem_type and prob["problem_type"] != problem_type:
                continue
            if grade_level and prob["grade_level"] != grade_level:
                continue
            result.append(prob)
        return result
    
    def generate_guide_steps(self, problem: Dict[str, Any], student_grade: int) -> List[GuideStep]:
        """生成引导步骤"""
        
        if not settings.openai_api_key:
            return self._generate_mock_steps(problem)
        
        try:
            system_prompt, user_prompt = get_guide_prompts(
                problem["problem_type"],
                problem["content"],
                student_grade
            )
            
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.7,
                max_tokens=2000
            )
            
            return self._parse_guide_steps(response.choices[0].message.content, problem)
            
        except Exception as e:
            print(f"AI 调用失败，使用预设步骤：{e}")
            return self._generate_mock_steps(problem)
    
    def _generate_mock_steps(self, problem: Dict[str, Any]) -> List[GuideStep]:
        """生成模拟引导步骤"""
        steps_data = problem.get("solution_steps", [])
        steps = []
        
        for i, step in enumerate(steps_data):
            is_final = (i == len(steps_data) - 1)
            steps.append(GuideStep(
                step_number=i + 1,
                question=f"第{i + 1}步：{step}",
                hint="仔细思考一下这个步骤...",
                is_final=is_final
            ))
        
        # 如果没有预设步骤，创建通用步骤
        if not steps:
            steps = [
                GuideStep(
                    step_number=1,
                    question="请仔细阅读题目，说说这道题告诉了我们什么已知条件？要求什么问题？",
                    hint="可以尝试把已知条件和问题分别列出来",
                    is_final=False
                ),
                GuideStep(
                    step_number=2,
                    question="这道题属于什么类型的问题？解决这类问题通常需要用到什么公式或方法？",
                    hint="回想一下我们学过的知识点",
                    is_final=False
                ),
                GuideStep(
                    step_number=3,
                    question="现在你能尝试列出解题的步骤吗？一步一步来，不用着急。",
                    hint="可以先写出需要用的公式，再代入数据",
                    is_final=False
                ),
                GuideStep(
                    step_number=4,
                    question="很好！现在请你计算出最终答案，并检查一下是否合理。",
                    hint="注意单位是否统一，计算是否正确",
                    is_final=True
                ),
            ]
        
        return steps
    
    def _parse_guide_steps(self, content: str, problem: Dict[str, Any]) -> List[GuideStep]:
        """解析 AI 生成的引导步骤"""
        # 简化解析，实际项目可用更完善的解析逻辑
        lines = content.split('\n')
        steps = []
        step_num = 1
        
        for line in lines:
            line = line.strip()
            if line and (line[0].isdigit() or '问题' in line or '步骤' in line):
                steps.append(GuideStep(
                    step_number=step_num,
                    question=line,
                    hint="如果需要帮助，可以点击查看提示",
                    is_final=(step_num >= 4)
                ))
                step_num += 1
        
        return steps if steps else self._generate_mock_steps(problem)
    
    def evaluate_answer(self, session: GuideSession, answer: str) -> Dict[str, Any]:
        """评估学生答案"""
        problem = PROBLEMS_DB.get(session.problem_id, {})
        
        if not settings.openai_api_key:
            return self._mock_evaluate(session, answer, problem)
        
        try:
            system_prompt = GUIDE_SYSTEM_PROMPT + "\n\n现在请评估学生的回答是否正确，并给出反馈。"
            user_prompt = f"""题目：{problem.get('content', '')}
当前步骤：{session.current_step + 1}/{len(session.steps)}
学生回答：{answer}

请评估：
1. 学生的回答是否正确
2. 如果不正确，指出问题所在并给出提示
3. 如果正确，给予鼓励并准备下一步"""
            
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.5,
                max_tokens=500
            )
            
            return {
                "feedback": response.choices[0].message.content,
                "is_correct": True,  # 简化处理
            }
            
        except Exception as e:
            print(f"AI 评估失败：{e}")
            return self._mock_evaluate(session, answer, problem)
    
    def _mock_evaluate(self, session: GuideSession, answer: str, problem: Dict[str, Any]) -> Dict[str, Any]:
        """模拟答案评估"""
        is_last_step = session.current_step >= len(session.steps) - 1
        
        if is_last_step:
            return {
                "feedback": f"很好！你已经完成了这道题的解答。{problem.get('key_knowledge', '')}",
                "is_correct": True,
            }
        else:
            return {
                "feedback": "回答得很好！让我们继续下一步。",
                "is_correct": True,
            }


GUIDE_SYSTEM_PROMPT = """你是一位经验丰富的中小学数学教师，擅长通过引导式提问帮助学生自主思考解题。
你的教学理念是：绝不直接给出答案，而是通过层层递进的问题引导学生自己发现解题思路。
请用亲切、耐心的语气与学生对话。"""
