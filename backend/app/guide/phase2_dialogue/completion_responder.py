import json

from langchain_core.output_parsers import StrOutputParser

from ..llm_provider import get_llm
from .dialogue_graph_state import DialogueGraphState
from .prompts import COMPLETION_RESPONDER_PROMPT
from .state_tracker import _serialize_dialogue

import logging

logger = logging.getLogger(__name__)


def run_completion_responder(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    题目完成后的收尾回复 Agent。

    在学生解决题目后，生成一段温暖的收尾回复，告知学生可以查看完整题解。
    不经过 Guardrail（题目已解决，无泄题风险）。

    Args:
        graph_state: 当前图状态，session_state.is_solved 必须为 True

    Returns:
        更新了 generated_question 字段的图状态
    """
    state = graph_state["session_state"]

    llm = get_llm()
    chain = COMPLETION_RESPONDER_PROMPT | llm | StrOutputParser()

    response = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
    })

    response = response.strip()
    logger.info("Completion Responder 生成收尾回复：%s", response[:50])

    state.dialogue_history.append({"role": "tutor", "content": response})

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],
        "generated_question": response,
        "rejection_reason": graph_state["rejection_reason"],
        "retry_count": graph_state["retry_count"],
        "teaching_guidance": graph_state["teaching_guidance"],
    }
