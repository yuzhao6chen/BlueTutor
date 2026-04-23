# Guide Module

`guide` 是后端的苏格拉底式讲题核心模块，负责把学生题目输入转为多轮引导对话，并在会话结束后生成报告、题解和可视化数据。

## 1. 模块边界

- 对外入口（API 路由）：`backend/app/api/guide_api.py`
- 会话门面类：`backend/app/guide/session.py` 中的 `SocraticTutorSession`
- 会话编排层：`backend/app/guide/session_manager.py`
- 会话持久化层：`backend/app/guide/session_store.py`

当前代码中，`guide_api.py` 主要提供以下能力：

- 创建会话：`POST /api/guide/sessions`
- 获取会话列表与详情：`GET /api/guide/sessions`、`GET /api/guide/sessions/{session_id}`
- 执行一轮对话（非流式/流式）：`POST /turns`、`POST /turns/stream`
- 获取思维树：`GET /thinking-tree`
- 生成报告：`POST /report`
- 生成/获取题解：`POST /solution`、`POST /solution/stream`、`GET /solution`
- 生成/获取可视化：`POST /visualization`、`GET /visualization`

## 2. 目录职责

```text
guide/
  phase1_parser/      # 第一阶段：题目解析
  phase2_dialogue/    # 第二阶段：对话流程与状态更新
  phase3_solution/    # 第三阶段：可视化生成
  data/sessions/      # 会话 JSON 持久化目录
  schema.py           # Guide API 的请求/响应模型
  session.py          # 单会话门面（SocraticTutorSession）
  session_manager.py  # 会话生命周期与缓存/持久化编排
  session_store.py    # 文件存储（index.json + session_id.json）
```

## 3. 核心流程（高层）

1. 创建会话：`create_session(problem_text)`
2. 内部初始化：`SocraticTutorSession(problem_text)`
   - 调用 `phase1_parser.parse_problem` 解析题目
   - 构造 `SessionState`
   - 构建对话图（`create_dialogue_graph` / `create_pre_graph`）
3. 每轮对话：`run_turn` 或 `run_turn_stream`
   - 更新会话状态（如 `dialogue_history`、`thinking_tree`、`is_solved`）
   - 轮次结束后持久化会话 JSON
4. 对话后能力：
   - 报告：`generate_report`
   - 题解：`generate_solution` / `stream_solution`
   - 可视化：`generate_visualization`

## 4. 状态与存储

- 内存缓存：`session_manager.py` 中 `_cache: dict[str, SocraticTutorSession]`
- 文件存储目录：`guide/data/sessions/`
- 索引文件：`guide/data/sessions/index.json`
- 单会话文件：`guide/data/sessions/{session_id}.json`

会话读取策略：优先读内存缓存；缓存未命中时从 JSON 文件恢复。

## 5. 开发约定（当前版本）

- 业务异常统一使用 `TutorSessionError` 表达。
- 流式接口采用 SSE，事件类型见 `guide_api.py` 中注释（如 `token`、`retry`、`done`、`error`）。
- 题解和可视化生成均带缓存：若已生成，会直接返回已缓存结果。

## 6. 后续文档拆分建议

为了保持文档可维护性，建议在以下目录继续补充 README：

- `backend/app/guide/phase1_parser/README.md`
- `backend/app/guide/phase2_dialogue/README.md`
- `backend/app/guide/phase3_solution/README.md`
- `backend/app/guide/data/sessions/README.md`

建议阅读顺序：`phase1_parser` -> `phase2_dialogue` -> `phase3_solution` -> `data/sessions`。

