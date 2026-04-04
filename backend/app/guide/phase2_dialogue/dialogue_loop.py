from langgraph.graph import StateGraph, END

from phase2_dialogue.dialogue_graph_state import DialogueGraphState
from phase2_dialogue.state_tracker import run_state_tracker
from phase2_dialogue.question_generator import run_question_generator
from phase2_dialogue.guardrail import run_guardrail, should_retry
from phase2_dialogue.session_state import SessionState
from phase1_parser.parser import parse_problem


def create_dialogue_graph():
    """构建对话循环的 LangGraph 工作流"""
    workflow = StateGraph(DialogueGraphState)

    # 添加三个 Agent 节点
    workflow.add_node("state_tracker", run_state_tracker)
    workflow.add_node("question_generator", run_question_generator)
    workflow.add_node("guardrail", run_guardrail)

    # 设置入口节点
    workflow.set_entry_point("state_tracker")

    # 固定边：state_tracker → question_generator → guardrail
    workflow.add_edge("state_tracker", "question_generator")
    workflow.add_edge("question_generator", "guardrail")

    # 条件边：guardrail 根据审核结果决定走向
    workflow.add_conditional_edges(
        "guardrail",
        should_retry,
        {
            "question_generator": "question_generator",  # 打回重写
            "end": END                                    # 通过，本轮结束
        }
    )

    return workflow.compile()


def start_session(problem_text: str) -> tuple[SessionState, object]:
    """
    开始一次新的讲题会话。
    调用第一阶段解析题目，初始化会话状态，返回会话状态和对话图。

    Args:
        problem_text: 学生提交的原始题目文本

    Returns:
        (初始化好的 SessionState, 编译好的对话图)
    """
    parsed = parse_problem(problem_text)
    state = SessionState(parsed_problem=parsed)
    graph = create_dialogue_graph()
    return state, graph


def run_one_turn(graph, state: SessionState, student_input: str) -> tuple[SessionState, str]:
    """
    执行对话循环的一轮。

    Args:
        graph: 编译好的对话图
        state: 当前会话状态
        student_input: 学生的输入

    Returns:
        (更新后的 SessionState, 发送给学生的引导问题)
    """
    graph_state = DialogueGraphState(
        session_state=state,
        student_input=student_input,
        generated_question="",
        rejection_reason=None,
        retry_count=0
    )

    result = graph.invoke(graph_state)

    # 将老师的问题记录到对话历史
    final_question = result["generated_question"]
    result["session_state"].dialogue_history.append(
        {"role": "老师", "content": final_question}
    )

    return result["session_state"], final_question


if __name__ == "__main__":
    print("=== 苏格拉底讲题模块 - 第二阶段测试 ===\n")
    problem = input("请输入题目：\n>>> ")

    print("\n正在解析题目...")
    state, graph = start_session(problem)
    print("题目解析完成，开始对话。\n")

    while True:
        student_input = input("学生：")
        if student_input.strip().lower() == "quit":
            print("讲题结束")
            break

        state, question = run_one_turn(graph, state, student_input)
        print(f"老师：{question}\n")
