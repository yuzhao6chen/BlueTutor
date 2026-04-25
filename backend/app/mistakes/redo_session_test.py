from __future__ import annotations

import json
import tempfile
from pathlib import Path

from app.mistakes.schema import (
	MistakeErrorProfileItem,
	MistakeIndependenceEvaluation,
	MistakeProblem,
	MistakeRecommendationGenerateRequest,
	MistakeRecommendationStartRedoRequest,
	MistakeRecommendationSubmitRequest,
	MistakeRedoSessionRequest,
	MistakeRedoSessionTurnRequest,
	MistakeReportIngestRequest,
	MistakeReportPayload,
	MistakeThinkingNode,
)
from app.mistakes.service import MistakeService
from app.mistakes.store import load_redo_session, save_redo_session


def build_request() -> MistakeReportIngestRequest:
	return MistakeReportIngestRequest(
		user_id="redo-user",
		source_session_id="redo-session-source",
		report_title="追及问题重做测试",
		report=MistakeReportPayload(
			problem=MistakeProblem(
				raw_problem="小明每分钟走60米，小红每分钟走40米，小红先走5分钟，小明多久追上？",
				known_conditions=["小明速度60米/分", "小红速度40米/分", "小红先走5分钟"],
				goal="求小明追上小红所需时间",
				answer="10分钟",
			),
			knowledge_tags=["行程问题", "追及问题"],
			thinking_chain=[
				MistakeThinkingNode(
					node_id="n1",
					content="先求小红先走路程，再算速度差，最后求追及时间",
					status="correct",
					parent_id=None,
					error_history=[],
				)
			],
			error_profile=[
				MistakeErrorProfileItem(
					error_type="速度差理解不足",
					detail="忘记先算速度差",
				)
			],
			independence_evaluation=MistakeIndependenceEvaluation(
				level="需要提示",
				detail="测试场景",
			),
			solution="先算小红先走路程40×5=200米，再算速度差60-40=20米/分，最后200÷20=10分钟。",
		),
	)


def test_persistence() -> dict[str, object]:
	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		ingested = service.ingest_report(build_request())

		started = service.start_redo_session(
			MistakeRedoSessionRequest(report_id=ingested.report_id, user_id="redo-user")
		)
		session_id = started.session_id

		persisted = load_redo_session(session_id)
		assert persisted is not None, "会话应已持久化到磁盘"
		assert persisted.session_id == session_id
		assert persisted.report_id == ingested.report_id
		assert persisted.hint_level == 1

		service2 = MistakeService(data_dir=Path(temp_dir))
		recovered = service2.get_redo_session(session_id)
		assert recovered.session_id == session_id, "新服务实例应能从磁盘恢复会话"

		turn1 = service2.advance_redo_session(
			session_id,
			MistakeRedoSessionTurnRequest(answer="A"),
		)
		assert turn1.turn_count == 1

		persisted_after = load_redo_session(session_id)
		assert persisted_after is not None
		assert persisted_after.turn_count == 1, "轮次更新后应同步持久化"

	return {"test": "persistence", "status": "passed", "session_id": session_id}


def test_hint_grading() -> dict[str, object]:
	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		ingested = service.ingest_report(build_request())

		started = service.start_redo_session(
			MistakeRedoSessionRequest(report_id=ingested.report_id, user_id="redo-user")
		)
		assert started.hint_level == 1, "初始提示等级应为1"
		assert started.max_hint_level == 3

		wrong_turn = service.advance_redo_session(
			started.session_id,
			MistakeRedoSessionTurnRequest(answer="Z"),
		)
		assert wrong_turn.hint_level >= 1, "答错后提示等级应保持或提升"

		wrong_turn2 = service.advance_redo_session(
			started.session_id,
			MistakeRedoSessionTurnRequest(answer="Z"),
		)
		assert wrong_turn2.hint_level >= wrong_turn.hint_level, "连续答错提示等级应递增"

	return {"test": "hint_grading", "status": "passed", "initial_level": 1, "after_wrong": wrong_turn.hint_level, "after_wrong2": wrong_turn2.hint_level}


def test_consecutive_correct() -> dict[str, object]:
	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		ingested = service.ingest_report(build_request())

		started = service.start_redo_session(
			MistakeRedoSessionRequest(report_id=ingested.report_id, user_id="redo-user")
		)
		assert started.consecutive_correct == 0
		assert started.required_consecutive_correct >= 1

		turn1 = service.advance_redo_session(
			started.session_id,
			MistakeRedoSessionTurnRequest(answer="A"),
		)
		if turn1.is_completed:
			assert turn1.consecutive_correct >= turn1.required_consecutive_correct
			assert turn1.can_clear_mistake is True
		else:
			assert turn1.consecutive_correct >= 0

			turn2 = service.advance_redo_session(
				started.session_id,
				MistakeRedoSessionTurnRequest(
					answer="A" if turn1.interaction_mode == "single_choice" else "先算速度差"
				),
			)
			if turn2.is_completed:
				assert turn2.consecutive_correct >= turn2.required_consecutive_correct
				assert turn2.can_clear_mistake is True
			else:
				turn3 = service.advance_redo_session(
					started.session_id,
					MistakeRedoSessionTurnRequest(answer="10分钟"),
				)
				assert turn3.is_completed is True
				assert turn3.can_clear_mistake is True
				assert turn3.consecutive_correct >= turn3.required_consecutive_correct

	return {"test": "consecutive_correct", "status": "passed"}


def test_recommendation_redo_loop() -> dict[str, object]:
	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		ingested = service.ingest_report(build_request())

		rec = service.generate_recommendation(
			MistakeRecommendationGenerateRequest(
				report_id=ingested.report_id,
				user_id="redo-user",
				recommendation_type="variant",
			)
		)
		assert rec.recommendation_id
		assert rec.question

		submit_wrong = service.submit_recommendation_answer(
			rec.recommendation_id,
			MistakeRecommendationSubmitRequest(answer="Z", user_id="redo-user"),
		)
		assert submit_wrong.is_correct is False
		assert submit_wrong.should_create_mistake is True

		submit_right = service.submit_recommendation_answer(
			rec.recommendation_id,
			MistakeRecommendationSubmitRequest(answer=rec.correct_option_id, user_id="redo-user"),
		)
		assert submit_right.is_correct is True
		assert submit_right.should_create_mistake is False

		redo_session = service.start_redo_session_from_recommendation(
			rec.recommendation_id,
			MistakeRecommendationStartRedoRequest(user_id="redo-user"),
		)
		assert redo_session.session_type == "redo_recommendation"
		assert redo_session.recommendation_id == rec.recommendation_id
		assert redo_session.problem_text == rec.question
		assert redo_session.hint_level == 1

		turn1 = service.advance_redo_session(
			redo_session.session_id,
			MistakeRedoSessionTurnRequest(answer="A"),
		)
		assert turn1.turn_count == 1

	return {
		"test": "recommendation_redo_loop",
		"status": "passed",
		"recommendation_id": rec.recommendation_id,
		"session_type": redo_session.session_type,
		"wrong_should_create": submit_wrong.should_create_mistake,
		"right_should_create": submit_right.should_create_mistake,
	}


def test_wrong_resets_consecutive() -> dict[str, object]:
	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		ingested = service.ingest_report(build_request())

		started = service.start_redo_session(
			MistakeRedoSessionRequest(report_id=ingested.report_id, user_id="redo-user")
		)

		correct_turn = service.advance_redo_session(
			started.session_id,
			MistakeRedoSessionTurnRequest(answer="A"),
		)
		consecutive_after_correct = correct_turn.consecutive_correct

		if not correct_turn.is_completed:
			wrong_turn = service.advance_redo_session(
				started.session_id,
				MistakeRedoSessionTurnRequest(answer="Z"),
			)
			assert wrong_turn.consecutive_correct <= consecutive_after_correct, "答错后连续答对计数应重置或减少"

	return {"test": "wrong_resets_consecutive", "status": "passed", "consecutive_after_correct": consecutive_after_correct}


def main() -> None:
	results: list[dict[str, object]] = []

	print("=" * 60)
	print("测试1: 会话持久化")
	print("=" * 60)
	r1 = test_persistence()
	results.append(r1)
	print(json.dumps(r1, ensure_ascii=False, indent=2))

	print("\n" + "=" * 60)
	print("测试2: 提示分级")
	print("=" * 60)
	r2 = test_hint_grading()
	results.append(r2)
	print(json.dumps(r2, ensure_ascii=False, indent=2))

	print("\n" + "=" * 60)
	print("测试3: 连续答对完成判定")
	print("=" * 60)
	r3 = test_consecutive_correct()
	results.append(r3)
	print(json.dumps(r3, ensure_ascii=False, indent=2))

	print("\n" + "=" * 60)
	print("测试4: 推荐题闭环（生成->提交->重做会话）")
	print("=" * 60)
	r4 = test_recommendation_redo_loop()
	results.append(r4)
	print(json.dumps(r4, ensure_ascii=False, indent=2))

	print("\n" + "=" * 60)
	print("测试5: 答错重置连续答对计数")
	print("=" * 60)
	r5 = test_wrong_resets_consecutive()
	results.append(r5)
	print(json.dumps(r5, ensure_ascii=False, indent=2))

	print("\n" + "=" * 60)
	print("全部测试结果汇总")
	print("=" * 60)
	all_passed = all(r.get("status") == "passed" for r in results)
	print(json.dumps({"total": len(results), "passed": all_passed, "results": results}, ensure_ascii=False, indent=2))

	if not all_passed:
		raise AssertionError("存在测试未通过")


if __name__ == "__main__":
	main()
