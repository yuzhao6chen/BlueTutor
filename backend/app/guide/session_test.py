"""
苏格拉底讲题模块 - 集成测试
运行方式（在 backend/ 目录下执行）：
    python -m app.guide.session_test
"""
from __future__ import annotations

import json
import logging

# 配置日志：调试模式下输出 DEBUG 级别日志
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)

from app.guide.session import SocraticTutorSession
from app.guide.phase2_dialogue.state_tracker import _serialize_tree


def _print_tree_outline(thinking_tree: dict) -> str:
    """只提取 _serialize_tree 输出中的可读大纲部分"""
    full = _serialize_tree(thinking_tree)
    lines = full.split("\n")
    outline_lines = []
    for line in lines:
        if line.startswith("【JSON结构"):
            break
        outline_lines.append(line)
    return "\n".join(outline_lines).strip()


def main() -> None:
    print("=== 苏格拉底讲题模块 - 集成测试 ===\n")
    problem = input("请输入题目：\n>>> ")

    print("\n正在解析题目...")
    session = SocraticTutorSession(problem)
    print("题目解析完成，开始对话。\n")

    while True:
        student_input = input("学生：")
        if student_input.strip().lower() == "quit":
            print("\n讲题结束，正在生成报告...")
            report = session.generate_report()
            print(json.dumps(report, ensure_ascii=False, indent=2))
            break

        question = session.run_one_turn(student_input)

        state = session._state
        print(f"\n【思维树】\n{_print_tree_outline(state.thinking_tree)}")
        print(f"【Stuck Count】{state.stuck_count}")
        print(f"\n老师：{question}\n")
        print("-" * 50)


if __name__ == "__main__":
    main()
