# Preview 模块实现方案

## 1. 模块目标

Preview 模块负责预习指导场景的后端能力，聚焦两件事：

1. 对用户在 PDF/教材/练习册中划选或截图得到的文本内容进行知识点分析，返回当前页面可能涉及的知识点，供前端展示和选择。
2. 在用户选择知识点后，提供面向预习场景的 AI 对话能力，帮助学生理解概念、公式、例子和学习重点。

本模块不负责：

1. OCR 图像识别。
2. 做题引导、错因分析、错题生成。
3. 复杂可视化绘制。

当前实现建议直接调用大模型 API。

## 2. 业务定位

### 2.1 用户流程

1. 用户在前端打开 PDF 阅读器。
2. 用户通过划选文本或截图，得到一段教材/习题内容。
3. 前端或 shared 层将文本传给 Preview 模块的知识点分析接口。
4. Preview 模块返回知识点列表。
5. 前端展示知识点列表，允许用户选择一个或多个知识点。
6. 用户上滑进入预习对话界面，围绕所选知识点进行提问。
7. Preview 模块结合上下文和所选知识点，返回预习型讲解内容。

### 2.2 设计原则

1. 先保证接口稳定，再优化模型效果。
2. 先实现“文本输入 -> 知识点输出 -> 对话回复”的闭环。
3. 与 shared 层采用松耦合设计，先约定输入输出，不阻塞独立开发。
4. Prompt、路由、业务编排、模型调用分层，避免后期难维护。

## 3. 模块边界

### 3.1 上游输入

上游可能有两种来源：

1. shared/OCR 模块传来的 `content_text`。
2. 前端直接传来的划选文本。

Preview 模块统一只接收文本，不接收图片本体。

### 3.2 下游输出

1. 返回知识点列表给前端展示。
2. 返回预习问答回复给前端对话界面。
3. 可选：将交互记录写入本地日志，后续供 shared/Profile 模块消费。

## 4. 建议目录职责

### 4.1 [backend/app/preview/schema.py](backend/app/preview/schema.py)

当前状态：已实现基础数据模型，可作为后续 prompt、agent、service、api 层的统一输入输出契约。

定义 Preview 模块的请求、响应、内部数据结构。建议包含：

1. `ApiResponse[T]`
2. `KnowledgePointItem`
3. `PreviewKnowledgeRequest`
4. `PreviewKnowledgeData`
5. `PreviewKnowledgeResponse`
6. `PreviewChatRequest`
7. `PreviewChatData`
8. `PreviewChatResponse`
9. 可选：`PreviewChatMessage`，用于保留多轮对话历史

建议字段如下。

`PreviewKnowledgeRequest`

```json
{
  "user_id": "u001",
  "content_text": "本节内容介绍速度、时间和路程三者之间的关系",
  "source_type": "pdf_selection",
  "page_hint": 12,
  "session_id": "preview_s001"
}
```

字段说明：

1. `user_id`: 用户标识。
2. `content_text`: 待分析的文本内容，核心字段。
3. `source_type`: 文本来源，建议枚举为 `pdf_selection`、`screenshot_ocr`、`manual_input`。
4. `page_hint`: 可选，记录页码，方便前端定位。
5. `session_id`: 可选，对齐一次预习会话。

`PreviewKnowledgeResponse`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "knowledge_points": [
      {
        "id": "kp_001",
        "title": "速度、时间、路程的关系",
        "description": "理解三个量之间的基本公式和换算关系",
        "confidence": 0.93
      },
      {
        "id": "kp_002",
        "title": "速度公式",
        "description": "速度=路程÷时间，表示单位时间内通过的路程",
        "confidence": 0.89
      }
    ],
    "summary": "该内容主要围绕速度、时间和路程三者关系展开。"
  }
}
```

`PreviewChatRequest`

```json
{
  "user_id": "u001",
  "text": "什么是速度公式？",
  "context_text": "本节内容讲解速度、时间和路程之间的关系",
  "selected_knowledge_points": [
    "速度、时间、路程的关系",
    "速度公式"
  ],
  "session_id": "preview_s001",
  "history": [
    {
      "role": "user",
      "content": "这一页主要在讲什么？"
    },
    {
      "role": "assistant",
      "content": "这一页主要在讲速度、时间、路程的关系。"
    }
  ]
}
```

`PreviewChatResponse`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "reply": "速度公式是速度=路程÷时间，表示单位时间内通过的路程。你可以把它理解为单位时间里走了多远。",
    "follow_up_questions": [
      "如果已知速度和时间，怎样求路程？",
      "为什么速度要用路程除以时间？"
    ]
  }
}
```

### 4.2 [backend/app/preview/prompt.py](backend/app/preview/prompt.py)

当前状态：已实现 Prompt 常量与构造函数，知识点提取和预习对话的 JSON 输出格式已固定。

负责存放 Prompt 模板，不直接发请求。建议包含：

1. `PREVIEW_KNOWLEDGE_SYSTEM_PROMPT`
2. `PREVIEW_CHAT_SYSTEM_PROMPT`
3. `build_knowledge_extraction_prompt(content_text)`
4. `build_preview_chat_prompt(text, context_text, selected_knowledge_points, history)`

Prompt 设计要求：

1. 输出必须约束为 JSON，便于后端解析。
2. 强调目标用户是小学三年级至初中学生，解释风格要简洁、友好、启发式。
3. 明确禁止答偏为“做题引导模块”或“错题模块”。
4. 对知识点提取结果限制数量，建议返回 3 到 6 个最相关知识点。
5. 对预习对话要求优先解释概念、公式、直观理解和简单例子，不直接展开复杂解题。
6. 当前代码中已经将知识点提取和对话输出分别约束为固定 JSON 结构，后续 agent 层必须按该结构解析。

知识点提取 Prompt 的输出格式建议：

```json
{
  "summary": "字符串",
  "knowledge_points": [
    {
      "title": "字符串",
      "description": "字符串",
      "confidence": 0.95
    }
  ]
}
```

预习对话 Prompt 的输出格式建议：

```json
{
  "reply": "字符串",
  "follow_up_questions": ["字符串", "字符串"]
}
```

### 4.3 [backend/app/preview/agent.py](backend/app/preview/agent.py)

当前状态：已实现基于阿里云百炼的模型调用封装，默认读取 backend/.env 中的配置，当前模型为 qwen3.6-plus，并包含 JSON 清洗与自动重试逻辑。

负责封装模型调用，不处理 HTTP，不直接做业务编排。建议定义一个 `PreviewAgent` 类。

建议方法：

1. `extract_knowledge_points(content_text: str) -> dict`
2. `chat(text: str, context_text: str, selected_knowledge_points: list[str], history: list[dict] | None = None) -> dict`

实现建议：

1. 在这里统一调用大模型 API。
2. 从环境变量读取模型配置，例如：
   - `LLM_BASE_URL`
   - `LLM_API_KEY`
   - `LLM_MODEL_NAME`
3. 封装统一的 `_call_llm_json(prompt)` 方法。
4. 做 JSON 解析失败重试。
5. 对模型返回做最小清洗，例如裁掉 markdown 代码块。
6. 当前实现已经落地 `.env -> agent.py` 的读取链路，密钥不写入代码文件。

如果后续你们决定用某个具体厂商，例如 OpenAI 兼容接口、讯飞星火、DeepSeek，这一层是唯一需要替换的地方。

### 4.4 [backend/app/preview/service.py](backend/app/preview/service.py)

负责业务编排，是 Preview 模块的核心服务层。建议定义：

1. `PreviewService`
2. `get_knowledge_points(request: PreviewKnowledgeRequest) -> PreviewKnowledgeData`
3. `preview_chat(request: PreviewChatRequest) -> PreviewChatData`

服务层职责：

1. 校验输入文本是否为空、过短、过长。
2. 调用 `PreviewAgent` 获取模型结果。
3. 对结果进行结构化整理，补充 `id` 等字段。
4. 写入本地日志。
5. 预留向 shared/Profile 上报的接口，不在第一版强依赖它。

建议这里再拆两个私有方法：

1. `_build_knowledge_point_ids()`
2. `_log_preview_interaction()`

### 4.5 [backend/app/api/preview_api.py](backend/app/api/preview_api.py)

只负责路由和请求响应转换，不写 Prompt，不写模型调用细节。

建议提供两个正式接口：

1. `POST /api/preview/knowledge-points`
2. `POST /api/preview/chat`

可选调试接口：

1. `GET /api/preview/health`

路由层职责：

1. 接收 Pydantic 请求模型。
2. 调用 `PreviewService`。
3. 包装统一响应结构。
4. 捕获异常并返回稳定错误码。

### 4.6 [backend/app/main.py](backend/app/main.py)

负责创建 FastAPI 应用并挂载路由。

建议内容：

1. 创建 `FastAPI(title="BlueTutor Backend")`
2. 注册 `preview_router`
3. 后续再注册 `guide_router`、`mistakes_router`、`shared_router`
4. 提供根路由 `/` 或 `/health`

## 5. 接口定义建议

### 5.1 知识点分析接口

路径：`POST /api/preview/knowledge-points`

请求体：

```json
{
  "user_id": "u001",
  "content_text": "本节内容介绍速度、时间和路程三者之间的关系",
  "source_type": "pdf_selection",
  "page_hint": 12,
  "session_id": "preview_s001"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "knowledge_points": [
      {
        "id": "kp_001",
        "title": "速度、时间、路程的关系",
        "description": "理解三个量之间的基本公式和换算关系",
        "confidence": 0.93
      }
    ],
    "summary": "该内容主要围绕速度、时间和路程三者关系展开。"
  }
}
```

异常响应建议：

```json
{
  "code": 4001,
  "message": "content_text is empty",
  "data": null
}
```

### 5.2 预习对话接口

路径：`POST /api/preview/chat`

请求体：

```json
{
  "user_id": "u001",
  "text": "什么是速度公式？",
  "context_text": "本节内容讲解速度、时间和路程之间的关系",
  "selected_knowledge_points": [
    "速度、时间、路程的关系",
    "速度公式"
  ],
  "session_id": "preview_s001",
  "history": []
}
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "reply": "速度公式是速度=路程÷时间，表示单位时间内通过的路程。",
    "follow_up_questions": [
      "如果已知速度和时间，怎样求路程？",
      "速度和时间有什么区别？"
    ]
  }
}
```

## 6. 与 shared 层的衔接方式

### 6.1 当前建议

第一版不强依赖 shared 层完成后再开发，而是先约定接口输入输出。

Preview 模块只认下面这类输入：

```json
{
  "user_id": "u001",
  "content_text": "OCR 或划选得到的文本"
}
```

这样 shared/OCR 做完后，直接把 `question_text` 改为预习场景下的 `content_text` 传入即可。

### 6.2 与用户画像模块的关系

预习模块可以预留一处轻量上报，不在第一版强耦合实现。建议记录：

1. `user_id`
2. `session_id`
3. `content_text`
4. `knowledge_points`
5. `selected_knowledge_points`
6. `question`
7. `reply`
8. `timestamp`

第一版可直接落本地 JSONL 文件，例如：

1. `backend/data/preview_logs/knowledge_points.jsonl`
2. `backend/data/preview_logs/chat.jsonl`

## 7. 模型调用建议

### 7.1 统一抽象

不要把厂商 SDK 代码散落在 service 或 api 层，统一放在 `agent.py`。

当前版本说明：

1. 当前已接入阿里云百炼平台的 OpenAI 兼容接口。
2. 当前默认模型为 `qwen3.6-plus`。
3. 当前配置文件放在 backend/.env，并由仓库根目录的 .gitignore 忽略。

### 7.2 输出约束

所有模型调用都要求返回 JSON。后端只接受结构化输出，不接受自由文本作为原始响应。

### 7.3 失败处理

建议至少做三类处理：

1. 网络异常：返回统一错误信息。
2. 模型返回非 JSON：自动重试 1 到 2 次。
3. 模型输出字段缺失：服务层补默认值，保证前端字段稳定。

## 8. 开发顺序

按下面顺序实现，返工最少：

1. 已完成 [backend/app/preview/schema.py](backend/app/preview/schema.py)
2. 已完成 [backend/app/preview/prompt.py](backend/app/preview/prompt.py)
3. 已完成 [backend/app/preview/agent.py](backend/app/preview/agent.py)
4. 再写 [backend/app/preview/service.py](backend/app/preview/service.py)
5. 再写 [backend/app/api/preview_api.py](backend/app/api/preview_api.py)
6. 最后在 [backend/app/main.py](backend/app/main.py) 注册路由

原因：

1. 先固定 schema，接口就稳了。
2. Prompt 和 agent 先写好，service 才容易编排。
3. api 层最后接，调试路径最短。

## 9. 第一版验收标准

满足以下条件即可视为 Preview 模块 MVP 完成：

1. 能启动 FastAPI 服务。
2. `POST /api/preview/knowledge-points` 能接收文本并返回知识点列表。
3. `POST /api/preview/chat` 能结合上下文返回预习型讲解。
4. 返回结构稳定，前端不需要猜字段。
5. 至少保留本地日志，便于后续接用户画像模块。

## 10. 第二版可扩展项

第一版完成后，再考虑下面这些增强：

1. 支持历史对话摘要压缩，减少 token 消耗。
2. 支持用户选择多个知识点后进行聚合讲解。
3. 加入“这一页重点是什么”的摘要接口。
4. 与 shared/Profile 打通，形成预习行为记录。
5. 增加 Prompt 版本号和模型版本号记录，便于比赛答辩说明。

## 11. 你当前应完成的具体交付物

如果按你的职责收敛，当前建议交付以下内容：

1. Preview 模块接口文档。
2. Preview 模块后端代码骨架。
3. 基于大模型的知识点提取能力。
4. 基于大模型的预习对话能力。
5. 本地日志记录能力。

这五项完成后，你的负责范围就已经完整且可演示。

当前进度：

1. 已完成接口文档。
2. 已完成 [backend/app/preview/schema.py](backend/app/preview/schema.py) 的请求响应模型定义。
3. 已完成 [backend/app/preview/prompt.py](backend/app/preview/prompt.py) 的系统提示词与 JSON 输出约束。
4. 已完成 [backend/app/preview/agent.py](backend/app/preview/agent.py) 的百炼模型调用封装。
5. 待实现 service、api 与 main 接入。