"""错题模块 - Prompt 模板"""

MISTAKE_ANALYSIS_PROMPT = """你是一位经验丰富的中小学数学教师，擅长分析学生的错题并找出根本原因。

请分析学生的错题，从以下角度进行诊断：
1. **错误类型判断**：审题错误、计算错误、概念理解错误、解题思路错误
2. **知识漏洞**：指出学生可能欠缺的知识点
3. **根因分析**：深入分析为什么会犯这个错误
4. **学习建议**：给出针对性的改进建议
5. **同类题推荐**：推荐 2-3 道类似的练习题

请用鼓励的语气，让学生明白犯错是学习的一部分，关键是找到原因并改进。"""


MISTAKE_PRACTICE_PROMPT = """请根据以下错题的知识点，生成 3 道针对性练习题：

【原错题】{problem_content}
【学生答案】{student_answer}
【正确答案】{correct_answer}
【知识漏洞】{knowledge_gaps}

要求：
1. 题目难度适中，比原题略简单或相当
2. 每道题都要针对具体的知识漏洞
3. 提供适当的提示（hint）
4. 题目要贴近生活，有趣味性

请按格式返回题目列表。"""


# 错误类型定义
MISTAKE_TYPES = {
    "reading": {
        "name": "审题错误",
        "description": "没有正确理解题意，看错条件或问题",
        "suggestions": [
            "养成圈画关键词的习惯",
            "读完题后用自己的话复述题意",
            "注意单位、否定词等细节"
        ]
    },
    "calculation": {
        "name": "计算错误",
        "description": "思路正确但计算过程中出错",
        "suggestions": [
            "提高计算准确率，多进行口算练习",
            "养成验算的习惯",
            "草稿纸书写要工整，避免看错"
        ]
    },
    "concept": {
        "name": "概念错误",
        "description": "对基本概念、公式理解不清",
        "suggestions": [
            "重新复习相关概念和公式",
            "通过举例加深对概念的理解",
            "整理概念笔记，定期复习"
        ]
    },
    "strategy": {
        "name": "思路错误",
        "description": "解题方法选择不当或思路不完整",
        "suggestions": [
            "多总结各类题型的解题思路",
            "学会画图、列表等辅助分析方法",
            "遇到难题先分解成小问题"
        ]
    }
}


# 模拟练习题库
PRACTICE_PROBLEMS_DB = {
    "distance_practice": [
        {
            "id": "dp_001",
            "content": "小红步行上学，每分钟走 60 米，走了 15 分钟到达学校。小红家离学校有多远？",
            "difficulty": 2,
            "related_knowledge": "路程 = 速度 × 时间",
            "hint": "直接用公式计算即可"
        },
        {
            "id": "dp_002",
            "content": "一辆汽车以每小时 80 千米的速度行驶，3.5 小时可以行驶多少千米？",
            "difficulty": 2,
            "related_knowledge": "路程 = 速度 × 时间",
            "hint": "注意小数乘法"
        },
    ],
    "chicken_rabbit_practice": [
        {
            "id": "crp_001",
            "content": "笼子里有鸡和兔共 20 只，共有 56 只脚。鸡和兔各有多少只？",
            "difficulty": 3,
            "related_knowledge": "假设法解鸡兔同笼问题",
            "hint": "假设全是鸡，看看脚数差多少"
        },
    ],
    "default_practice": [
        {
            "id": "default_001",
            "content": "请回顾相关知识点，尝试解答：一个数的 3 倍加上 5 等于 20，这个数是多少？",
            "difficulty": 2,
            "related_knowledge": "一元一次方程",
            "hint": "设这个数为 x，列方程求解"
        },
        {
            "id": "default_002",
            "content": "小明有若干元钱，买了一支钢笔花了 15 元，又买了一本笔记本花了 8 元，还剩 27 元。小明原来有多少钱？",
            "difficulty": 2,
            "related_knowledge": "逆向思维解题",
            "hint": "从剩下的钱倒推回去"
        },
        {
            "id": "default_003",
            "content": "一个长方形的长是宽的 2 倍，周长是 36 厘米。求这个长方形的面积。",
            "difficulty": 3,
            "related_knowledge": "长方形周长和面积",
            "hint": "先设宽为 x，用周长公式求出长和宽"
        },
    ]
}
