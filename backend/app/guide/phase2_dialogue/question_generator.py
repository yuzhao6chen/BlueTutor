import json
import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from phase2_dialogue.dialogue_graph_state import DialogueGraphState

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

【本轮最新操作的节点】
{last_updated_node_id}
""")

])


def run_question_generator(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    提问生成Agent节点函数。
    根据当前状态生成苏格拉底式引导问题，返回新的图状态。
    """
    from phase2_dialogue.state_tracker import _serialize_tree, _serialize_dialogue

    state = graph_state["session_state"]

    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus"
    )

    chain = QUESTION_GENERATOR_PROMPT | llm | StrOutputParser()

    result = chain.invoke({
        "parsed_problem": json.dumps(state.parsed_problem, ensure_ascii=False),
        "thinking_tree": _serialize_tree(state.thinking_tree),
        "dialogue_history": _serialize_dialogue(state.dialogue_history),
        "stuck_count": state.stuck_count,
        "last_updated_node_id": state.last_updated_node_id if state.last_updated_node_id else "无"
    })

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],  # 保持不变
        "generated_question": result.strip(),
        "rejection_reason": None,  # 每次重新生成时清空上一轮的打回原因
        "retry_count": graph_state["retry_count"]  # 保持不变
    }



if __name__ == '__main__':
    from phase2_dialogue.session_state import SessionState, ThinkingNode, NodeStatus

    # 重构后的测试用例，使用state_tracker测试结果的状态
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
                error_history=["学生表示不知从何入手，尚未形成具体步骤，但假设法是本题典型切入点"]
            )
        },
        dialogue_history=[{"role": "学生", "content": "我不知道这道题从何入手。"}],
        current_stuck_node_id="a1",
        stuck_count=1,
        last_updated_node_id="a1"
    )

    # 创建测试用的DialogueGraphState
    test_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "",  # 初始化为空，因为问题生成不依赖最新输入
        "generated_question": "",  # 初始化为空
        "rejection_reason": None,  # 初始化为None
        "retry_count": 0  # 初始化为0
    }

    # 调用问题生成器
    updated_graph_state = run_question_generator(test_graph_state)

    # 打印生成的提问
    print("Generated Question:")
    print(updated_graph_state["generated_question"])
