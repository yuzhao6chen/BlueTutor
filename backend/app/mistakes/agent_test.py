from __future__ import annotations

import json
import tempfile
from pathlib import Path

from app.mistakes.schema import (
	MistakeErrorProfileItem,
	MistakeIndependenceEvaluation,
	MistakeProblem,
	MistakeRecommendationGenerateRequest,
	MistakeReportIngestRequest,
	MistakeReportPayload,
	MistakeThinkingNode,
)
from app.mistakes.service import MistakeService


def build_request() -> MistakeReportIngestRequest:
	return MistakeReportIngestRequest(
		user_id="agent-user",
		source_session_id="agent-session-001",
		report_title="追及问题智能推荐源题",
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
					content="先求速度差，再求追及时间",
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
			solution="先算小红先走路程，再算速度差，最后求追及时间。",
		),
	)


def main() -> None:
	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		ingested = service.ingest_report(build_request())
		recommendation = service.generate_recommendation(
			MistakeRecommendationGenerateRequest(
				report_id=ingested.report_id,
				user_id="agent-user",
				recommendation_type="variant",
			)
		)

		assert recommendation.origin_report_id == ingested.report_id
		assert recommendation.recommendation_type == "variant"
		assert recommendation.question
		assert len(recommendation.options) == 4
		assert recommendation.correct_option_id in {item.id for item in recommendation.options}
		assert recommendation.why_recommended
		assert len(recommendation.agent_traces) == 3

		print(
			json.dumps(
				{
					"recommendation_id": recommendation.recommendation_id,
					"type": recommendation.recommendation_type,
					"difficulty": recommendation.difficulty,
					"options": len(recommendation.options),
					"agents": [trace.agent for trace in recommendation.agent_traces],
				},
				ensure_ascii=False,
				indent=2,
			)
		)


if __name__ == "__main__":
	main()
