from __future__ import annotations

import json
import tempfile
from pathlib import Path
from uuid import uuid4

from app.mistakes.schema import (
	MistakeDialogueSessionRequest,
	MistakeDialogueSessionTurnRequest,
	MistakeErrorProfileItem,
	MistakeIndependenceEvaluation,
	MistakeProblem,
	MistakeRecommendationGenerateRequest,
	MistakeReportIngestRequest,
	MistakeReportPayload,
	MistakeThinkingNode,
)
from app.mistakes.service import MistakeService


def build_request(
	*,
	user_id: str,
	error_type: str,
	knowledge_tags: list[str],
	raw_problem: str,
) -> MistakeReportIngestRequest:
	return MistakeReportIngestRequest(
		user_id=user_id,
		source_session_id=f"session-{uuid4().hex[:8]}",
		report_title=f"测试题-{error_type}",
		report=MistakeReportPayload(
			problem=MistakeProblem(
				raw_problem=raw_problem,
				known_conditions=["条件1"],
				goal="求解",
				answer="42",
			),
			knowledge_tags=knowledge_tags,
			thinking_chain=[
				MistakeThinkingNode(
					node_id="n1",
					content="第一步分析",
					status="incorrect",
					parent_id=None,
					error_history=["概念混淆"],
				)
			],
			error_profile=[
				MistakeErrorProfileItem(
					error_type=error_type,
					detail=f"{error_type}的详细描述",
				)
			],
			independence_evaluation=MistakeIndependenceEvaluation(
				level="需要帮助",
				detail="测试场景",
			),
			solution="根据公式计算即可。",
		),
	)


def test_user_profile() -> None:
	print("=" * 60)
	print("功能3测试：用户画像构建与动态更新")
	print("=" * 60)

	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		user_id = f"profile-user-{uuid4().hex[:8]}"

		first = service.ingest_report(
			build_request(
				user_id=user_id,
				error_type="概念混淆",
				knowledge_tags=["分数运算", "通分"],
				raw_problem="计算 1/2 + 1/3",
			)
		)
		assert first.report_id

		profile = service.build_user_profile(user_id)
		assert profile.user_id == user_id
		assert profile.total_reports >= 1
		assert len(profile.weak_knowledge_tags) >= 1
		assert len(profile.weak_points) >= 1
		assert profile.independence_summary
		print(f"  初始画像：total={profile.total_reports}, weak_tags={profile.weak_knowledge_tags}")

		second = service.ingest_report(
			build_request(
				user_id=user_id,
				error_type="计算粗心",
				knowledge_tags=["分数运算", "约分"],
				raw_problem="计算 2/3 - 1/4",
			)
		)
		assert second.report_id

		updated_profile = service.get_user_profile(user_id)
		assert updated_profile is not None
		assert updated_profile.total_reports >= 2
		print(f"  更新画像：total={updated_profile.total_reports}, weak_tags={updated_profile.weak_knowledge_tags}")
		print("  [PASS] 用户画像构建与动态更新测试通过")


def test_profile_aware_recommendation() -> None:
	print()
	print("=" * 60)
	print("功能1测试：基于用户画像的相似题生成")
	print("=" * 60)

	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		user_id = f"rec-user-{uuid4().hex[:8]}"

		ingested = service.ingest_report(
			build_request(
				user_id=user_id,
				error_type="速度差理解不足",
				knowledge_tags=["行程问题", "追及问题"],
				raw_problem="小明每分钟走60米，小红每分钟走40米，小红先走5分钟，小明多久追上？",
			)
		)

		rec = service.generate_recommendation(
			MistakeRecommendationGenerateRequest(
				report_id=ingested.report_id,
				user_id=user_id,
				recommendation_type="similar",
			)
		)
		assert rec.origin_report_id == ingested.report_id
		assert rec.recommendation_type == "similar"
		assert rec.question
		assert len(rec.options) == 4
		assert rec.why_recommended
		print(f"  推荐题ID: {rec.recommendation_id}")
		print(f"  题目: {rec.question[:50]}...")
		print(f"  难度: {rec.difficulty}")
		print(f"  推荐理由: {rec.why_recommended[:60]}...")
		print("  [PASS] 基于用户画像的相似题生成测试通过")


def test_dialogue_session() -> None:
	print()
	print("=" * 60)
	print("功能2测试：用户讲题验证对话")
	print("=" * 60)

	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		user_id = f"dlg-user-{uuid4().hex[:8]}"

		ingested = service.ingest_report(
			build_request(
				user_id=user_id,
				error_type="速度差理解不足",
				knowledge_tags=["行程问题", "追及问题"],
				raw_problem="小明每分钟走60米，小红每分钟走40米，小红先走5分钟，小明多久追上？",
			)
		)

		session = service.start_dialogue_session(
			MistakeDialogueSessionRequest(
				report_id=ingested.report_id,
				user_id=user_id,
			)
		)
		assert session.session_id
		assert session.report_id == ingested.report_id
		assert len(session.messages) >= 1
		assert session.messages[0].role == "assistant"
		assert not session.is_completed
		print(f"  会话ID: {session.session_id}")
		print(f"  AI开场: {session.messages[0].content[:60]}...")

		turn1 = service.advance_dialogue_session(
			session.session_id,
			MistakeDialogueSessionTurnRequest(
				message="首先算出小红先走的路程，40乘以5等于200米"
			),
		)
		assert len(turn1.messages) >= 3
		assert turn1.messages[-2].role == "user"
		assert turn1.messages[-1].role == "assistant"
		print(f"  用户: 首先算出小红先走的路程，40乘以5等于200米")
		print(f"  AI回复: {turn1.messages[-1].content[:60]}...")

		turn2 = service.advance_dialogue_session(
			session.session_id,
			MistakeDialogueSessionTurnRequest(
				message="然后算速度差，60减40等于20米每分，最后200除以20等于10分钟"
			),
		)
		assert len(turn2.messages) >= 5
		print(f"  用户: 然后算速度差，60减40等于20米每分，最后200除以20等于10分钟")
		print(f"  AI回复: {turn2.messages[-1].content[:60]}...")
		print(f"  掌握判定: {turn2.mastery_verdict}")
		print("  [PASS] 用户讲题验证对话测试通过")


def main() -> None:
	test_user_profile()
	test_profile_aware_recommendation()
	test_dialogue_session()
	print()
	print("=" * 60)
	print("全部新功能测试通过！")
	print("=" * 60)


if __name__ == "__main__":
	main()
