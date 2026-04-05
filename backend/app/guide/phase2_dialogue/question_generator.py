import json
import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from phase2_dialogue.dialogue_graph_state import DialogueGraphState

load_dotenv()

QUESTION_GENERATOR_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位经验丰富、富有同理心的苏格拉底式数学教师，擅长通过提问引导小学生自主思考。

你会收到以下信息：
1. 题目信息（已知条件、求解目标、标准答案）
2. 当前思维树（学生的解题思路和当前卡点）
3. 完整对话历史
4. 当前卡点被引导的次数（stuck_count）
5. 情境分析与教学建议（对学生当前状态的判断，以及对你本次回复的具体指导）

你的任务是根据以上信息，生成一段回复，发送给学生。

【关于思维树的重要说明】
思维树中 content 以"【引导方向】"开头的节点，是系统为引导学生而预设的方向，并非学生已经说出的内容。
在分析学生状态和生成教学建议时，必须严格遵守以下两点：
1. 不可将此类节点视为学生的进展或成果，不可在建议中出现"学生想到了……"、"从……入手很对"等归因于学生的表述。
2. 此类节点只能作为"下一步引导的方向"来使用，建议的措辞应为"可以引导学生尝试……"而非"肯定学生已经……"。


【回复原则】
1. 严格遵照【情境分析与教学建议】中的指导，调整语气、引导力度和是否给予情感支持
2. 如果建议中提到学生答对了某步骤，必须先给予明确、真诚的肯定，再引导下一步
3. 如果建议中提到学生情绪低落或沮丧，必须先给予温暖的鼓励，再提出问题
4. 引导问题只针对当前卡点，不跳步，不超前
5. 绝对不能直接给出答案，也不能暗示答案（除非情境分析明确允许给出具体提示）
6. 一次只问一件事，问题简洁明了
7. 语气亲切自然，符合与小学生对话的风格，可以使用"我们"、"你试试看"等拉近距离的表达

【根据stuck_count调整引导力度】
- stuck_count 为 1-2 次：提出抽象的启发性问题，如"你觉得下一步应该做什么？"
- stuck_count 为 3 次及以上：可以给出该步骤的具体提示，但仍不能给出最终答案

【输出格式】
直接输出发送给学生的完整回复，可以包含肯定、鼓励和引导问题，不要输出任何分析、标注或前缀。
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

【上次被打回的原因（如有）】
{rejection_reason}

【情境分析与教学建议】
{teaching_guidance}
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
        "last_updated_node_id": state.last_updated_node_id if state.last_updated_node_id else "无",
        "rejection_reason": "\n".join(
            f"{i + 1}. {reason}"
            for i, reason in enumerate(graph_state["rejection_reason"])
        ) if graph_state["rejection_reason"] else "无",
        "teaching_guidance": graph_state["teaching_guidance"] if graph_state["teaching_guidance"] else "无"
    })

    return {
        "session_state": state,
        "student_input": graph_state["student_input"],  # 保持不变
        "generated_question": result.strip(),
        "rejection_reason": graph_state["rejection_reason"],  # 保持不变
        "retry_count": graph_state["retry_count"],  # 保持不变
        "teaching_guidance": graph_state["teaching_guidance"] # 保持不变
    }


if __name__ == '__main__':
    from phase2_dialogue.session_state import SessionState
    from phase2_dialogue.state_tracker import run_state_tracker
    from phase2_dialogue.situation_analyzer import run_situation_analyzer

    # 创建初始 SessionState（思维树为空，模拟学生刚开始作答）
    test_session_state = SessionState(
        parsed_problem={
            'known_conditions': ['鸡和兔共有30个头', '鸡和兔共有80只脚', '鸡有2只脚', '兔有4只脚'],
            'goal': '求鸡的只数和兔的只数',
            'answer': '鸡20只，兔10只'
        },
        thinking_tree={},
        dialogue_history=[],
    )

    # 初始化图状态
    test_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "我不知道这道题从何入手。",
        "generated_question": "",
        "rejection_reason": [],
        "retry_count": 0,
        "teaching_guidance": ""
    }

    # 第一步：调用 state_tracker
    state_after_tracker = run_state_tracker(test_graph_state)
    print("=== State Tracker Output ===")
    print(f"Thinking Tree: {state_after_tracker['session_state'].thinking_tree}")
    print(f"Stuck Count: {state_after_tracker['session_state'].stuck_count}\n")

    # 第二步：调用 situation_analyzer
    state_after_analyzer = run_situation_analyzer(state_after_tracker)
    print("=== Situation Analyzer Output ===")
    print(f"Teaching Guidance: {state_after_analyzer['teaching_guidance']}\n")

    # 第三步：调用 question_generator
    final_state = run_question_generator(state_after_analyzer)
    print("=== Question Generator Output ===")
    print(f"Generated Question:\n{final_state['generated_question']}")
