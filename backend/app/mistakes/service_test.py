from __future__ import annotations

import json
import tempfile
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import uuid4

from app.mistakes.schema import (
	MistakeErrorProfileItem,
	MistakeIndependenceEvaluation,
	MistakeProblem,
	MistakeReportData,
	MistakeReportIngestRequest,
	MistakeReportPayload,
	MistakeReportStatusUpdateRequest,
	MistakeThinkingNode,
)
from app.mistakes.service import MistakeService


NOW = datetime.now(timezone.utc)


def build_request(
	*,
	user_id: str,
	report_title: str,
	raw_problem: str,
	knowledge_tags: list[str],
	error_type: str,
	solution: str | None,
) -> MistakeReportIngestRequest:
	return MistakeReportIngestRequest(
		user_id=user_id,
		source_session_id=f"session-{report_title}",
		report_title=report_title,
		report=MistakeReportPayload(
			problem=MistakeProblem(
				raw_problem=raw_problem,
				known_conditions=["???? 1"],
				goal="???",
				answer="????",
			),
			knowledge_tags=knowledge_tags,
			thinking_chain=[
				MistakeThinkingNode(
					node_id="n1",
					content="??????",
					status="correct",
					parent_id=None,
					error_history=[],
				)
			],
			error_profile=[
				MistakeErrorProfileItem(
					error_type=error_type,
					detail="??????",
				)
			],
			independence_evaluation=MistakeIndependenceEvaluation(
				level="????",
				detail="????",
			),
			solution=solution,
		),
	)


def patch_created_at(report: MistakeReportData, *, delta_days: int) -> MistakeReportData:
	created_at = (NOW - timedelta(days=delta_days)).isoformat()
	return report.model_copy(update={"created_at": created_at})


def main() -> None:
	with tempfile.TemporaryDirectory() as temp_dir:
		service = MistakeService(data_dir=Path(temp_dir))
		test_user_id = f"test-user-{uuid4().hex[:8]}"

		first = service.ingest_report(
			build_request(
				user_id=test_user_id,
				report_title="?????",
				raw_problem="????????????",
				knowledge_tags=["????", "????"],
				error_type="???????",
				solution="?????????????",
			)
		)
		second = service.ingest_report(
			build_request(
				user_id=test_user_id,
				report_title="?????",
				raw_problem="??????",
				knowledge_tags=["????"],
				error_type="????",
				solution=None,
			)
		)

		service._reports[first.report_id] = patch_created_at(service.get_report(first.report_id), delta_days=0)
		service._reports[second.report_id] = patch_created_at(service.get_report(second.report_id), delta_days=1)

		lecture = service.get_lecture(first.report_id)
		assert lecture.report_id == first.report_id
		assert len(lecture.lecture_sections) >= 3
		assert any(section.kind == "solution" for section in lecture.lecture_sections)
		assert len(lecture.review_steps) >= 2
		assert len(lecture.key_takeaways) >= 1

		filtered_list = service.list_reports(
			user_id=test_user_id,
			status="pending",
			knowledge_tag="????",
			has_solution=True,
		)
		assert filtered_list.count == 1
		assert filtered_list.reports[0].report_id == first.report_id

		filtered_timeline = service.get_timeline(
			user_id=test_user_id,
			status="pending",
			knowledge_tag="????",
			has_solution=True,
		)
		assert filtered_timeline.count == 1
		assert filtered_timeline.groups[0].items[0].report_id == first.report_id

		service.update_status(first.report_id, MistakeReportStatusUpdateRequest(status="mastered"))
		daily_plan = service.get_daily_plan(user_id=test_user_id, limit=3)
		assert daily_plan.count == 1
		assert daily_plan.items[0].report_id == second.report_id
		assert daily_plan.items[0].action == "redo_problem"
		assert daily_plan.summary

		print(
			json.dumps(
				{
					"lecture_report_id": lecture.report_id,
					"lecture_sections": len(lecture.lecture_sections),
					"review_steps": len(lecture.review_steps),
					"filtered_list_count": filtered_list.count,
					"filtered_timeline_count": filtered_timeline.count,
					"daily_plan_count": daily_plan.count,
					"daily_plan_action": daily_plan.items[0].action,
				},
				ensure_ascii=False,
				indent=2,
			)
		)


if __name__ == "__main__":
	main()
