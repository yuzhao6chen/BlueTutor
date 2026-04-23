from langchain_core.output_parsers import JsonOutputParser

from ..llm_provider import get_llm
from .dialogue_graph_state import DialogueGraphState
from .prompts import GUARDRAIL_PROMPT
from .state_tracker import _serialize_dialogue

import logging

logger = logging.getLogger(__name__)

MAX_RETRY = 4  # 守门Agent最大打回次数
FALLBACK_QUESTION = "你觉得下一步应该怎么做呢？"  # 超过重试次数后的兜底问题


def run_guardrail(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    守门Agent节点函数。
    检查生成的引导问题是否泄题，将判断结果写入图状态。
    """

    state = graph_state["session_state"]
    question = graph_state["generated_question"]
    retry_count = graph_state["retry_count"]

    llm = get_llm()
    parser = JsonOutputParser()
    chain = GUARDRAIL_PROMPT | llm | parser

    result = chain.invoke({
        "answer": state.parsed_problem["answer"],
        "question": question,
        "previous_rejection": "\n".join(
            f"{i + 1}. {reason}"
            for i, reason in enumerate(graph_state["rejection_reason"])
        ) if graph_state["rejection_reason"] else "无",
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "teaching_guidance": graph_state["teaching_guidance"] if graph_state["teaching_guidance"] else "无"
    })

    # 测试
    # print(result)

    if result["passed"]:
        # 通过审核，清空打回原因和重试计数
        logger.info("Guardrail 审核通过，问题：%s", question[:50])

        return {
            "session_state": state,
            "student_input": graph_state["student_input"],
            "generated_question": question,
            "rejection_reason": [],
            "retry_count": 0,
            "teaching_guidance": graph_state["teaching_guidance"]
        }
    else:
        # 未通过审核，记录打回原因，递增重试计数
        new_retry_count = retry_count + 1
        # 测试
        # print(graph_state["rejection_reason"] + [
        #     result["reason"]] if new_retry_count < MAX_RETRY else [])

        # 已达最大重试次数，替换为兜底问题并清空 rejection_reason，让 should_retry 直接结束
        if retry_count >= MAX_RETRY:
            logger.warning("Guardrail 触发兜底，已达最大重试次数 %d", MAX_RETRY)

            return {
                "session_state": state,
                "student_input": graph_state["student_input"],
                "generated_question": FALLBACK_QUESTION,
                "rejection_reason": [],
                "retry_count": new_retry_count,
                "teaching_guidance": graph_state["teaching_guidance"]
            }

        logger.info("Guardrail 打回（第 %d 次），原因：%s", retry_count + 1, result.get("reason", ""))

        return {
            "session_state": state,
            "student_input": graph_state["student_input"],
            "generated_question": question,
            "rejection_reason": graph_state["rejection_reason"] + [result["reason"]],
            "retry_count": new_retry_count,
            "teaching_guidance": graph_state["teaching_guidance"]
        }


def should_retry(graph_state: "DialogueGraphState") -> str:
    """
    条件边函数：判断是否需要重试。
    rejection_reason 为空（通过审核或已触发兜底）则结束，否则重试。
    """
    if not graph_state["rejection_reason"]:
        return "end"
    return "question_generator"


if __name__ == '__main__':
    from phase2_dialogue.session_state import SessionState
    from phase2_dialogue.state_tracker import run_state_tracker
    from phase2_dialogue.situation_analyzer import run_situation_analyzer
    from phase2_dialogue.question_generator import run_question_generator

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
    initial_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "我不知道这道题从何入手。",
        "generated_question": "",
        "rejection_reason": [],
        "retry_count": 0,
        "teaching_guidance": ""
    }

    # 链式调用：state_tracker → situation_analyzer → question_generator → guardrail
    print("=== State Tracker ===")
    state1 = run_state_tracker(initial_graph_state)
    print(f"Thinking Tree: {state1['session_state'].thinking_tree}")
    print(f"Stuck Count: {state1['session_state'].stuck_count}\n")

    print("=== Situation Analyzer ===")
    state2 = run_situation_analyzer(state1)
    print(f"Teaching Guidance: {state2['teaching_guidance']}\n")

    print("=== Question Generator ===")
    state3 = run_question_generator(state2)
    print(f"Generated Question:\n{state3['generated_question']}\n")

    print("=== Guardrail ===")
    state4 = run_guardrail(state3)
    print(f"Generated Question: {state4['generated_question']}")
    print(f"Rejection Reason: {state4['rejection_reason']}")
    print(f"Retry Count: {state4['retry_count']}")
    print(f"Should Retry: {should_retry(state4)}")
