# BlueTutor 错题模块 — 系统架构与功能流程文档

---

## 一、模块总览

BlueTutor 错题模块是一个面向小学生的**错题闭环学习系统**，覆盖从"错题采集"到"掌握验证"的完整学习链路。模块以多 Agent 架构驱动大模型能力，实现个性化推荐、引导式重做、讲题验证等智能功能。

### 1.1 核心设计理念

| 理念 | 说明 |
|------|------|
| **闭环学习** | 每一道错题都经历"采集 → 讲义回顾 → 巩固练习 → 掌握验证"的完整闭环 |
| **多 Agent 协作** | 不同 Agent 各司其职（分析、生成、审核、引导、评估），通过 Prompt 编排协同完成复杂任务 |
| **用户画像驱动** | 所有推荐和引导都基于用户画像个性化，而非通用模板 |
| **优雅降级** | 大模型不可用时自动回退到规则引擎，保证接口始终可用 |
| **增量更新** | 用户画像随新报告增量更新，无需全量重建 |

### 1.2 模块分层架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Android 前端                                 │
│   首页卡片 │ 时间轴 │ 错题详情 │ 讲义页 │ 重做 │ 讲题验证 │ 画像页   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ RESTful JSON API
┌──────────────────────────────▼──────────────────────────────────────┐
│                      API 层 (mistakes_api.py)                        │
│  20 个端点 · 统一响应格式 · 错误码体系 · 参数校验                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                    服务层 (service.py)                                │
│  MistakeService · 业务编排 · 状态管理 · 画像构建 · 对话管理            │
└──────────┬───────────────────────────────────────┬──────────────────┘
           │                                       │
┌──────────▼──────────┐               ┌───────────▼──────────────────┐
│  多 Agent 层         │               │  持久化层 (store.py)          │
│  (agent.py)          │               │  JSON 文件存储 · 索引机制      │
│  LLM 调用 + 降级回退  │               │  reports / profiles /         │
│  Prompt 编排         │               │  redo_sessions / dialogue /   │
│                      │               │  recommendations              │
└──────────┬──────────┘               └──────────────────────────────┘
           │
┌──────────▼──────────┐
│  Prompt 层           │
│  (prompt.py)         │
│  12 个 Prompt 模板   │
│  结构化 JSON 输出    │
└─────────────────────┘
```

---

## 二、数据实体关系图

```
┌──────────────────┐        1:N        ┌──────────────────────┐
│   用户 (user_id)  │──────────────────▶│   错题报告 (Report)    │
│                   │                   │   report_id           │
│                   │                   │   status: pending /   │
│                   │                   │           mastered    │
│                   │                   │   problem             │
│                   │                   │   thinking_chain      │
│                   │                   │   error_profile       │
│                   │                   │   independence_eval   │
│                   │                   │   solution            │
│                   │                   └──────┬───────────────┘
│                   │                          │
│                   │        1:1               │ 1:N
│                   │──────────────────┐       │
│                   │                  ▼       ▼
│            ┌──────▼──────┐   ┌──────────────────────┐
│            │ 用户画像      │   │   推荐题 (Recommendation) │
│            │ (UserProfile)│   │   recommendation_id   │
│            │ weak_points  │   │   type: similar /     │
│            │ error_dist   │   │         variant       │
│            │ independence │   │   question + options  │
│            │ style_hints  │   │   agent_traces        │
│            └─────────────┘   └──────┬───────────────┘
│                                    │
│                                    │ 1:1 (可选)
│                                    ▼
│                            ┌──────────────────────┐
│                            │  重做会话 (RedoSession) │
│                            │  session_id           │
│                            │  stage: 5 阶段         │
│                            │  history + hint       │
│                            │  can_clear_mistake    │
│                            └──────────────────────┘
│
│        Report 1:N ──────────────────────────────────────┐
│                                                         │
│                                                         ▼
│                                                ┌──────────────────────┐
│                                                │ 讲题验证对话 (Dialogue) │
│                                                │ session_id           │
│                                                │ messages[]           │
│                                                │ mastery_verdict      │
│                                                │ similar_question     │
│                                                └──────────────────────┘
└─────────────────────────────────────────────────────────────────────
```

---

## 三、核心功能流程图

### 3.1 全局闭环流程

```
  ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
  │  讲题模块  │     │  错题采集  │     │  讲义回顾  │     │  巩固练习  │     │  掌握验证  │
  │  (Guide)  │────▶│  (Ingest) │────▶│(Lecture) │────▶│(Practice)│────▶│(Verify)  │
  └──────────┘     └────┬─────┘     └──────────┘     └────┬─────┘     └────┬─────┘
                        │                                    │                │
                        │  自动更新                            │                │
                        ▼                                    │                │
                  ┌──────────┐                               │                │
                  │ 用户画像   │◀──────────────────────────────┘                │
                  │(Profile)  │                                                 │
                  └────┬─────┘                                                 │
                       │ 画像驱动推荐                                            │
                       ▼                                                        │
                 mastered ◀────────────────────────────────────────────────────┘
```

### 3.2 功能 A：错题报告采集与画像更新

```
Guide 模块完成讲题
        │
        ▼
┌──────────────────────────────────────────────────────────────────┐
│  POST /api/mistakes/report/ingest                                 │
│                                                                    │
│  输入：user_id + report(problem, thinking_chain, error_profile,   │
│        independence_evaluation, solution)                          │
│                                                                    │
│  ┌─────────────┐   ┌──────────────┐   ┌────────────────────┐     │
│  │ 报告规范化   │──▶│ 派生字段生成  │──▶│ 持久化 (JSON 文件)  │     │
│  │ solution清理│   │ report_id    │   │ reports/{id}.json   │     │
│  │ 空白过滤    │   │ problem_prev │   │ index.json 更新     │     │
│  └─────────────┘   │ has_solution │   └────────────────────┘     │
│                    │ solution_prev│                               │
│                    │ primary_error│                               │
│                    └──────┬───────┘                               │
│                           │                                       │
│                           ▼                                       │
│                 ┌─────────────────────┐                           │
│                 │  用户画像增量更新     │                           │
│                 │                     │                           │
│                 │  有新错误类型/知识点？ │                           │
│                 │     ┌───YES───┐     │                           │
│                 │     │         │     │                           │
│                 │     ▼         ▼     │                           │
│                 │  LLM 智能更新  仅计数+1 │                           │
│                 │  (profile-     (不调用 │                           │
│                 │   updater      LLM)  │                           │
│                 │   agent)             │                           │
│                 │     │         │     │                           │
│                 │     └────┬────┘     │                           │
│                 │          ▼          │                           │
│                 │  画像持久化           │                           │
│                 │  profiles/{uid}.json│                           │
│                 └─────────────────────┘                           │
└──────────────────────────────────────────────────────────────────┘
```

### 3.3 功能 B：首页与时间轴

```
┌──────────────────────────────────────────────────────────────────┐
│  GET /api/mistakes/home-summary                                   │
│                                                                    │
│  返回：                                                            │
│  ┌────────────────────────────────────────────────┐               │
│  │  today_pending_count    今日新增待巩固           │               │
│  │  pending_review_count   待巩固总数              │               │
│  │  completed_this_week    本周已巩固              │               │
│  │  mastered_error_types   已突破错因类型数         │               │
│  │  weak_knowledge_tags    高频薄弱知识点           │               │
│  │  recent_timeline        近期时间轴              │               │
│  └────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  GET /api/mistakes/timeline                                       │
│                                                                    │
│  筛选：user_id / status / knowledge_tag / has_solution             │
│                                                                    │
│  分组逻辑：                                                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                        │
│  │   今天    │  │   昨天    │  │   更早    │                        │
│  │  item[]  │  │  item[]  │  │  item[]  │                        │
│  └──────────┘  └──────────┘  └──────────┘                        │
└──────────────────────────────────────────────────────────────────┘
```

### 3.4 功能 C：讲义/复盘页

```
┌──────────────────────────────────────────────────────────────────┐
│  GET /api/mistakes/reports/{report_id}/lecture                    │
│                                                                    │
│  规则版（不调用大模型），从报告数据直接加工：                          │
│                                                                    │
│  ┌──────────────────────────────────────────────┐                 │
│  │  lecture_sections (讲义区块)                   │                 │
│  │  ├── summary    题目回顾                      │                 │
│  │  ├── knowledge  涉及知识点                    │                 │
│  │  ├── thinking   关键思路回放 (思维链前5条)     │                 │
│  │  ├── error      本题易错点 (错因前3条)         │                 │
│  │  ├── solution   题解讲义 (如有)               │                 │
│  │  └── suggestion 下次遇到要先想什么             │                 │
│  │                                                │                 │
│  │  review_steps (复盘步骤)                       │                 │
│  │  ├── Step 1  先看清题目在问什么     [done]     │                 │
│  │  ├── Step 2  先匹配知识点           [done]     │                 │
│  │  ├── Step 3  重点盯住这一步         [focus]    │                 │
│  │  └── Step 4  最后自检答案           [done]     │                 │
│  │                                                │                 │
│  │  key_takeaways (复盘要点，最多4条)              │                 │
│  └──────────────────────────────────────────────┘                 │
└──────────────────────────────────────────────────────────────────┘
```

### 3.5 功能 D：基于用户画像的智能推荐题生成

```
┌──────────────────────────────────────────────────────────────────┐
│  POST /api/mistakes/recommendations/generate                      │
│                                                                    │
│  输入：report_id + user_id + recommendation_type (similar/variant) │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────┐      │
│  │                 用户画像查询                              │      │
│  │           GET /api/mistakes/profiles/{user_id}           │      │
│  │                     │                                    │      │
│  │            ┌───────┴───────┐                            │      │
│  │            │  有画像？      │                            │      │
│  │            ├──YES─────NO──┤                            │      │
│  │            ▼              ▼                            │      │
│  │   画像驱动流程      基础版流程                           │      │
│  └─────────────────────────────────────────────────────────┘      │
│                                                                    │
│  ═══ 画像驱动流程 (有画像) ═══                                     │
│                                                                    │
│  ┌──────────────────┐                                              │
│  │ profile-aware-   │  Prompt: build_profile_aware_analysis_prompt │
│  │ analyzer         │  输入: 报告 + 推荐类型 + 用户画像              │
│  │                  │  输出: problem_type, knowledge_tags,          │
│  │                  │        error_focus, variant_axes,             │
│  │                  │        recommendation_goal                   │
│  └────────┬─────────┘                                              │
│           ▼                                                        │
│  ┌──────────────────┐                                              │
│  │ profile-aware-   │  Prompt: build_profile_aware_variant_prompt  │
│  │ generator        │  输入: 分析结果 + 报告 + 画像                 │
│  │                  │  输出: title, question, options(4),           │
│  │                  │        correct_option_id, answer,            │
│  │                  │        explanation, why_recommended           │
│  └────────┬─────────┘                                              │
│           ▼                                                        │
│  ┌──────────────────┐                                              │
│  │ quality-reviewer │  Prompt: build_quality_review_prompt         │
│  │                  │  检查: 选项数=4, correct_id有效,              │
│  │                  │        非空字段, 自动修正小问题                │
│  └────────┬─────────┘                                              │
│           ▼                                                        │
│  推荐题持久化 → recommendations/{id}.json                          │
│                                                                    │
│  ═══ 基础版流程 (无画像) ═══                                       │
│                                                                    │
│  problem-analyzer → variant-generator → quality-reviewer           │
│  (与画像驱动流程结构相同，但 Prompt 中不包含用户画像段落)             │
└──────────────────────────────────────────────────────────────────┘
```

### 3.6 功能 E：引导式重做会话

```
┌──────────────────────────────────────────────────────────────────┐
│  POST /api/mistakes/redo-sessions/start                           │
│                                                                    │
│  ┌────────────────────────────────────────────────────────┐       │
│  │  4 Agent 协作启动                                       │       │
│  │                                                         │       │
│  │  pedagogy-planner ──▶ option-generator ──▶ hint-gen    │       │
│  │  (规划引导步骤)       (生成4个选项)        (生成提示)    │       │
│  └────────────────────────────────────────────────────────┘       │
│                           │                                       │
│                           ▼                                       │
│  ┌────────────────────────────────────────────────────────┐       │
│  │  重做阶段流转 (5 阶段)                                   │       │
│  │                                                         │       │
│  │  understand_problem ──▶ identify_first_step ──▶ solve  │       │
│  │       (理解题意)            (确定第一步)         (完整解答)│       │
│  │                                              │         │       │
│  │                                              ▼         │       │
│  │                                        final_check ──▶ completed│
│  │                                        (检查验证)       │       │
│  └────────────────────────────────────────────────────────┘       │
│                                                                    │
│  每轮交互：                                                        │
│  ┌────────────────────────────────────────────────────────┐       │
│  │  POST /api/mistakes/redo-sessions/{id}/turn             │       │
│  │                                                         │       │
│  │  学生提交答案 ──▶ response-evaluator 评估               │       │
│  │                     │                                   │       │
│  │           ┌─────────┼─────────┐                        │       │
│  │           ▼         ▼         ▼                        │       │
│  │        correct    partial   incorrect                   │       │
│  │        连续答对+1  保持当前   连续答对归零                │       │
│  │           │         │         │                        │       │
│  │           │    提示等级+1 ◀───┘                        │       │
│  │           ▼                                              │       │
│  │    达到连续答对要求？                                      │       │
│  │     YES → can_clear_mistake = true                      │       │
│  │     NO  → 继续下一阶段                                    │       │
│  └────────────────────────────────────────────────────────┘       │
│                                                                    │
│  提示等级体系：                                                    │
│  Level 1: 轻微引导（只给方向）                                     │
│  Level 2: 中度提示（提到关键公式）                                  │
│  Level 3: 直接提示（接近完整答案）                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 3.7 功能 F：讲题验证对话（用户讲题，AI 听）

```
┌──────────────────────────────────────────────────────────────────┐
│  POST /api/mistakes/dialogue-sessions/start                       │
│                                                                    │
│  启动流程：                                                        │
│  ┌────────────┐   ┌──────────────┐   ┌───────────────┐           │
│  │ 查询报告    │──▶│ 生成相似题    │──▶│ listener 开场  │           │
│  │            │   │ (自动调用     │   │ (AI发出引导,   │           │
│  │            │   │  推荐题生成)  │   │  邀请学生讲解) │           │
│  └────────────┘   └──────────────┘   └───────┬───────┘           │
│                                               │                   │
│                                               ▼                   │
│  ┌────────────────────────────────────────────────────────┐       │
│  │  多轮对话循环                                           │       │
│  │                                                         │       │
│  │  POST /api/mistakes/dialogue-sessions/{id}/turn         │       │
│  │                                                         │       │
│  │  学生讲解 ──▶ listener Agent ──▶ AI 回复                 │       │
│  │                    │                                    │       │
│  │     ┌──────────────┼──────────────┐                    │       │
│  │     │              │              │                    │       │
│  │     ▼              ▼              ▼                    │       │
│  │  is_probing=true  is_probing=false  should_continue=false│       │
│  │  (追问/质疑)      (肯定/引导)       (对话结束)          │       │
│  │     │              │              │                    │       │
│  │     └──────┬───────┘              │                    │       │
│  │            ▼                      ▼                    │       │
│  │    mastery_signal 检查      ┌──────────────┐          │       │
│  │    >= 0.8 或轮次>=10?       │ mastery-judge │          │       │
│  │      YES │   NO 继续循环    │ (最终判定)    │          │       │
│  │           ▼                 └──────┬───────┘          │       │
│  │    ┌──────────────┐               │                   │       │
│  │    │ mastery-judge │───────────────┘                   │       │
│  │    └──────┬───────┘                                    │       │
│  │           │                                            │       │
│  │     ┌─────┼─────────────┐                             │       │
│  │     ▼     ▼             ▼                             │       │
│  │  mastered  not_mastered  in_progress                   │       │
│  │  (已掌握)  (未掌握)      (继续对话)                     │       │
│  │     │                                                  │       │
│  │     ▼                                                  │       │
│  │  自动标记报告 mastered                                  │       │
│  └────────────────────────────────────────────────────────┘       │
│                                                                    │
│  listener Agent 核心原则：                                         │
│  ┌────────────────────────────────────────────────────────┐       │
│  │  1. AI 是"听者"，不是"讲者"——不主动讲解                  │       │
│  │  2. 根据学生讲解提出针对性疑问或追问                      │       │
│  │  3. 参考讲题报告判断是否遗漏关键步骤                      │       │
│  │  4. 参考相似题引导理解迁移                                │       │
│  │  5. 讲解正确时给予肯定                                    │       │
│  │  6. 存在错误时用提问引导自我发现，不直接纠正               │       │
│  └────────────────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────────────┘
```

### 3.8 功能 G：用户画像构建与动态更新

```
┌──────────────────────────────────────────────────────────────────┐
│  GET /api/mistakes/profiles/{user_id}                             │
│                                                                    │
│  ┌────────────────────────────────────────────────────────┐       │
│  │  画像构建逻辑                                           │       │
│  │                                                         │       │
│  │  已有画像？──YES──▶ 直接返回                             │       │
│  │      │                                                  │       │
│  │     NO                                                  │       │
│  │      ▼                                                  │       │
│  │  遍历该用户所有报告，聚合：                               │       │
│  │  ├── weak_knowledge_tags  (按频次降序，最多10个)          │       │
│  │  ├── weak_points          (每个知识点的错误类型+频次)     │       │
│  │  ├── error_type_distribution (错误类型→出现次数)         │       │
│  │  ├── independence_summary (基于独立完成度等级的统计)      │       │
│  │  └── learning_style_hints (基于错误模式的学习建议)       │       │
│  └────────────────────────────────────────────────────────┘       │
│                                                                    │
│  ┌────────────────────────────────────────────────────────┐       │
│  │  增量更新逻辑 (每次 ingest 自动触发)                     │       │
│  │                                                         │       │
│  │  新报告入库                                              │       │
│  │      │                                                  │       │
│  │      ▼                                                  │       │
│  │  新错误类型/知识点 在画像中已存在？                       │       │
│  │      ├──YES──▶ 仅更新计数 (pending_count+1 等)          │       │
│  │      │         不调用大模型                              │       │
│  │      │                                                  │       │
│  │      NO                                                 │       │
│  │      │                                                  │       │
│  │      ▼                                                  │       │
│  │  调用 profile-updater Agent                             │       │
│  │  Prompt: build_profile_update_prompt                    │       │
│  │  输入: 现有画像 + 新报告                                 │       │
│  │  输出: updated (是否更新) + profile (完整画像)           │       │
│  │      │                                                  │       │
│  │      ▼                                                  │       │
│  │  校验画像数据合法性 → 持久化                              │       │
│  │  (校验失败则全量重建)                                    │       │
│  └────────────────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────────────┘
```

### 3.9 推荐题闭环：生成 → 作答 → 重做

```
┌──────────────────────────────────────────────────────────────────┐
│  推荐题生成                                                       │
│  POST /api/mistakes/recommendations/generate                      │
│           │                                                       │
│           ▼                                                       │
│  ┌──────────────┐                                                │
│  │ 推荐题数据    │  question + 4 options + correct_option_id      │
│  │              │  answer + explanation + why_recommended         │
│  └──────┬───────┘                                                │
│         │                                                         │
│    ┌────┴────────────────────┐                                   │
│    ▼                         ▼                                   │
│  直接作答                   引导重做                               │
│  POST /recommendations/{id}  POST /recommendations/{id}/start-redo│
│  /submit                                                         │
│    │                         │                                    │
│    ▼                         ▼                                   │
│  ┌──────────────┐   ┌──────────────────┐                        │
│  │ 判定对错      │   │ 启动重做会话      │                        │
│  │ is_correct   │   │ (以推荐题为题目)  │                        │
│  │ should_create│   │ session_type=     │                        │
│  │ _mistake     │   │  redo_recommendation│                       │
│  └──────────────┘   └──────────────────┘                        │
│                                                                    │
│  答错时 should_create_mistake=true → 可回写为新错题                │
└──────────────────────────────────────────────────────────────────┘
```

---

## 四、多 Agent 协作全景图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MistakeMultiAgent                                 │
│                                                                          │
│  ┌───────────────── 推荐题生成 Agent 链 ─────────────────┐              │
│  │                                                         │              │
│  │  [有画像]  profile-aware-analyzer ──▶ profile-aware-   │              │
│  │           (分析+画像)                generator          │              │
│  │                                     (生成+画像)        │              │
│  │                                                         │              │
│  │  [无画像]  problem-analyzer ──▶ variant-generator      │              │
│  │           (基础分析)          (基础生成)                │              │
│  │                                                         │              │
│  │  [共同]   quality-reviewer (质量审核+自动修正)          │              │
│  └─────────────────────────────────────────────────────────┘              │
│                                                                          │
│  ┌───────────────── 重做会话 Agent 链 ──────────────────┐               │
│  │                                                        │               │
│  │  pedagogy-planner ──▶ option-generator ──▶ hint-generator│              │
│  │  (教学规划)           (选项生成)           (提示生成)    │               │
│  │                                                        │               │
│  │  response-evaluator (回答评估)                         │               │
│  └────────────────────────────────────────────────────────┘               │
│                                                                          │
│  ┌───────────────── 讲题验证 Agent 链 ──────────────────┐               │
│  │                                                        │               │
│  │  listener (听者Agent - 多轮对话)                       │               │
│  │  mastery-judge (掌握判定Agent - 最终裁决)              │               │
│  └────────────────────────────────────────────────────────┘               │
│                                                                          │
│  ┌───────────────── 画像更新 Agent ─────────────────────┐               │
│  │                                                        │               │
│  │  profile-updater (增量画像更新)                        │               │
│  └────────────────────────────────────────────────────────┘               │
│                                                                          │
│  ┌───────────────── 降级机制 ───────────────────────────┐               │
│  │                                                        │               │
│  │  每个 Agent 均有 _fallback_* 方法                      │               │
│  │  LLM 不可用时自动回退到规则引擎                         │               │
│  │  保证接口始终可用                                       │               │
│  └────────────────────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、Prompt 工程体系

| Prompt 模板 | Agent | 功能 | 输出格式 |
|-------------|-------|------|----------|
| `build_problem_analysis_prompt` | problem-analyzer | 基础题目分析 | JSON: problem_type, knowledge_tags, error_focus, variant_axes, recommendation_goal |
| `build_variant_generation_prompt` | variant-generator | 基础推荐题生成 | JSON: title, question, options(4), correct_option_id, answer, explanation, why_recommended |
| `build_profile_aware_analysis_prompt` | profile-aware-analyzer | 画像驱动分析 | 同上 + 结合用户薄弱点 |
| `build_profile_aware_variant_prompt` | profile-aware-generator | 画像驱动生成 | 同上 + 难度/干扰项个性化 |
| `build_quality_review_prompt` | quality-reviewer | 质量审核 | JSON: passed, issues, normalized_result |
| `build_redo_plan_prompt` | pedagogy-planner | 重做教学规划 | JSON: stage, interaction_mode, teaching_goal, prompt |
| `build_redo_option_prompt` | option-generator | 重做选项生成 | JSON: options(4), correct_option_id |
| `build_redo_hint_prompt` | hint-generator | 重做提示生成 | JSON: hint (3级提示体系) |
| `build_redo_evaluation_prompt` | response-evaluator | 重做回答评估 | JSON: result, feedback, next_stage, should_offer_hint, can_clear_mistake |
| `build_dialogue_listener_prompt` | listener | 讲题验证对话 | JSON: reply, is_probing, topic_focus, should_continue, mastery_signal |
| `build_dialogue_mastery_prompt` | mastery-judge | 掌握最终判定 | JSON: verdict, detail, weak_aspects, confidence |
| `build_profile_update_prompt` | profile-updater | 画像增量更新 | JSON: updated, profile |

**Prompt 设计核心原则：**
- 所有 Prompt 要求输出合法 JSON 对象，禁止 markdown 和解释文字
- 包含 `_json()` 序列化确保中文可读
- 每个 Prompt 都有对应的 `_fallback_*` 方法作为降级保障
- 对话类 Prompt 限制回复长度（80字以内），适配手机端

---

## 六、持久化架构

```
data/
├── reports/
│   ├── index.json                    ← 报告索引（轻量查询用）
│   ├── mr_xxxxxx.json               ← 单条报告完整数据
│   └── ...
├── profiles/
│   ├── index.json                    ← 画像索引
│   ├── u001.json                     ← 用户画像完整数据
│   └── ...
├── redo_sessions/
│   ├── index.json                    ← 重做会话索引
│   ├── redo_xxxxxx.json             ← 单个会话完整数据
│   └── ...
├── dialogue_sessions/
│   ├── index.json                    ← 对话会话索引
│   ├── dlg_xxxxxxxxxxxx.json        ← 单个对话完整数据
│   └── ...
└── recommendations/
    ├── index.json                    ← 推荐题索引
    ├── rec_xxxx_similar.json        ← 单条推荐题完整数据
    └── ...
```

**设计特点：**
- 每个实体一个 JSON 文件，避免大文件读写
- `index.json` 索引文件用于列表查询，无需遍历所有文件
- 服务重启后自动从磁盘加载所有数据到内存
- 所有写入操作同时更新内存和磁盘

---

## 七、API 端点全景 (20 个)

| # | 方法 | 路径 | 功能 |
|---|------|------|------|
| 1 | GET | /api/mistakes/health | 模块健康检查 |
| 2 | POST | /api/mistakes/report/ingest | 写入错题报告 |
| 3 | GET | /api/mistakes/reports | 错题列表（支持筛选） |
| 4 | GET | /api/mistakes/reports/{id} | 错题详情 |
| 5 | GET | /api/mistakes/reports/{id}/lecture | 讲义/复盘页 |
| 6 | PATCH | /api/mistakes/reports/{id}/status | 更新巩固状态 |
| 7 | GET | /api/mistakes/stats | 错题统计 |
| 8 | GET | /api/mistakes/timeline | 时间轴（支持筛选） |
| 9 | GET | /api/mistakes/home-summary | 首页摘要 |
| 10 | GET | /api/mistakes/daily-plan | 每日提分计划 |
| 11 | POST | /api/mistakes/recommendations/generate | 生成推荐题 |
| 12 | POST | /api/mistakes/recommendations/{id}/submit | 推荐题作答 |
| 13 | POST | /api/mistakes/recommendations/{id}/start-redo | 推荐题→重做 |
| 14 | POST | /api/mistakes/redo-sessions/start | 启动重做会话 |
| 15 | GET | /api/mistakes/redo-sessions/{id} | 查询重做会话 |
| 16 | POST | /api/mistakes/redo-sessions/{id}/turn | 推进重做会话 |
| 17 | GET | /api/mistakes/profiles/{user_id} | 获取用户画像 |
| 18 | POST | /api/mistakes/dialogue-sessions/start | 启动讲题验证对话 |
| 19 | GET | /api/mistakes/dialogue-sessions/{id} | 查询对话会话 |
| 20 | POST | /api/mistakes/dialogue-sessions/{id}/turn | 推进对话 |

---

## 八、错误码体系

| 范围 | 模块 |
|------|------|
| 4101-4108 | 报告写入、查询、推荐题生成 |
| 4109-4116 | 重做会话、推荐题作答 |
| 4120-4124 | 用户画像、讲题验证对话 |

---

## 九、技术亮点总结

1. **多 Agent 编排架构**：11 个专业 Agent 通过 Prompt 编排协同工作，各司其职
2. **画像驱动的个性化推荐**：推荐题的难度、干扰项、推荐理由均基于用户画像定制
3. **讲题验证对话**：AI 扮演"听者"而非"讲者"，通过苏格拉底式提问检验掌握程度
4. **增量画像更新**：新报告入库时智能判断是否需要调用 LLM 更新画像，避免不必要的 API 调用
5. **三级提示体系**：重做会话中提示从"方向引导"到"接近答案"逐级递进
6. **优雅降级**：每个 Agent 都有规则引擎 fallback，LLM 不可用时系统仍可正常运行
7. **掌握自动标记**：讲题验证判定 mastered 时自动将错题状态更新为已巩固
8. **推荐题闭环**：生成 → 作答 → 答错回写新错题 → 引导重做，形成完整练习闭环

---

## 十、全功能手动测试指南（傻瓜版）

> 以下指南假设你已经安装了 Python 和项目依赖，并且能启动后端服务器。
> 所有测试通过 HTTP 请求完成，推荐使用 **Postman** 或 **VS Code 的 REST Client 插件**。
> 如果你什么都不会，就用浏览器直接访问 GET 请求的 URL；POST 请求用下面的 curl 命令。

### 10.0 前置准备：启动服务器

**第一步：打开终端（PowerShell）**

按 `Win + X`，选择"Windows PowerShell"或"终端"。

**第二步：进入项目目录**

```powershell
cd d:\DDesk\BlueTutor\backend
```

**第三步：启动后端服务**

```powershell
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

看到类似 `Uvicorn running on http://0.0.0.0:8000` 就说明成功了。

**第四步：验证服务是否正常**

在浏览器打开：http://localhost:8000/api/mistakes/health

你应该看到类似：`{"code":0,"message":"ok","data":{"status":"ok","module":"mistakes"}}`

如果看到了，说明服务器跑起来了，继续下面的测试。

---

### 10.1 测试功能 A：写入错题报告

这是所有功能的基础——没有错题数据，其他功能都测不了。

**请求：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/report/ingest -H "Content-Type: application/json" -d "{\"user_id\":\"test_user_001\",\"report\":{\"problem\":{\"raw_problem\":\"计算 1/2 + 1/3 = ?\",\"subject\":\"数学\",\"grade\":\"五年级\",\"knowledge_tags\":[\"分数运算\",\"通分\"]},\"thinking_chain\":[{\"step\":1,\"content\":\"需要通分\",\"is_correct\":true},{\"step\":2,\"content\":\"2和3的最小公倍数是6\",\"is_correct\":true},{\"step\":3,\"content\":\"1/2 = 3/6, 1/3 = 2/6\",\"is_correct\":true},{\"step\":4,\"content\":\"3/6 + 2/6 = 5/6\",\"is_correct\":true}],\"error_profile\":{\"error_type\":\"概念混淆\",\"error_description\":\"通分时分子分母同时变化没搞清楚\",\"error_tags\":[\"通分\",\"分子分母关系\"]},\"independence_evaluation\":{\"level\":\"partial\",\"description\":\"需要提示才想到通分\"},\"solution\":{\"title\":\"分数加法解题步骤\",\"steps\":[\"找到分母的最小公倍数\",\"将每个分数通分\",\"分子相加，分母不变\",\"化简结果\"],\"key_insight\":\"异分母分数相加必须先通分\"}}}"
```

**你应该看到什么：**

返回的 JSON 里有 `report_id`（一串字母数字），**把这个 ID 复制保存下来**，后面所有测试都要用！

**验证点：**
- `code` 是 `0`
- `data` 里有 `report_id`
- `data.report_title` 不为空

**再写入一条不同类型的错题（用于后续画像测试）：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/report/ingest -H "Content-Type: application/json" -d "{\"user_id\":\"test_user_001\",\"report\":{\"problem\":{\"raw_problem\":\"一个三角形底边8cm，高5cm，求面积\",\"subject\":\"数学\",\"grade\":\"五年级\",\"knowledge_tags\":[\"三角形面积\",\"几何\"]},\"thinking_chain\":[{\"step\":1,\"content\":\"三角形面积=底x高\",\"is_correct\":false},{\"step\":2,\"content\":\"8x5=40\",\"is_correct\":false}],\"error_profile\":{\"error_type\":\"公式记忆错误\",\"error_description\":\"忘了除以2\",\"error_tags\":[\"三角形面积公式\"]},\"independence_evaluation\":{\"level\":\"low\",\"description\":\"完全独立完成但做错了\"},\"solution\":{\"title\":\"三角形面积正确解法\",\"steps\":[\"三角形面积 = 底 x 高 / 2\",\"8 x 5 / 2 = 20\",\"面积是20平方厘米\"],\"key_insight\":\"三角形面积要除以2，容易忘\"}}}"
```

同样保存返回的 `report_id`。

---

### 10.2 测试功能 B：查看错题列表

**请求（直接在浏览器打开）：**

```
http://localhost:8000/api/mistakes/reports?user_id=test_user_001
```

**你应该看到什么：**

返回一个 JSON，`data.items` 里面有你刚才写入的 2 条错题记录。

**验证点：**
- `data.total` 是 `2`
- 每条记录都有 `report_id`、`report_title`、`status`

**试试筛选功能：**

```
http://localhost:8000/api/mistakes/reports?user_id=test_user_001&status=pending
```

应该只显示 `status` 为 `pending` 的记录。

---

### 10.3 测试功能 C：查看错题详情

把之前保存的 `report_id` 替换到 URL 里：

```
http://localhost:8000/api/mistakes/reports/{你的report_id}
```

**验证点：**
- 能看到完整的 `problem`、`thinking_chain`、`error_profile`、`solution` 等信息
- `report_title` 不为空

---

### 10.4 测试功能 D：查看讲义/复盘页

```
http://localhost:8000/api/mistakes/reports/{你的report_id}/lecture
```

**你应该看到什么：**

返回的 JSON 里有 `lecture_sections`（讲义区块）和 `review_steps`（复盘步骤）。

**验证点：**
- `lecture_sections` 包含 summary、knowledge、thinking、error、solution、suggestion 等区块
- `review_steps` 有 4 个步骤
- `key_takeaways` 不为空

---

### 10.5 测试功能 E：生成推荐题（基于用户画像）

**请求：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/recommendations/generate -H "Content-Type: application/json" -d "{\"report_id\":\"{你的report_id}\",\"user_id\":\"test_user_001\",\"recommendation_type\":\"similar\"}"
```

**你应该看到什么：**

返回的 JSON 里有完整的推荐题：`question`（题目）、`options`（4个选项）、`correct_option_id`（正确答案ID）、`explanation`（解析）等。

**验证点：**
- `options` 有且仅有 4 个选项
- `correct_option_id` 对应的选项确实在 `options` 里
- `why_recommended` 不为空（说明推荐理由）
- `agent_traces` 里有 3 条记录（analyzer、generator、reviewer）

**保存返回的 `recommendation_id`**，后面要用。

**再试 variant 类型：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/recommendations/generate -H "Content-Type: application/json" -d "{\"report_id\":\"{你的report_id}\",\"user_id\":\"test_user_001\",\"recommendation_type\":\"variant\"}"
```

---

### 10.6 测试功能 F：推荐题作答

**答对的情况：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/recommendations/{你的recommendation_id}/submit -H "Content-Type: application/json" -d "{\"selected_option_id\":\"{correct_option_id}\"}"
```

**验证点：**
- `is_correct` 为 `true`
- `should_create_mistake` 为 `false`（答对了不需要创建新错题）

**答错的情况：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/recommendations/{你的recommendation_id}/submit -H "Content-Type: application/json" -d "{\"selected_option_id\":\"{随便一个错误选项的id}\"}"
```

**验证点：**
- `is_correct` 为 `false`
- `should_create_mistake` 为 `true`（答错了应该创建新错题）

---

### 10.7 测试功能 G：引导式重做会话

**第一步：启动重做会话**

```powershell
curl -X POST http://localhost:8000/api/mistakes/redo-sessions/start -H "Content-Type: application/json" -d "{\"report_id\":\"{你的report_id}\",\"user_id\":\"test_user_001\"}"
```

**你应该看到什么：**

返回的 JSON 里有 `session_id`、`stage`（当前阶段）、`current_prompt`（引导问题）、`current_options`（4个选项）。

**保存 `session_id`！**

**第二步：提交答案（推进会话）**

```powershell
curl -X POST http://localhost:8000/api/mistakes/redo-sessions/{你的session_id}/turn -H "Content-Type: application/json" -d "{\"answer\":\"{某个选项id}\"}"
```

**你应该看到什么：**

返回更新后的会话数据，`stage` 可能推进到下一阶段，`history` 里多了你的作答记录。

**第三步：重复提交直到完成**

一直调用上面的 turn 接口，每次选一个答案。直到 `is_completed` 变成 `true`。

**验证点：**
- 每轮返回的 `stage` 会逐步推进（understand_problem → identify_first_step → solve → final_check → completed）
- 答错时 `should_offer_hint` 为 `true`，可以获取提示
- 连续答对后 `can_clear_mistake` 变为 `true`

**获取提示（答错后）：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/redo-sessions/{你的session_id}/turn -H "Content-Type: application/json" -d "{\"answer\":\"{错误选项id}\",\"request_hint\":true}"
```

返回里会有 `hint` 字段。

---

### 10.8 测试功能 H：讲题验证对话（用户讲题，AI 听）

**第一步：启动对话会话**

```powershell
curl -X POST http://localhost:8000/api/mistakes/dialogue-sessions/start -H "Content-Type: application/json" -d "{\"report_id\":\"{你的report_id}\",\"user_id\":\"test_user_001\"}"
```

**你应该看到什么：**

返回的 JSON 里有：
- `session_id`（保存！）
- `messages` 里有一条 `role: "assistant"` 的消息——这是 AI 的开场白，邀请你讲解
- `similar_question`——自动生成的相似题（可能为 null 如果 LLM 不可用）

**第二步：你来讲题（发送你的讲解）**

```powershell
curl -X POST http://localhost:8000/api/mistakes/dialogue-sessions/{你的session_id}/turn -H "Content-Type: application/json" -d "{\"user_message\":\"这道题要先通分，1/2变成3/6，1/3变成2/6，然后分子相加得到5/6\"}"
```

**你应该看到什么：**

AI 会回复一条消息。可能是：
- 追问你某个步骤（`is_probing: true`）
- 肯定你的讲解（`is_probing: false`）

**第三步：继续对话**

继续发送你的讲解，AI 会根据你的回答继续追问或引导。多聊几轮。

```powershell
curl -X POST http://localhost:8000/api/mistakes/dialogue-sessions/{你的session_id}/turn -H "Content-Type: application/json" -d "{\"user_message\":\"因为2和3的最小公倍数是6，所以分母都变成6\"}"
```

**第四步：查看对话是否结束**

每次 turn 返回的数据里检查：
- `is_completed`：是否结束
- `mastery_verdict`：掌握判定结果（`mastered`/`not_mastered`/`in_progress`）

当 `is_completed` 为 `true` 时，对话结束，`mastery_verdict` 和 `mastery_detail` 会告诉你最终判定。

**验证点：**
- AI 不会主动讲解，只会提问或追问
- AI 的回复不超过 80 字
- 多轮对话后最终会给出掌握判定
- 判定为 `mastered` 时，关联的错题报告状态会自动更新

---

### 10.9 测试功能 I：用户画像

**查看用户画像：**

```
http://localhost:8000/api/mistakes/profiles/test_user_001
```

**你应该看到什么：**

返回的 JSON 里有完整的用户画像：
- `total_reports`：错题总数（应该是 2）
- `pending_count`：待巩固数量
- `weak_knowledge_tags`：薄弱知识点列表（应该包含"分数运算"、"通分"、"三角形面积"等）
- `weak_points`：每个薄弱点的详细信息
- `error_type_distribution`：错误类型分布（概念混淆:1, 公式记忆错误:1）
- `independence_summary`：独立完成度总评
- `learning_style_hints`：学习风格建议

**验证点：**
- `total_reports` >= 2
- `weak_knowledge_tags` 不为空
- `weak_points` 不为空
- `error_type_distribution` 有数据

**测试画像动态更新：**

再写入一条新类型的错题：

```powershell
curl -X POST http://localhost:8000/api/mistakes/report/ingest -H "Content-Type: application/json" -d "{\"user_id\":\"test_user_001\",\"report\":{\"problem\":{\"raw_problem\":\"24 x 15 = ?\",\"subject\":\"数学\",\"grade\":\"五年级\",\"knowledge_tags\":[\"乘法运算\",\"计算\"]},\"thinking_chain\":[{\"step\":1,\"content\":\"24x10=240\",\"is_correct\":true},{\"step\":2,\"content\":\"24x5=100\",\"is_correct\":false}],\"error_profile\":{\"error_type\":\"计算粗心\",\"error_description\":\"24x5算成了100，应该是120\",\"error_tags\":[\"乘法口诀\"]},\"independence_evaluation\":{\"level\":\"high\",\"description\":\"思路正确但计算出错\"},\"solution\":{\"title\":\"乘法分步计算\",\"steps\":[\"24x10=240\",\"24x5=120\",\"240+120=360\"],\"key_insight\":\"拆分法计算更准确\"}}}"
```

然后再查看画像：

```
http://localhost:8000/api/mistakes/profiles/test_user_001
```

**验证点：**
- `total_reports` 变成了 3
- `weak_knowledge_tags` 里多了"乘法运算"或"计算"
- `error_type_distribution` 里多了"计算粗心"

---

### 10.10 测试功能 J：首页摘要与统计

**首页摘要：**

```
http://localhost:8000/api/mistakes/home-summary?user_id=test_user_001
```

**验证点：**
- `today_pending_count` >= 0
- `pending_review_count` >= 0
- `weak_knowledge_tags` 不为空

**错题统计：**

```
http://localhost:8000/api/mistakes/stats?user_id=test_user_001
```

**验证点：**
- `total_reports` >= 3
- `by_status` 里有 pending 的数量

**时间轴：**

```
http://localhost:8000/api/mistakes/timeline?user_id=test_user_001
```

**验证点：**
- `groups` 不为空
- 每个组里有 `label`（如"今天"）和 `items`

**每日提分计划：**

```
http://localhost:8000/api/mistakes/daily-plan?user_id=test_user_001&limit=3
```

**验证点：**
- 返回最多 3 条待巩固的错题

---

### 10.11 测试功能 K：更新错题状态

```powershell
curl -X PATCH http://localhost:8000/api/mistakes/reports/{你的report_id}/status -H "Content-Type: application/json" -d "{\"status\":\"mastered\"}"
```

**验证点：**
- 返回的报告数据里 `status` 变成了 `mastered`
- 再次查看列表时，该报告不再出现在 `status=pending` 的筛选结果中

---

### 10.12 测试功能 L：推荐题→重做闭环

**第一步：从推荐题启动重做**

```powershell
curl -X POST http://localhost:8000/api/mistakes/recommendations/{你的recommendation_id}/start-redo -H "Content-Type: application/json" -d "{\"user_id\":\"test_user_001\"}"
```

**验证点：**
- 返回一个重做会话，`session_type` 为 `redo_recommendation`
- `recommendation_id` 对应你之前生成的推荐题

**第二步：正常走重做流程**

参考 10.7 的步骤继续测试。

---

### 10.13 测试功能 M：错误场景

**访问不存在的报告：**

```
http://localhost:8000/api/mistakes/reports/not_exist_id
```

**验证点：**
- 返回 404 状态码
- `code` 为 `4104`

**用不存在的报告启动对话：**

```powershell
curl -X POST http://localhost:8000/api/mistakes/dialogue-sessions/start -H "Content-Type: application/json" -d "{\"report_id\":\"not_exist_id\",\"user_id\":\"test_user_001\"}"
```

**验证点：**
- 返回 404 状态码
- `code` 为 `4121`

---

### 10.14 测试功能 N：运行自动化测试

如果你不想一个个手动测，也可以跑自动化测试：

```powershell
cd d:\DDesk\BlueTutor\backend
python -m pytest app/mistakes/ -v
```

**验证点：**
- 所有测试用例通过（显示 PASSED）
- 没有 FAILED 或 ERROR

**跑新功能专项测试：**

```powershell
cd d:\DDesk\BlueTutor\backend
python -m app.mistakes.new_features_test
```

**验证点：**
- 三个功能测试全部显示 `[PASS]`

---

### 10.15 测试清单速查表

| # | 功能 | 测试方式 | 关键验证 |
|---|------|----------|----------|
| A | 写入错题报告 | POST /report/ingest | 返回 report_id |
| B | 错题列表 | GET /reports?user_id=xxx | total >= 写入数量 |
| C | 错题详情 | GET /reports/{id} | 完整数据 |
| D | 讲义/复盘 | GET /reports/{id}/lecture | lecture_sections 不为空 |
| E | 生成推荐题 | POST /recommendations/generate | 4个选项，有推荐理由 |
| F | 推荐题作答 | POST /recommendations/{id}/submit | is_correct 判定正确 |
| G | 引导式重做 | POST /redo-sessions/start + turn | 阶段推进，最终 completed |
| H | 讲题验证对话 | POST /dialogue-sessions/start + turn | AI 追问，最终给出判定 |
| I | 用户画像 | GET /profiles/{user_id} | weak_points 不为空 |
| J | 首页/统计 | GET /home-summary, /stats, /timeline | 数据一致 |
| K | 更新状态 | PATCH /reports/{id}/status | status 变更 |
| L | 推荐题→重做 | POST /recommendations/{id}/start-redo | session_type=redo_recommendation |
| M | 错误场景 | 请求不存在的资源 | 返回 404 + 错误码 |
| N | 自动化测试 | pytest / python 脚本 | 全部 PASSED |
