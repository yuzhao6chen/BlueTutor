from typing import List

from typing_extensions import TypedDict

from phase2_dialogue.session_state import SessionState


class DialogueGraphState(TypedDict):
    session_state: SessionState  # 完整会话状态
    student_input: str  # 当前轮次学生的输入
    generated_question: str  # 提问生成Agent的输出
    rejection_reason: List[str]  # 守门Agent的打回原因，合格时为[]
    retry_count: int  # 当前轮次的重试次数
    teaching_guidance: str # 情境分析Agent的输出，自然语言教学指导意见，初始为空字符串
