# 苏格拉底讲题模块 (Socratic Tutor Module)

## 项目简介
基于多智能体架构的小升初数学讲题系统，通过启发式提问引导学生思考，而非直接给出答案。

## 开发环境
- Python 3.12
- Conda 虚拟环境：`aigc_blue_tutor_env`
- 大模型：通义千问 qwen-plus

## 已完成阶段

### 第一阶段：题目解析 (Problem Parsing) ✅
**功能**：接收学生提交的题目，提取核心已知条件、求解目标和标准答案。

**运行方式**：
```bash
python run_phase1.py
