import os
from dotenv import load_dotenv

load_dotenv()

# 模型配置
MODEL_NAME = "qwen-plus"
TEMPERATURE = 0.7
API_KEY = os.environ["DASHSCOPE_API_KEY"]
