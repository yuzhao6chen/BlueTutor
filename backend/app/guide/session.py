from .phase1_parser.parser import parse_problem
from .phase2_dialogue.dialogue_graph_state import DialogueGraphState
from .phase2_dialogue.dialogue_loop import create_dialogue_graph, create_pre_graph
from .phase2_dialogue.report_generator import generate_report
from .phase2_dialogue.session_state import SessionState

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
            self._pre_graph = create_pre_graph()
            logger.info("会话初始化完成，题目：%s", problem_text[:30])
        except Exception as e:
            raise TutorSessionError(f"会话初始化失败：{e}") from e

    def run_one_turn(self, student_input: str) -> str:
        """
        执行对话循环的一轮。

        Args:
            student_input: 学生的输入文本

        Returns:
            tuple: (老师的引导问题文本, 题目是否已解决)


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

            return final_question, self._state.is_solved
        except Exception as e:
            raise TutorSessionError(f"对话执行失败：{e}") from e

    def run_one_turn_stream(self, student_input: str):
        """
        执行对话循环的一轮（流式版本）。

        流程：
        1. 同步执行前段子图（State Tracker → Situation Analyzer）
        2. 在图外循环执行 Question Generator（流式）+ Guardrail：
           - Guardrail 通过 → yield 所有 Token，结束
           - Guardrail 打回 → yield 一个 retry 事件标记，重新生成
        3. 更新 self._state

        Yields:
            str: 老师回复的每个 Token，或特殊标记 "__RETRY__" 表示打回重试

        Raises:
            TutorSessionError: LLM 调用或图执行失败时抛出
        """
        from .phase2_dialogue.question_generator import stream_question_generator
        from .phase2_dialogue.guardrail import run_guardrail, should_retry, MAX_RETRY, FALLBACK_QUESTION
        from .phase2_dialogue.dialogue_graph_state import DialogueGraphState

        try:
            logger.info("开始执行第 %d 轮对话（流式）", len(self._state.dialogue_history) // 2 + 1)

            # ── 第一段：同步执行前段子图 ──────────────────────────────
            graph_state = DialogueGraphState(
                session_state=self._state,
                student_input=student_input,
                generated_question="",
                rejection_reason=[],
                retry_count=0,
                teaching_guidance=""
            )
            pre_result = self._pre_graph.invoke(graph_state)

            # ── 第二段：流式生成 + Guardrail 循环 ────────────────────
            rejection_reason = []
            retry_count = 0
            final_question = ""

            while True:
                # 将当前 rejection_reason 注入 graph_state，供 Question Generator 参考
                pre_result["rejection_reason"] = rejection_reason
                pre_result["retry_count"] = retry_count

                # 流式生成，同时拼接完整文字
                tokens = []
                for token in stream_question_generator(pre_result):
                    tokens.append(token)

                full_text = "".join(tokens).strip()

                # 触发兜底：超过最大重试次数
                if retry_count >= MAX_RETRY:
                    full_text = FALLBACK_QUESTION
                    final_question = full_text
                    # 推送兜底文字（逐字符 yield，保持流式体验）
                    for ch in full_text:
                        yield ch
                    if self._state.is_solved:
                        yield "__SOLVED__"
                    break

                # Guardrail 检查
                pre_result["generated_question"] = full_text
                guardrail_result = run_guardrail(pre_result)

                if should_retry(guardrail_result) == "end":
                    # 通过审核，正式推送所有 Token
                    final_question = full_text
                    for token in tokens:
                        yield token
                    if self._state.is_solved:
                        yield "__SOLVED__"
                    break
                else:
                    # 打回：通知前端清空，准备重试
                    yield "__RETRY__"
                    rejection_reason = guardrail_result["rejection_reason"]
                    retry_count = guardrail_result["retry_count"]
                    logger.info("Guardrail 打回（第 %d 次），准备重试", retry_count)

            # ── 更新会话状态 ──────────────────────────────────────────
            guardrail_result["session_state"].dialogue_history.append(
                {"role": "tutor", "content": final_question}
            )
            self._state = guardrail_result["session_state"]

            logger.info("本轮对话完成（流式），老师问题：%s", final_question[:50])

        except Exception as e:
            raise TutorSessionError(f"对话执行失败（流式）：{e}") from e

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

    def generate_solution(self) -> str:
        """
        生成本次讲题的个性化题解（带缓存）。

        若题解已生成，直接返回缓存内容，不再调用 LLM。
        生成完成后立即持久化到 SessionState。

        Returns:
            题解 Markdown 文本

        Raises:
            TutorSessionError: 题解生成失败时抛出
        """
        # 缓存命中，直接返回
        if self._state.solution is not None:
            logger.info("题解已存在，返回缓存内容")
            return self._state.solution

        try:
            from .phase2_dialogue.solution_generator import generate_solution as _generate
            logger.info("开始生成题解")
            solution_text = _generate(self._state)
            # 持久化到 SessionState
            self._state.solution = solution_text
            return solution_text
        except Exception as e:
            raise TutorSessionError(f"题解生成失败：{e}") from e

    def stream_solution(self):
        """
        生成本次讲题的个性化题解（流式版本，带缓存）。

        若题解已生成，直接一次性 yield 缓存内容。
        生成完成后将完整文本持久化到 SessionState。

        Yields:
            str: 题解文本的 Token 片段

        Raises:
            TutorSessionError: 题解生成失败时抛出
        """
        # 缓存命中，一次性返回
        if self._state.solution is not None:
            logger.info("题解已存在，从缓存返回")
            yield self._state.solution
            return

        try:
            from .phase2_dialogue.solution_generator import stream_solution_generator
            logger.info("开始生成题解（流式）")
            chunks = []
            for chunk in stream_solution_generator(self._state):
                chunks.append(chunk)
                yield chunk
            # 所有 Token 生成完毕，持久化完整内容
            self._state.solution = "".join(chunks)
        except Exception as e:
            raise TutorSessionError(f"题解生成失败（流式）：{e}") from e

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
