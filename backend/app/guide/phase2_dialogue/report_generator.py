import json
import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate

from phase2_dialogue.session_state import SessionState
from phase2_dialogue.state_tracker import _serialize_tree, _serialize_dialogue

load_dotenv()

REPORT_GENERATOR_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位专业的数学教育分析师，负责在一次讲题对话结束后，生成一份结构化的讲题报告。

你会收到以下信息：
1. 题目信息（已知条件、求解目标、标准答案）
2. 当前思维树（学生的完整解题思路，含每个节点的状态和错误历史）
3. 完整对话历史

你需要输出一个 JSON 对象，包含以下三个字段：

---

【knowledge_tags】
类型：字符串数组
内容：这道题涉及的数学知识点或题型标签，用于后续生成相似题目和构建用户画像。
要求：
- 标签应简洁，每个标签不超过8个字
- 标签数量控制在2-5个
- 优先使用小学数学的标准术语，如"鸡兔同笼"、"假设法"、"方程思想"、"行程问题"等

---

【error_profile】
类型：对象数组，每个对象包含 error_type 和 detail 两个字段
内容：对学生在本次解题过程中出现的所有错误的汇总分析
要求：
- error_type：简短的错误类型标签，如"概念混淆"、"计算失误"、"方法选择错误"、"理解题意有误"
- detail：结合对话历史，用一句话描述该错误的具体表现
- 若学生本次解题没有出现任何错误，输出空数组 []

---

【independence_evaluation】
类型：对象，包含 level 和 detail 两个字段
内容：对学生本次解题独立性的评估
要求：
- level：必须从以下四个等级中选择一个：
  "完全独立"（学生几乎不需要引导，自主完成解题）
  "少量引导"（学生在个别步骤需要提示，整体较为独立）
  "需要较多引导"（学生在多个步骤卡住，需要反复引导才能推进）
  "高度依赖引导"（学生基本无法自主推进，几乎每步都需要提示）
- detail：用一到两句话描述学生的具体表现，作为 level 判断的依据

---

请严格按照以下 JSON 格式输出，不要输出任何其他内容：
{{
  "knowledge_tags": [...],
  "error_profile": [...],
  "independence_evaluation": {{
    "level": "...",
    "detail": "..."
  }}
}}
"""),
    ("human", """【题目信息】
{parsed_problem}

【当前思维树】
{thinking_tree}

【完整对话历史】
{dialogue_history}
""")
])


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
    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus"
    )

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
        "independence_evaluation": llm_result["independence_evaluation"]
    }

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
