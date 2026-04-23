## Backend

当前后端采用按功能划分的目录结构，核心目录在 `backend/app/` 下。后端启动命令为：
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## 1. Preview 模块

### 1.1 当前进度

目前优先完成的是 Preview 预习指导模块，已完成的部分包括：

1. `preview/schema.py`：请求与响应数据模型
2. `preview/prompt.py`：知识点提取与预习对话的 Prompt 模板
3. `preview/agent.py`：阿里云百炼模型调用封装、JSON 清洗和重试逻辑
4. `preview/service.py`：预习业务编排、本地日志落盘、模型结果结构化
5. `api/preview_api.py`：预习模块接口与错误响应封装
6. `main.py`：FastAPI 应用入口与 Preview 路由注册

更详细的模块设计和开发进度见：`backend/app/preview/README.md`

### 1.2 模块说明

Preview 模块面向 PDF 阅读和划选场景，主要负责两件事：

1. 接收 OCR 或前端传来的文本，提取当前页面可能涉及的知识点
2. 围绕用户选择的知识点进行预习型 AI 对话

当前默认接入阿里云百炼平台，模型为 `qwen3.6-plus`。

### 1.3 环境变量配置

后端使用 `backend/.env` 保存模型配置，该文件不会上传到仓库。

格式如下：

```env
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
LLM_API_KEY=your_api_key_here
LLM_MODEL_NAME=qwen3.6-plus
LLM_TIMEOUT_SECONDS=60
LLM_MAX_RETRIES=2
```

字段说明：

1. `LLM_BASE_URL`：百炼兼容接口地址
2. `LLM_API_KEY`：百炼平台 API Key
3. `LLM_MODEL_NAME`：当前使用的模型名
4. `LLM_TIMEOUT_SECONDS`：单次请求超时时间
5. `LLM_MAX_RETRIES`：模型输出非法 JSON 时的最大重试次数

## 2. 安装依赖

推荐使用 conda 环境 `bluetutor`。

```powershell
conda activate bluetutor
pip install -r backend/requirements.txt
```
