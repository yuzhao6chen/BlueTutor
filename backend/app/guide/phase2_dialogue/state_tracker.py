import os
import json
from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser

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

node_id：节点的唯一标识符。
- 思路分支节点命名规则：approach_1、approach_2，依次递增
- 步骤节点命名规则：approach_1_step_1、approach_1_step_2，依次递增
- 禁止重复使用已存在的node_id

content：节点的内容描述。
- 思路分支节点：必须写明思路名称和核心特征，如"假设法：假设全部是鸡，共60只脚"
- 步骤节点：用一句话描述该步骤的具体操作，如"计算多余的脚数：60-80=-20"

status：节点当前状态，只能是以下四种之一：
- correct：该步骤在数学上是正确的
- incorrect：该步骤存在错误，但学生尚未意识到或未修正
- stuck：学生当前卡在这里，无法继续推进
- abandoned：学生已明确放弃该思路，转而尝试其他方法

parent_id：父节点的node_id。
- 新建思路分支节点时，parent_id 为 null（直接挂在根节点下）
- 新建步骤节点时，parent_id 必须是其所属思路分支的node_id或上一步骤的node_id

error_type：仅在status为incorrect或stuck时填写，用自然语言描述错误的具体原因，如"混淆了周长和面积的计算公式"。其他情况填 null。

---

【重要规则】

1. 在新增节点前，必须先检查思维树中是否已存在语义相近的思路分支。若存在，优先在已有分支下新增步骤节点，而非创建新的思路分支。
2. 操作指令只有两种类型：add_node 和 update_node。
3. 每次只输出一条操作指令。
4. 如果学生的输入无法判断出任何有效的解题行为（如只是表达困惑或闲聊），输出 null。

---

请严格按照以下 JSON 格式输出，不要输出任何其他内容：

新增节点：
{{
    "action": "add_node",
    "node": {{
    "node_id": "...",
    "content": "...",
    "status": "...",
    "parent_id": "...",
    "error_type": "..."
    }}
}}

更新节点：
{{
    "action": "update_node",
    "node_id": "...",
    "status": "...",
    "error_type": "..."
}}

无有效行为：
null
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
        error_info = f" | 错误类型：{node.error_type}" if node.error_type else ""
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
            "error_type": node.error_type,
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


def run_state_tracker(state: SessionState, student_input: str) -> SessionState:
    """
    运行状态判断Agent，根据学生输入更新思维树。

    Args:
        state: 当前会话状态
        student_input: 学生的最新输入

    Returns:
        更新后的会话状态
    """
    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus",
        temperature=0.2  # 低温度，保证输出稳定
    )

    parser = JsonOutputParser()
    chain = STATE_TRACKER_PROMPT | llm | parser

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "student_input": student_input
    })

    # 根据Agent输出的指令更新思维树
    if result is None:
        return state

    action = result.get("action")

    if action == "add_node":
        node_data = result["node"]
        new_node = ThinkingNode(
            node_id=node_data["node_id"],
            content=node_data["content"],
            status=NodeStatus(node_data["status"]),
            parent_id=node_data.get("parent_id"),
            error_type=node_data.get("error_type")
        )
        state.add_node(new_node)

        # 如果新节点是卡点，更新stuck信息
        if new_node.status == NodeStatus.STUCK:
            if state.current_stuck_node_id == new_node.node_id:
                state.stuck_count += 1
            else:
                state.current_stuck_node_id = new_node.node_id
                state.stuck_count = 1

    elif action == "update_node":
        state.update_node(
            node_id=result["node_id"],
            status=NodeStatus(result["status"]),
            error_type=result.get("error_type")
        )

        # 如果卡点被解决，重置stuck信息
        if result["status"] == NodeStatus.CORRECT.value:
            if state.current_stuck_node_id == result["node_id"]:
                state.current_stuck_node_id = None
                state.stuck_count = 0

    return state
