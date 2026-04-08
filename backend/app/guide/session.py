from phase1_parser.parser import parse_problem
from phase2_dialogue.dialogue_graph_state import DialogueGraphState
from phase2_dialogue.dialogue_loop import create_dialogue_graph
from phase2_dialogue.report_generator import generate_report
from phase2_dialogue.session_state import SessionState

import logging

logger = logging.getLogger(__name__)


class TutorSessionError(Exception):
    """苏格拉底讲题模块的业务异常"""
    pass


class SocraticTutorSession:
    """
    苏格拉底讲题模块的对外门面类。
    封装第一阶段（题目解析）和第二阶段（对话循环）的完整会话逻辑。
    每个用户的每次讲题对应一个实例。
    """

    def __init__(self, problem_text: str):
        """
        初始化一次讲题会话：解析题目、构建对话图。

        Args:
            problem_text: 学生提交的原始题目文本

        Raises:
            TutorSessionError: 题目解析失败时抛出
        """
        try:
            parsed = parse_problem(problem_text)
            self._state = SessionState(parsed_problem=parsed, raw_problem=problem_text)
            self._graph = create_dialogue_graph()
            logger.info("会话初始化完成，题目：%s", problem_text[:30])
        except Exception as e:
            raise TutorSessionError(f"会话初始化失败：{e}") from e

    def run_one_turn(self, student_input: str) -> str:
        """
        执行对话循环的一轮。

        Args:
            student_input: 学生的输入文本

        Returns:
            老师的引导问题文本

        Raises:
            TutorSessionError: LLM 调用或图执行失败时抛出
        """
        try:
            logger.info("开始执行第 %d 轮对话", len(self._state.dialogue_history) // 2 + 1)

            graph_state = DialogueGraphState(
                session_state=self._state,
                student_input=student_input,
                generated_question="",
                rejection_reason=[],
                retry_count=0,
                teaching_guidance=""
            )

            result = self._graph.invoke(graph_state)

            final_question = result["generated_question"]
            result["session_state"].dialogue_history.append(
                {"role": "tutor", "content": final_question}
            )
            self._state = result["session_state"]

            logger.info("本轮对话完成，老师问题：%s", final_question[:50])

            return final_question
        except Exception as e:
            raise TutorSessionError(f"对话执行失败：{e}") from e

    def generate_report(self) -> dict:
        """
        生成本次讲题的结构化报告。

        Returns:
            结构化报告字典，可直接 json.dumps

        Raises:
            TutorSessionError: 报告生成失败时抛出
        """
        try:
            logger.info("开始生成讲题报告")

            return generate_report(self._state)
        except Exception as e:
            raise TutorSessionError(f"报告生成失败：{e}") from e

    @property
    def is_finished(self) -> bool:
        """判断对话是否已结束（预留接口，当前始终返回 False）"""
        return False


if __name__ == "__main__":
    import json
    import logging

    # 配置日志：调试模式下输出 DEBUG 级别日志
    logging.basicConfig(
        level=logging.DEBUG,
        format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
    )

    from phase2_dialogue.state_tracker import _serialize_tree


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
