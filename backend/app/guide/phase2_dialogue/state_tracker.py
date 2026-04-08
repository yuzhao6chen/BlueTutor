import json
import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate
from typing import Optional

from phase2_dialogue.dialogue_graph_state import DialogueGraphState
from phase2_dialogue.session_state import SessionState, ThinkingNode, NodeStatus

load_dotenv()

STATE_TRACKER_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位专业的数学教育分析师，负责追踪学生的解题思维过程。

    你会收到以下信息：
    1. 题目信息（已知条件、求解目标、标准答案）
    2. 当前思维树（包含两部分：可读结构大纲用于理解层级关系，JSON结构用于引用 node_id）
    3. 完整对话历史
    4. 学生的最新输入

    你的任务是分析学生的最新输入，输出一个操作指令列表来更新思维树。

    ---

    【思维树结构说明】

    思维树是一棵 n 叉树，每个节点代表学生解题过程中的一个认知步骤。
    任何节点都可以有任意数量的子节点（分叉），分叉表示学生从同一步骤出发尝试了不同的后续方向。
    树的结构完全由 parent_id 字段决定，与 node_id 的数值无关。

    ---

    【node_id 规则】

    node_id 是节点的唯一标识符，格式为 n1、n2、n3，由系统自动分配，全局自增。
    - 在 add_node 操作中，node_id 字段留空字符串 ""，系统会自动填入。
    - 在 mark_stuck、mark_correct、mark_abandoned 操作中，node_id 必须填写思维树中已存在的节点 ID（从 JSON 结构中查找）。

    ---

    【字段说明】

    content：用一句话描述该步骤的具体内容，必须来源于学生的实际表述。


    status：节点当前状态，只能是以下四种之一：
    - correct：该步骤在数学上是正确的
    - incorrect：该步骤存在错误
    - stuck：学生当前卡在这里，无法继续推进
    - abandoned：学生已明确放弃从该节点开始的这条路


    parent_id：父节点的 node_id，决定该步骤在思维树中的位置。

    判断 parent_id 的核心规则：
    - 若该步骤是解题的起点（无前驱步骤），parent_id 为 null
    - 若该步骤是对某个已有步骤的继续推进（使用了前一步的结论、或是对前一步某个量的进一步说明），parent_id 为该前驱步骤的 node_id
    - 若学生从同一个步骤出发，明确尝试了两条完全不同的方向（如放弃一条路后重新尝试），两个新节点共享同一 parent_id（真正的分叉）
    - 禁止跳层挂载：新节点的 parent_id 必须是其直接前驱步骤，不得跳过中间节点

    区分"顺序推进"和"平行分叉"的判断标准：
    - 顺序推进：第二步在逻辑上依赖第一步的结论，或是对第一步中某个量/概念的进一步展开（如"其中……"、"因此……"、"所以……"）。此时第二步的 parent_id 应为第一步（若在同一轮新增，填 "__prev__"）。
    - 平行分叉：两步是对同一问题的两种独立尝试，互不依赖，或学生明确切换了方向（如"换一种方法"、"或者"）。此时两步共享同一 parent_id。


    error_type：用简短的短语描述错误类型，避免写入推理过程。status 为 incorrect 或 stuck 时必填，其余情况填 null。

    ---

    【操作指令类型】

    共四种，每次输出一个 JSON 数组，数组中可包含多条指令（按执行顺序排列）：

    1. 新增节点：
    {{
      "action": "add_node",
      "node": {{
        "node_id": "",
        "content": "...",
        "status": "correct 或 incorrect 或 stuck",
        "parent_id": "已有节点的 node_id，或 null，或 __prev__",
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

    若学生只是表达困惑或闲聊，无法判断出任何解题行为，输出空数组：[]

    ---

    【重要规则】

    1. 每次输出必须是一个 JSON 数组，即使只有一条指令，也要用 [] 包裹。

    2. 在新增节点前，先检查思维树中是否已存在语义相近的节点。若存在，优先在已有节点下新增子节点，而非重新创建起点节点。

    3. 若学生在一次回答中完成了多个逻辑步骤，拆分为多条 add_node 指令，每条描述一个独立的推导步骤。
       若后一条是前一条的顺序推进，parent_id 填写 "__prev__"，系统会自动替换为上一条新增节点的实际 ID。
       若后一条与前一条是平行分叉，parent_id 填写它们共同的父节点 ID。
       拆分的依据是逻辑跳跃，而非句子数量。若学生在一次回答中表达了一个完整的认知状态（如"知道A，但不知道B"），应合并为单个节点，status 取卡住或错误的状态，content 完整描述该认知状态，不得强行拆分。

    4. 判断学生输入的含义时，必须结合上一轮老师的问题（对话历史中最后一条 role 为"老师"的内容）。若老师刚刚提问了一个新问题，学生的回答应被理解为对该新问题的回答，而不是对之前错误的重复。禁止仅凭数值相同就判定为重复错误。

    5. mark_stuck 只用于学生在已有节点上再次卡住的情况，不用于新节点。

    6. 节点 content 的内容必须来源于学生的实际表述，不得包含老师的引导内容。
    
    7. 思维树只记录学生主动表达的认知步骤。老师问题中的提示、引导、已知条件的重述，以及学生尚未自主推导出的中间结论，禁止写入思维树。写入前必须自问：这个内容是学生在本轮回答中自己说出来的吗？若不是，不得写入。即使老师在上一轮已经告知了某个结论，只要学生本轮没有主动复述或运用它，也不得将其写入思维树。
    
    8. 思维树中始终存在一个固定根节点 n0（content="解题起点"），是整棵树的逻辑根。
       - 当学生表示完全没有思路时，对 n0 执行 mark_stuck（不新建节点）。
       - 当学生开始尝试某个解题方向时，新建子节点，parent_id 填 "n0"。
       - 禁止修改 n0 的 content 和 parent_id。

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
                parent_id = last_added_node_id

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

    # 将学生输入记录到对话历史
    state.dialogue_history.append({"role": "student", "content": student_input})

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
