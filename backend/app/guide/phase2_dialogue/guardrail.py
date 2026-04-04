import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate

from phase2_dialogue.dialogue_graph_state import DialogueGraphState
from phase2_dialogue.question_generator import run_question_generator

load_dotenv()

MAX_RETRY = 3  # 守门Agent最大重试次数
FALLBACK_QUESTION = "你觉得下一步应该怎么做呢？"  # 超过重试次数后的兜底问题

GUARDRAIL_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位严格的教学质量审核员，负责检查教师提出的引导问题是否符合苏格拉底式教学原则。

你会收到以下信息：
1. 题目的标准答案
2. 待审核的引导问题
3. 如果是重写请求，还会收到上一次被打回的原因

【审核标准】
以下任意一条成立，则判定为"不合格"：
- 直接给出了最终答案或中间步骤的具体数值
- 问题本身已经暗示了正确的解题方向，学生只需照做即可
- 问题包含了不必要的计算提示，使学生无需独立思考

以下情况判定为"合格"：
- 问题只是引导学生思考，没有透露任何答案信息
- 问题给出了具体提示（当stuck_count>=3时允许），但未给出最终答案

请严格按照以下 JSON 格式输出，不要输出任何其他内容：

合格：
{{
  "passed": true,
  "reason": null
}}

不合格：
{{
  "passed": false,
  "reason": "具体说明哪里泄题了，以便提问生成Agent修正"
}}
"""),
    ("human", """【标准答案】
{answer}

【待审核的引导问题】
{question}

【上次被打回的原因（如有）】
{previous_rejection}
""")
])


def run_guardrail(graph_state: "DialogueGraphState") -> "DialogueGraphState":
    """
    守门Agent节点函数。
    检查生成的引导问题是否泄题，将判断结果写入图状态。
    """

    state = graph_state["session_state"]
    question = graph_state["generated_question"]
    retry_count = graph_state["retry_count"]

    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus"
    )

    parser = JsonOutputParser()
    chain = GUARDRAIL_PROMPT | llm | parser

    result = chain.invoke({
        "answer": state.parsed_problem["answer"],
        "question": question,
        "previous_rejection": graph_state["rejection_reason"] if graph_state["rejection_reason"] else "无"
    })

    if result["passed"]:
        # 通过审核，清空打回原因和重试计数
        return {
            "session_state": state,
            "student_input": graph_state["student_input"],
            "generated_question": question,
            "rejection_reason": None,
            "retry_count": 0
        }
    else:
        # 未通过审核，记录打回原因，递增重试计数
        new_retry_count = retry_count + 1
        # 超过最大重试次数，使用兜底问题
        final_question = FALLBACK_QUESTION if new_retry_count >= MAX_RETRY else question
        return {
            "session_state": state,
            "student_input": graph_state["student_input"],
            "generated_question": final_question,
            "rejection_reason": result["reason"] if new_retry_count < MAX_RETRY else None,
            "retry_count": new_retry_count
        }


def should_retry(graph_state: "DialogueGraphState") -> str:
    """
    条件边路由函数。
    根据守门Agent的判断结果决定下一个节点。
    """
    # 没有打回原因，说明通过了审核或已使用兜底问题
    if graph_state["rejection_reason"] is None:
        return "end"
    return "question_generator"



if __name__ == '__main__':
    # 测试用例
    from phase2_dialogue.session_state import SessionState, ThinkingNode, NodeStatus

    # 创建测试用的SessionState，使用question_generator测试的状态
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

    # 创建初始的DialogueGraphState（无问题）
    initial_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "",  # 初始化为空
        "generated_question": "",  # 初始化为空
        "rejection_reason": None,  # 初始化为None
        "retry_count": 0  # 初始化为0
    }

    # 先调用问题生成器生成问题
    graph_state_with_question = run_question_generator(initial_graph_state)
    print("Question Generator Output:")
    print(f"Generated Question: {graph_state_with_question['generated_question']}")
    print(f"Rejection Reason: {graph_state_with_question['rejection_reason']}")
    print(f"Retry Count: {graph_state_with_question['retry_count']}\n")

    # 然后调用守门Agent审核问题
    final_graph_state = run_guardrail(graph_state_with_question)
    print("Guardrail Final Output:")
    print(f"Generated Question: {final_graph_state['generated_question']}")
    print(f"Rejection Reason: {final_graph_state['rejection_reason']}")
    print(f"Retry Count: {final_graph_state['retry_count']}")
    print(f"Should Retry: {should_retry(final_graph_state)}")
