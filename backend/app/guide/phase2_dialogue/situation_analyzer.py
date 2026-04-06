import json
import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from phase2_dialogue.dialogue_graph_state import DialogueGraphState

load_dotenv()

SITUATION_ANALYZER_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位富有经验和同理心的教育心理顾问，负责在每一轮对话后分析学生的学习状态，为教师提供教学风格上的指导建议。

    你会收到以下信息：
    1. 题目信息（已知条件、求解目标、标准答案）
    2. 当前思维树（含最新操作结果）
    3. 完整对话历史（最后一条是学生本轮的输入）
    4. 当前卡点被引导的次数（stuck_count）

    【关于思维树的重要说明】
    思维树中节点的 error_history 是该节点的历史错误记录，记录的是过去发生过的错误，不代表学生当前仍然持有这些错误观念。
    分析学生当前状态时，必须以本轮对话（dialogue_history 中最后一条学生输入）为主要依据，error_history 仅作为背景参考，用于了解学生的历史薄弱点。
    禁止因为 error_history 中存在某条错误记录，就判定学生"仍然"持有该错误。

    【你的职责】
    你的唯一职责是分析学生的情绪与心理状态，给出教学风格上的建议。
    你不负责规划解题路径，不负责给出任何具体的解题提示或步骤内容。

    【分析维度】
    1. 解题状态：学生本轮是否取得了真实进展？（基于对话历史中学生的实际表述）
       - 答对了某步骤 / 推进了新思路 / 原地卡住 / 完全没有思路 / 放弃了某条路
    2. 情绪与信心状态：
       - 是否表现出挫败感、焦虑或沮丧？（如"我完全不会"、"我好笨"）
       - 是否表现出犹豫或不自信？（如"我感觉是……？"、"不确定对不对"）
       - 是否情绪平稳、正常推进？

    【输出要求】
    输出一段面向教师的建议，只包含以下内容：
    1. 对学生当前情绪与状态的简要判断（1句）
    2. 建议的语气风格：鼓励型 / 平和型 / 温和纠正型
    3. 建议的引导力度：抽象启发（不给具体提示）/ 方向性提示（可指出大方向但不给步骤）/ 具体提示（可给出该步骤的具体操作，但不给最终答案）
    4. 若学生情绪低落，是否需要先给予情感支持
    5. 若学生答对了某步骤，是否需要先给予明确肯定

    【禁止事项】
    - 禁止在建议中包含任何解题步骤、数值、公式或具体操作提示
    - 禁止替教师规划"下一步问什么"的具体内容
    - 禁止因 error_history 中存在历史错误就判定学生当前仍有该错误

    【输出格式】
    只输出建议本身，不要输出任何标题、前缀或分析过程，控制在80字以内。
    """),
    ("human", """【题目信息】
{parsed_problem}

【当前思维树】
{thinking_tree}

【对话历史】
{dialogue_history}

【当前卡点被引导次数】
{stuck_count}
""")
])


def run_situation_analyzer(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    情境分析Agent节点函数。
    分析学生的解题状态与情绪状态，生成教学指导意见，写入图状态。
    """
    from phase2_dialogue.state_tracker import _serialize_tree, _serialize_dialogue

    state = graph_state["session_state"]

    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus"
    )

    chain = SITUATION_ANALYZER_PROMPT | llm | StrOutputParser()

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "stuck_count": state.stuck_count,
    })

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],
        "generated_question": graph_state["generated_question"],
        "rejection_reason": graph_state["rejection_reason"],
        "retry_count": graph_state["retry_count"],
        "teaching_guidance": result.strip()
    }


if __name__ == '__main__':
    from phase2_dialogue.session_state import SessionState, ThinkingNode, NodeStatus

    test_session_state = SessionState(
        parsed_problem={
            'known_conditions': ['鸡和兔共有30个头', '鸡和兔共有80只脚', '鸡有2只脚', '兔有4只脚'],
            'goal': '求鸡的只数和兔的只数',
            'answer': '鸡20只，兔10只'
        },
        thinking_tree={
            "a1": ThinkingNode(
                node_id="a1",
                content="假设法：假设全部是鸡，共60只脚",
                status=NodeStatus.STUCK,
                parent_id=None,
                error_history=["学生表示不知从何入手，尚未形成具体步骤"]
            )
        },
        dialogue_history=[
            {"role": "学生", "content": "我完全不知道怎么做，感觉自己好笨……"}
        ],
        current_stuck_node_id="a1",
        stuck_count=2,
        last_updated_node_id="a1"
    )

    test_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "我完全不知道怎么做，感觉自己好笨……",
        "generated_question": "",
        "rejection_reason": [],
        "retry_count": 0,
        "teaching_guidance": ""
    }

    result_state = run_situation_analyzer(test_graph_state)
    print("Teaching Guidance:")
    print(result_state["teaching_guidance"])
