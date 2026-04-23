# Phase 3 Solution

`phase3_solution` 负责将题解内容转为可视化数据，输出可直接给前端渲染的 SVG 片段集合。

## 1. 职责边界

本模块负责：

- 根据会话状态规划题解可视化步骤
- 为每个步骤生成 SVG 片段并做合法性校验
- 单步失败时降级，不阻塞整体返回
- 识别题型并返回 `problem_type`

本模块不负责：

- 题目解析（`phase1_parser`）
- 多轮对话引导（`phase2_dialogue`）
- 题解文本本身生成（由 `phase2_dialogue/solution_generator.py` 负责）

## 2. 文件与核心符号

- `visualization.py`
  - `generate_visualization(session_state)`：对外主入口
  - `_call_planner(...)`：图意规划阶段
  - `_call_svg_generator(...)`：单步 SVG 生成阶段
  - `_generate_single_svg(...)`：单步重试与降级封装
  - `_validate_and_wrap_svg(...)`：XML 校验并补全 `<svg>` 外层
  - `_detect_problem_type(...)`：题型识别
- `prompts.py`
  - `VISUAL_PLANNER_PROMPT`：规划每个 `[VISUAL:step_id]` 要画什么
  - `SVG_GENERATOR_PROMPT`：把单步图意转成 SVG 内部元素

## 3. 输入输出契约

### 输入

`generate_visualization(session_state: SessionState)` 主要使用：

- `session_state.parsed_problem`
- `session_state.get_solution_path()`
- `session_state.solution`（若无则使用“题解尚未生成”占位）

### 输出

返回结构：

```json
{
  "problem_type": "chicken_rabbit | distance | unknown",
  "visuals": [
    {
      "step_id": "step_overview",
      "title": "...",
      "svg": "<svg>...</svg>"
    }
  ]
}
```

## 4. 两阶段生成流程

1. 规划阶段（一次调用）
   - 使用 `VISUAL_PLANNER_PROMPT`
   - 目标：产出每个步骤的 `step_id/title/visual_type/description/layout_hint`
2. 绘图阶段（按步骤逐个调用）
   - 使用 `SVG_GENERATOR_PROMPT`
   - 目标：生成 SVG 内部元素，再由后端统一包裹 `<svg>`

这样拆分的好处是：先统一“画什么”，再逐步“怎么画”，便于定位问题与局部降级。

## 5. 校验、重试与降级

- 每个步骤生成后会进行 XML 解析校验（`ElementTree.fromstring`）。
- 单步失败会重试 1 次（共最多 2 次尝试）。
- 单步最终失败时返回 `_FALLBACK_SVG` 占位图，不影响其他步骤。
- 若规划阶段整体失败，返回 `problem_type="unknown"` + 单个降级可视化步骤。

## 6. 题型识别规则

`_detect_problem_type` 当前基于关键词统计：

- 命中鸡兔同笼关键词 >= 2：`chicken_rabbit`
- 命中行程题关键词 >= 2：`distance`
- 其余：`unknown`

该规则是轻量启发式识别，适合作为前端展示分流的初版策略。

## 7. 与上层模块关系

- 在 `backend/app/guide/session.py` 中由 `SocraticTutorSession.generate_visualization()` 调用。
- 在 `backend/app/guide/session_manager.py` 中由 `generate_visualization(session_id)` 包装并持久化。
- 在 `backend/app/api/guide_api.py` 中通过：
  - `POST /api/guide/sessions/{session_id}/visualization` 触发生成
  - `GET /api/guide/sessions/{session_id}/visualization` 获取已生成结果

## 8. 开发注意事项

- `VISUAL_PLANNER_PROMPT` 默认依赖题解中的 `[VISUAL:step_id]` 占位标记，需与题解生成规则保持一致。
- 若扩展新题型，建议同步更新：
  - `_detect_problem_type` 关键词
  - planner 对应 `visual_type` 的策略
  - SVG 规范与测试样例
- 任何 SVG 规范变更都应优先保证 XML 可解析和前端可直接渲染。

