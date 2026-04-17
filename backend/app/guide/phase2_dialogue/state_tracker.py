import json
from typing import Optional

from langchain_core.output_parsers import JsonOutputParser

from ..llm_provider import get_llm
from .dialogue_graph_state import DialogueGraphState
from .prompts import STATE_TRACKER_PROMPT
from .session_state import SessionState, ThinkingNode, NodeStatus

import logging

logger = logging.getLogger(__name__)


def _serialize_tree(thinking_tree: dict) -> str:
    """将思维树序列化为缩进大纲 + JSON 双格式，提升模型理解准确性"""
    if not thinking_tree:
        return "（思维树为空，学生尚未开始作答）"

    # 找出所有根节点（parent_id 为 None 的节点）
    root_nodes = [n for n in thinking_tree.values() if n.parent_id is None]

    outline_lines = ["【可读结构】"]

    def build_outline(node_id: str, indent: int = 0):
        node = thinking_tree[node_id]
        prefix = "  " * indent + "└─ " if indent > 0 else ""
        error_info = f" | 错误历史：{node.error_history}" if node.error_history else ""
        outline_lines.append(
            f"{prefix}[{node_id}] {node.content} ({node.status.value}){error_info}"
        )
        for child_id in node.children:
            build_outline(child_id, indent + 1)

    for root_node in root_nodes:
        build_outline(root_node.node_id)

    # 附上 JSON 结构供模型操作 node_id
    json_data = {}
    for node_id, node in thinking_tree.items():
        json_data[node_id] = {
            "content": node.content,
            "status": node.status.value,
            "parent_id": node.parent_id,
            "error_history": node.error_history,
            "children": node.children
        }

    outline_lines.append("\n【JSON结构（用于引用 node_id）】")
    outline_lines.append(json.dumps(json_data, ensure_ascii=False, indent=2))

    return "\n".join(outline_lines)


def _serialize_dialogue(dialogue_history: list) -> str:
    """将对话历史格式化为可读字符串"""
    if not dialogue_history:
        return "（暂无对话记录）"
    lines = []
    for turn in dialogue_history:
        role = "学生" if turn["role"] == "student" else "老师"
        lines.append(f"{role}：{turn['content']}")
    return "\n".join(lines)


def run_state_tracker(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    状态判断Agent节点函数。
    读取学生输入，更新思维树，返回新的图状态。
    """

    state = graph_state["session_state"]
    student_input = graph_state["student_input"]

    llm = get_llm()
    parser = JsonOutputParser()
    chain = STATE_TRACKER_PROMPT | llm | parser

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "student_input": student_input
    })

    # result 是一个操作指令列表（可能为空列表或 None）
    if not result:
        state.dialogue_history.append({"role": "student", "content": student_input})
        return {
            "session_state": state,
            "student_input": graph_state["student_input"],
            "generated_question": graph_state["generated_question"],
            "rejection_reason": graph_state["rejection_reason"],
            "retry_count": graph_state["retry_count"],
            "teaching_guidance": graph_state["teaching_guidance"]
        }

    # 记录本轮最后一个新增节点的 ID，用于解析 __prev__ 占位符
    last_added_node_id: Optional[str] = None

    for instruction in result:
        action = instruction.get("action")

        if action == "add_node":
            node_data = instruction["node"]

            # 处理 parent_id 中的 __prev__ 占位符
            parent_id = node_data.get("parent_id")
            if parent_id == "__prev__":
                if last_added_node_id is not None:
                    # 本轮已有前驱节点，挂在前驱节点下（原有逻辑，正确）
                    parent_id = last_added_node_id
                else:
                    # 本轮第一条指令误用了 __prev__，回退到上一轮最后操作的节点
                    parent_id = state.last_updated_node_id

            # 由代码自动生成 node_id
            new_node_id = state.next_node_id()

            new_node = ThinkingNode(
                node_id=new_node_id,
                content=node_data["content"],
                status=NodeStatus(node_data["status"]),
                parent_id=parent_id,
                error_history=[node_data["error_type"]] if node_data.get("error_type") else []
            )
            state.add_node(new_node)
            state.last_updated_node_id = new_node_id
            last_added_node_id = new_node_id

            # 新节点为卡点时，更新 stuck 信息
            if new_node.status == NodeStatus.STUCK:
                state.current_stuck_node_id = new_node_id
                state.stuck_count = 1

        elif action == "mark_stuck":
            node_id = instruction["node_id"]
            error_type = instruction.get("error_type")
            if error_type:
                state.append_error(node_id, error_type)
            state.update_node_status(node_id, NodeStatus.STUCK)
            state.last_updated_node_id = node_id

            if state.current_stuck_node_id == node_id:
                state.stuck_count += 1
            else:
                state.current_stuck_node_id = node_id
                state.stuck_count = 1

        elif action == "mark_correct":
            node_id = instruction["node_id"]
            state.update_node_status(node_id, NodeStatus.CORRECT)
            state.last_updated_node_id = node_id

            if state.current_stuck_node_id == node_id:
                state.current_stuck_node_id = None
                state.stuck_count = 0

        elif action == "mark_abandoned":
            node_id = instruction["node_id"]
            state.update_node_status(node_id, NodeStatus.ABANDONED)
            state.last_updated_node_id = node_id

        elif action == "mark_solved":
            state.is_solved = True
            logger.info("State Tracker：题目已解决，标记 is_solved = True")

    # 将学生输入记录到对话历史
    state.dialogue_history.append({"role": "student", "content": student_input})

    logger.debug("State Tracker 完成，最新操作节点：%s，stuck_count：%d",
                 state.last_updated_node_id, state.stuck_count)

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],
        "generated_question": graph_state["generated_question"],
        "rejection_reason": graph_state["rejection_reason"],
        "retry_count": graph_state["retry_count"],
        "teaching_guidance": graph_state["teaching_guidance"]
    }


if __name__ == '__main__':
    # 重构后的测试用例
    from phase2_dialogue.dialogue_graph_state import DialogueGraphState

    # 创建测试用的SessionState
    test_session_state = SessionState(
        parsed_problem={
            'known_conditions': ['鸡和兔共有30个头', '鸡和兔共有80只脚', '鸡有2只脚', '兔有4只脚'],
            'goal': '求鸡的只数和兔的只数',
            'answer': '鸡20只，兔10只'
        },
        thinking_tree={},
        dialogue_history=[],
    )

    # 创建测试用的DialogueGraphState
    test_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "我不知道这道题从何入手。",
        "generated_question": "",  # 初始化为空
        "rejection_reason": [],  # 初始化为[]
        "retry_count": 0  # 初始化为0
    }

    # 调用状态跟踪器
    updated_graph_state = run_state_tracker(test_graph_state)

    # 提取更新后的session_state
    updated_session_state = updated_graph_state["session_state"]

    # 打印更新后的session_state（用于调试）
    print("Updated Session State:")
    print(f"Thinking Tree: {updated_session_state.thinking_tree}")
    print(f"Dialogue History: {updated_session_state.dialogue_history}")
    print(f"Current Stuck Node ID: {updated_session_state.current_stuck_node_id}")
    print(f"Stuck Count: {updated_session_state.stuck_count}")
    print(f"Last Updated Node ID: {updated_session_state.last_updated_node_id}")

    # 打印思维树的序列化表示
    print("\nSerialized Thinking Tree:")
    print(_serialize_tree(updated_session_state.thinking_tree))
