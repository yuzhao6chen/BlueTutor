import os

from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate

from phase2_dialogue.dialogue_graph_state import DialogueGraphState

load_dotenv()

MAX_RETRY = 3  # 守门Agent最大打回次数
FALLBACK_QUESTION = "你觉得下一步应该怎么做呢？"  # 超过重试次数后的兜底问题

GUARDRAIL_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位严格但有经验的教学质量审核员，负责检查教师提出的引导回复是否符合苏格拉底式教学原则。

你会收到以下信息：
1. 题目的标准答案
2. 学生的上一轮回答
3. 待审核的教师回复
4. 如果是重写请求，还会收到上次被打回的原因
5. 情境分析与教学建议（说明当前学生状态及允许的引导力度）

【审核标准】
以下任意一条成立，则判定为"不合格"：
- 直接给出了最终答案
- 给出了题目中未出现的、学生尚未推导出的中间计算结果（如直接说出"多出20只脚"）
- 直接给出了完整的解题步骤或方程，使学生无需思考即可照做
- 情境分析建议中未明确允许具体提示时，暗示了特定的代数设定（如"设鸡为x只"）

以下情况判定为"合格"：
- 引用了题目原文中已有的已知条件（如速度比5:4、共30个头），不属于泄题
- 复述或确认了学生上一轮回答中已经说出的内容，属于承接而非泄题
- 对学生的正确回答给予肯定，并在此基础上引导下一步思考，不属于泄题
- 情境分析建议中明确允许给出具体提示时，给出了方向性提示但未给出最终答案

请严格按照以下 JSON 格式输出，不要输出任何其他内容：

通过审核时：
{{
  "passed": true,
  "reason": null
}}

    未通过审核时：
{{
  "passed": false,
  "reason": "具体说明哪里泄题了，以便提问生成Agent修正"
}}
"""),
    ("human", """【标准答案】
{answer}

【学生的上一轮回答】
{last_student_input}

【待审核的教师回复】
{question}

【上次被打回的原因（如有）】
{previous_rejection}

【情境分析与教学建议】
{teaching_guidance}
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
        "previous_rejection": "\n".join(
            f"{i + 1}. {reason}"
            for i, reason in enumerate(graph_state["rejection_reason"])
        ) if graph_state["rejection_reason"] else "无",
        "last_student_input": graph_state["session_state"].dialogue_history[-1]["content"]
        if graph_state["session_state"].dialogue_history else "无",
        "teaching_guidance": graph_state["teaching_guidance"] if graph_state["teaching_guidance"] else "无"
    })

    # 测试
    # print(result)

    if result["passed"]:
        # 通过审核，清空打回原因和重试计数
        return {
            "session_state": state,
            "student_input": graph_state["student_input"],
            "generated_question": question,
            "rejection_reason": [],
            "retry_count": 0,
            "teaching_guidance": graph_state["teaching_guidance"]
        }
    else:
        # 未通过审核，记录打回原因，递增重试计数
        new_retry_count = retry_count + 1
        # 测试
        # print(graph_state["rejection_reason"] + [
        #     result["reason"]] if new_retry_count < MAX_RETRY else [])

        # 已达最大重试次数，替换为兜底问题并清空 rejection_reason，让 should_retry 直接结束
        if retry_count >= MAX_RETRY:
            return {
                "session_state": state,
                "student_input": graph_state["student_input"],
                "generated_question": FALLBACK_QUESTION,
                "rejection_reason": [],
                "retry_count": new_retry_count,
                "teaching_guidance": graph_state["teaching_guidance"]
            }
        return {
            "session_state": state,
            "student_input": graph_state["student_input"],
            "generated_question": question,
            "rejection_reason": graph_state["rejection_reason"] + [result["reason"]],
            "retry_count": new_retry_count,
            "teaching_guidance": graph_state["teaching_guidance"]
        }


def should_retry(graph_state: "DialogueGraphState") -> str:
    """
    条件边函数：判断是否需要重试。
    rejection_reason 为空（通过审核或已触发兜底）则结束，否则重试。
    """
    if not graph_state["rejection_reason"]:
        return "end"
    return "question_generator"



if __name__ == '__main__':
    from phase2_dialogue.session_state import SessionState
    from phase2_dialogue.state_tracker import run_state_tracker
    from phase2_dialogue.situation_analyzer import run_situation_analyzer
    from phase2_dialogue.question_generator import run_question_generator

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
    initial_graph_state: DialogueGraphState = {
        "session_state": test_session_state,
        "student_input": "我不知道这道题从何入手。",
        "generated_question": "",
        "rejection_reason": [],
        "retry_count": 0,
        "teaching_guidance": ""
    }

    # 链式调用：state_tracker → situation_analyzer → question_generator → guardrail
    print("=== State Tracker ===")
    state1 = run_state_tracker(initial_graph_state)
    print(f"Thinking Tree: {state1['session_state'].thinking_tree}")
    print(f"Stuck Count: {state1['session_state'].stuck_count}\n")

    print("=== Situation Analyzer ===")
    state2 = run_situation_analyzer(state1)
    print(f"Teaching Guidance: {state2['teaching_guidance']}\n")

    print("=== Question Generator ===")
    state3 = run_question_generator(state2)
    print(f"Generated Question:\n{state3['generated_question']}\n")

    print("=== Guardrail ===")
    state4 = run_guardrail(state3)
    print(f"Generated Question: {state4['generated_question']}")
    print(f"Rejection Reason: {state4['rejection_reason']}")
    print(f"Retry Count: {state4['retry_count']}")
    print(f"Should Retry: {should_retry(state4)}")
