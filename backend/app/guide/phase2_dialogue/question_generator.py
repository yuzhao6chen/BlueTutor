import json

from langchain_core.output_parsers import StrOutputParser

from ..llm_provider import get_llm
from .dialogue_graph_state import DialogueGraphState
from .prompts import QUESTION_GENERATOR_PROMPT

import logging

logger = logging.getLogger(__name__)


def run_question_generator(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    提问生成Agent节点函数。
    根据当前状态生成苏格拉底式引导问题，返回新的图状态。
    """
    from .state_tracker import _serialize_tree, _serialize_dialogue

    state = graph_state["session_state"]

    llm = get_llm()

    chain = QUESTION_GENERATOR_PROMPT | llm | StrOutputParser()

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "stuck_count": state.stuck_count,
        "last_updated_node_id": state.last_updated_node_id if state.last_updated_node_id else "无",
        "rejection_reason": "\n".join(
            f"{i + 1}. {reason}"
            for i, reason in enumerate(graph_state["rejection_reason"])
        ) if graph_state["rejection_reason"] else "无",
        "teaching_guidance": graph_state["teaching_guidance"] if graph_state["teaching_guidance"] else "无"
    })

    logger.debug("Question Generator 完成，生成问题：%s", result.strip()[:50])

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],  # 保持不变
        "generated_question": result.strip(),
        "rejection_reason": graph_state["rejection_reason"],  # 保持不变
        "retry_count": graph_state["retry_count"],  # 保持不变
        "teaching_guidance": graph_state["teaching_guidance"]  # 保持不变
    }


if __name__ == '__main__':
    from phase2_dialogue.session_state import SessionState
    from phase2_dialogue.state_tracker import run_state_tracker
    from phase2_dialogue.situation_analyzer import run_situation_analyzer

    # 创建初始 SessionState（思维树为空，模拟学生刚开始作答）
    test_session_state = SessionState(
        parsed_problem={
            'known_conditions': ['鸡和兔共有30个头', '鸡和兔共有80只脚', '鸡有2只脚', '兔有4只脚'],
            'goal': '求鸡的只数和兔的只数',
            'answer': '鸡20只，兔10只'
        },
        thinking_tree={},
        dialogue_history=[],
    )

    # 初始化图状态
    test_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "我不知道这道题从何入手。",
        "generated_question": "",
        "rejection_reason": [],
        "retry_count": 0,
        "teaching_guidance": ""
    }

    # 第一步：调用 state_tracker
    state_after_tracker = run_state_tracker(test_graph_state)
    print("=== State Tracker Output ===")
    print(f"Thinking Tree: {state_after_tracker['session_state'].thinking_tree}")
    print(f"Stuck Count: {state_after_tracker['session_state'].stuck_count}\n")

    # 第二步：调用 situation_analyzer
    state_after_analyzer = run_situation_analyzer(state_after_tracker)
    print("=== Situation Analyzer Output ===")
    print(f"Teaching Guidance: {state_after_analyzer['teaching_guidance']}\n")

    # 第三步：调用 question_generator
    final_state = run_question_generator(state_after_analyzer)
    print("=== Question Generator Output ===")
    print(f"Generated Question:\n{final_state['generated_question']}")
