import os
from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser
from pydantic import BaseModel, Field

load_dotenv()


# 定义输出的数据结构
class ParsedProblem(BaseModel):
    known_conditions: list[str] = Field(description="题目中的核心已知条件列表")
    goal: str = Field(description="题目的求解目标")
    answer: str = Field(description="该题目的标准答案")


# 构建 Prompt
PARSE_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一位专业的小学及初中数学教师助手。
你的任务是解析学生提交的数学题目，提取关键信息。

请严格按照以下要求输出：
1. 提取题目中所有核心已知条件
2. 明确题目的求解目标
3. 给出该题目的标准答案

【重要】你只需提取信息和给出答案，绝对不要生成解题步骤或解题过程。

请以 JSON 格式输出，结构如下：
{{
  "known_conditions": ["条件1", "条件2", ...],
  "goal": "求解目标",
  "answer": "标准答案"
}}"""),
    ("human", "请解析以下数学题目：\n\n{problem}")
])


def parse_problem(problem_text: str) -> ParsedProblem:
    """
    解析数学题目，提取已知条件、求解目标和标准答案。

    Args:
        problem_text: 学生提交的原始题目文本

    Returns:
        ParsedProblem: 结构化的题目解析结果
    """
    llm = ChatTongyi(
        api_key=os.environ["DASHSCOPE_API_KEY"],
        model="qwen-plus",
        temperature=0.7
    )

    parser = JsonOutputParser(pydantic_object=ParsedProblem)
    chain = PARSE_PROMPT | llm | parser

    result = chain.invoke({"problem": problem_text})
    return result


if __name__ == "__main__":
    # 测试用例
    test_problem = """
    鸡兔同笼，共有30个头，80只脚，问鸡和兔各有多少只？
    """
    result = parse_problem(test_problem)
    print(result)
