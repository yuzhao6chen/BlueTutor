# 错题模块后续智能能力设计与总体规划

## 1. 文档目的

本文档用于承接错题模块当前已完成的非多 agent 后端能力，并给出后续多 agent 智能功能的可实施设计方案。

目标不是空泛讨论，而是为后续真正落地以下能力提供可直接执行的设计依据：

1. 智能推荐相似题 / 变式题
2. 学生能力画像与学习报告
3. 错题重做引导式对话
4. 提示生成
5. 图解展示
6. 对话过程记录、过程评估、结果回写错题资产

本文档默认前提：

1. 当前 `mistakes` 基础后端已具备
2. `guide -> mistakes` 联调已无阻塞
3. Android 前端暂不在本轮改动范围内
4. 当前先保留规则版接口，后续再逐步替换为智能版

## 2. 当前已完成后端能力基线

当前 `mistakes` 模块已经具备：

1. 错题报告写入
2. 错题列表
3. 错题详情
4. 错题状态更新
5. 错题统计
6. 时间轴
7. 首页摘要
8. `solution` 兼容消费
9. 讲义 / 复盘结构化接口
10. 列表 / 时间轴筛选
11. 规则版每日提分计划

这意味着后续多 agent 系统不需要重复发明“错题资产层”，而是应建立在已有 `mistakes` 资产层之上。

## 3. 错题模块总体功能分层规划

建议后续把错题模块拆成四层：

### 3.1 资产层

负责“存什么”。

核心对象：

1. 原始错题报告
2. 讲义结构化内容
3. 每次重做过程记录
4. 每次引导对话记录
5. 每次推荐题记录
6. 用户能力画像快照

### 3.2 规则编排层

负责“什么时候触发什么功能”。

示例：

1. 首页点击“开始今日提分”时先取今日计划
2. 点进某题后优先看是否有历史重做记录
3. 若该题已有题解，先允许“回顾讲义”或“直接重做”
4. 若学生连续卡住两轮，再允许 agent 提升提示强度

### 3.3 智能代理层

负责“怎么推理、怎么生成”。

这里才是真正的多 agent 系统。

### 3.4 展示协议层

负责“最终给前端什么结构”。

重点是：

1. 文本必须适合手机屏展示
2. 文本长度要受控
3. 不把大模型原始长文本直接丢给前端
4. 所有智能输出都必须先归一化成稳定结构

## 4. 多 agent 系统的总体原则

### 4.1 不让单个 agent 包办一切

要避免一个大 prompt 同时做：

1. 题目分析
2. 出题
3. 提示生成
4. 选项生成
5. 学生答案评估
6. 下步决策
7. 报告生成

这样容易不稳定。

### 4.2 采用“协调者 + 专职 agent”结构

建议采用：

1. `orchestrator` 协调者
2. `problem-analyzer` 题目分析 agent
3. `variant-generator` 相似题 / 变式题生成 agent
4. `option-generator` 选项生成 agent
5. `hint-generator` 提示生成 agent
6. `response-evaluator` 学生响应评估 agent
7. `pedagogy-planner` 教学推进 agent
8. `diagram-planner` 图解规划 agent
9. `report-writer` 过程报告生成 agent
10. `profile-synthesizer` 学习画像 agent

### 4.3 每个 agent 只做一件事

例如：

- `option-generator` 只负责把当前轮问题转成 2~4 个适合手机点选的选项
- `response-evaluator` 只负责判断学生回答是否正确、是否部分正确、是否暴露了新的误区
- `pedagogy-planner` 只负责决定下一轮是继续追问、给提示、回退一步还是结束

### 4.4 所有 agent 输出都必须结构化

禁止直接让前端消费自然语言原文。

每个 agent 的输出都必须是 JSON 协议，例如：

```json
{
  "decision": "give_hint",
  "reason": "student_stuck_on_speed_difference",
  "confidence": 0.88
}
```

## 5. 功能一：智能推荐相似题 / 变式题

### 5.1 目标

为“为你推荐”“再做一题”“智能选题”区域提供真正的智能题目。

### 5.2 输入

1. 目标错题原报告
2. 用户近期错题集合
3. 已掌握 / 未掌握标签
4. 最近练习历史
5. 当前年级与知识范围

### 5.3 agent 分工

#### A. `problem-analyzer`

输入：原错题报告

输出：

1. 题型
2. 核心知识点
3. 关键解法模式
4. 当前错因
5. 可变化维度

示例输出：

```json
{
  "problem_type": "追及问题",
  "knowledge_tags": ["行程问题", "速度差"],
  "method_pattern": "先求先走路程，再求速度差，再求追及时间",
  "error_focus": "未先求速度差",
  "variant_axes": ["数字替换", "背景替换", "已知量改写"]
}
```

#### B. `variant-generator`

基于分析结果生成：

1. 相似题
2. 变式题
3. 难度微调题

输出必须包含：

1. 题目正文
2. 标准答案
3. 核心步骤
4. 难度等级
5. 与原题关系说明

#### C. `quality-checker`（后续可与协调者合并）

检查：

1. 题目是否可解
2. 条件是否充分
3. 答案是否一致
4. 难度是否越界
5. 是否真的围绕原错因

### 5.4 后端建议接口

建议后续新增：

1. `POST /api/mistakes/recommendations/generate`
2. `GET /api/mistakes/recommendations`
3. `GET /api/mistakes/recommendations/{recommendation_id}`
4. `POST /api/mistakes/recommendations/{recommendation_id}/submit`

### 5.5 返回给前端的展示结构

```json
{
  "recommendation_id": "rec_xxx",
  "type": "variant",
  "difficulty": "medium",
  "title": "变式题",
  "question": "已知路程为3600米，两种交通方式时间比为5:2，分别求两种速度。",
  "origin_report_id": "mr_xxx",
  "knowledge_tags": ["行程问题", "速度时间路程"],
  "why_recommended": "你最近在速度差和时间关系上容易混淆，这题正好训练这一点。"
}
```

## 6. 功能二：学习画像与详细能力报告

### 6.1 目标

基于用户全部错题与重做历史，生成立体学习画像。

### 6.2 画像不应只是简单统计

需要至少覆盖：

1. 知识薄弱点
2. 常见错因模式
3. 解题独立性
4. 容易卡住的步骤类型
5. 进步趋势
6. 适合的教学方式

### 6.3 agent 分工

#### A. `profile-synthesizer`

整合：

1. 错题报告集合
2. 时间维度趋势
3. 状态流转
4. 重做结果
5. 对话中暴露的疑惑标签

输出画像 JSON：

```json
{
  "strengths": ["基础列式速度较快"],
  "weaknesses": ["速度差问题", "单位换算稳定性不足"],
  "error_patterns": [
    {
      "type": "concept_confusion",
      "label": "容易把速度和路程关系混淆",
      "count": 6
    }
  ],
  "independence_level": "medium_low",
  "trend": "improving",
  "teaching_preferences": ["先给一个方向提示，再让学生自己列式"]
}
```

#### B. `profile-writer`

把画像压缩成适合手机端展示的模块化文案：

1. 一句话总结
2. 优势卡片
3. 待加强卡片
4. 最近进步
5. 建议训练顺序

### 6.4 后端建议接口

1. `GET /api/mistakes/profile/summary`
2. `GET /api/mistakes/profile/report`
3. `POST /api/mistakes/profile/refresh`

## 7. 功能三：错题重做引导式对话

这是后续最核心、也是最复杂的部分。

### 7.1 目标

不是“再讲一遍”，而是引导学生自己重新做出来。

### 7.2 要解决的问题

系统必须在每一轮都能判断：

1. 当前该问什么
2. 该给几个选项
3. 要不要给提示
4. 学生回答是不是对
5. 学生是不会、半会，还是纯打字表达不清
6. 何时判定该题已真正完成
7. 何时提供“消除错题”按钮

### 7.3 会话状态机建议

建议采用显式状态机，而不是让模型自由发挥。

状态示例：

1. `ready`
2. `asking_step`
3. `waiting_choice`
4. `waiting_free_text`
5. `giving_hint`
6. `showing_diagram`
7. `checking_final_answer`
8. `solved`
9. `failed`
10. `archived`

### 7.4 会话核心数据结构

建议新增：

1. `redo_session_id`
2. `origin_report_id`
3. `session_type`
  - `redo_original`
  - `practice_variant`
4. `current_stage`
5. `turns`
6. `mistake_events`
7. `confusion_events`
8. `resolved`
9. `mastery_recommendation`

### 7.5 agent 分工

#### A. `pedagogy-planner`

输入：

1. 原错题报告
2. 当前会话状态
3. 学生上轮回答评估结果

输出：

1. 下一步教学目标
2. 互动模式
  - 单选
  - 自由文本
  - 提示
  - 图解
3. 期望学生完成的最小认知动作

示例：

```json
{
  "goal": "identify_speed_difference",
  "interaction_mode": "single_choice",
  "target_skill": "recognize_relative_speed",
  "need_hint": false
}
```

#### B. `option-generator`

若当前轮适合单选，则生成：

1. 2~4 个选项
2. 仅 1 个最优选项
3. 干扰项要与常见错因一致

输出：

```json
{
  "question": "小明想追上小红，第一步最该先算什么？",
  "options": [
    {"id": "A", "text": "小红先走的路程"},
    {"id": "B", "text": "两人的速度和"},
    {"id": "C", "text": "小明走了几分钟"}
  ],
  "correct_option_id": "A"
}
```

#### C. `hint-generator`

输出必须分层：

1. 轻提示
2. 中提示
3. 强提示

避免一上来直接给答案。

#### D. `response-evaluator`

评估学生输入：

1. 是否正确
2. 是否部分正确
3. 是否暴露新的错误类型
4. 是否只是表达问题
5. 是否说明需要回退一步

#### E. `diagram-planner`

当当前轮适合图解时，输出图解规范，而不是只返回一段话。

示例：

```json
{
  "diagram_type": "distance_chase_line",
  "elements": [
    {"type": "segment", "label": "小红先走路程", "value": "200米"},
    {"type": "arrow", "label": "速度差", "value": "20米/分"}
  ],
  "caption": "先补上小红先走的那段路，再用速度差追。"
}
```

#### F. `report-writer`

会话结束后生成重做报告。

## 8. 功能四：提示按钮

### 8.1 目标

不是一个固定提示，而是与当前步骤强相关。

### 8.2 提示等级机制

建议每轮都维护 `hint_level`：

1. `0`：未提示
2. `1`：轻提示
3. `2`：中提示
4. `3`：强提示

### 8.3 触发规则

1. 学生主动点击“给我个提示”
2. 学生连续两轮答错
3. 学生长时间停滞
4. `pedagogy-planner` 判断卡点明显

### 8.4 输出格式

```json
{
  "hint_level": 2,
  "hint_text": "想一想，小红已经先走了5分钟，这5分钟对应的路程要不要先算出来？",
  "hint_type": "question_prompt"
}
```

## 9. 功能五：图解展示

### 9.1 目标

图解不是纯图片生成，而是先做“图解规划协议”。

### 9.2 为什么先做规划协议

因为直接接图像模型风险太高：

1. 输出不稳定
2. 文字标注不可控
3. 与数学题结构对不齐

### 9.3 推荐路线

先做三阶段：

1. `diagram-planner` 输出结构化图解描述
2. 后端把描述转成前端可绘制 schema
3. Android 用 Canvas / Compose 自绘

只有后续确实需要，再加图片生成 agent。

### 9.4 推荐支持的图解类型

第一批只做最有价值的几类：

1. 行程线段图
2. 鸡兔同笼表格图
3. 分数关系图
4. 长方形 / 周长面积关系图
5. 数量关系条形图

## 10. 功能六：会话结束与错题消除判定

### 10.1 目标

不是学生答出一个结果就立刻“消除”。

### 10.2 判定维度

建议至少看：

1. 最终答案是否正确
2. 关键步骤是否真的理解
3. 是否依赖强提示才完成
4. 是否出现重复旧错误
5. 对相似变式题是否也能通过

### 10.3 建议输出

```json
{
  "session_result": "solved_with_minor_prompt",
  "can_clear_mistake": true,
  "should_assign_variant": false,
  "mastery_confidence": 0.82
}
```

### 10.4 “消除错题”按钮逻辑

仅在以下条件同时满足时出现：

1. 当前是重做原错题，不是推荐新题
2. 最终答案正确
3. 关键步骤判断通过
4. 提示依赖不高于阈值

若是推荐新相似题：

- 不显示“消除原错题”
- 若新题也做错，则按新的普通错题写入 `mistakes`

## 11. 对话过程如何回写为报告

### 11.1 两类报告要区分

#### A. 重做原错题

不是创建全新独立错题，而是新增“重做记录”。

建议新增：

1. `redo_records[]`
2. `latest_redo_summary`
3. `mastery_history[]`

#### B. 智能推荐的新相似题 / 变式题

如果学生做错，应作为新的普通错题入库。

建议字段：

1. `source_type = recommendation_variant`
2. `origin_report_id`
3. `recommendation_id`

### 11.2 重做报告结构建议

```json
{
  "session_type": "redo_original",
  "origin_report_id": "mr_xxx",
  "final_result": "solved",
  "turn_count": 6,
  "hint_usage_count": 1,
  "confusions": ["速度差概念仍不稳"],
  "new_error_profile": [
    {
      "error_type": "concept_confusion",
      "detail": "仍会把速度和路程混淆"
    }
  ],
  "mastery_recommendation": "can_clear"
}
```

## 12. 后端目录建议

后续多 agent 功能建议放在 `backend/app/mistakes/` 下扩展，而不是散落到别处。

建议目录：

1. `backend/app/mistakes/agents/`
  - 各 agent 定义
2. `backend/app/mistakes/orchestrator/`
  - 多 agent 编排
3. `backend/app/mistakes/session/`
  - 重做会话状态机
4. `backend/app/mistakes/recommendation/`
  - 推荐题生成与存储
5. `backend/app/mistakes/profile/`
  - 学习画像
6. `backend/app/mistakes/diagram/`
  - 图解 schema 与渲染协议
7. `backend/app/mistakes/store.py`
  - 继续复用资产存储入口，必要时拆分子存储文件

## 13. 建议实现顺序

后续真正做多 agent 时，推荐严格按顺序推进：

### 第 1 阶段

1. 推荐题数据模型
2. 推荐题生成接口
3. 推荐题提交与结果记录

### 第 2 阶段

1. 重做会话状态机
2. 单轮规划 agent
3. 单选题选项 agent
4. 答案评估 agent
5. 提示 agent

### 第 3 阶段

1. 会话结束判定
2. 重做报告生成
3. 原错题消除逻辑
4. 新推荐题错题化逻辑

### 第 4 阶段

1. 图解规划 agent
2. 图解 schema
3. 前端绘制协议

### 第 5 阶段

1. 学习画像聚合
2. 画像报告生成
3. 首页推荐计划从规则版升级到智能版

## 14. 你当前这套错题模块的合理页面 / 能力布局建议

### 14.1 错题首页

当前后端已可支撑：

1. 统计卡片
2. 薄弱知识点
3. 时间轴
4. 规则版每日提分计划

后续智能升级点：

1. 推荐题轮播
2. 智能计划摘要
3. 今日最值得重做的 1~3 题

### 14.2 错题本列表页

当前后端已可支撑：

1. 全量列表
2. 待巩固 / 已掌握筛选
3. 知识点筛选
4. 是否有题解筛选

### 14.3 错题详情页

当前后端已可支撑：

1. 完整原报告
2. 原题
3. 错因
4. 思维链
5. 题解摘要

### 14.4 错题讲义页 / 复盘页

当前后端已可支撑：

1. 讲义区块
2. 复盘步骤
3. 复盘要点

### 14.5 今日提分页

当前建议先接规则版 `daily-plan`。

后续升级为：

1. 计划由 agent 动态生成
2. 点一项进入“重做原错题”或“做推荐题”

### 14.6 引导重做页

这会是多 agent 核心主战场。

页面应支持：

1. 顶部题目卡片
2. 中间对话区
3. 底部输入框
4. 单选按钮区
5. 提示按钮
6. 图解按钮
7. 完成后“消除错题”按钮

## 15. 当前最优落地结论

如果要保证后续实现顺畅、风险可控，最合理策略是：

1. 保留当前已完成的非智能后端能力作为稳定底座
2. 先让 Android 可以联调首页、列表、详情、讲义、规则版计划
3. 多 agent 先从“推荐题生成”开始，不要一上来就做完整引导对话
4. 重做引导对话必须建立显式状态机，不要只靠 prompt 自由生成
5. 图解优先做结构化绘制协议，不要直接依赖图片模型
6. 学习画像放在推荐题和引导对话之后做，数据会更完整

## 16. 对你当前职责范围的建议总结

你当前负责错题后端，最合理的职责边界是：

1. 维护错题资产层
2. 设计并落地稳定接口协议
3. 实现多 agent 编排与状态机
4. 不去改 Android 前端实现
5. 不去侵入 guide 模块逻辑

这样能最大程度符合团队协作约束，也最利于后续逐步把 UI 图里的智能能力真正落地。