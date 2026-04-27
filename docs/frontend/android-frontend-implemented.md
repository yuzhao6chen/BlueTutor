# Android 前端已实现内容说明

## 1. 文档目的

本文档用于记录 `android/` 目录下已经真实落地的前端实现，强调当前代码中已经具备的页面结构、导航层级、联网能力和实现边界。

本文档只描述当前仓库中的已实现状态，不描述尚未编码的设想。

## 2. 技术概览

当前 Android 前端主要基于以下技术栈：

1. Kotlin
2. Jetpack Compose
3. Material 3
4. Navigation Compose
5. OkHttp
6. Kotlinx Serialization / org.json（按模块分别使用）
7. Markwon + HtmlPlugin + JLatexMathPlugin（Markdown / LaTeX 渲染）

关键入口与壳层代码主要位于：

1. `android/app/src/main/java/com/bluetutor/android/MainActivity.kt`
2. `android/app/src/main/java/com/bluetutor/android/app/BluetutorApp.kt`
3. `android/app/src/main/java/com/bluetutor/android/navigation/MainScaffold.kt`
4. `android/app/src/main/java/com/bluetutor/android/navigation/BluetutorNavHost.kt`
5. `android/app/build.gradle.kts`

## 3. 应用主壳与导航

当前已经实现完整的主应用壳，包含：

1. App 入口 `MainActivity`
2. Compose 应用根组件 `BluetutorApp`
3. 底部四栏主导航 `MainScaffold`
4. 四个一级页面的路由注册 `BluetutorNavHost`
5. 各模块进入二级 / 三级页时的底部栏隐藏逻辑

四个一级界面分别是：

1. 预习
2. 引导解题
3. 错题练习
4. 我的

## 4. 各一级界面的当前实现

### 4.1 预习界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/preview/PreviewRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/preview/PreviewUiState.kt`
3. `android/app/src/main/java/com/bluetutor/android/feature/preview/component/*`
4. `android/app/src/main/java/com/bluetutor/android/feature/preview/data/PreviewApiClient.kt`

当前已实现内容：

1. 预习首页 Hero 区块、连续学习信息、本周预习进度和品牌插画。
2. 快捷预习专题入口与“今日推荐”轮播。
3. 讲义页与 AI 对话页两级路由拆分，并在进入时隐藏底部栏。
4. 快捷专题会请求后端 preview 接口，生成知识点摘要和知识点列表。
5. AI 对话已经接入后端流式接口，支持边生成边展示，并在结束后给出追问建议。
6. 预习模块已支持本地缓存，重新进入同一专题时可恢复知识点选择、摘要与对话状态。
7. 上传讲义卡片保留了完整的视觉交互状态机，包括 Idle、Processing、Success。

当前实现边界：

1. 快捷专题正文仍然是前端预置讲义内容，不是由真实上传文档动态生成。
2. 上传讲义交互目前仍然是占位实现，尚未真正调用后端文件上传与解析接口。
3. 预习模块已经具备真实知识点提取与 AI 对话链路，但“真实文档上传 -> 讲义生成 -> 问答上下文联动”尚未在 Android 侧闭合。

### 4.2 引导解题界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/solve/SolveRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/solve/data/GuideApiClient.kt`
3. `android/app/src/main/java/com/bluetutor/android/feature/practice/data/MistakesApiClient.kt`

当前已实现内容：

1. Solve 已从旧的体验页重构为正式分层流程：题目获取页、题目确认页、解题工作台、结果页。
2. 题目获取页支持拍照上传、从相册导入、手动输入三种入口。
3. 拍照与相册入口已经接入真实图片选择、裁切与 OCR 链路，识别结果会回填题目输入区，但不会自动开始引导。
4. 题目确认页作为 1.5 级页面存在，负责让用户确认题干后再创建 Guide 会话。
5. 解题工作台已经接入 Guide 后端 `turns/stream`，支持流式追问 / 引导。
6. 工作台主屏支持 Markdown 渲染，且公式渲染已接入 Markwon + JLatexMath，并带运行时安全降级。
7. 工作台顶部固定题目压缩卡，默认收起；主屏之外左右两页分别用于“思路树”和“线索与进展”。
8. 工作台完成后可通过右上角菜单进入三个三级结果页：报告、题解、可视化。
9. 报告、题解、可视化均已接入后端接口，并对返回格式不一致、长耗时、可视化重试等情况做了前端处理。
10. 最近对话、报告摘要、题解正文等都支持 Markdown 渲染。
11. Solve 完成后会自动把报告同步写入错题本，并在完成弹窗中提示同步结果。
12. Solve 的非首页页面会隐藏底部栏，形成完整的多级任务流。

当前实现边界：

1. 主要通过编译验证和后端契约验证完成联调，真机端的完整全链路回归记录仍需补齐。
2. OCR 与 Guide 已经接通，但图片质量、局域网地址、真机网络等运行环境仍有进一步配置化空间。
3. 结果页视觉和信息层级已经可用，但仍可继续做更细致的动效和阅读体验打磨。

### 4.3 错题练习界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/practice/PracticeRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/practice/PracticeUiState.kt`
3. `android/app/src/main/java/com/bluetutor/android/feature/practice/data/MistakesApiClient.kt`
4. `android/app/src/main/java/com/bluetutor/android/feature/practice/component/*`

当前已实现内容：

1. 错题首页、时间轴、报告详情、重做、推荐、讲给 AI 听等多页面流程已经存在。
2. `PracticeRoute` 已切换到联网版状态机容器，不再只是静态首页展示。
3. `MistakesApiClient` 已接入后端 mistakes 接口族。
4. Solve 完成后的报告可自动写入 Mistakes 模块，形成 Solve -> Mistakes 的初步闭环。
5. 二级页进入后隐藏底部栏，整体交互与 Preview / Profile 保持一致。

当前实现边界：

1. 真机与模拟器下的完整联调回归仍需继续补记录。
2. 环境地址仍然偏向开发态配置，尚未抽象成更稳健的多环境切换方式。

### 4.4 我的界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/profile/ProfileRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/profile/ProfileUiState.kt`
3. `android/app/src/main/java/com/bluetutor/android/feature/profile/component/*`

当前已实现内容：

1. 用户头部主卡片、周学习打卡、四项概览统计与能力画像区域。
2. 本地打卡动画与状态切换。
3. 设置二级页，支持修改昵称、上传头像、恢复默认头像、切换年级、偏好开关、本地资料重置。
4. “帮助与反馈”已经下沉到三级页面。

当前实现边界：

1. 目前仍是本地单用户资料模型，未接真实账号体系。
2. 学习画像与统计大多为静态或本地态展示，未与后端行为数据联动。

## 5. 公共设计系统与基础能力

当前 Android 前端已经沉淀出一批公共组件和主题资源，主要位于：

1. `android/app/src/main/java/com/bluetutor/android/core/designsystem/component/*`
2. `android/app/src/main/java/com/bluetutor/android/ui/theme/*`

已经具备的基础能力包括：

1. Hero 卡片、渐变卡片、统计卡片、按钮、标签 Chip 等通用组件。
2. 颜色、排版、间距、阴影和渐变等统一主题配置。
3. 上传状态卡片与多种任务流中的状态提示组件。
4. 多级页面统一的窄顶栏 / 无底栏布局模式。

## 6. 当前整体边界总结

从当前代码看，Android 前端已经完成的是：

1. 完整的四 tab 一级导航壳。
2. Preview 的快捷预习、知识点提取、AI 流式对话与本地缓存链路。
3. Solve 的拍照 / 相册 / OCR / 题目确认 / Guide 流式工作台 / 结果页 / Mistakes 同步链路。
4. Mistakes 的联网版多页面容器与 Solve 侧联通。
5. Profile 的本地资料与设置能力。

尚未完全完成的是：

1. Preview 的真实文档上传、文档解析结果消费与讲义正文自动生成。
2. 各模块在真机环境下的系统性联调回归与验收文档。
3. 更完整的用户体系、云同步、多设备恢复与环境配置化。

## 7. 结论

当前 Android 前端已经不再是单纯的视觉原型，而是一个具备真实导航结构、后端接线、流式交互和多级任务流的可运行应用壳。

当前最自然的后续方向是：

1. 把 Preview 的上传讲义从占位交互升级成真实文档解析链路。
2. 继续补 Solve / Mistakes 的真机联调与结果验收。
3. 再决定 Profile 和全局配置是否接入真实用户体系。