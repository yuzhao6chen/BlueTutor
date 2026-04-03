"""讲题模块 - Prompt 模板"""

GUIDE_SYSTEM_PROMPT = """你是一位经验丰富的中小学数学教师，擅长通过引导式提问帮助学生自主思考解题。
你的教学理念是：
1. 绝不直接给出答案
2. 通过层层递进的问题引导学生自己发现解题思路
3. 当学生卡住时，提供适当的提示而非解答
4. 用鼓励的语气，建立学生的自信心
5. 最后帮助学生总结解题方法和关键知识点

请用亲切、耐心的语气，像一位身边的家教老师一样与学生对话。"""


GUIDE_PROBLEM_TYPES = {
    "distance": {
        "name": "路程问题",
        "description": "涉及速度、时间、路程的关系：路程 = 速度 × 时间",
        "grade_range": (4, 7),
    },
    "chicken_rabbit": {
        "name": "鸡兔同笼",
        "description": "经典的假设法解题：已知头数和脚数，求鸡兔各多少",
        "grade_range": (4, 6),
    },
    "work": {
        "name": "工程问题",
        "description": "涉及工作效率、工作时间、工作总量的关系",
        "grade_range": (5, 8),
    },
    "fraction_application": {
        "name": "分数应用题",
        "description": "涉及分数的实际应用，如比例、百分数等",
        "grade_range": (5, 7),
    },
}


# 示例题目库
PROBLEMS_DB = {
    "distance_001": {
        "id": "distance_001",
        "content": "小明骑自行车从家到学校，速度是每小时 15 千米，用了 20 分钟。请问小明家离学校有多远？",
        "problem_type": "distance",
        "grade_level": 5,
        "difficulty": 2,
        "solution_steps": [
            "理解题意：已知速度和时间，求路程",
            "统一单位：20 分钟 = 1/3 小时",
            "应用公式：路程 = 速度 × 时间 = 15 × 1/3 = 5 千米",
        ],
        "key_knowledge": "路程 = 速度 × 时间，注意单位统一",
    },
    "chicken_rabbit_001": {
        "id": "chicken_rabbit_001",
        "content": "笼子里有若干只鸡和兔，共有 35 个头，94 只脚。请问鸡和兔各有多少只？",
        "problem_type": "chicken_rabbit",
        "grade_level": 5,
        "difficulty": 3,
        "solution_steps": [
            "理解题意：鸡有 2 只脚，兔有 4 只脚",
            "假设法：假设全是鸡，则有 35×2=70 只脚",
            "计算差值：实际 94 只脚，多了 94-70=24 只脚",
            "每只兔子比鸡多 2 只脚，所以兔子有 24÷2=12 只",
            "鸡有 35-12=23 只",
        ],
        "key_knowledge": "假设法解题：先假设一种情况，再根据差值调整",
    },
    "work_001": {
        "id": "work_001",
        "content": "一项工程，甲单独做需要 10 天完成，乙单独做需要 15 天完成。如果两人合作，需要多少天完成？",
        "problem_type": "work",
        "grade_level": 6,
        "difficulty": 3,
        "solution_steps": [
            "理解题意：把工程总量看作单位'1'",
            "甲每天完成 1/10，乙每天完成 1/15",
            "合作每天完成：1/10 + 1/15 = 3/30 + 2/30 = 5/30 = 1/6",
            "所以需要 6 天完成",
        ],
        "key_knowledge": "工作效率相加，工作时间 = 总量 ÷ 效率和",
    },
}


def get_guide_prompts(problem_type: str, problem_content: str, student_grade: int) -> tuple:
    """获取讲题的 system 和 user prompt"""
    
    user_prompt = f"""请为以下数学题设计引导式讲解：

【题目】{problem_content}

【题目类型】{problem_type}
【学生年级】{student_grade} 年级

请按以下步骤进行引导：
1. 首先让学生理解题意，明确已知条件和所求问题
2. 引导学生回忆相关的知识点或公式
3. 一步步引导学生思考解题思路（不要直接给答案）
4. 如果学生回答正确，给予肯定并进入下一步
5. 如果学生回答错误或卡住，提供适当的提示
6. 最后帮助学生总结解题方法和关键知识点

请设计 3-5 个引导性问题，难度循序渐进。"""
    
    return GUIDE_SYSTEM_PROMPT, user_prompt
