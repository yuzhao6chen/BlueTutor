from langgraph.graph import StateGraph, END

from phase1_parser.parser import parse_problem
from phase2_dialogue.dialogue_graph_state import DialogueGraphState
from phase2_dialogue.session_state import SessionState

from phase2_dialogue.state_tracker import run_state_tracker
from phase2_dialogue.situation_analyzer import run_situation_analyzer
from phase2_dialogue.question_generator import run_question_generator
from phase2_dialogue.guardrail import run_guardrail, should_retry



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
        rejection_reason=[],
        retry_count=0,
        teaching_guidance=""
    )

    result = graph.invoke(graph_state)

    # 将老师的问题记录到对话历史
    final_question = result["generated_question"]
    result["session_state"].dialogue_history.append(
        {"role": "tutor", "content": final_question}
    )

    return result["session_state"], final_question


if __name__ == "__main__":
    from phase2_dialogue.state_tracker import _serialize_tree


    def _print_tree_outline(thinking_tree: dict) -> str:
        """只提取 _serialize_tree 输出中的缩进大纲部分（去掉 JSON 结构）"""
        full = _serialize_tree(thinking_tree)
        # 截取【可读结构】到【JSON结构】之间的内容
        lines = full.split("\n")
        outline_lines = []
        for line in lines:
            if line.startswith("【JSON结构"):
                break
            outline_lines.append(line)
        return "\n".join(outline_lines).strip()

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

        # 手动逐步调用各节点，以便观察中间结果
        from phase2_dialogue.state_tracker import run_state_tracker
        from phase2_dialogue.situation_analyzer import run_situation_analyzer
        from phase2_dialogue.question_generator import run_question_generator
        from phase2_dialogue.guardrail import run_guardrail, should_retry

        graph_state = {
            "session_state": state,
            "student_input": student_input,
            "generated_question": "",
            "rejection_reason": [],
            "retry_count": 0,
            "teaching_guidance": ""
        }

        # --- State Tracker ---
        graph_state = run_state_tracker(graph_state)
        print("\n【State Tracker】")
        print(f"  思维树：\n{_print_tree_outline(graph_state['session_state'].thinking_tree)}")
        print(f"  当前卡点：{graph_state['session_state'].current_stuck_node_id}")
        print(f"  Stuck Count：{graph_state['session_state'].stuck_count}")
        print(f"  最新操作节点：{graph_state['session_state'].last_updated_node_id}")

        # --- Situation Analyzer ---
        graph_state = run_situation_analyzer(graph_state)
        print("\n【Situation Analyzer】")
        print(f"  教学建议：{graph_state['teaching_guidance']}")

        # --- Question Generator + Guardrail 循环（含重试）---
        retry_round = 0
        while True:
            graph_state = run_question_generator(graph_state)
            print(f"\n【Question Generator】（第 {retry_round + 1} 次生成）")
            print(f"  生成问题：{graph_state['generated_question']}")

            graph_state = run_guardrail(graph_state)
            print(f"\n【Guardrail】（第 {retry_round + 1} 次审核）")
            print(f"  审核结果：{'通过 ✅' if not graph_state['rejection_reason'] else '打回 ❌'}")
            if graph_state['rejection_reason']:
                print(f"  打回原因：{graph_state['rejection_reason'][-1]}")

            if should_retry(graph_state) == "end":
                break
            retry_round += 1

        # 将老师的问题记录到对话历史
        final_question = graph_state["generated_question"]
        graph_state["session_state"].dialogue_history.append(
            {"role": "tutor", "content": final_question}
        )
        state = graph_state["session_state"]

        print(f"\n老师：{final_question}\n")
        print("-" * 50)
