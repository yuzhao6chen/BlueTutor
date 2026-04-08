# API 设计文档

## 1. 文档范围

本文档仅记录当前已经实现并可调用的后端接口。

当前已实现模块：

1. Preview 预习指导模块
2. 应用级健康检查接口

未实现的 Guide、Mistakes、Shared 接口暂不写入本文档。

## 2. 通用约定

### 2.1 基础信息

1. 接口风格：RESTful JSON API
2. 编码：UTF-8
3. 服务默认入口：`app.main:app`
4. 响应格式：JSON

### 2.2 成功响应结构

业务接口统一采用如下结构：

```json
{
	"code": 0,
	"message": "success",
	"data": {}
}
```

字段说明：

1. `code`：业务状态码，`0` 表示成功
2. `message`：业务提示信息
3. `data`：具体响应数据

### 2.3 失败响应结构

```json
{
	"code": 4001,
	"message": "content_text is empty",
	"data": null
}
```

字段说明：

1. `code`：业务错误码
2. `message`：错误信息
3. `data`：失败时固定为 `null`

## 3. 应用级接口

### 3.1 根接口

路径：`GET /`

说明：用于确认服务已启动。

成功响应：

```json
{
	"name": "BlueTutor Backend",
	"status": "ok"
}
```

### 3.2 全局健康检查

路径：`GET /health`

说明：用于服务存活检测。

成功响应：

```json
{
	"status": "ok"
}
```

## 4. Preview 模块接口

### 4.1 模块健康检查

路径：`GET /api/preview/health`

说明：用于确认 Preview 模块路由已注册并可访问。

成功响应：

```json
{
	"code": 0,
	"message": "success",
	"data": {
		"status": "ok",
		"module": "preview"
	}
}
```

### 4.2 知识点分析接口

路径：`POST /api/preview/knowledge-points`

说明：

1. 接收用户在 PDF/教材/练习册中划选或 OCR 得到的文本
2. 分析当前页面可能涉及的知识点
3. 返回知识点列表供前端展示和选择

请求体：

```json
{
	"user_id": "u001",
	"content_text": "本节内容介绍速度、时间和路程三者之间的关系，并通过例子说明速度公式的意义。",
	"source_type": "manual_input",
	"page_hint": 12,
	"session_id": "preview_s001"
}
```

字段说明：

1. `user_id`：用户标识
2. `content_text`：待分析文本
3. `source_type`：文本来源，当前支持 `pdf_selection`、`screenshot_ocr`、`manual_input`
4. `page_hint`：可选页码提示
5. `session_id`：可选预习会话标识

成功响应：

```json
{
	"code": 0,
	"message": "success",
	"data": {
		"knowledge_points": [
			{
				"id": "kp_001",
				"title": "速度、时间与路程的关系",
				"description": "理解三者之间的基本数量关系，掌握已知其中两个量求第三个量的方法。",
				"confidence": 0.95
			},
			{
				"id": "kp_002",
				"title": "速度公式及其意义",
				"description": "掌握速度公式，理解其表示单位时间内通过的路程。",
				"confidence": 0.95
			}
		],
		"summary": "本节主要讲解速度、时间与路程三者之间的基本关系，并结合实例阐释速度公式的意义。"
	}
}
```

失败响应示例：

```json
{
	"code": 4001,
	"message": "content_text is empty",
	"data": null
}
```

当前已使用错误码：

1. `4001`：知识点分析请求参数错误
2. `5001`：知识点分析阶段模型调用或结果解析失败

### 4.3 预习对话接口

路径：`POST /api/preview/chat`

说明：

1. 基于当前页面内容和所选知识点进行预习型讲解
2. 返回简洁解释与推荐追问问题

请求体：

```json
{
	"user_id": "u001",
	"text": "什么是速度公式？",
	"context_text": "本节内容介绍速度、时间和路程三者之间的关系，并通过例子说明速度公式的意义。",
	"selected_knowledge_points": [
		"速度、时间与路程的关系",
		"速度公式及其意义"
	],
	"session_id": "preview_s001",
	"history": []
}
```

字段说明：

1. `user_id`：用户标识
2. `text`：用户当前问题
3. `context_text`：当前页面或划选内容
4. `selected_knowledge_points`：前端已选知识点标题列表
5. `session_id`：可选会话标识
6. `history`：可选历史对话数组

成功响应：

```json
{
	"code": 0,
	"message": "success",
	"data": {
		"reply": "速度公式其实就是用来衡量物体运动有多快的数学表达，基本形式是：速度 = 路程 ÷ 时间。",
		"follow_up_questions": [
			"如果知道速度和时间，该怎么求路程呢？",
			"生活中还有哪些地方会用到速度公式？",
			"米/分和千米/时这两种速度单位有什么不同？"
		]
	}
}
```

失败响应示例：

```json
{
	"code": 4002,
	"message": "text is empty",
	"data": null
}
```

当前已使用错误码：

1. `4002`：预习对话请求参数错误
2. `5002`：预习对话阶段模型调用或结果解析失败

## 5. 当前实现说明

### 5.1 已完成能力

1. Preview 模块数据模型定义
2. Prompt 模板与固定 JSON 输出协议
3. 阿里云百炼模型调用封装
4. 业务编排与本地日志记录
5. Preview 路由实现
6. FastAPI 应用入口与路由注册

### 5.2 已验证内容

已完成真实联调验证的接口：

1. `GET /health`
2. `POST /api/preview/knowledge-points`
3. `POST /api/preview/chat`

验证方式：

1. 本地启动 FastAPI 服务
2. 使用真实阿里云百炼配置发起请求
3. 接口返回 200 且 `code = 0`
4. 本地日志文件正确记录中文结果
