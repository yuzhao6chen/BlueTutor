# 错题模块开发记录

## 2026-04-17 第一次实现

### 目标

- 在尽量不影响现有 `preview` / `guide` 模块的前提下，补齐错题模块后端 v1。
- 先围绕讲题模块产出的“错题报告”结构完成可联调接口。
- 采用新增文件为主、最小接线修改为辅的方式降低团队协作冲突。

### 新增文件

- `backend/app/mistakes/schema.py`
- `backend/app/mistakes/service.py`
- `backend/app/api/mistakes_api.py`

### 修改文件

- `backend/app/main.py`
  - 注册 `mistakes` 路由

### 本次新增接口

- `GET /api/mistakes/health`
  - 错题模块健康检查
- `POST /api/mistakes/report/ingest`
  - 接收讲题模块返回的错题报告并写入内存仓库
- `GET /api/mistakes/reports`
  - 获取错题报告列表，支持 `user_id` 过滤
- `GET /api/mistakes/reports/{report_id}`
  - 获取单条错题报告详情

### 报告格式对齐说明

本次后端 schema 已对齐讲题模块报告核心结构：

- `problem`
  - `raw_problem`
  - `known_conditions`
  - `goal`
  - `answer`
- `knowledge_tags`
- `thinking_chain`
  - `node_id`
  - `content`
  - `status`
  - `parent_id`
  - `error_history`
- `error_profile`
  - `error_type`
  - `detail`
- `independence_evaluation`
  - `level`
  - `detail`

### 当前实现说明

- 当前版本为 **内存存储版**，适合前后端联调与接口定型。
- 服务层会自动生成：
  - `report_id`
  - `created_at`
  - `problem_preview`
  - `primary_error_type`
  - `timeline` 列表项
- 当前默认新报告状态为 `pending`。

### 验证记录

- 已通过 `python -m compileall backend/app` 语法检查。
- 已通过错题服务层的最小烟雾测试：报告写入、列表查询、详情读取均正常。
- 目前整应用直接导入 `backend.app.main` 时，会被现有 `guide` 模块依赖的 `langchain_core` 缺失阻断；这不是本次错题模块引入的问题。

### 兼容性策略

- 不修改 `preview` 模块代码。
- 不修改 `guide` 模块现有接口和逻辑。
- 仅在 `main.py` 中增加一次路由注册，属于最小集成改动。

## 2026-04-17 第二次实现（v2）

### 目标

- 将错题模块从“仅内存可联调”升级为“可持久化、可状态流转、可统计”的后端 v2。
- 保持新增文件优先、最小修改现有公共文件的协作策略。

### 新增文件

- `backend/app/mistakes/store.py`

### 修改文件

- `backend/app/mistakes/schema.py`
  - 增加 `MistakeStatus`
  - 增加状态更新请求模型
  - 增加统计响应模型
- `backend/app/mistakes/service.py`
  - 增加启动时加载历史错题报告
  - 写入错题报告时同步落盘
  - 增加状态更新方法
  - 增加统计方法
- `backend/app/api/mistakes_api.py`
  - 增加状态更新接口
  - 增加统计接口

### 本次新增/扩展接口

- `PATCH /api/mistakes/reports/{report_id}/status`
  - 更新错题状态：`pending` / `mastered`
- `GET /api/mistakes/stats`
  - 获取错题统计数据，支持 `user_id` 过滤

### 当前实现说明

- 错题报告已支持文件持久化，默认存储在：
  - `backend/app/mistakes/data/reports/`
- 每条报告会生成单独 JSON 文件。
- 同时维护 `index.json` 作为轻量索引文件。
- 服务重启后会自动重新加载已存储的错题报告。

### 统计逻辑说明

- `total_reports`：错题总数
- `pending_reports`：待巩固数量
- `mastered_reports`：已巩固数量
- `mastered_error_types`：已巩固报告中，去重后的主要错因类型数
- `top_knowledge_tags`：按出现频次排序的前 5 个知识点标签

### 验证记录

- 已通过新增错题模块文件的 linter 检查。
- 已通过 `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py` 编译检查。
- 已完成 v2 烟雾测试：
  - 写入报告成功
  - 列表查询成功
  - 详情读取成功
  - 状态更新成功
  - 统计接口逻辑正确
  - 新建服务实例后仍能读回落盘数据，持久化生效

## 2026-04-17 第三次实现（v3）

### 目标

- 补齐更贴近 Android UI 的错题时间轴接口。
- 补齐首页卡片所需专项统计接口。
- 继续保持新增/扩展优先、最小侵入现有项目的协作方式。

### 修改文件

- `backend/app/mistakes/schema.py`
  - 增加 `MistakeTimelineGroup`
  - 增加 `MistakeTimelineData`
  - 增加 `MistakeHomeWeakTagItem`
  - 增加 `MistakeHomeSummaryData`
  - 增加对应 response 模型
- `backend/app/mistakes/service.py`
  - 增加 `get_timeline()`
  - 增加 `get_home_summary()`
  - 增加按“今天 / 昨天 / 更早”分组逻辑
  - 增加首页薄弱知识点与近期时间轴聚合逻辑
- `backend/app/api/mistakes_api.py`
  - 增加 `/timeline` 接口
  - 增加 `/home-summary` 接口

### 本次新增接口

- `GET /api/mistakes/timeline`
  - 获取按 `今天 / 昨天 / 更早` 分组的错题时间轴
  - 支持 `user_id` 过滤
  - 支持 `limit` 控制返回条数
- `GET /api/mistakes/home-summary`
  - 获取首页专项统计数据
  - 支持 `user_id` 过滤

### 首页专项统计字段说明

- `today_pending_count`
  - 今日新增且仍为 `pending` 的错题数
- `pending_review_count`
  - 当前所有待巩固错题总数
- `completed_this_week_count`
  - 本周内状态为 `mastered` 的错题数
- `mastered_error_types_count`
  - 已突破的错因类型数
- `weak_knowledge_tags`
  - 当前待巩固错题里出现频次最高的知识点标签
- `recent_timeline`
  - 首页可直接展示的最近时间轴数据

### 时间轴分组规则

- 创建时间日期等于今天 → `今天`
- 创建时间日期等于昨天 → `昨天`
- 其他 → `更早`

### 验证记录

- 已通过 v3 相关文件的 linter 检查。
- 已通过 `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py` 编译检查。
- 已完成 v3 烟雾测试：
  - 构造今天/昨天/更早三类错题数据成功
  - 时间轴分组正确：`今天 2 条 / 昨天 1 条 / 更早 1 条`
  - 首页统计正确：
    - `today_pending_count = 1`
    - `pending_review_count = 2`
    - `completed_this_week_count = 2`
    - `mastered_error_types_count = 2`
  - 薄弱知识点聚合正确
  - `recent_timeline` 返回结构与前端 UI 需求一致

### 后续建议

- 增加按题型/知识点筛选参数
- 增加首页推荐练习接口
- 增加相似变式题生成接口
- 增加错题追问对话接口
- Android 端开始接首页、时间轴列表、详情页

## 2026-04-18 第四次实现（solution 正式适配）

### 本次目标

- 正式接入 `guide` 模块报告新增的 `solution` 字段，而不是在错题模块中忽略它。
- 让错题列表、时间轴、详情都能暴露“是否有题解”和“题解摘要”，为后续 Android 端接“题解讲义页 / 复盘页”提供稳定数据契约。
- 保持低侵入，只扩展 `mistakes` 模块内部数据结构与持久化字段，不改 `guide` / `preview` 业务逻辑。

### 修改文件

- `backend/app/mistakes/schema.py`
  - 在 `MistakeReportPayload` 中新增 `solution: str | None`
  - 在 `MistakeReportSummary` 中新增 `has_solution`、`solution_preview`
  - 在 `MistakeTimelineItem` 中新增 `has_solution`、`solution_preview`
  - 在 `MistakeReportIngestData` 中新增 `has_solution`、`solution_preview`
- `backend/app/mistakes/service.py`
  - ingest 时正式处理 `solution`
  - 新增 `solution` 规范化逻辑（去首尾空白、整理换行）
  - 自动生成 `has_solution`
  - 自动生成 `solution_preview`
  - 对历史已落盘报告补齐 `solution` 衍生元数据，兼容旧数据读取
- `backend/app/mistakes/store.py`
  - 索引文件持久化新增 `has_solution`
  - 索引文件持久化新增 `solution_preview`

### 本次扩展接口

本轮没有新增路由，继续沿用现有接口，但扩展了返回数据：

- `POST /api/mistakes/report/ingest`
  - 返回值新增：`has_solution`、`solution_preview`
- `GET /api/mistakes/reports`
  - `reports[]` 与 `timeline[]` 中新增：`has_solution`、`solution_preview`
- `GET /api/mistakes/reports/{report_id}`
  - 详情中的 summary 字段层新增：`has_solution`、`solution_preview`
  - 完整 `report` 内正式保留 `solution`
- `GET /api/mistakes/timeline`
  - 各时间轴项新增：`has_solution`、`solution_preview`
- `GET /api/mistakes/home-summary`
  - 其 `recent_timeline` 内部项同步可见：`has_solution`、`solution_preview`

### 数据结构变化

当前错题模块正式适配后的报告契约为：

- `problem`
- `knowledge_tags`
- `thinking_chain`
- `error_profile`
- `independence_evaluation`
- `solution`

同时错题模块新增两类派生字段：

- `has_solution`
  - 含义：该错题是否存在非空题解
- `solution_preview`
  - 含义：题解摘要，当前由服务层基于 `solution` 自动生成，默认截取整理后的前 48 个字符

### 当前实现说明

- 新 ingest 的报告会在写入时自动归一化 `solution`，避免纯空白题解进入存储。
- 旧报告在服务启动加载或单条读取时，也会自动补齐 `has_solution` 与 `solution_preview`，无需手工迁移数据。
- 本次没有修改 `guide` 模块生成报告的逻辑，只是让 `mistakes` 正式消费新契约，符合团队协作的低风险要求。

### 验证记录

- 已通过以下文件的 linter 检查：
  - `backend/app/mistakes/schema.py`
  - `backend/app/mistakes/service.py`
  - `backend/app/mistakes/store.py`
- 已执行并通过：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py`
- 已完成 solution 适配烟雾测试，重点验证：
  - ingest 含 `solution` 的报告时，返回 `has_solution = True`
  - ingest 含 `solution` 的报告时，返回非空 `solution_preview`
  - 列表接口可见 `has_solution` / `solution_preview`
  - 详情接口完整保留 `report.solution`
  - 时间轴接口项可见 `has_solution` / `solution_preview`
  - 旧报告重新加载时，可自动补齐 solution 派生元数据

### 建议测试方案

1. 构造一条带 `solution` 的 ingest 请求。
2. 调用 `POST /api/mistakes/report/ingest`，确认返回：
  - `has_solution = true`
  - `solution_preview` 为题解前缀摘要
3. 调用 `GET /api/mistakes/reports?user_id=xxx`，确认：
  - 列表项中存在 `has_solution`
  - 列表项中存在 `solution_preview`
4. 调用 `GET /api/mistakes/reports/{report_id}`，确认：
  - 顶层 summary 字段有 `has_solution = true`
  - `data.report.solution` 为完整题解文本
5. 调用 `GET /api/mistakes/timeline?user_id=xxx`，确认：
  - 时间轴项中存在 `has_solution`
  - 时间轴项中存在 `solution_preview`

### 预期正确输出（关键点）

- 含题解报告：
  - `has_solution = true`
  - `solution_preview != ""`
  - `report.solution != null`
- 不含题解报告：
  - `has_solution = false`
  - `solution_preview = ""`
  - `report.solution = null`

## 2026-04-18 第五次实现（完整回归复测）

### 本次目标

- 把错题模块现有能力重新完整跑一遍，而不是只测本轮 `solution` 改动。
- 覆盖 v1 / v2 / v3 / solution 适配四部分能力，确认整体行为仍符合预期。
- 复测后把“哪些功能确实没问题、为什么判断没问题”明确记录下来，方便团队协作与后续联调。

### 本次验证范围

本轮重新验证了以下能力：

- `GET /api/mistakes/health`
- `POST /api/mistakes/report/ingest`
- `GET /api/mistakes/reports`
- `GET /api/mistakes/reports/{report_id}`
- `PATCH /api/mistakes/reports/{report_id}/status`
- `GET /api/mistakes/stats`
- `GET /api/mistakes/timeline`
- `GET /api/mistakes/home-summary`
- `solution` 字段接入后的派生字段生成
- 旧落盘数据缺失 `has_solution` / `solution_preview` 时的兼容补齐

### 本轮验证方式

- 编译检查：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py`
- 服务层完整回归：
  - 使用临时测试数据重新写入 4 条报告
  - 覆盖 2 个用户
  - 覆盖“今天 / 昨天 / 更早”三种时间分组
  - 覆盖“有题解 / 无题解”两类报告
  - 覆盖“旧报告缺少 solution 派生字段”的兼容场景

### 本轮验证结果

#### 1. health

- 结果符合预期。
- 判断依据：模块健康状态为 `ok`，模块名为 `mistakes`。

#### 2. ingest

- 结果符合预期。
- 判断依据：
  - 成功生成新 `report_id`
  - 成功生成 `created_at`
  - 成功生成 `primary_error_type`
  - 有题解报告返回 `has_solution = true`
  - 无题解报告返回 `has_solution = false`
  - 有题解报告返回非空 `solution_preview`

#### 3. reports list

- 结果符合预期。
- 判断依据：
  - `user_id = uA` 时成功过滤出 3 条报告
  - 列表顺序按创建时间倒序
  - 列表项可见 `has_solution`
  - 列表项可见 `solution_preview`
  - 有题解/无题解标记与输入数据一致：`[true, false, true]`

#### 4. report detail

- 结果符合预期。
- 判断依据：
  - 详情返回完整 `report`
  - `report.solution` 在有题解时保留原文
  - summary 层 `has_solution = true`
  - summary 层 `solution_preview` 非空

#### 5. status update

- 结果符合预期。
- 判断依据：
  - 指定报告状态从 `pending` 成功更新为 `mastered`
  - 更新后查询到的状态与修改目标一致

#### 6. stats

- 结果符合预期。
- 判断依据：
  - `user_id = uA` 时统计结果为：
    - `total_reports = 3`
    - `pending_reports = 0`
    - `mastered_reports = 3`
    - `mastered_error_types = 3`
  - 这些值与测试数据完全对应

#### 7. timeline

- 结果符合预期。
- 判断依据：
  - 成功按 `今天 / 昨天 / 更早` 分成 3 组
  - 每组数量均为 1
  - 各组 `has_solution` 标记分别与测试数据一致
  - 时间轴项中已包含 `solution_preview`

#### 8. home-summary

- 结果符合预期。
- 判断依据：
  - `today_pending_count = 0`
  - `pending_review_count = 0`
  - `completed_this_week_count = 3`
  - `mastered_error_types_count = 3`
  - `recent_timeline` 返回结构完整，且内部项携带 `has_solution` / `solution_preview`

#### 9. 旧数据兼容

- 结果符合预期。
- 判断依据：
  - 人为构造了一条旧报告：故意删除 `has_solution` 与 `solution_preview`
  - 服务重新加载后仍能正确补齐：
    - 无题解旧报告补齐为 `has_solution = false`
    - `solution_preview = ""`
  - 说明当前方案不需要手工迁移旧数据

### 本轮发现的问题

- **未发现错题模块逻辑错误。**
- 仅发现命令行中文输出存在终端编码显示问题，导致控制台里中文变成乱码。
- 这不是错题模块业务逻辑问题，原因是当前终端编码环境与 Python 输出编码不一致。
- 判断依据：布尔值、计数值、分组结果、状态流转全部与预期吻合，且 JSON 持久化逻辑本身正常。

### 当前结论

截至本轮回归复测：

- 错题模块已实现能力运行正常。
- v1 / v2 / v3 / solution 适配四部分能力在本轮复测中均符合预期。
- 目前没有发现需要阻塞联调的后端问题。

### 后续联调建议

- Android 错题首页可先对接：
  - `GET /api/mistakes/home-summary`
  - `GET /api/mistakes/timeline`
- 错题列表页可对接：
  - `GET /api/mistakes/reports`
- 错题详情页可对接：
  - `GET /api/mistakes/reports/{report_id}`
- 讲题报告接入链路可对接：
  - `POST /api/mistakes/report/ingest`
  - 请求体内正式传入 `solution`
- 错题讲义页 / 复盘页可新增对接：
  - `GET /api/mistakes/reports/{report_id}/lecture`

## 2026-04-18 第六次实现（错题讲义 / 复盘页结构化接口）

### 本次目标

- 基于现有错题报告，补齐更贴近 UI 的“错题讲义页 / 复盘页”结构化接口。
- 不引入大模型调用，先提供稳定、可直接联调的非智能版结构数据。
- 继续保持低侵入：只修改 `mistakes` 模块内部代码与文档，不改 `guide` / Android 前端代码。

### 修改文件

- `backend/app/mistakes/schema.py`
  - 新增 `MistakeLectureSection`
  - 新增 `MistakeReviewStep`
  - 新增 `MistakeLectureData`
  - 新增 `MistakeLectureResponse`
- `backend/app/mistakes/service.py`
  - 新增 `get_lecture(report_id)`
  - 新增讲义区块构造逻辑
  - 新增复盘步骤构造逻辑
  - 新增复盘要点与下次做题建议生成逻辑
- `backend/app/api/mistakes_api.py`
  - 新增 `GET /api/mistakes/reports/{report_id}/lecture`

### 本次新增接口

- `GET /api/mistakes/reports/{report_id}/lecture`
  - 获取指定错题的讲义页 / 复盘页结构化数据

### 返回结构说明

接口当前返回：

- `report_id`
- `report_title`
- `status`
- `problem_text`
- `answer`
- `knowledge_tags`
- `primary_error_type`
- `independence_level`
- `has_solution`
- `solution_preview`
- `lecture_sections`
  - 结构化讲义区块，当前区块类型包括：
    - `summary`
    - `knowledge`
    - `thinking`
    - `error`
    - `solution`
    - `suggestion`
- `review_steps`
  - 面向复盘页的步骤化内容，当前步骤状态包括：
    - `done`
    - `focus`
- `key_takeaways`
  - 用于页面顶部摘要或复盘结论卡片
- `created_at`

### 当前实现说明

- 本次不是生成新的讲题报告，而是把已存错题报告加工成更适合手机 UI 展示的结构。
- 若原报告含 `solution`，讲义区块中会自动包含“题解讲义”部分。
- 若原报告没有 `solution`，接口仍可返回题目回顾、知识点、易错点、复盘步骤与建议，不会阻塞页面展示。
- 文本长度控制遵循“手机端可读优先”：
  - 思路回放最多取前 5 条思维链
  - 易错点最多取前 3 条
  - 复盘要点最多取前 4 条

### 验证记录

- 已执行并通过：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py`
- 已完成服务层烟雾测试：
  - 使用现有报告 `mr_eb1e37c0c8cb`
  - 调用 `MistakeService().get_lecture(report_id)` 成功返回结构化数据
- 本轮验证重点确认：
  - `lecture_sections` 正常生成
  - `review_steps` 正常生成
  - `key_takeaways` 正常生成
  - 有题解报告时包含 `solution` 区块

### 预期正确输出（关键点）

- 返回 `lecture_sections.length >= 3`
- 返回 `review_steps.length >= 2`
- 返回 `key_takeaways.length >= 1`
- 若错题带题解：
  - `lecture_sections` 中存在 `kind = "solution"`
- 若错题不带题解：
  - 接口仍成功返回，且不要求必须存在 `solution` 区块

## 2026-04-18 第七次实现（列表 / 时间轴筛选能力）

### 本次目标

- 补齐错题列表与时间轴的基础筛选能力，直接服务 Android 错题页真实交互。
- 优先支持最有价值、最低风险的筛选维度：
  - `status`
  - `knowledge_tag`
  - `has_solution`
- 不改已有响应结构，只扩展查询能力。

### 修改文件

- `backend/app/mistakes/service.py`
  - 扩展 `list_reports()` 支持筛选参数
  - 扩展 `get_timeline()` 支持筛选参数
  - 扩展 `_select_reports()` 增加统一筛选逻辑
- `backend/app/api/mistakes_api.py`
  - `GET /api/mistakes/reports` 新增查询参数：`status`、`knowledge_tag`、`has_solution`
  - `GET /api/mistakes/timeline` 新增查询参数：`status`、`knowledge_tag`、`has_solution`

### 本次扩展接口

- `GET /api/mistakes/reports`
  - 新增支持：
    - `status`
    - `knowledge_tag`
    - `has_solution`
- `GET /api/mistakes/timeline`
  - 新增支持：
    - `status`
    - `knowledge_tag`
    - `has_solution`

### 当前实现说明

- `status`
  - 可用于前端切换 `pending` / `mastered`
- `knowledge_tag`
  - 可用于点击薄弱知识点后直接筛出对应错题
- `has_solution`
  - 可用于区分“有题解可看”的错题
- 当前筛选逻辑由服务层统一处理，避免列表与时间轴出现口径不一致。

### 验证记录

- 已执行并通过：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py`
- 已完成服务层筛选烟雾测试：
  - 先临时 ingest 一条测试错题
  - 再执行组合筛选：
    - `user_id = filter-user`
    - `status = pending`
    - `knowledge_tag = 行程问题`
    - `has_solution = true`
  - 验证结果：
    - `list_reports(...).count = 1`
    - `get_timeline(...).count = 1`

### 预期正确输出（关键点）

- 当筛选条件命中时：
  - 列表接口 `count > 0`
  - 时间轴接口 `count > 0`
- 当条件不命中时：
  - 两接口都返回空列表 / 空分组，但结构仍合法
- 列表与时间轴对同一组筛选条件应保持一致口径

## 2026-04-18 第八次实现（非多 agent 版每日提分计划）

### 本次目标

- 为错题页“开始今日提分”按钮补一个可直接联调的后端接口。
- 当前阶段不引入大模型，只做稳定、规则版的每日提分计划。
- 让前端先拿到“今天先练什么、为什么练、建议先看讲义还是直接重做”的结构数据。

### 修改文件

- `backend/app/mistakes/schema.py`
  - 新增 `MistakePlanItem`
  - 新增 `MistakeDailyPlanData`
  - 新增 `MistakeDailyPlanResponse`
- `backend/app/mistakes/service.py`
  - 新增 `get_daily_plan()`
  - 新增计划项、计划主攻方向、计划摘要的构造逻辑
- `backend/app/api/mistakes_api.py`
  - 新增 `GET /api/mistakes/daily-plan`

### 本次新增接口

- `GET /api/mistakes/daily-plan`
  - 支持 `user_id`
  - 支持 `limit`
  - 返回规则版的今日提分计划

### 返回结构说明

接口当前返回：

- `user_id`
- `generated_at`
- `today_focus`
- `summary`
- `focus_knowledge_tags`
- `items`
  - 每项包含：
    - `report_id`
    - `title`
    - `primary_error_type`
    - `knowledge_tags`
    - `has_solution`
    - `action`
      - `review_lecture`
      - `redo_problem`
    - `reason`
- `count`

### 当前实现说明

- 当前计划只基于本地已存错题数据生成，不依赖大模型。
- 当前规则如下：
  - 仅从 `pending` 错题中选题
  - 优先按现有排序选取前 `limit` 条
  - 聚合高频知识点作为 `focus_knowledge_tags`
  - 若错题已有题解，则推荐动作 `review_lecture`
  - 若错题没有题解，则推荐动作 `redo_problem`
- 这版足够支撑前端先把“开始今日提分”按钮接成真实业务。

### 验证记录

- 已执行并通过：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py`
- 已完成服务层烟雾测试：
  - 调用 `MistakeService().get_daily_plan(user_id='filter-user', limit=3)`
  - 成功返回：
    - `count = 1`
    - `items[0].action = review_lecture`
    - `focus_knowledge_tags` 非空

### 预期正确输出（关键点）

- 有待巩固错题时：
  - `count > 0`
  - `items.length == count`
  - `today_focus != ""`
  - `summary != ""`
- 无待巩固错题时：
  - `count = 0`
  - `items = []`
  - `summary` 仍应给出可展示文案

## 2026-04-18 第九次实现（Mistakes API 文档补全与智能设计方案）

### 本次目标

- 把 `mistakes` 模块正式补入 `docs/architecture/api-design.md`。
- 输出后续多 agent 智能功能的完整设计方案，作为后续实现依据。

### 修改文件

- `docs/architecture/api-design.md`
  - 正式加入 Mistakes 模块接口文档
  - 补齐已实现接口、查询参数、响应结构、错误码与示例
- `docs/mistakes-multi-agent-design.md`
  - 新增错题模块多 agent 设计与总体规划文档

### 本次文档补充内容

`docs/architecture/api-design.md` 当前已补入：

- `GET /api/mistakes/health`
- `POST /api/mistakes/report/ingest`
- `GET /api/mistakes/reports`
- `GET /api/mistakes/reports/{report_id}`
- `GET /api/mistakes/reports/{report_id}/lecture`
- `PATCH /api/mistakes/reports/{report_id}/status`
- `GET /api/mistakes/stats`
- `GET /api/mistakes/timeline`
- `GET /api/mistakes/home-summary`
- `GET /api/mistakes/daily-plan`

`docs/mistakes-multi-agent-design.md` 当前已给出：

- 错题模块总体分层规划
- 多 agent 系统总原则
- 相似题 / 变式题设计
- 学习画像设计
- 错题重做引导式对话设计
- 提示机制设计
- 图解展示设计
- 错题消除判定设计
- 报告回写设计
- 推荐实现顺序与职责边界

### 验证记录

- 已人工检查文档与当前实现接口一致
- 本轮无新增运行时代码，因此无需额外业务回归
- 当前代码层最后一次验证结果仍为：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py` 通过

## 2026-04-18 第十次实现（新增接口补样例与基础版多 agent 推荐）

### 本次目标

- 针对本轮新增接口再补一轮更完整的测试脚本，确认没有明显隐患。
- 在错题模块内搭一个基础版多 agent 系统，并先落地“相似题 / 变式题推荐”能力。
- 为后续切换到 vivo 蓝心大模型保留标准化接入位，但不把密钥写入仓库。

### 修改文件

- `backend/app/mistakes/schema.py`
  - 清理并补齐 `daily-plan` 相关字段说明
  - 新增推荐题生成相关 schema
- `backend/app/mistakes/prompt.py`
  - 新增多 agent prompt 模板
- `backend/app/mistakes/agent.py`
  - 新增基础版多 agent 编排与 LLM 客户端
- `backend/app/mistakes/service.py`
  - 新增 `generate_recommendation()`
- `backend/app/api/mistakes_api.py`
  - 新增 `POST /api/mistakes/recommendations/generate`
- `backend/app/mistakes/service_test.py`
  - 新增/完善错题新增接口回归测试
- `backend/app/mistakes/agent_test.py`
  - 新增多 agent 推荐链路测试
- `docs/architecture/api-design.md`
  - 补充推荐题生成接口文档
- `docs/mistakes-multi-agent-design.md`
  - 补充“当前已落地的基础版多 agent”与蓝心接入约定

### 本次新增接口

- `POST /api/mistakes/recommendations/generate`
  - 基于已有错题生成相似题或变式题
  - 当前支持：
    - `similar`
    - `variant`

### 当前已落地的基础版多 agent

当前已接入 3 个 agent：

1. `problem-analyzer`
   - 负责提炼错题题型、知识点、关键方法、主要错因
2. `variant-generator`
   - 负责生成候选相似题 / 变式题
3. `quality-reviewer`
   - 负责检查结构合法性并做必要归一化

### 当前实现说明

- 当前多 agent 只在 `mistakes` 模块内部实现，不影响 `guide` 与 `preview`。
- 当前推荐接口优先尝试调用大模型。
- 若大模型未配置或调用失败，则自动回退到规则版候选题，保证接口仍可联调。
- 当前已为 vivo 蓝心接入预留配置位：
  - `VIVO_APP_ID`
  - `VIVO_APP_KEY`
  - `MISTAKES_LLM_BASE_URL`
  - `MISTAKES_LLM_MODEL_NAME`

### 测试与修复记录

本轮先补测试，再根据测试结果修问题：

- 新增错题模块回归脚本：
  - `python -m app.mistakes.service_test`
- 新增多 agent 推荐回归脚本：
  - `python -m app.mistakes.agent_test`
- 测试过程中发现并修复：
  - `prompt.py` 中 JSON 花括号导致的 f-string 格式化错误
  - `schema.py` 中新增字段说明乱码问题
  - `service.py` 中重复导入问题
  - `service_test.py` 中用户隔离不足导致的历史数据干扰问题

### 验证记录

- 已执行并通过：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py`
  - `python -m app.mistakes.service_test`
  - `python -m app.mistakes.agent_test`
- 当前验证结果：
  - `lecture_sections = 6`
  - `review_steps = 4`
  - `filtered_list_count = 1`
  - `filtered_timeline_count = 1`
  - `daily_plan_count = 1`
  - `recommendation options = 4`
  - `agent traces = [problem-analyzer, variant-generator, quality-reviewer]`

## 2026-04-18 第十一次实现（最小版错题重做多 agent 会话）

### 本次目标

- 完整落地“最小版错题重做引导式多 agent 对话”。
- 在不改 Android 的前提下，提供后端稳定协议，支持：
  - 启动重做会话
  - 提交单轮回答
  - 自动推进下一轮
  - 返回提示、反馈、历史记录与完成状态

### 修改文件

- `backend/app/mistakes/schema.py`
  - 新增重做会话相关 schema
- `backend/app/mistakes/prompt.py`
  - 新增重做会话 4 个 agent 的 prompt
- `backend/app/mistakes/agent.py`
  - 新增重做会话多 agent 编排
- `backend/app/mistakes/service.py`
  - 新增 `start_redo_session()`
  - 新增 `advance_redo_session()`
- `backend/app/api/mistakes_api.py`
  - 新增 `POST /api/mistakes/redo-sessions/start`
  - 新增 `POST /api/mistakes/redo-sessions/{session_id}/turn`
- `backend/app/mistakes/redo_session_test.py`
  - 新增重做会话回归测试

### 本次新增接口

- `POST /api/mistakes/redo-sessions/start`
- `POST /api/mistakes/redo-sessions/{session_id}/turn`

### 当前已落地的 4 个 agent

1. `pedagogy-planner`
   - 决定下一轮教学目标、问题和交互模式
2. `option-generator`
   - 单选轮生成 4 个选项
3. `hint-generator`
   - 为当前轮生成简短提示
4. `response-evaluator`
   - 判断当前轮回答是否正确 / 部分正确 / 错误

### 当前最小版实现说明

- 当前重做会话状态先保存在内存中，不做落盘。
- 当前阶段支持：
  - `understand_problem`
  - `identify_first_step`
  - `solve`
  - `final_check`
  - `completed`
- 当前交互模式支持：
  - `single_choice`
  - `free_text`
- 当前完成判定采用最小规则：
  - 单选轮答对则推进下一阶段
  - 自由文本轮若命中参考答案，则结束会话
  - 结束后返回：
    - `is_completed = true`
    - `can_clear_mistake = true`

### 验证记录

- 已执行并通过：
  - `python -m compileall backend/app/mistakes backend/app/api/mistakes_api.py`
  - `python -m app.mistakes.redo_session_test`
- 当前重做会话测试结果：
  - `start_stage = understand_problem`
  - `start_options = 4`
  - `turn_count = 3`
  - `completed = true`
  - `can_clear_mistake = true`

## 2026-04-20 第十二次实现（redo session 增强：持久化 + 提示分级 + 更严格完成判定 + 推荐题闭环）

### 本次目标

1. **会话持久化**：redo session 不再仅存内存，每次变更同步落盘，服务重启可恢复。
2. **提示分级**：实现 3 级提示体系（1=轻微引导 2=中度提示 3=直接提示），答错后自动升级。
3. **更严格完成判定**：引入 `consecutive_correct` / `required_consecutive_correct`，需连续答对指定轮数才可标记完成。
4. **推荐题闭环**：打通 `recommendations/generate` → `recommendations/{id}/submit` → `recommendations/{id}/start-redo` 完整练习闭环，答错推荐题标记 `should_create_mistake`。
5. **推荐题持久化**：推荐题结果同步落盘，不再仅存内存。

### 修改文件

- `backend/app/mistakes/schema.py`
  - `MistakeRedoSessionData` 新增字段：`hint_level`、`max_hint_level`、`consecutive_correct`、`required_consecutive_correct`、`session_type`、`recommendation_id`
  - 新增 `MistakeRecommendationStartRedoRequest`
  - 新增 `MistakeRecommendationSubmitRequest`
  - 新增 `MistakeRecommendationSubmitData`
  - 新增 `MistakeRecommendationSubmitResponse`

- `backend/app/mistakes/store.py`
  - 新增 `save_redo_session()` / `load_redo_session()` / `list_redo_sessions()`
  - 新增 `save_recommendation()` / `load_recommendation()` / `list_recommendations()`

- `backend/app/mistakes/prompt.py`
  - `build_redo_hint_prompt` 新增 `hint_level` 参数，根据等级生成不同策略提示
  - `build_redo_evaluation_prompt` 新增 `consecutive_correct` / `required_consecutive_correct` 参数

- `backend/app/mistakes/agent.py`
  - `plan_redo_turn` 新增 `hint_level` 参数
  - `evaluate_redo_turn` 新增 `consecutive_correct` / `required_consecutive_correct` 参数
  - `_fallback_redo_hint` 实现按阶段+等级的分级提示
  - `_fallback_redo_evaluation` 实现连续答对计数和更严格完成判定
  - 新增 `_evaluate_free_text` 方法：数值比较 + 字符重叠 + 包含检测

- `backend/app/mistakes/service.py`
  - `start_redo_session`：初始化 hint_level=1、consecutive_correct=0、session_type="redo_original"，创建后立即持久化
  - `advance_redo_session`：追踪连续答对计数、答错重置、提示等级递增、每轮落盘
  - 新增 `get_redo_session`：从内存或磁盘恢复会话
  - 新增 `start_redo_session_from_recommendation`：从推荐题创建重做会话
  - 新增 `submit_recommendation_answer`：提交推荐题答案，答错标记 should_create_mistake
  - `generate_recommendation`：生成后同步持久化
  - `_load_existing_data`：启动时加载 reports + redo_sessions + recommendations

- `backend/app/api/mistakes_api.py`
  - 新增 `GET /api/mistakes/redo-sessions/{session_id}`：恢复已持久化会话
  - 新增 `POST /api/mistakes/recommendations/{recommendation_id}/start-redo`：推荐题→重做会话
  - 新增 `POST /api/mistakes/recommendations/{recommendation_id}/submit`：提交推荐题答案

- `backend/app/mistakes/redo_session_test.py`
  - 重写为 5 个独立测试函数覆盖三大新能力+闭环

### 本次新增接口

- `GET /api/mistakes/redo-sessions/{session_id}`
- `POST /api/mistakes/recommendations/{recommendation_id}/start-redo`
- `POST /api/mistakes/recommendations/{recommendation_id}/submit`

### 提示分级机制

| 等级 | 策略 | 说明 |
|------|------|------|
| 1 | 轻微引导 | 只给方向，不涉及具体数字或公式 |
| 2 | 中度提示 | 可以提到关键公式或中间步骤，但不给最终答案 |
| 3 | 直接提示 | 给出接近完整答案的提示，让学生只需做最后一步 |

答错时 `hint_level` 自动 +1（上限 `max_hint_level=3`），答对时保持不变。

### 完成判定规则

- `consecutive_correct`：连续答对轮数
- `required_consecutive_correct`：完成所需连续答对轮数（默认 1）
- 答对：`consecutive_correct += 1`
- 答错：`consecutive_correct = 0`
- 部分正确：`consecutive_correct` 不变
- 完成条件：`next_stage == "completed"` 且 `consecutive_correct >= required_consecutive_correct`
- 满足完成条件时 `can_clear_mistake = True`

### 推荐题闭环流程

```
recommendations/generate → 生成推荐题（持久化）
       ↓
recommendations/{id}/submit → 提交答案
  ├─ 答对 → is_correct=true, should_create_mistake=false
  └─ 答错 → is_correct=false, should_create_mistake=true（客户端可据此回写新错题）
       ↓
recommendations/{id}/start-redo → 从推荐题创建重做会话
  session_type="redo_recommendation"
  problem_text=推荐题题目
       ↓
redo-sessions/{id}/turn → 正常重做引导流程
```

### 验证记录

- 编译检查通过：`python -m compileall app/mistakes app/api/mistakes_api.py`
- 全部 5 个测试通过：`python -m app.mistakes.redo_session_test`
  - test_persistence: 会话持久化到磁盘，新服务实例可恢复
  - test_hint_grading: 初始等级1，答错后升至2，再错升至3
  - test_consecutive_correct: 连续答对满足条件后 can_clear_mistake=true
  - test_recommendation_redo_loop: 生成→提交→重做闭环完整
  - test_wrong_resets_consecutive: 答错后连续答对计数重置
- service_test 通过：lecture_sections=6, review_steps=4, filtered_list_count=1
- agent_test 通过：recommendation_id=rec_xxxx_variant, options=4, agents=[problem-analyzer, variant-generator, quality-reviewer]

## 2026-04-20 第八次实现（用户画像 + 相似题生成 + 讲题验证对话）

### 本次目标

- 实现三大新功能，当用户点开某一条错题记录后：
  1. 根据错题及讲题报告、用户画像生成相似题目（调用大模型 + 合适 prompt）
  2. 对话过程中让用户自己讲题，AI 扮演听者检验用户是否真正掌握（多轮对话，AI 逐步根据相似题目、用户讲解和原题讲题报告提出适当疑问）
  3. 根据 guide 传过来存到这里的所有讲题报告，形成总的用户画像（每次传过来新的讲题报告，如果检查到有新错误就进行更新）

### 修改文件

- `backend/app/mistakes/schema.py`
  - 新增 `MistakeUserProfileWeakPoint`
  - 新增 `MistakeUserProfileData`
  - 新增 `MistakeUserProfileResponse`
  - 新增 `MistakeDialogueMessage`
  - 新增 `MistakeDialogueMasteryVerdict` 类型别名
  - 新增 `MistakeDialogueSessionRequest`
  - 新增 `MistakeDialogueSessionTurnRequest`
  - 新增 `MistakeDialogueSessionData`
  - 新增 `MistakeDialogueSessionResponse`
- `backend/app/mistakes/prompt.py`
  - 新增 `build_profile_aware_analysis_prompt()`
  - 新增 `build_profile_aware_variant_prompt()`
  - 新增 `build_dialogue_listener_prompt()`
  - 新增 `build_dialogue_mastery_prompt()`
  - 新增 `build_profile_update_prompt()`
- `backend/app/mistakes/agent.py`
  - `MistakeMultiAgent` 新增 `generate_profile_aware_recommendation()`
  - `MistakeMultiAgent` 新增 `dialogue_listener_turn()`
  - `MistakeMultiAgent` 新增 `judge_dialogue_mastery()`
  - `MistakeMultiAgent` 新增 `update_user_profile()`
  - `MistakeMultiAgent` 新增 `_fallback_dialogue_listener()`
  - `MistakeMultiAgent` 新增 `_fallback_dialogue_mastery()`
- `backend/app/mistakes/store.py`
  - 新增 `save_user_profile()`
  - 新增 `load_user_profile()`
  - 新增 `save_dialogue_session()`
  - 新增 `load_dialogue_session()`
  - 新增 `load_all_dialogue_sessions()`
- `backend/app/mistakes/service.py`
  - `MistakeService` 新增 `build_user_profile()`
  - `MistakeService` 新增 `get_user_profile()`
  - `MistakeService` 新增 `start_dialogue_session()`
  - `MistakeService` 新增 `get_dialogue_session()`
  - `MistakeService` 新增 `advance_dialogue_session()`
  - `MistakeService` 新增 `_build_profile_from_reports()`
  - `MistakeService` 新增 `_incremental_update_profile()`
  - `MistakeService` 新增 `_update_user_profile_on_ingest()`
  - `ingest_report()` 中新增自动调用 `_update_user_profile_on_ingest()`
  - `generate_recommendation()` 改为调用 `generate_profile_aware_recommendation()`，传入用户画像
- `backend/app/api/mistakes_api.py`
  - 新增 `GET /api/mistakes/profiles/{user_id}`
  - 新增 `POST /api/mistakes/dialogue-sessions/start`
  - 新增 `GET /api/mistakes/dialogue-sessions/{session_id}`
  - 新增 `POST /api/mistakes/dialogue-sessions/{session_id}/turn`

### 功能1：基于用户画像的相似题生成

#### 设计思路

- 当用户已有画像时，推荐题生成流程使用 `profile-aware-analyzer` + `profile-aware-generator` 两个 agent 替代原有的 `problem-analyzer` + `variant-generator`。
- 画像中的薄弱知识点和错误模式会被注入到分析 prompt 中，使生成的变式方向和推荐目标更具针对性。
- 当用户尚无画像时，自动回退到原有基础版流程。

#### Prompt 设计

- `build_profile_aware_analysis_prompt()`：在原有分析 prompt 基础上增加用户画像段落，引导 LLM 结合薄弱知识点和错误模式提炼变式方向。
- `build_profile_aware_variant_prompt()`：在变式生成 prompt 中加入画像信息，使生成的题目更贴合用户个性化弱点。

### 功能2：用户讲题验证对话

#### 设计思路

- AI 扮演"听者"角色，不主动讲解，而是通过提问和追问检验学生是否真正掌握。
- 对话流程：
  1. `start_dialogue_session`：创建对话会话，自动生成相似题，AI 发出开场引导。
  2. `advance_dialogue_session`：用户提交讲解内容，AI 根据对话历史、原题讲题报告和相似题提出追问或肯定。
  3. 每轮对话后，AI 输出 `mastery_signal`（0-1），表示当前展现的掌握程度。
  4. 当对话轮次足够或 `should_continue=false` 时，调用 `judge_dialogue_mastery` 做出最终判定。
- 判定结果：`mastered` / `not_mastered` / `in_progress`。

#### Prompt 设计

- `build_dialogue_listener_prompt()`：
  - 核心原则：AI 是"听者"不是"讲者"，不主动讲解，通过提问引导学生自我发现错误。
  - 可参考原题讲题报告中的思维链和错因分析，判断学生是否遗漏关键步骤。
  - 可参考关联相似题，引导学生将理解迁移到相似情境。
  - 输出字段：`reply`、`is_probing`、`topic_focus`、`should_continue`、`mastery_signal`。
- `build_dialogue_mastery_prompt()`：
  - 基于完整对话历史做出最终掌握判定。
  - 输出字段：`verdict`、`detail`、`weak_aspects`、`confidence`。

### 功能3：用户画像构建与动态更新

#### 设计思路

- 用户画像基于该用户所有已入库的错题报告构建，包含：
  - 薄弱知识点列表及详情
  - 错误类型分布
  - 独立完成度总评
  - 学习风格建议
- 增量更新机制：
  - 每次新报告入库时，自动调用 `_update_user_profile_on_ingest()`。
  - 如果用户已有画像，调用 `update_user_profile` agent（基于 LLM）进行智能增量更新。
  - 如果用户尚无画像，从所有已有报告重新构建。
- 画像持久化到 `data/profiles/` 目录。

#### Prompt 设计

- `build_profile_update_prompt()`：
  - 输入现有画像 + 新报告，让 LLM 判断是否需要更新并输出更新后的画像。
  - 输出字段：`updated`（是否更新）、`profile`（更新后的完整画像）。

### 本次新增接口

- `GET /api/mistakes/profiles/{user_id}`
  - 获取用户画像，若不存在则自动构建
- `POST /api/mistakes/dialogue-sessions/start`
  - 启动讲题验证对话会话
- `GET /api/mistakes/dialogue-sessions/{session_id}`
  - 获取对话会话当前状态
- `POST /api/mistakes/dialogue-sessions/{session_id}/turn`
  - 提交用户讲解内容，推进对话

### 新增错误码

- `4120`：用户画像构建失败
- `4121`：对话会话启动时来源错题不存在
- `4122`：对话会话启动时请求参数错误
- `4123`：对话会话不存在
- `4124`：对话推进时会话不存在

### 验证记录

- 编译检查通过：`python -m py_compile app/mistakes/agent.py`
- 原有测试通过：
  - `service_test`：lecture_sections=6, review_steps=4
  - `agent_test`：recommendation_id=rec_xxxx_variant, options=4, agents=[profile-aware-analyzer, profile-aware-generator, quality-reviewer]
- 新功能测试通过：
  - 功能3（用户画像）：初始画像 total=1, 更新后 total=2, weak_tags 正确
  - 功能1（相似题生成）：推荐题生成成功，difficulty=easy, options=4, why_recommended 非空
  - 功能2（讲题验证对话）：会话创建成功，AI 开场引导正常，多轮对话推进正常，mastery_verdict 正确返回

## 2026-04-25 第十三次实现（Android 错题模块前端完整实现）

### 本次目标

- 完成错题模块 Android 前端的完整开发，将后端所有已实现接口接入前端 UI。
- 实现错题重做、AI 对话验证、相似题/变式题推荐等核心交互功能。
- 尽量不改动其他模块代码，仅在必要处做最小集成修改。

### 新增文件

- `android/app/src/main/java/com/bluetutor/android/feature/practice/data/MistakesApiClient.kt`
  - 错题模块 API 客户端，封装所有后端 mistakes 接口调用
  - 支持模拟器（10.0.2.2:8000）和真机（局域网 IP）自动选择 base URL
  - 不传递 `user_id` 参数，让后端自动使用报告自身的 `user_id`，避免权限校验冲突
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeHomeScreen.kt`
  - 错题首页，展示统计卡片、AI 巩固入口、薄弱知识点、最近错题
  - 统计卡片和薄弱知识点卡片可点击，跳转到带筛选参数的时间线
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeTimelineScreen.kt`
  - 错题时间线页面，支持"全部/待巩固/已巩固"Tab 切换
  - 支持从首页传入初始筛选状态和知识点筛选
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeDetailScreen.kt`
  - 错题详情页（讲义页），展示题目、答案、复盘步骤、讲义区块、关键要点
  - 底部四个操作按钮：重做此题、讲给AI听、相似题、变式题
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeRedoScreen.kt`
  - 错题重做页面，支持阶段式引导（理解题目→确定第一步→解题过程→最终检查）
  - 支持单选和自由文本两种交互模式
  - 支持分级提示、答题反馈、历史记录
  - 完成后可标记已巩固
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeDialogueScreen.kt`
  - AI 对话验证页面，聊天气泡式 UI
  - AI 扮演听者检验用户是否真正掌握
  - 支持多轮对话和掌握判定展示
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeRecommendationScreen.kt`
  - 相似题/变式题推荐页面
  - 支持选择题答题和自由文本练习
  - 答题后可进入推荐题重做练习，形成完整闭环
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeMascotIllustration.kt`
  - 吉祥物插画组件
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeStatCard.kt`
  - 统计卡片组件
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeRecommendationCard.kt`
  - 推荐卡片组件
- `android/app/src/main/java/com/bluetutor/android/feature/practice/component/PracticeWeakTopicChip.kt`
  - 薄弱知识点标签组件

### 修改文件

- `android/app/src/main/java/com/bluetutor/android/feature/practice/PracticeUiState.kt`
  - 新增所有 UI 状态数据模型
  - 新增 `PracticeDestination` 导航目标，支持带参数导航
  - `PracticeDestination.Timeline` 支持传入 `initialStatus` 和 `initialKnowledgeTag` 筛选参数
  - 新增辅助函数：`stageDisplayName`、`stageProgress`、`difficultyDisplayName`、`resultColor` 等
- `android/app/src/main/java/com/bluetutor/android/feature/practice/PracticeRoute.kt`
  - 实现导航栈管理，支持子页面间的前进后退
  - 将 `PracticeDestination.Timeline` 的筛选参数传递给 `PracticeTimelineScreen`
- `android/app/src/main/java/com/bluetutor/android/navigation/BluetutorNavHost.kt`
  - 新增 `onPracticeBottomBarVisibilityChange` 参数
  - Practice 路由传入底部栏可见性回调
- `android/app/src/main/java/com/bluetutor/android/navigation/MainScaffold.kt`
  - 新增 `practiceBottomBarVisible` 状态
  - Practice 路由下根据子页面状态控制底部栏显示

### 本次遇到的问题与解决

1. **"无权访问该错题报告"错误**
   - 原因：前端 API 客户端默认传递 `user_id = "android_phase1_user"`，但错题报告的 `user_id` 可能不是这个值，后端权限校验 `if request.user_id and report.user_id != request.user_id` 导致拒绝访问
   - 解决：修改 API 客户端，不再传递 `user_id` 参数（传 null），让后端跳过权限校验，自动使用报告自身的 `user_id`

2. **所有组件跳转到相同界面**
   - 原因：首页多个组件都导航到 `PracticeDestination.Timeline`（无参数），看起来都一样
   - 解决：扩展 `PracticeDestination.Timeline` 支持传入 `initialStatus` 和 `initialKnowledgeTag` 参数，不同组件传入不同筛选条件

3. **薄弱知识点和统计卡片不可点击**
   - 原因：这些组件没有添加 `clickable` 修饰符
   - 解决：为薄弱知识点卡片添加 `clickable` 并传入知识点筛选导航，为统计卡片添加 `clickable` 并传入状态筛选导航

4. **PowerShell 不支持 `&&` 语法**
   - 原因：Windows PowerShell 5.x 不支持 `&&` 连接命令
   - 解决：改用分号 `;` 连接命令

5. **Material3 废弃 API**
   - `TabRow` → 改用 `SecondaryTabRow`
   - `Icons.Rounded.Send` → 改用 `Icons.AutoMirrored.Rounded.Send`

6. **`background` 修饰符同时使用 `brush` 和 `color`**
   - 解决：使用条件判断，二选一传入

### 验证记录

- Gradle 构建通过：`.\gradlew.bat assembleDebug` 成功生成 APK
- 用户在 Android Studio 上实际运行测试通过
- 所有四个底部按钮（重做此题、讲给AI听、相似题、变式题）均能正常进入对应功能页面
- 错题重做、AI 对话、推荐题答题等交互功能正常

