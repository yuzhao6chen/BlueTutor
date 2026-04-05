import json
import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate

from phase2_dialogue.dialogue_graph_state import DialogueGraphState
from phase2_dialogue.session_state import SessionState, ThinkingNode, NodeStatus

load_dotenv()

STATE_TRACKER_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位专业的数学教育分析师，负责追踪学生的解题思维过程。

    你会收到以下信息：
    1. 题目信息（已知条件、求解目标、标准答案）
    2. 当前思维树（包含两部分：可读结构大纲用于理解层级关系，JSON结构用于操作node_id）
    3. 完整对话历史
    4. 学生的最新输入

    你的任务是分析学生的最新输入，输出一条操作指令来更新思维树。

    ---

    【字段说明】
    
    node_id：节点的唯一标识符，采用路径式命名，反映节点在树中的位置。
    - 顶层思路分支：a1、a2，依次递增
    - 某思路下的步骤：a1_s1、a1_s2，依次递增
    - 某步骤下的分叉：a1_s2_b1、a1_s2_b2，依次递增
    - 分叉下的后续步骤：a1_s2_b1_s1，依次类推
    - 命名时必须根据 parent_id 推断当前节点所处的层级，确保命名与树结构一致
    - 禁止重复使用已存在的 node_id

    content：节点的内容描述。
    - 思路分支节点：必须写明思路名称和核心特征，如"假设法：假设全部是鸡，共60只脚"
    - 步骤节点：用一句话描述该步骤的具体操作，如"计算多余的脚数：60-80=-20"

    status：节点当前状态，只能是以下四种之一：
    - correct：该步骤在数学上是正确的
    - incorrect：该步骤存在错误
    - stuck：学生当前卡在这里，无法继续推进
    - abandoned：学生已明确放弃该思路

    parent_id：父节点的node_id。
    - 新建思路分支节点时，parent_id 为 null（直接挂在根节点下）
    - 新建步骤节点时，parent_id 为该步骤在逻辑上的前驱节点的node_id，可以是思路分支节点，也可以是任意步骤节点
    - 若学生从某个中间步骤出发尝试了不同的后续方法，应在该步骤节点下新建分支，而非创建新的顶层思路分支

    error_type：用自然语言描述错误的具体原因，如"混淆了周长和面积的计算公式"。

    ---

    【操作指令类型】

    共四种，每次只输出一条，不要输出任何其他内容：

    1. 新增节点（学生推进了新步骤，或切换了解题思路）：
    {{
      "action": "add_node",
      "node": {{
        "node_id": "...",
        "content": "...",
        "status": "correct 或 incorrect 或 stuck",
        "parent_id": "... 或 null",
        "error_type": "... 或 null"
      }}
    }}

    2. 标记卡点（学生在已有节点上再次卡住，stuck_count 将递增）：
    {{
      "action": "mark_stuck",
      "node_id": "...",
      "error_type": "..."
    }}

    3. 标记正确（学生修正了某个节点的错误）：
    {{
      "action": "mark_correct",
      "node_id": "..."
    }}

    4. 标记放弃（学生明确放弃了从某个节点开始的这条路）：
    {{
      "action": "mark_abandoned",
      "node_id": "..."
    }}
    注意：mark_abandoned 可作用于任意节点，表示从该节点开始的整条路径被放弃，不限于顶层思路分支。

    5. 无有效行为（学生只是表达困惑或闲聊，无法判断出任何解题行为）：
    null

    ---

    【重要规则】
    1. 在新增节点前，必须先检查思维树中是否已存在语义相近的思路分支。若存在，优先在已有分支下新增步骤节点，而非创建新的思路分支。
    2. mark_stuck 只用于学生在已有节点上再次卡住的情况，不用于新节点。
    3. mark_abandoned 只作用于思路分支节点，不作用于步骤节点。
    4. 节点 content 的内容必须来源于学生的实际表述。若学生表示完全没有思路，允许根据题目特征推断一个合理的起点思路新建节点，但必须在 content 开头加上"【引导方向】"前缀，例如："【引导方向】假设法：假设全部是鸡，共60只脚"，以区别于学生主动表述的步骤。
    """),
    ("human", """【题目信息】
{parsed_problem}

【当前思维树】
{thinking_tree}

【对话历史】
{dialogue_history}

【学生最新输入】
{student_input}
""")
])


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

        # 根据node_id判断节点类型
        if "_" not in node_id:
            node_type = "【思路分支】"
        elif node_id.split("_")[-1].startswith("b"):
            node_type = "【分叉】"
        else:
            node_type = "【步骤】"

        error_info = f" | 错误历史：{node.error_history}" if node.error_history else ""
        outline_lines.append(
            f"{prefix}{node_type}[{node_id}] {node.content} ({node.status.value}){error_info}"
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

    outline_lines.append("\n【JSON结构（用于操作node_id）】")
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

    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus"
    )

    parser = JsonOutputParser()
    chain = STATE_TRACKER_PROMPT | llm | parser

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "student_input": student_input
    })

    if result is not None:
        action = result.get("action")

        if action == "add_node":
            node_data = result["node"]
            new_node = ThinkingNode(
                node_id=node_data["node_id"],
                content=node_data["content"],
                status=NodeStatus(node_data["status"]),
                parent_id=node_data.get("parent_id"),
                error_history=[node_data["error_type"]] if node_data.get("error_type") else []
            )
            state.add_node(new_node)
            state.last_updated_node_id = new_node.node_id

            # 新节点为卡点时，重置stuck信息（新卡点，从1开始）
            if new_node.status == NodeStatus.STUCK:
                state.current_stuck_node_id = new_node.node_id
                state.stuck_count = 1

        elif action == "mark_stuck":
            # 学生在已有节点上再次卡住，追加错误记录并递增stuck_count
            node_id = result["node_id"]
            error_type = result.get("error_type")
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
            node_id = result["node_id"]
            state.update_node_status(node_id, NodeStatus.CORRECT)
            state.last_updated_node_id = node_id

            # 卡点被解决，重置stuck信息
            if state.current_stuck_node_id == node_id:
                state.current_stuck_node_id = None
                state.stuck_count = 0

        elif action == "mark_abandoned":
            node_id = result["node_id"]
            state.update_node_status(node_id, NodeStatus.ABANDONED)
            state.last_updated_node_id = node_id

    # 将学生输入记录到对话历史
    state.dialogue_history.append({"role": "学生", "content": student_input})

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],  # 保持不变
        "generated_question": graph_state["generated_question"],  # 保持不变
        "rejection_reason": graph_state["rejection_reason"],  # 保持不变
        "retry_count": graph_state["retry_count"],  # 保持不变
        "teaching_guidance": graph_state["teaching_guidance"] # 保持不变
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
