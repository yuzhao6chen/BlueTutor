import json

from langchain_core.output_parsers import JsonOutputParser

from ..llm_provider import get_llm
from .prompts import REPORT_GENERATOR_PROMPT
from .session_state import SessionState
from .state_tracker import _serialize_tree, _serialize_dialogue

import logging

logger = logging.getLogger(__name__)


def generate_report(session_state: SessionState) -> dict:
    """
    接收完整的 SessionState，生成结构化讲题报告。

    Args:
        session_state: 对话结束后的完整会话状态

    Returns:
        结构化报告字典，可直接 json.dumps

    Raises:
        Exception: LLM 调用失败时直接抛出异常
    """
    llm = get_llm()
    parser = JsonOutputParser()
    chain = REPORT_GENERATOR_PROMPT | llm | parser

    llm_result = chain.invoke({
        "parsed_problem": json.dumps(session_state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(session_state.thinking_tree),
        "dialogue_history": _serialize_dialogue(session_state.dialogue_history),
    })

    # 直接从思维树提取思维链记录（按插入顺序，即时间顺序）
    thinking_chain = [
        {
            "node_id": node.node_id,
            "content": node.content,
            "status": node.status.value,
            "parent_id": node.parent_id,
            "error_history": node.error_history
        }
        for node in session_state.thinking_tree.values()
    ]

    # 组装完整报告
    report = {
        "problem": {
            "raw_problem": session_state.raw_problem,
            **session_state.parsed_problem
        },
        "knowledge_tags": llm_result["knowledge_tags"],
        "thinking_chain": thinking_chain,
        "error_profile": llm_result["error_profile"],
        "independence_evaluation": llm_result["independence_evaluation"],
        "solution": session_state.solution  # 题解文本，未生成时为 None
    }

    logger.info("讲题报告生成完成，知识点标签：%s", llm_result.get("knowledge_tags", []))

    return report


if __name__ == "__main__":
    from phase2_dialogue.session_state import ThinkingNode, NodeStatus

    # 构造一个模拟的对话结束状态，用于测试
    test_state = SessionState(
        parsed_problem={
            "known_conditions": ["鸡和兔共有30个头", "鸡和兔共有80只脚", "鸡有2只脚", "兔有4只脚"],
            "goal": "求鸡的只数和兔的只数",
            "answer": "鸡20只，兔10只"
        },
        raw_problem="鸡和兔共有30个头，80只脚，求鸡和兔各有多少只？",
        thinking_tree={}
    )

    # 手动构造思维树（模拟学生经过引导后完成解题）
    test_state.thinking_tree["n0"].status = NodeStatus.CORRECT
    n1 = ThinkingNode(
        node_id="n1",
        content="假设全部是鸡，共60只脚",
        status=NodeStatus.CORRECT,
        parent_id="n0"
    )
    n2 = ThinkingNode(
        node_id="n2",
        content="实际有80只脚，多出20只脚",
        status=NodeStatus.INCORRECT,
        parent_id="n1",
        error_history=["计算失误"]
    )
    n3 = ThinkingNode(
        node_id="n3",
        content="实际比假设少20只脚",
        status=NodeStatus.CORRECT,
        parent_id="n1"
    )
    n4 = ThinkingNode(
        node_id="n4",
        content="每把一只鸡换成兔，脚数增加2，共换10只，兔有10只，鸡有20只",
        status=NodeStatus.CORRECT,
        parent_id="n3"
    )
    for node in [n1, n2, n3, n4]:
        test_state.thinking_tree[node.node_id] = node

    test_state.dialogue_history = [
        {"role": "student", "content": "我不知道怎么做"},
        {"role": "tutor", "content": "如果全部都是鸡，会有多少只脚呢？"},
        {"role": "student", "content": "30只鸡，60只脚"},
        {"role": "tutor", "content": "实际有80只脚，和你算的60只相比，多了还是少了？"},
        {"role": "student", "content": "我算错了，应该是少了20只脚"},
        {"role": "tutor", "content": "很好！每把一只鸡换成兔，脚数会怎么变化？"},
        {"role": "student", "content": "增加2只脚，少了20只脚就要换10只，所以兔有10只，鸡有20只"},
    ]

    print("正在生成讲题报告...")
    report = generate_report(test_state)
    print(json.dumps(report, ensure_ascii=False, indent=2))
