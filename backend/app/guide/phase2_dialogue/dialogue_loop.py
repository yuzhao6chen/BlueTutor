from langgraph.graph import StateGraph, END

from .dialogue_graph_state import DialogueGraphState
from .state_tracker import run_state_tracker
from .situation_analyzer import run_situation_analyzer
from .question_generator import run_question_generator
from .guardrail import run_guardrail, should_retry


def create_dialogue_graph():
    """构建对话循环的 LangGraph 工作流"""
    workflow = StateGraph(DialogueGraphState)

    # 添加三个 Agent 节点
    workflow.add_node("state_tracker", run_state_tracker)
    workflow.add_node("situation_analyzer", run_situation_analyzer)
    workflow.add_node("question_generator", run_question_generator)
    workflow.add_node("guardrail", run_guardrail)

    # 设置入口节点
    workflow.set_entry_point("state_tracker")

    # 固定边：state_tracker → situation_analyzer → question_generator → guardrail
    workflow.add_edge("state_tracker", "situation_analyzer")
    workflow.add_edge("situation_analyzer", "question_generator")
    workflow.add_edge("question_generator", "guardrail")

    # 条件边：guardrail 根据审核结果决定走向
    workflow.add_conditional_edges(
        "guardrail",
        should_retry,
        {
            "question_generator": "question_generator",  # 打回重写
            "end": END  # 通过，本轮结束
        }
    )

    return workflow.compile()
