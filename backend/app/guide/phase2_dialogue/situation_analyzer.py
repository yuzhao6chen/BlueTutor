import json

from langchain_core.output_parsers import StrOutputParser

from llm_provider import get_llm
from phase2_dialogue.dialogue_graph_state import DialogueGraphState
from phase2_dialogue.prompts import SITUATION_ANALYZER_PROMPT

import logging

logger = logging.getLogger(__name__)


def run_situation_analyzer(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    情境分析Agent节点函数。
    分析学生的解题状态与情绪状态，生成教学指导意见，写入图状态。
    """
    from phase2_dialogue.state_tracker import _serialize_tree, _serialize_dialogue

    state = graph_state["session_state"]

    llm = get_llm()

    chain = SITUATION_ANALYZER_PROMPT | llm | StrOutputParser()

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "stuck_count": state.stuck_count,
    })

    logger.debug("Situation Analyzer 完成，教学建议：%s", result.strip()[:50])

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],
        "generated_question": graph_state["generated_question"],
        "rejection_reason": graph_state["rejection_reason"],
        "retry_count": graph_state["retry_count"],
        "teaching_guidance": result.strip()
    }


if __name__ == '__main__':
    from phase2_dialogue.session_state import SessionState, ThinkingNode, NodeStatus

    test_session_state = SessionState(
        parsed_problem={
            'known_conditions': ['鸡和兔共有30个头', '鸡和兔共有80只脚', '鸡有2只脚', '兔有4只脚'],
            'goal': '求鸡的只数和兔的只数',
            'answer': '鸡20只，兔10只'
        },
        thinking_tree={
            "a1": ThinkingNode(
                node_id="a1",
                content="假设法：假设全部是鸡，共60只脚",
                status=NodeStatus.STUCK,
                parent_id=None,
                error_history=["学生表示不知从何入手，尚未形成具体步骤"]
            )
        },
        dialogue_history=[
            {"role": "学生", "content": "我完全不知道怎么做，感觉自己好笨……"}
        ],
        current_stuck_node_id="a1",
        stuck_count=2,
        last_updated_node_id="a1"
    )

    test_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "我完全不知道怎么做，感觉自己好笨……",
        "generated_question": "",
        "rejection_reason": [],
        "retry_count": 0,
        "teaching_guidance": ""
    }

    result_state = run_situation_analyzer(test_graph_state)
    print("Teaching Guidance:")
    print(result_state["teaching_guidance"])
