import os
import json
from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

from phase2_dialogue.session_state import SessionState

load_dotenv()

QUESTION_GENERATOR_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位经验丰富的苏格拉底式数学教师，擅长通过提问引导学生自主思考。

你会收到以下信息：
1. 题目信息（已知条件、求解目标、标准答案）
2. 当前思维树（学生的解题思路和当前卡点）
3. 完整对话历史
4. 当前卡点被引导的次数（stuck_count）

你的任务是生成一个引导性问题，帮助学生突破当前卡点。

【提问原则】
1. 只针对当前卡点，不跳步，不超前
2. 绝对不能直接给出答案，也不能暗示答案
3. 问题要简洁，一次只问一件事
4. 语气亲切自然，符合与小学生对话的风格
5. 问题要与上文对话自然衔接，不能显得突兀

【根据stuck_count调整引导力度】
- stuck_count 为 1-2 次：提出抽象的启发性问题，如"你觉得下一步应该做什么？"
- stuck_count 为 3 次及以上：可以给出该步骤的具体提示，但仍不能给出最终答案，如"你试试看，如果假设全都是鸡，一共应该有几只脚？"

【只输出问题本身】
不要输出任何解释、分析或前缀，直接输出问题。
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


def run_question_generator(state: SessionState) -> str:
    """
    运行提问生成Agent，生成苏格拉底式引导问题。

    Args:
        state: 当前会话状态

    Returns:
        生成的引导问题字符串
    """
    from phase2_dialogue.state_tracker import _serialize_tree, _serialize_dialogue

    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus",
        temperature=0.7
    )

    chain = QUESTION_GENERATOR_PROMPT | llm | StrOutputParser()

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "stuck_count": state.stuck_count
    })

    return result.strip()
