import os
from pathlib import Path
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv(Path(__file__).parent / ".env")

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
 )

response = client.chat.completions.create(
    model="qwen3.6-flash",
    messages=[{"role": "user", "content": "你好"}]
)
print(response.choices[0].message.content)
