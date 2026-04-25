## Backend

当前后端采用按功能划分的目录结构，核心目录在 `backend/app/` 下。后端启动命令为：
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

当前 `app.main` 已注册以下模块路由：

1. Preview：`/api/preview/*`
2. Shared：`/api/shared/*`
3. Mistakes：`/api/mistakes/*`
4. Guide：`/api/guide/*`

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

## 2. Mistakes 模块（错题练习）

### 2.1 当前已实现能力

Mistakes 模块已经形成完整的错题闭环能力，主要包括：

1. 错题报告入库与查询（报告列表、详情、状态更新）
2. 时间轴与首页摘要（`timeline`、`stats`、`home-summary`、`daily-plan`）
3. 错题讲义化（`lecture`）
4. 纠错重做会话（启动重做、按轮推进、查看会话）
5. 推荐题能力（生成推荐题、提交答案、从推荐题进入重做）
6. 讲给 AI 听会话（启动会话、按轮推进、读取会话）
7. 用户画像聚合（`profiles/{user_id}`）

### 2.2 主要 API 分组

1. 基础与汇总：
	- `GET /api/mistakes/health`
	- `GET /api/mistakes/stats`
	- `GET /api/mistakes/home-summary`
	- `GET /api/mistakes/daily-plan`
2. 报告与讲义：
	- `POST /api/mistakes/report/ingest`
	- `GET /api/mistakes/reports`
	- `GET /api/mistakes/reports/{report_id}`
	- `GET /api/mistakes/reports/{report_id}/lecture`
	- `PATCH /api/mistakes/reports/{report_id}/status`
3. 时间轴：
	- `GET /api/mistakes/timeline`
4. 重做与推荐：
	- `POST /api/mistakes/redo-sessions/start`
	- `GET /api/mistakes/redo-sessions/{session_id}`
	- `POST /api/mistakes/redo-sessions/{session_id}/turn`
	- `POST /api/mistakes/recommendations/generate`
	- `POST /api/mistakes/recommendations/{recommendation_id}/submit`
	- `POST /api/mistakes/recommendations/{recommendation_id}/start-redo`
5. 讲给 AI 听：
	- `POST /api/mistakes/dialogue-sessions/start`
	- `GET /api/mistakes/dialogue-sessions/{session_id}`
	- `POST /api/mistakes/dialogue-sessions/{session_id}/turn`

### 2.3 本地数据与缓存

Mistakes 模块服务侧数据目录位于：

1. `backend/app/mistakes/data/`

该目录已在仓库 `.gitignore` 中忽略，便于本地开发时沉淀数据而不污染主仓库。

### 2.4 运行验证（本次核查）

已在 `bluetutor` 环境下启动后端，并验证以下接口可正常返回 `HTTP 200`：

1. `/api/mistakes/health`
2. `/api/mistakes/home-summary`
3. `/api/mistakes/timeline`
4. `/api/mistakes/stats`
5. `/api/mistakes/daily-plan`
6. `POST /api/mistakes/report/ingest`（已成功写入测试报告）
7. `GET /api/mistakes/reports/{report_id}`（可读取新写入报告详情）
8. `GET /api/mistakes/reports/{report_id}/lecture`（可生成讲义结构）

注：PowerShell 默认编码下中文可能显示乱码，但接口 JSON 结构与业务字段返回正常。

## 3. Guide 模块（引导解题）

Guide 模块是后端苏格拉底式讲题核心，当前代码主链路如下：

1. API 路由：`backend/app/api/guide_api.py`
2. 会话门面：`backend/app/guide/session.py`（`SocraticTutorSession`）
3. 会话编排：`backend/app/guide/session_manager.py`
4. 会话持久化：`backend/app/guide/session_store.py`

### 3.1 当前已实现能力

1. 创建会话与会话列表/详情读取
2. 单轮引导对话（非流式 + SSE 流式）
3. 思维树读取
4. 讲题报告生成
5. 题解生成与读取（非流式 + SSE 流式）
6. 可视化生成与读取

### 3.2 主要 API 分组

1. 会话管理：
	- `GET /api/guide/sessions`
	- `POST /api/guide/sessions`
	- `GET /api/guide/sessions/{session_id}`
2. 对话与思维树：
	- `POST /api/guide/sessions/{session_id}/turns`
	- `POST /api/guide/sessions/{session_id}/turns/stream`
	- `GET /api/guide/sessions/{session_id}/thinking-tree`
3. 报告与题解：
	- `POST /api/guide/sessions/{session_id}/report`
	- `GET /api/guide/sessions/{session_id}/solution`
	- `POST /api/guide/sessions/{session_id}/solution`
	- `POST /api/guide/sessions/{session_id}/solution/stream`
4. 可视化：
	- `GET /api/guide/sessions/{session_id}/visualization`
	- `POST /api/guide/sessions/{session_id}/visualization`

### 3.3 启动与依赖说明

1. `app.main` 现在采用直接导入并注册 Guide 路由（不再使用 `try/except` 动态引入）。
2. Guide 依赖 `langchain-core`、`langchain-openai`、`langgraph`，请确保已执行：
	- `pip install -r backend/requirements.txt`
3. 创建会话会触发大模型调用，Guide 会按以下顺序读取 API Key：
	- 优先：`DASHSCOPE_API_KEY`
	- 回退：`LLM_API_KEY`（兼容 `backend/.env` 现有配置）
4. 若两个 Key 都未配置，会返回：
	- `HTTP 422`
	- `code=4001`
	- `message=会话初始化失败：DASHSCOPE_API_KEY/LLM_API_KEY is missing or empty`

### 3.4 运行验证（本次核查）

已在 `bluetutor` 环境完成 Guide 冒烟验证：

1. `GET /api/guide/sessions`：`HTTP 200`
2. `POST /api/guide/sessions`：`HTTP 200`，成功创建会话并返回 `session_id`
3. `GET /api/guide/sessions/{session_id}`：`HTTP 200`
4. `POST /api/guide/sessions/{session_id}/turns`：`HTTP 200`，返回 `question` 与 `is_solved`
5. `GET /api/guide/sessions/{session_id}/thinking-tree`：`HTTP 200`
6. `POST /api/guide/sessions/{session_id}/report`：`HTTP 200`
7. 不存在会话的读取与动作接口（`session_id=not_exist_session_id`）：
	- `GET /sessions/{id}` -> `4041`
	- `GET /sessions/{id}/thinking-tree` -> `4042`
	- `POST /sessions/{id}/turns` -> `4043`
	- `POST /sessions/{id}/report` -> `4044`
	- `GET/POST /sessions/{id}/solution` -> `4045`
	- `GET/POST /sessions/{id}/visualization` -> `4047`

结论：Guide 路由注册、正向讲题流程和错误处理链路均正常；使用 `backend/.env` 中的 `LLM_API_KEY` 已可跑通 Guide 核心流程。

## 4. 安装依赖

推荐使用 conda 环境 `bluetutor`。

```powershell
conda activate bluetutor
pip install -r backend/requirements.txt
```
