# 📘 BlueTutor AI 教学助手

> **让每一次学习都充满启发的智能教育伙伴**

BlueTutor 是一款基于大语言模型（LLM）的 K12 智能教学辅助系统。它摒弃传统的"直接给答案"模式，采用**苏格拉底式引导教学法**，通过循序渐进的提问和提示，帮助学生自主思考、构建知识体系，实现从"学会"到"会学"的转变。

---

## ✨ 核心特色

### 🎯 教育理念
- **引导而非灌输**：不直接提供答案，而是通过层层递进的提示引导学生思考
- **个性化适配**：根据学生年级、知识掌握程度动态调整讲解策略
- **闭环学习**：预习 → 讲题 → 错题分析 → 针对性练习，形成完整学习闭环

### 🚀 技术亮点
- **多模块协同**：预习感知、引导讲题、错题闭环、用户画像四大模块无缝衔接
- **Prompt 工程优化**：精心设计的系统提示词，确保 AI 行为符合教育规律
- **灵活架构**：模块化设计，支持快速扩展新学科、新题型
- **模拟/真实双模式**：无 API Key 时可运行演示模式，接入 OpenAI 后即刻启用真实能力

---

## 📦 功能模块

| 模块 | 功能描述 | 核心能力 |
|------|----------|----------|
| **📚 预习智能感知** | 课前知识点预热 | 知识点图谱生成、引导式预习内容、难度自适应 |
| **🧮 AI 引导式讲题** | 解题过程启发式教学 | 分步引导、错误诊断、思路点拨、变式训练 |
| **❌ 错题闭环加练** | 薄弱环节精准突破 | 错因分析、同类题推荐、举一反三练习 |
| **👤 个性化用户画像** | 学情数据可视化 | 知识掌握度分析、学习轨迹追踪、能力雷达图 |

---

## 🚀 快速开始

### 环境要求
- Python 3.9+
- pip 包管理器

### 安装步骤

```bash
# 1. 进入后端目录
cd backend

# 2. 创建虚拟环境（可选但推荐）
python -m venv venv
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate     # Windows

# 3. 安装依赖
pip install -r requirements.txt

# 4. 配置环境变量（可选）
cp .env.example .env
# 编辑 .env 文件，填入你的 OpenAI API Key
```

### 启动服务

```bash
# 开发模式（支持热重载）
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# 生产模式
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 访问文档

服务启动后，打开浏览器访问：
- **API 文档**: http://localhost:8000/docs
- **备用文档**: http://localhost:8000/redoc

---

## 📖 API 使用示例

### 1. 获取预习知识点

```bash
curl http://localhost:8000/api/v1/preview/knowledge-points?grade=5&subject=math
```

### 2. 开始引导式讲题

```bash
curl -X POST http://localhost:8000/api/v1/guide/start \
  -H "Content-Type: application/json" \
  -d '{
    "problem_id": "distance_001",
    "student_grade": 5,
    "hint_level": 1
  }'
```

### 3. 提交答案并获取反馈

```bash
curl -X POST http://localhost:8000/api/v1/guide/submit \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "sess_123",
    "answer": "60km/h",
    "thinking_process": "我用路程除以时间..."
  }'
```

### 4. 添加错题并分析

```bash
# 添加错题
curl -X POST http://localhost:8000/api/v1/mistakes/add \
  -H "Content-Type: application/json" \
  -d '{
    "problem_id": "distance_001",
    "wrong_answer": "30km/h",
    "error_type": "conceptual"
  }'

# 获取错因分析
curl http://localhost:8000/api/v1/mistakes/{mistake_id}/analyze
```

> 💡 更多详细接口文档请访问 `/docs` 页面，支持在线调试和参数探索。

---

## ⚙️ 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `OPENAI_API_KEY` | OpenAI API 密钥 | 无（使用模拟模式） |
| `OPENAI_BASE_URL` | API 基础地址 | `https://api.openai.com/v1` |
| `OPENAI_MODEL` | 使用的模型 | `gpt-4o-mini` |
| `LOG_LEVEL` | 日志级别 | `INFO` |

### 模拟模式 vs 真实模式

- **模拟模式**：未配置 `OPENAI_API_KEY` 时自动启用，返回预设的演示数据，适合快速体验和开发测试。
- **真实模式**：配置有效的 API Key 后，系统将调用真实的 LLM 进行智能分析和生成。

---

## 🏗️ 项目结构

```
backend/
├── requirements.txt          # Python 依赖
├── .env.example              # 环境变量示例
└── app/
    ├── main.py               # FastAPI 应用入口
    ├── shared/
    │   ├── config.py         # 配置管理
    │   └── utils.py          # 通用工具函数
    ├── preview/              # 预习模块
    │   ├── schema.py         # 数据模型定义
    │   ├── prompt.py         # Prompt 模板
    │   ├── agent.py          # LLM 调用封装
    │   └── service.py        # 业务逻辑层
    ├── guide/                # 讲题模块（结构同上）
    ├── mistakes/             # 错题模块（结构同上）
    └── api/                  # API 路由层
        ├── preview_api.py
        ├── guide_api.py
        ├── mistakes_api.py
        └── shared_api.py
```

---

## 🧪 测试与开发

### 运行测试

```bash
# 安装测试依赖
pip install pytest pytest-asyncio httpx

# 运行单元测试
pytest tests/ -v

# 运行带覆盖率的测试
pytest tests/ --cov=app --cov-report=html
```

### 代码规范

项目遵循 PEP 8 规范，推荐使用以下工具：

```bash
# 代码格式化
black app/

# 代码检查
flake8 app/

# 类型检查
mypy app/
```

---

## 🤝 贡献指南

欢迎贡献代码、提出建议或报告问题！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 📬 联系方式

- 📧 Email: support@bluetutor.ai
- 💬 问题反馈：请通过 GitHub Issues 提交

---

## 🙏 致谢

感谢所有为教育事业贡献力量的人们。希望 BlueTutor 能帮助更多孩子爱上学习，享受思考的乐趣！

**让教育更有温度，让学习更有效率。** 🌟
