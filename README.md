# BlueTutor 🧑‍🏫
面向小学三年级至初中阶段的**AI引导式元认知教学助手**，针对学生学习中的「思维断层」问题，打造「预习感知 → 可视化讲题 → 错题闭环加练」的全流程智能学习系统，通过引导式提问帮助学生主动构建知识体系，从根源解决「听得懂、不会做」的学习痛点。

---

## 🎯 项目背景
中小学生在数学学习中普遍存在 **「听得懂课、看得懂题、自己做就错」** 的思维断层问题，核心原因是被动接收知识，缺乏主动思考和元认知能力。

BlueTutor跳出「直接给答案」的传统AI讲题模式，通过**引导式交互**让学生从「被动听题」变为「主动思考」，在解题过程中完成知识内化，真正提升学习能力。

---

## ✨ 核心功能
### 1. 📚 预习智能感知模块
- 针对数学课本知识点，AI自动拆解知识脉络，生成引导式提问
- 帮助学生提前建立知识框架，带着问题听课，大幅提升课堂效率
- 支持知识点可视化拆解，降低抽象概念的理解门槛

### 2. 🧮 AI引导式讲题模块
- 聚焦小学/初中数学核心题型（路程问题、鸡兔同笼等高频考点）
- 拒绝直接输出答案，通过**层层递进的引导式提问**，让学生自主推导解题步骤
- 可视化拆解解题逻辑，让学生理解「为什么这么做」，而非「怎么做」
- 前端页面区分场景，砍掉冗余分类Agent，响应速度提升10倍+

### 3. ❌ 错题闭环加练模块
- 自动分析错题根因（审题错误、公式错误、思维断层等）
- 生成同类针对性练习题，实现「做一道会一类」的巩固效果
- 构建个人错题本，跟踪学习进度，形成完整学习闭环

### 4. 👤 个性化用户画像
- 记录学生学习行为、错题类型、知识点掌握情况
- 生成个性化学习报告，针对性推荐学习内容
- 为教师/家长提供学生学情的可视化数据支撑

---

## 🏗️ 系统架构
采用垂直切片（Feature-Based）的高内聚低耦合架构，每个功能模块独立封装，便于维护、扩展和多人协作：

---

## 运行与配置

### 1.后端 .env 格式

后端在 backend 目录下读取 .env，用于统一管理大模型和文档解析相关配置。推荐至少包含下面这些字段：

```env
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
LLM_API_KEY=your_llm_api_key
LLM_MODEL_NAME=qwen3.6-flash
LLM_TIMEOUT_SECONDS=60
LLM_MAX_RETRIES=2

DASHSCOPE_API_KEY=your_dashscope_api_key

ACCESS_KEY_ID=your_aliyun_access_key_id
ACCESS_KEY_SECRET=your_aliyun_access_key_secret
```

字段说明：

1. LLM_BASE_URL：OpenAI 兼容接口地址。
2. LLM_API_KEY：模型接口 Key。
3. LLM_MODEL_NAME：默认使用的模型名。
4. LLM_TIMEOUT_SECONDS：单次模型请求超时秒数。
5. LLM_MAX_RETRIES：模型输出异常时的最大重试次数。
6. DASHSCOPE_API_KEY：Guide 链路优先读取的 DashScope Key；未配置时会回退到 LLM_API_KEY。
7. ACCESS_KEY_ID：阿里云文档解析服务 Access Key ID。
8. ACCESS_KEY_SECRET：阿里云文档解析服务 Access Key Secret。

### 2.Android 前端后端地址配置文件

Android 前端不再把后端地址分散写死在多个 API Client 中，而是统一从 assets 里的 JSON 文件读取。

实际生效文件：android/app/src/main/assets/backend_endpoints.json

示例模板文件： [android/app/src/main/assets/backend_endpoints.template.json](android/app/src/main/assets/backend_endpoints.template.json)

配置格式如下：

```json
{
	"base_urls": [
		"http://10.0.2.2:8000",
		"http://10.1.2.120:8000"
	]
}
```

说明：

1. base_urls 按顺序尝试，排在前面的地址优先级更高。
2. 切换 WLAN、本地后端或云服务器时，只需要在这个数组里新增或调整 URL 顺序。
3. android/app/src/main/assets/backend_endpoints.json 已加入 [android/.gitignore](android/.gitignore)，适合保留本机私有配置。
4. 如果本机配置文件不存在，应用会回退到模板文件，再回退到代码内置兜底地址。
