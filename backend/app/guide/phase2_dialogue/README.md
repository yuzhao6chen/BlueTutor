# Phase 2 Dialogue

`phase2_dialogue` 负责讲题会话中的多轮对话核心逻辑：跟踪学生状态、分析学习情境、生成引导问题、执行泄题审核，并在会话结束后生成报告和题解。

## 1. 职责边界

本模块负责：

- 维护并更新 `SessionState`（思维树、对话历史、卡点状态）
- 执行一轮对话主流程（非流式 / 流式）
- 对教师回复执行 Guardrail 审核与重试
- 基于会话状态生成报告与题解文本

本模块不负责：

- 初始题目解析（由 `phase1_parser` 负责）
- 题解可视化数据生成（由 `phase3_solution` 负责）

## 2. 核心状态模型

### `SessionState`（`session_state.py`）

关键字段：

- `parsed_problem`：Phase1 的结构化解析结果
- `thinking_tree`：思维树（`node_id -> ThinkingNode`）
- `dialogue_history`：完整对话记录
- `current_stuck_node_id` / `stuck_count`：当前卡点与引导次数
- `last_updated_node_id`：最近被操作节点
- `is_solved`：是否已解出题目
- `solution` / `visualization`：后处理缓存字段

### `ThinkingNode` 与 `NodeStatus`

- `ThinkingNode` 表示一个认知步骤，含 `parent_id`、`error_history`、`children`
- `NodeStatus` 枚举：`correct` / `incorrect` / `stuck` / `abandoned`

### `DialogueGraphState`（`dialogue_graph_state.py`）

单轮图执行时的上下文容器：

- `session_state`
- `student_input`
- `generated_question`
- `rejection_reason`
- `retry_count`
- `teaching_guidance`

## 3. 对话流程

### 非流式流程（`create_dialogue_graph`）

执行路径：

`state_tracker -> (completion_responder | situation_analyzer -> question_generator -> guardrail -> [retry or end])`

规则：

- `state_tracker` 发现 `is_solved=True` 时，直接进入 `completion_responder` 并结束
- 未解出时进入常规引导链路
- `guardrail` 不通过时回到 `question_generator` 重写

### 流式流程（`create_pre_graph` + 图外循环）

- 图内仅执行前半段：`state_tracker -> situation_analyzer`
- 图外循环执行：`stream_question_generator` + `run_guardrail`
- 审核打回时输出 `__RETRY__`，通过后逐 token 输出，必要时输出 `__SOLVED__`
- 达到重试上限后使用 `FALLBACK_QUESTION`

## 4. Guardrail 机制

文件：`guardrail.py`

- `run_guardrail`：审核是否泄题，返回通过/打回结果
- `should_retry`：条件路由函数（继续重试或结束）
- `MAX_RETRY = 4`：最大打回次数
- `FALLBACK_QUESTION`：超过重试上限后的兜底问题

设计目标：在保证引导质量的同时，尽量避免直接泄露标准答案或超前步骤。

## 5. 关键文件职责

- `state_tracker.py`：解析学生新输入，更新思维树和状态
- `situation_analyzer.py`：分析情绪与学习状态，生成教学风格建议
- `question_generator.py`：生成苏格拉底式引导问题（含流式版本）
- `guardrail.py`：审核问题是否合规，决定重试或放行
- `completion_responder.py`：题目解出后的收尾回复
- `report_generator.py`：生成结构化讲题报告
- `solution_generator.py`：生成个性化题解（含流式版本）
- `dialogue_loop.py`：组装 LangGraph 工作流
- `prompts.py`：集中维护各 Agent 提示词模板

## 6. 与上层的集成关系

- 会话入口：`backend/app/guide/session.py`
  - 非流式：`run_one_turn`
  - 流式：`run_one_turn_stream`
- 编排与持久化：`backend/app/guide/session_manager.py`

即，`phase2_dialogue` 负责“单轮能力”，`session.py`/`session_manager.py` 负责“会话生命周期”。

## 7. 开发注意事项

- 修改 `SessionState` 字段时，需要同步检查：序列化/反序列化（`to_dict`、`from_dict`）及持久化兼容性。
- 修改 `DialogueGraphState` 字段时，需要同步更新所有节点函数的输入输出字典。
- 调整 Guardrail 规则时，需同步验证流式路径（图外循环）与非流式路径行为一致性。
- `prompts.py` 体量较大，建议通过小步迭代修改，并保留回归样例验证效果。

