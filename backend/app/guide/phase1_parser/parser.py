from langchain_core.output_parsers import JsonOutputParser
from pydantic import BaseModel, Field

from llm_provider import get_llm
from phase1_parser.prompts import PARSE_PROMPT


# 定义输出的数据结构
class ParsedProblem(BaseModel):
    known_conditions: list[str] = Field(description="题目中的核心已知条件列表")
    goal: str = Field(description="题目的求解目标")
    answer: str = Field(description="该题目的标准答案")


def parse_problem(problem_text: str) -> ParsedProblem:
    """
    解析数学题目，提取已知条件、求解目标和标准答案。

    Args:
        problem_text: 学生提交的原始题目文本

    Returns:
        ParsedProblem: 结构化的题目解析结果
    """
    llm = get_llm()

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
