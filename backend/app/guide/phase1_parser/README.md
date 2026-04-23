# Phase 1 Parser

`phase1_parser` 负责把学生原始题目文本解析为结构化信息，供后续对话与状态管理使用。

## 1. 职责边界

本模块只做两件事：

- 提取题目核心已知条件（`known_conditions`）
- 提取求解目标（`goal`）并给出标准答案（`answer`）

本模块不负责：

- 生成讲解步骤
- 多轮对话引导
- 题解可视化

这些能力分别由 `phase2_dialogue`、`phase3_solution` 提供。

## 2. 文件与符号

- `parser.py`
  - `ParsedProblem`：解析结果的数据模型
  - `parse_problem(problem_text: str)`：解析入口函数
- `prompts.py`
  - `PARSE_PROMPT`：解析阶段使用的提示词模板

## 3. 输入输出契约

### 输入

- `problem_text: str`（学生提交的原始题目文本）

### 输出

`parse_problem` 返回 `ParsedProblem`，包含 3 个字段：

- `known_conditions: list[str]`：题目核心已知条件
- `goal: str`：题目求解目标
- `answer: str`：标准答案

> 约定：`answer` 应是最终答案表达，不是完整解题过程。

## 4. 执行链路

`parse_problem` 的当前调用链如下：

1. 通过 `get_llm()` 获取模型实例
2. 用 `JsonOutputParser(pydantic_object=ParsedProblem)` 约束输出结构
3. 组装链路：`PARSE_PROMPT | llm | parser`
4. 调用 `chain.invoke({"problem": problem_text})`

该设计的目标是保证上游输入为自然语言题目、下游输出为稳定 JSON 结构。

## 5. 上下游关系

- 上游调用方：`backend/app/guide/session.py` 中 `SocraticTutorSession.__init__`
- 下游消费方：`backend/app/guide/phase2_dialogue/session_state.py` 中 `SessionState.parsed_problem`

即：会话创建时先解析题目，再把解析结果作为固定上下文带入后续对话阶段。

## 6. 异常与注意事项

- 本模块依赖 LLM 可用性（由 `llm_provider` 提供）。
- 当模型输出不符合 `ParsedProblem` 结构时，会在解析链路抛出异常。
- 这些异常会在上层会话初始化中被包装为 `TutorSessionError`。

## 7. 开发建议

- 修改 `ParsedProblem` 字段时，需同步检查：
  - `PARSE_PROMPT` 输出格式说明
  - `SessionState.parsed_problem` 的使用点
  - 相关 API 返回与前端消费逻辑
- 若后续需要支持 OCR 噪声文本，建议优先在 prompt 层做鲁棒性增强，再评估 schema 扩展。

