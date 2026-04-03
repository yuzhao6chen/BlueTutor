"""预习模块 - Prompt 模板"""

PREVIEW_SYSTEM_PROMPT = """你是一位经验丰富的中小学数学教师，擅长通过引导式提问帮助学生建立知识框架。
你的任务是根据知识点生成预习材料，包括：
1. 简洁易懂的知识介绍（适合目标年级学生理解）
2. 3-5 个层层递进的引导式问题
3. 每个问题提供适当的提示（hint）
4. 建议的可视化辅助说明

请用亲切、鼓励的语气，避免使用过于专业的术语。"""


PREVIEW_USER_PROMPT = """请为以下知识点生成预习材料：

知识点：{knowledge_name}
知识点描述：{knowledge_description}
目标年级：{grade_level} 年级
学习风格偏好：{learning_style}

请按照以下格式返回：
1. 知识介绍（100-200 字）
2. 引导式问题列表（3-5 个，难度递增）
3. 可视化辅助建议（如图表、实物演示等）
4. 预计学习时间"""


KNOWLEDGE_POINTS_DB = {
    "fraction_basics": {
        "name": "分数的初步认识",
        "description": "理解分数的含义，掌握分子、分母的概念，学会读写分数",
        "grade_level": 3,
    },
    "area_rectangle": {
        "name": "长方形的面积计算",
        "description": "掌握长方形面积公式：面积 = 长 × 宽，并能解决实际问题",
        "grade_level": 4,
    },
    "linear_equation": {
        "name": "一元一次方程",
        "description": "理解方程的概念，掌握解一元一次方程的基本方法",
        "grade_level": 7,
    },
    "pythagorean": {
        "name": "勾股定理",
        "description": "理解并掌握勾股定理：直角三角形两直角边的平方和等于斜边的平方",
        "grade_level": 8,
    },
}
