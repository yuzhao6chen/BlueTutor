from typing import Optional
from typing_extensions import TypedDict
from phase2_dialogue.session_state import SessionState


class DialogueGraphState(TypedDict):
    session_state: SessionState       # 完整会话状态
    student_input: str                # 当前轮次学生的输入
    generated_question: str           # 提问生成Agent的输出
    rejection_reason: Optional[str]   # 守门Agent的打回原因，合格时为None
    retry_count: int                  # 当前轮次的重试次数
