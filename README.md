# BlueTutor 🦉

<div align="center">

**基于大模型的启发式 AI 数学学习助手**  
面向小学三年级至初中阶段，帮助学生从“看答案”走向“会思考”。

<br/>

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Backend](https://img.shields.io/badge/Backend-FastAPI-009688?style=flat-square&logo=fastapi&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin%20%7C%20Python-blue?style=flat-square)
![Model](https://img.shields.io/badge/Model-vivo%20蓝心大模型-2F80ED?style=flat-square)
![AI](https://img.shields.io/badge/AI-LLM%20Agent-7B61FF?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

</div>

---

## 📌 项目简介

**BlueTutor** 是一款面向小学三年级至初中阶段学生的 AI 引导式数学学习助手。项目依托 **vivo 蓝心大模型、通义千问** 的理解、生成与多轮对话能力，聚焦学生在数学学习中常见的“听得懂解析，但自己不会做”“会看答案，但不会迁移”等问题，尝试将 AI 从“答案生成器”转变为“学习过程中的认知支架”。

与传统拍照搜题工具不同，BlueTutor 不以“快速给出答案”为核心，而是通过 **课前预习、启发式追问、动态可视化、错题迁移与个性化学习画像**，帮助学生逐步建立自己的解题思路。

> **AI 不替学生思考，而是帮助学生把思考走下去。**

---

## 🎯 项目背景

在小学高年级到初中阶段，学生的数学学习逐渐从简单模仿走向抽象理解，很多学生会出现典型的“思维断层”：课堂上听得懂，解析也能看懂，但一到自己独立做题就不知道从哪里下手。

当前很多学习工具更擅长“给答案”和“给解析”，但学生真正缺少的往往不是答案本身，而是：

- 不知道题目条件之间有什么关系；
- 不知道第一步该从哪里想；
- 看完解析觉得懂了，换一道题又不会；
- 遇到抽象数量关系时，难以形成直观图像；
- 错题做完就过去了，没有形成持续巩固和迁移。

BlueTutor 希望通过 vivo 蓝心大模型的自然语言理解与交互生成能力，让 AI 像一位耐心的学习伙伴一样，在学生预习、解题、订正和复习的过程中持续提供启发式支持。

---

## ✨ 核心功能

### 1. 启思预学：课前认知搭桥

学生可以上传教材、讲义或题目资料，也可以选择系统推荐的知识点进行快速预习。系统会自动提取重点内容，并以更容易理解的方式进行解释，帮助学生在正式学习或做题前先建立基本知识框架。

在阅读预习材料时，学生可以选中不理解的语句或知识点，直接向 AI 发起提问。AI 会围绕当前内容进行针对性讲解，而不是脱离材料泛泛回答。

同时，系统会感知学生的学习状态，例如长时间停留、反复切换、明显卡顿或表现出困惑时，主动弹出提示并提供帮助，让预习从“自己看一遍”变成一个有引导、有反馈的学习过程。

### 2. 追问导学：启发式解题

学生可以通过拍照、相册上传或手动输入题目进入解题流程。系统识别题目后，会创建解题 Session，并根据学生的回答持续追踪思路状态。

BlueTutor 不会直接输出完整答案，而是通过层层递进的问题帮助学生自己推进：

- 题目告诉了我们哪些条件？
- 真正要求的是什么？
- 当前这一步应该关注哪个数量关系？
- 有没有忽略“先出发”“剩余距离”“速度差”等关键信息？
- 这一步能不能先画成图再理解？

这种方式让学生不只是“知道怎么算”，而是理解“为什么这样算”。

### 3. 动态可视化：把抽象关系变直观

对于路程问题、几何问题、数量关系问题等内容，BlueTutor 可以生成辅助图示，帮助学生把抽象关系看清楚。

例如在追及问题中，系统可以把“先走距离”“速度差”“追上过程”转化为路线图，让学生理解：后出发者真正要追上的不是全程，而是先出发者提前走出的距离。

### 4. 讲练迁移：错题巩固与变式训练

当一次引导解题完成后，系统会将学习过程同步到错题模块，生成结构化学习记录。学生可以在错题本中查看自己的错误类型、知识薄弱点和思维卡点。

系统还可以围绕错题生成相似题和变式题，引导学生重新作答。更进一步，BlueTutor 设计了“讲给 AI 听”的训练方式：让学生像老师一样把解题思路讲给 AI，AI 再根据学生讲解情况继续追问，帮助学生真正把方法说清楚、讲明白。

### 5. 学习罗盘：个性化成长支持

系统会沉淀学生在预习、解题、错题和讲题过程中的行为数据，逐步形成个人学习画像，用于记录：

- 常见错误类型；
- 薄弱知识点；
- 提问习惯；
- 思维卡点；
- 学习节奏与掌握情况。

后续推荐练习、提示深度和讲解粒度都可以基于画像进行调整，让学习支持更个性化。

---

## 🧩 Demo 场景示例

当前演示重点选取小学数学中常见的 **行程问题 / 追及问题** 作为代表场景，用一个真实生活化题目串联 BlueTutor 的完整学习链路。

示例题目：

> 周末上午，小明从家出发去图书馆参加阅读活动。小明每分钟走 50 米。  
> 小明出发 4 分钟后，妈妈发现他把借书证落在家里，便骑车从家出发，沿同一条路追小明。妈妈每分钟骑 150 米。  
> 请问：妈妈出发后几分钟能追上小明？

BlueTutor 的引导重点不是直接告诉学生答案，而是帮助学生理解：

1. 小明已经提前走了多远；
2. 妈妈每分钟比小明多走多少；
3. 为什么追及问题关注的是“速度差”，而不是“速度和”；
4. 如何把这类思路迁移到相遇问题、变式追及问题等题型中。

这一场景能够同时展示 **题目识别、启发式追问、动态可视化、错题沉淀与变式迁移** 等核心能力。

---

## 🏗️ 系统架构

BlueTutor 采用 **Android 前端 + FastAPI 后端 + vivo 蓝心大模型 + 多 Agent 业务模块** 的整体架构。系统将 OCR 识题、预习指导、引导解题、错题迁移等能力拆分为独立模块，各模块之间通过结构化数据进行衔接。

```text
BlueTutor
├── Android 前端
│   ├── Jetpack Compose 页面渲染
│   ├── 拍照 / 相册 / 手动输入题目
│   ├── SSE 流式对话展示
│   └── 学习状态感知与页面状态管理
│
├── FastAPI 后端
│   ├── Shared：OCR 识题与文档解析
│   ├── Preview：预习知识点提取与预习问答
│   ├── Guide：启发式追问、思维树、解题报告、可视化
│   └── Mistakes：错题入库、重做、推荐题、讲给 AI 听
│
└── 大模型能力层
    ├── vivo 蓝心大模型
    ├── OpenAI-compatible API
    ├── LangGraph / LangChain 编排
    └── 结构化 Session 与学习报告
```

---

## 🛠️ 技术栈

| 层级 | 技术 / 工具 | 说明 |
| --- | --- | --- |
| Android 前端 | Kotlin, Jetpack Compose | 移动端界面与交互实现 |
| 后端服务 | Python, FastAPI, Uvicorn | API 服务与业务编排 |
| Agent 编排 | LangGraph, LangChain | 引导式对话与多步骤流程控制 |
| 大模型能力 | vivo 蓝心大模型 | 负责理解、追问生成、解释生成与学习反馈 |
| 模型接口 | OpenAI-compatible API | 统一封装模型调用，便于服务接入与扩展 |
| 数据组织 | JSON Session Store | 用于学习会话、报告与错题记录的结构化管理 |
| 通信方式 | RESTful API, SSE | 常规接口请求与流式对话返回 |
| 文档解析 | 阿里云 DocMind / 文档智能服务 | 支持讲义、PDF、图片等学习材料解析 |

---

## 📁 项目结构

```text
BlueTutor/
├── android/                         # Android 前端工程
│   ├── app/
│   ├── gradle/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/                         # FastAPI 后端工程
│   ├── app/
│   │   ├── api/                     # API 路由层
│   │   ├── shared/                  # OCR 与文档解析共享层
│   │   ├── preview/                 # 启思预学模块
│   │   ├── guide/                   # 追问导学模块
│   │   ├── mistakes/                # 错题与讲练迁移模块
│   │   └── main.py                  # FastAPI 入口
│   ├── requirements.txt
│   └── README.md
│
├── docs/                            # 项目文档
│   ├── architecture/                # 架构设计文档
│   └── frontend/                    # 前端相关文档
│
├── README.md
├── LICENSE
└── .gitignore
```

---

## 🚀 运行与配置

### 1. 克隆项目

```bash
git clone https://github.com/yuzhao6chen/BlueTutor.git
cd BlueTutor
```

### 2. 启动后端

```bash
cd backend

# 推荐使用 conda 环境
conda create -n bluetutor python=3.11 -y
conda activate bluetutor

# 安装依赖
pip install -r requirements.txt

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

启动成功后，后端默认运行在：

```text
http://localhost:8000
```

### 3. 配置后端环境变量

在 `backend/` 目录下创建 `.env` 文件，用于配置大模型和文档解析服务。

```env
LLM_BASE_URL=your_vivo_bluemodel_or_openai_compatible_base_url
LLM_API_KEY=your_llm_api_key
LLM_MODEL_NAME=your_model_name
LLM_TIMEOUT_SECONDS=60
LLM_MAX_RETRIES=2

DASHSCOPE_API_KEY=your_dashscope_api_key

ACCESS_KEY_ID=your_aliyun_access_key_id
ACCESS_KEY_SECRET=your_aliyun_access_key_secret
```

> `.env` 文件包含私密 Key，请不要提交到 GitHub。

### 4. 配置 Android 前端后端地址

Android 前端通过 assets 中的 JSON 文件读取后端地址。

实际生效文件：

```text
android/app/src/main/assets/backend_endpoints.json
```

如果本机没有该文件，可以参考模板文件：

```text
android/app/src/main/assets/backend_endpoints.template.json
```

配置示例：

```json
{
  "base_urls": [
    "http://10.0.2.2:8000",
    "http://你的局域网IP:8000"
  ]
}
```

说明：

- Android 模拟器访问本机后端通常使用 `http://10.0.2.2:8000`；
- 真机调试时，需要将地址改为电脑在同一局域网下的 IP；
- `base_urls` 会按顺序尝试连接，排在前面的地址优先级更高。

### 5. 运行 Android 项目

使用 Android Studio 打开 `android/` 目录，等待 Gradle 同步完成后运行 `app` 即可。

---

## 🔌 后端 API 模块概览

| 模块 | 路由前缀 | 说明 |
| --- | --- | --- |
| Preview | `/api/preview/*` | 预习知识点提取、预习对话、流式问答 |
| Shared | `/api/shared/*` | OCR 识题、文档解析等共享能力 |
| Guide | `/api/guide/*` | 引导解题 Session、思维树、题解、报告、可视化 |
| Mistakes | `/api/mistakes/*` | 错题报告、重做练习、推荐题、讲给 AI 听 |

---

## 🌟 项目亮点

### 1. 从“答案型 AI”转向“引导型 AI”

BlueTutor 不直接把完整答案给学生，而是通过问题链帮助学生逐步推导，让学生在回答、修正和反思中形成自己的解题思路。

### 2. 基于 vivo 蓝心大模型的学习交互

项目结合 vivo 蓝心大模型的语言理解与生成能力，将多轮追问、知识解释、错因分析和变式练习生成融入学生真实学习流程中，使大模型能力服务于“引导理解”而不是“替代作答”。

### 3. 系统级学习状态感知

系统不仅等待学生提问，也会关注学生在预习或解题过程中的停留、切换和卡顿状态，在合适时机主动提供帮助。

### 4. 抽象题型可视化拆解

通过路线图、数量关系图等方式，把难以理解的抽象关系转化为可观察、可跟随的图示过程，降低学生理解门槛。

### 5. 错题不是终点，而是迁移起点

错题模块不仅记录结果，还会继续生成讲解、重做、相似题、变式题和“讲给 AI 听”的输出式训练，帮助学生从“做过一道题”走向“掌握一类题”。

### 6. 模块化垂直切片架构

后端按功能模块进行垂直切分，每个模块独立维护自己的 schema、service、agent 和 prompt，便于多人协作、后续扩展和单独调试。

---

## 💡 应用价值

BlueTutor 的价值不只是“让学生更快完成一道题”，而是让学生在预习、解题、订正和复习中逐步形成可迁移的思维方法。

它可以用于：

- 学生课前预习与自主学习；
- 家庭场景下的作业辅导；
- 错题订正与阶段复习；
- 教师或家长了解学生学情；
- AI 赋能个性化学习场景探索。

通过将 vivo 蓝心大模型能力与数学学习场景结合，BlueTutor 尝试探索一种更有边界、更重过程、更关注学生思维成长的教育 AI 产品形态。

---

## 👥 团队基础信息

- 参赛队伍：**vivo50 队**
- 参赛院校：**南开大学**
- 赛事方向：**2026 中国高校计算机大赛 AIGC 创新赛**
- 项目名称：**BlueTutor — 启发式 AI 辅助数学学习助手**

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<div align="center">

**BlueTutor：让 AI 不替代思考，而是点亮思考。**

</div>
