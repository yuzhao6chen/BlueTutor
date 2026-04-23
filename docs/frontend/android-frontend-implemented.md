# Android 前端已实现内容说明

## 1. 文档目的

本文档用于说明 `android/` 目录下当前已经完成的前端内容，重点记录已经落地的一级界面、导航结构、页面组成和当前实现边界。

本文档基于当前代码阅读结果整理，不包含后续计划中的功能假设。

## 2. 技术实现概览

当前 Android 前端采用以下技术栈：

1. Kotlin
2. Jetpack Compose
3. Material 3
4. Navigation Compose

对应入口与配置代码主要位于：

1. `android/app/src/main/java/com/bluetutor/android/MainActivity.kt`
2. `android/app/src/main/java/com/bluetutor/android/app/BluetutorApp.kt`
3. `android/app/src/main/java/com/bluetutor/android/navigation/MainScaffold.kt`
4. `android/app/src/main/java/com/bluetutor/android/navigation/BluetutorNavHost.kt`
5. `android/app/build.gradle.kts`

## 3. 已实现的整体应用壳

当前已经实现完整的应用主壳，包含：

1. App 入口 `MainActivity`
2. Compose 应用根组件 `BluetutorApp`
3. 底部四栏主导航 `MainScaffold`
4. 四个一级页面的导航注册 `BluetutorNavHost`

四个一级界面分别是：

1. 预习
2. 引导解题
3. 错题练习
4. 我的

对应导航定义位于：

1. `android/app/src/main/java/com/bluetutor/android/navigation/MainTab.kt`
2. `android/app/src/main/java/com/bluetutor/android/navigation/BluetutorDestination.kt`

## 4. 四个一级界面的已实现内容

### 4.1 预习界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/preview/PreviewRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/preview/PreviewUiState.kt`
3. `android/app/src/main/java/com/bluetutor/android/feature/preview/component/*`

当前已实现内容：

1. 顶部 Hero 区块，展示用户昵称、连续学习天数、本周预习进度和吉祥物插画。
2. “快捷预习”横向知识点选择区，当前包含图形面积、分数运算、行程问题、数据统计、方程等 mock 主题。
3. 每个快捷预习主题都带有前端预置的文章式讲义内容，进入后可查看完整讲义页。
4. 讲义页和 AI 对话页已经拆分成两个二级页面，并会在进入时隐藏底部栏。
5. 快捷预习会调用后端 preview 接口，真实获取知识点摘要与知识点列表。
6. AI 对话已经接入后端流式接口，支持边生成边展示 BlueTutor 回复，并在结束时补齐追问建议。
7. 预习会话、知识点选择和摘要结果已经支持本地缓存，重新进入同一专题时可恢复历史状态。
8. “上传讲义”卡片仍保留三种视觉状态：Idle、Processing、Success。
9. 上传交互目前仍是本地模拟逻辑：点击后会把文件名设置为固定 PDF，并经过短暂延迟切换到成功状态。
10. 两个快捷入口卡片，如“拍照提问”“选知识点”。
11. “今日推荐”轮播区域会从快捷预习主题中抽取内容呈现，点击后可进入同一讲义。
12. 推荐课程轮播指示器和循环分页效果。

当前实现特点：

1. 预习首页、讲义页、AI 对话页已经形成完整的前端链路。
2. 快捷预习的讲义正文目前由前端预置内容生成，结构统一为文章式自学资料。
3. 预习首页仍然保留一部分本地 UI 状态，例如上传卡片状态和首页轮播信息。
4. 快捷预习与 AI 对话已经接入真实后端接口，其中 AI 对话采用流式输出。
5. 预习模块已经具备本地缓存恢复能力，而不是单纯一次性演示页面。

当前未实现内容：

1. 真正的 PDF 阅读器。
2. 文本划选或截图后的内容传输。
3. 真实文件上传。
4. 基于真实教材或 PDF 内容动态生成讲义正文。
5. 更完整的上传讲义到讲义页 / AI 对话页的自动联动。

### 4.2 引导解题界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/solve/SolveRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/solve/component/SolveTeacherIllustration.kt`

当前已实现内容：

1. 顶部大幅 Hero 视觉区，突出“把题目发给我，我带你一步步把它搞定”的引导解题定位。
2. 三个核心入口卡片：拍照上传、从相册导入、手动输入题目。
3. “体验分步引导”卡片区，当前展示追及问题、鸡兔同笼两个方向。
4. “小提示”信息区，说明拍题清晰度、引导式解题、相似题推荐等体验要点。
5. 页面整体视觉风格和插画已经完成，适合做一级入口展示。

当前实现特点：

1. 页面重点在于明确用户进入解题流程的入口。
2. solve 页面数据使用 `solveRouteMockUiState()` 构造。
3. 追及问题、鸡兔同笼已经在页面文本层面突出，符合当前 demo 聚焦题型。

当前未实现内容：

1. 拍照、相册、手动输入的真实功能。
2. OCR 识别。
3. 题目上传到后端后的引导解题流程。
4. 分步可视化讲解的真实交互。

### 4.3 错题练习界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/practice/PracticeRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/practice/PracticeUiState.kt`
3. `android/app/src/main/java/com/bluetutor/android/feature/practice/component/*`

当前已实现内容：

1. 首页统计区，展示今日错题数量、待巩固题数、本周完成量、已突破题型等信息。
2. “AI 错题巩固”主功能卡片，突出错题回顾和提分场景。
3. “薄弱知识点”横向标签区，当前包含行程应用题、几何图形、数量关系、分数运算等 mock 数据。
4. “为你推荐”轮播区，展示相似题和变式题推荐。
5. 底部操作区域和多个统计/推荐组件的视觉实现。

当前实现特点：

1. 页面已经具备完整的信息架构和视觉层级。
2. 推荐练习卡片支持轮播。
3. 页面数据来自 `practiceMockUiState()`，当前属于静态演示数据。

当前未实现内容：

1. 错题数据的真实来源。
2. 与后端 mistakes 模块的联动。
3. 生成同类练习题后的答题交互。
4. 错题重做与结果回写。

### 4.4 我的界面

对应文件：

1. `android/app/src/main/java/com/bluetutor/android/feature/profile/ProfileRoute.kt`
2. `android/app/src/main/java/com/bluetutor/android/feature/profile/ProfileUiState.kt`
3. `android/app/src/main/java/com/bluetutor/android/feature/profile/component/*`

当前已实现内容：

1. 用户头部主卡片，展示昵称、年级、连续打卡天数和右上角设置入口。
2. 周学习打卡组件，支持直接在主卡片内完成今日打卡。
3. 打卡交互带有轻量庆祝动画和文案状态切换，适合作为本地养成入口。
4. 四项概览统计卡片，如完成题目、连续学习、引导完成、已突破题型。
5. “我的能力画像”区域，包含多个能力条和优势/待加强提示卡。
6. “最近学习”列表。
7. 右上角齿轮按钮可进入设置二级界面，设置页采用与预习讲义页接近的窄顶栏样式，并隐藏底部栏。
8. 设置二级界面已支持修改昵称、上传本地头像、恢复默认头像、切换年级、偏好开关、本地资料重置。
9. “帮助与反馈”已经拆成可继续进入的三级页面，三级页同样使用窄顶栏和无底栏布局。

当前实现特点：

1. 页面已经从纯 mock 展示层，升级为“静态画像 + 本地资料状态”的混合实现。
2. “我的能力画像”和四个统计方块目前仍保持静态展示，不跟本地设置联动。
3. 昵称、头像、打卡记录、年级和偏好开关都只保存在本地缓存中。
4. 头像上传使用本地图片选择，并复制到应用私有目录中保存；上传后会以更饱满的圆形头像展示。
5. 设置不再放在主页底部列表，而是收纳进由右上角齿轮进入的二级界面。
6. “今日目标”相关设置和展示已经移除，帮助与反馈改为三级页承载。

当前未实现内容：

1. 真实用户登录态。
2. 用户画像与学习记录的后端同步。
3. 多用户隔离。
4. 基于真实学习行为自动完成打卡。
5. 头像裁剪、云同步或跨设备恢复。

## 5. 已实现的公共设计系统能力

除了四个一级界面外，当前前端还已经沉淀出一批公共组件和主题资源，说明前端不只是做了静态页面，而是已经开始形成可复用的 UI 基础层。

主要位置：

1. `android/app/src/main/java/com/bluetutor/android/core/designsystem/component/*`
2. `android/app/src/main/java/com/bluetutor/android/ui/theme/*`

当前已具备的公共能力包括：

1. 按钮组件
2. 标签 Chip
3. 渐变卡片
4. Hero 卡片
5. 进度条
6. 统计卡片
7. 上传状态卡片
8. 间距、颜色、渐变、排版等主题配置

这意味着当前前端已经不是简单的单文件页面堆叠，而是具备继续扩展真实业务页面的基础。

## 6. 当前实现边界总结

从代码实际情况看，Android 前端当前已经完成的是：

1. 完整的四 tab 一级界面结构。
2. 四个一级页面的高保真视觉实现。
3. 基于 Compose 的导航壳与设计系统。
4. Preview 模块的快捷预习、讲义阅读、AI 流式对话和本地缓存恢复链路。
5. Profile 模块的本地资料管理、头像上传、主卡片打卡与设置二级页。

尚未完成的是：

1. Solve 与 Practice 模块和后端的真实联动。
2. OCR、拍照、相册、PDF 阅读等更完整的设备能力接入。
3. 上传讲义后的真实解析链路，以及从文档内容自动生成讲义与问答上下文。
4. 用户行为采集、通知提醒、悬浮窗和更细粒度的本地规则。
5. 用户体系、云同步和多端一致性。

## 7. 结论

当前 Android 前端已经实现了“主应用壳 + 四个一级界面 + 公共设计系统”的第一阶段成果，适合用于展示整体产品形态和一级导航结构。

如果后续继续开发，最自然的下一步是：

1. 将 Solve 页接入拍照 / OCR / 引导解题后端链路。
2. 将 Practice 页接入错题分析与推荐题接口。
3. 把上传讲义从本地占位交互升级成真实文档解析流程。
4. 再决定 Profile 页是继续走本地单用户模式，还是接入 shared / 用户体系。