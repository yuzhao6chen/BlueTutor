from __future__ import annotations

from collections import Counter
from datetime import datetime, timedelta, timezone
import os
from pathlib import Path
from uuid import uuid4

from .schema import (
	MistakeAgentTrace,
	MistakeDailyPlanData,
	MistakeDialogueMessage,
	MistakeDialogueSessionData,
	MistakeDialogueSessionRequest,
	MistakeDialogueSessionTurnRequest,
	MistakeRecommendationData,
	MistakeRecommendationSubmitData,
	MistakeErrorProfileItem,
	MistakeHomeSummaryData,
	MistakeHomeWeakTagItem,
	MistakeLectureData,
	MistakeLectureSection,
	MistakePlanItem,
	MistakeReportData,
	MistakeReportIngestData,
	MistakeReportIngestRequest,
	MistakeReportListData,
	MistakeReportPayload,
	MistakeReportStatusUpdateRequest,
	MistakeReportSummary,
	MistakeRecommendationGenerateRequest,
	MistakeRedoSessionData,
	MistakeRedoSessionRequest,
	MistakeRedoSessionTurnRequest,
	MistakeRedoTurnRecord,
	MistakeReviewStep,
	MistakeStatsData,
	MistakeThinkingNode,
	MistakeTimelineData,
	MistakeTimelineGroup,
	MistakeTimelineItem,
	MistakeUserProfileData,
	MistakeUserProfileWeakPoint,
)
from .agent import MistakeMultiAgent
from .store import list_reports as _list_stored_reports
from .store import load_report, save_report
from .store import list_redo_sessions as _list_stored_sessions
from .store import load_redo_session, save_redo_session
from .store import list_recommendations as _list_stored_recommendations
from .store import load_recommendation, save_recommendation
from .store import save_user_profile, load_user_profile
from .store import save_dialogue_session, load_dialogue_session
from .store import list_dialogue_sessions as _list_stored_dialogue_sessions


class MistakeService:
	def __init__(
		self,
		data_dir: Path | None = None,
		multi_agent: MistakeMultiAgent | None = None,
		persist_runtime_state: bool | None = None,
	) -> None:
		self._reports: dict[str, MistakeReportData] = {}
		self._redo_sessions: dict[str, MistakeRedoSessionData] = {}
		self._recommendations: dict[str, MistakeRecommendationData] = {}
		self._user_profiles: dict[str, MistakeUserProfileData] = {}
		self._dialogue_sessions: dict[str, MistakeDialogueSessionData] = {}
		self._data_dir = data_dir
		self._multi_agent = multi_agent or MistakeMultiAgent()
		self._persist_runtime_state = persist_runtime_state if persist_runtime_state is not None else os.getenv("MISTAKES_PERSIST_RUNTIME_STATE", "false").lower() == "true"
		self._load_existing_data()

	def ingest_report(self, request: MistakeReportIngestRequest) -> MistakeReportIngestData:
		normalized_report = self._normalize_report(request.report)
		report_id = f"mr_{uuid4().hex[:12]}"
		created_at = datetime.now(timezone.utc).isoformat()
		report_title = self._build_report_title(request.report_title, normalized_report)
		status = "pending"
		primary_error_type = self._extract_primary_error_type(normalized_report.error_profile)
		knowledge_tags = normalized_report.knowledge_tags[:6]
		has_solution = self._has_solution(normalized_report.solution)
		solution_preview = self._build_solution_preview(normalized_report.solution)

		stored_report = MistakeReportData(
			report_id=report_id,
			user_id=request.user_id,
			report_title=report_title,
			source_session_id=request.source_session_id,
			status=status,
			knowledge_tags=knowledge_tags,
			primary_error_type=primary_error_type,
			independence_level=normalized_report.independence_evaluation.level,
			has_solution=has_solution,
			solution_preview=solution_preview,
			created_at=created_at,
			problem_preview=self._build_problem_preview(normalized_report.problem.raw_problem),
			report=normalized_report,
		)
		self._reports[report_id] = stored_report
		save_report(stored_report)

		self._update_user_profile_on_ingest(request.user_id, stored_report)

		return MistakeReportIngestData(
			report_id=report_id,
			report_title=report_title,
			status=status,
			primary_error_type=primary_error_type,
			knowledge_tags=knowledge_tags,
			has_solution=has_solution,
			solution_preview=solution_preview,
			created_at=created_at,
		)

	def list_reports(
		self,
		*,
		user_id: str | None = None,
		status: str | None = None,
		knowledge_tag: str | None = None,
		has_solution: bool | None = None,
	) -> MistakeReportListData:
		reports = self._select_reports(
			user_id=user_id,
			status=status,
			knowledge_tag=knowledge_tag,
			has_solution=has_solution,
		)
		summaries = [self._to_summary(item) for item in reports]
		timeline = [self._to_timeline_item(item) for item in reports]
		return MistakeReportListData(reports=summaries, timeline=timeline, count=len(summaries))

	def get_report(self, report_id: str) -> MistakeReportData:
		report = self._reports.get(report_id)
		if report is not None:
			return report

		stored_report = load_report(report_id)
		if stored_report is None:
			raise KeyError(f"错题报告不存在：{report_id}")
		self._reports[report_id] = self._with_solution_metadata(stored_report)
		return self._reports[report_id]

	def get_lecture(self, report_id: str) -> MistakeLectureData:
		report = self.get_report(report_id)
		problem = report.report.problem
		return MistakeLectureData(
			report_id=report.report_id,
			report_title=report.report_title,
			status=report.status,
			problem_text=problem.raw_problem,
			answer=problem.answer,
			knowledge_tags=report.knowledge_tags,
			primary_error_type=report.primary_error_type,
			independence_level=report.independence_level,
			has_solution=report.has_solution,
			solution_preview=report.solution_preview,
			lecture_sections=self._build_lecture_sections(report),
			review_steps=self._build_review_steps(report),
			key_takeaways=self._build_key_takeaways(report),
			created_at=report.created_at,
		)

	def get_daily_plan(self, *, user_id: str | None = None, limit: int = 3) -> MistakeDailyPlanData:
		reports = self._select_reports(user_id=user_id, status='pending')
		planned_reports = reports[:limit]
		tag_counter = Counter(
			tag
			for report in reports
			for tag in report.knowledge_tags
			if tag.strip()
		)
		focus_knowledge_tags = [tag for tag, _ in tag_counter.most_common(3)]
		items = [self._build_plan_item(report) for report in planned_reports]
		today_focus = self._build_plan_focus(focus_knowledge_tags, planned_reports)
		return MistakeDailyPlanData(
			user_id=user_id,
			generated_at=datetime.now(timezone.utc).isoformat(),
			today_focus=today_focus,
			summary=self._build_plan_summary(planned_reports, focus_knowledge_tags),
			focus_knowledge_tags=focus_knowledge_tags,
			items=items,
			count=len(items),
		)

	def generate_recommendation(
		self,
		request: MistakeRecommendationGenerateRequest,
	) -> MistakeRecommendationData:
		report = self.get_report(request.report_id)
		if request.user_id and report.user_id != request.user_id:
			raise ValueError(f"无权访问该错题报告：{request.report_id}")
		user_profile_dict = None
		effective_user_id = request.user_id or report.user_id
		if effective_user_id:
			profile = self.get_user_profile(effective_user_id)
			if profile is not None:
				user_profile_dict = profile.model_dump()
		rec = self._multi_agent.generate_profile_aware_recommendation(
			report_payload=report.report.model_dump(),
			origin_report_id=report.report_id,
			recommendation_type=request.recommendation_type,
			user_profile=user_profile_dict,
		)
		self._recommendations[rec.recommendation_id] = rec
		if self._persist_runtime_state:
			save_recommendation(rec)
		return rec

	def start_redo_session(self, request: MistakeRedoSessionRequest) -> MistakeRedoSessionData:
		report = self.get_report(request.report_id)
		if request.user_id and report.user_id != request.user_id:
			raise ValueError(f"无权访问该错题报告：{request.report_id}")
		planned = self._multi_agent.plan_redo_turn(
			report_payload=report.report.model_dump(),
			stage="understand_problem",
			history=[],
			hint_level=1,
		)
		now = datetime.now(timezone.utc).isoformat()
		session = MistakeRedoSessionData(
			session_id=f"redo_{uuid4().hex[:12]}",
			report_id=report.report_id,
			user_id=request.user_id or report.user_id,
			report_title=report.report_title,
			problem_text=report.report.problem.raw_problem,
			stage=planned["planning"]["stage"],
			turn_count=0,
			current_prompt=planned["planning"]["prompt"],
			interaction_mode=planned["planning"]["interaction_mode"],
			options=planned["options"],
			hint=planned["hint"],
			hint_level=1,
			max_hint_level=3,
			last_feedback="",
			is_completed=False,
			can_clear_mistake=False,
			consecutive_correct=0,
			required_consecutive_correct=1,
			session_type="redo_original",
			recommendation_id=None,
			agent_traces=planned["agent_traces"],
			history=[],
			created_at=now,
			updated_at=now,
		)
		self._redo_sessions[session.session_id] = session.model_copy(deep=True)
		if self._persist_runtime_state:
			save_redo_session(session)
		return session

	def start_redo_session_from_recommendation(
		self,
		recommendation_id: str,
		request: "MistakeRecommendationStartRedoRequest",
	) -> MistakeRedoSessionData:
		rec = self._get_recommendation(recommendation_id)
		if rec is None:
			raise KeyError(f"推荐题不存在：{recommendation_id}")
		report = self.get_report(rec.origin_report_id)
		user_id = request.user_id or report.user_id
		if user_id and report.user_id != user_id:
			raise ValueError(f"无权访问该错题报告：{rec.origin_report_id}")

		report_payload = report.report.model_dump()
		report_payload["problem"] = {
			"raw_problem": rec.question,
			"known_conditions": [],
			"goal": f"完成{rec.recommendation_type}练习题",
			"answer": rec.answer,
		}
		planned = self._multi_agent.plan_redo_turn(
			report_payload=report_payload,
			stage="understand_problem",
			history=[],
			hint_level=1,
		)
		now = datetime.now(timezone.utc).isoformat()
		session = MistakeRedoSessionData(
			session_id=f"redo_{uuid4().hex[:12]}",
			report_id=report.report_id,
			user_id=user_id,
			report_title=rec.title,
			problem_text=rec.question,
			stage=planned["planning"]["stage"],
			turn_count=0,
			current_prompt=planned["planning"]["prompt"],
			interaction_mode=planned["planning"]["interaction_mode"],
			options=planned["options"],
			hint=planned["hint"],
			hint_level=1,
			max_hint_level=3,
			last_feedback="",
			is_completed=False,
			can_clear_mistake=False,
			consecutive_correct=0,
			required_consecutive_correct=1,
			session_type="redo_recommendation",
			recommendation_id=recommendation_id,
			agent_traces=planned["agent_traces"],
			history=[],
			created_at=now,
			updated_at=now,
		)
		self._redo_sessions[session.session_id] = session.model_copy(deep=True)
		if self._persist_runtime_state:
			save_redo_session(session)
		return session

	def submit_recommendation_answer(
		self,
		recommendation_id: str,
		request: "MistakeRecommendationSubmitRequest",
	) -> MistakeRecommendationSubmitData:
		rec = self._get_recommendation(recommendation_id)
		if rec is None:
			raise KeyError(f"推荐题不存在：{recommendation_id}")
		answer = request.answer.strip()
		is_correct = answer.upper() == rec.correct_option_id.upper()
		should_create = not is_correct
		return MistakeRecommendationSubmitData(
			recommendation_id=recommendation_id,
			is_correct=is_correct,
			selected_option_id=answer if len(answer) == 1 and answer.isalpha() else "",
			correct_option_id=rec.correct_option_id,
			feedback="回答正确！" if is_correct else f"正确答案是{rec.correct_option_id}，{rec.explanation}",
			should_create_mistake=should_create,
		)

	def get_redo_session(self, session_id: str) -> MistakeRedoSessionData:
		session = self._redo_sessions.get(session_id)
		if session is not None:
			return session
		if not self._persist_runtime_state:
			raise KeyError(f"重做会话不存在：{session_id}")
		stored = load_redo_session(session_id)
		if stored is None:
			raise KeyError(f"重做会话不存在：{session_id}")
		self._redo_sessions[session_id] = stored
		return stored

	def advance_redo_session(self, session_id: str, request: MistakeRedoSessionTurnRequest) -> MistakeRedoSessionData:
		session = self._redo_sessions.get(session_id)
		if session is None:
			if not self._persist_runtime_state:
				raise KeyError(f"重做会话不存在：{session_id}")
			stored = load_redo_session(session_id)
			if stored is None:
				raise KeyError(f"重做会话不存在：{session_id}")
			session = stored
			self._redo_sessions[session_id] = session
		if session.is_completed:
			return session

		report = self.get_report(session.report_id)
		report_payload = report.report.model_dump()
		if session.session_type == "redo_recommendation" and session.recommendation_id:
			rec = self._get_recommendation(session.recommendation_id)
			if rec is not None:
				report_payload["problem"] = {
					"raw_problem": rec.question,
					"known_conditions": [],
					"goal": f"完成{rec.recommendation_type}练习题",
					"answer": rec.answer,
				}

		planning = {
			"stage": session.stage,
			"interaction_mode": session.interaction_mode,
			"prompt": session.current_prompt,
		}
		correct_option_id = ""
		if session.interaction_mode == "single_choice" and session.options:
			correct_option_id = session.options[0].id

		evaluation = self._multi_agent.evaluate_redo_turn(
			report_payload=report_payload,
			planning=planning,
			answer=request.answer.strip(),
			correct_option_id=correct_option_id,
			consecutive_correct=session.consecutive_correct,
			required_consecutive_correct=session.required_consecutive_correct,
		)

		new_consecutive = evaluation.get("consecutive_correct", 0)
		if evaluation["result"] == "correct":
			new_consecutive = max(new_consecutive, session.consecutive_correct + 1)
		elif evaluation["result"] == "incorrect":
			new_consecutive = 0

		new_hint_level = session.hint_level
		if evaluation["should_offer_hint"]:
			new_hint_level = min(session.hint_level + 1, session.max_hint_level)

		history = list(session.history)
		history.append(
			MistakeRedoTurnRecord(
				turn_no=session.turn_count + 1,
				prompt=session.current_prompt,
				interaction_mode=session.interaction_mode,
				student_answer=request.answer.strip(),
				result=evaluation["result"],
				feedback=evaluation["feedback"],
				hint=session.hint if evaluation["should_offer_hint"] else "",
				stage=session.stage,
			)
		)

		now = datetime.now(timezone.utc).isoformat()

		if evaluation["next_stage"] == "completed":
			can_clear = evaluation["can_clear_mistake"] and new_consecutive >= session.required_consecutive_correct
			updated = session.model_copy(update={
				"turn_count": session.turn_count + 1,
				"last_feedback": evaluation["feedback"],
				"stage": "completed",
				"is_completed": True,
				"can_clear_mistake": can_clear,
				"consecutive_correct": new_consecutive,
				"agent_traces": evaluation["agent_traces"],
				"history": history,
				"updated_at": now,
			})
			self._redo_sessions[session_id] = updated
			if self._persist_runtime_state:
				save_redo_session(updated)
			return updated

		planned = self._multi_agent.plan_redo_turn(
			report_payload=report_payload,
			stage=evaluation["next_stage"],
			history=[item.model_dump() for item in history],
			hint_level=new_hint_level,
		)
		updated = session.model_copy(update={
			"turn_count": session.turn_count + 1,
			"stage": planned["planning"]["stage"],
			"current_prompt": planned["planning"]["prompt"],
			"interaction_mode": planned["planning"]["interaction_mode"],
			"options": planned["options"],
			"hint": planned["hint"],
			"hint_level": new_hint_level,
			"last_feedback": evaluation["feedback"],
			"is_completed": False,
			"can_clear_mistake": False,
			"consecutive_correct": new_consecutive,
			"agent_traces": evaluation["agent_traces"] + planned["agent_traces"],
			"history": history,
			"updated_at": now,
		})
		self._redo_sessions[session_id] = updated
		if self._persist_runtime_state:
			save_redo_session(updated)
		return updated

	def update_status(self, report_id: str, request: MistakeReportStatusUpdateRequest) -> MistakeReportData:
		report = self.get_report(report_id)
		updated_report = report.model_copy(update={"status": request.status})
		self._reports[report_id] = updated_report
		save_report(updated_report)
		return updated_report

	def get_stats(self, *, user_id: str | None = None) -> MistakeStatsData:
		reports = self._select_reports(user_id=user_id)
		pending_reports = [report for report in reports if report.status == "pending"]
		mastered_reports = [report for report in reports if report.status == "mastered"]
		mastered_error_types = {
			report.primary_error_type
			for report in mastered_reports
			if report.primary_error_type and report.primary_error_type != "待分析"
		}
		tag_counter = Counter(
			tag
			for report in reports
			for tag in report.knowledge_tags
			if tag.strip()
		)

		return MistakeStatsData(
			total_reports=len(reports),
			pending_reports=len(pending_reports),
			mastered_reports=len(mastered_reports),
			mastered_error_types=len(mastered_error_types),
			top_knowledge_tags=[tag for tag, _ in tag_counter.most_common(5)],
		)

	def get_home_summary(self, *, user_id: str | None = None) -> MistakeHomeSummaryData:
		reports = self._select_reports(user_id=user_id)
		now = datetime.now(timezone.utc)
		start_of_today = now.replace(hour=0, minute=0, second=0, microsecond=0)
		start_of_week = start_of_today - timedelta(days=start_of_today.weekday())

		today_pending_count = 0
		completed_this_week_count = 0
		weak_tag_counter: Counter[str] = Counter()
		mastered_error_types: set[str] = set()

		for report in reports:
			created_at = self._parse_datetime(report.created_at)
			if report.status == "pending" and created_at >= start_of_today:
				today_pending_count += 1
			if report.status == "mastered":
				if created_at >= start_of_week:
					completed_this_week_count += 1
				if report.primary_error_type and report.primary_error_type != "待分析":
					mastered_error_types.add(report.primary_error_type)
			else:
				for tag in report.knowledge_tags:
					if tag.strip():
						weak_tag_counter[tag] += 1

		weak_knowledge_tags = [
			MistakeHomeWeakTagItem(tag=tag, count=count)
			for tag, count in weak_tag_counter.most_common(5)
		]

		recent_timeline = self.get_timeline(user_id=user_id, limit=6).groups
		pending_review_count = sum(1 for report in reports if report.status == "pending")

		return MistakeHomeSummaryData(
			today_pending_count=today_pending_count,
			pending_review_count=pending_review_count,
			completed_this_week_count=completed_this_week_count,
			mastered_error_types_count=len(mastered_error_types),
			weak_knowledge_tags=weak_knowledge_tags,
			recent_timeline=recent_timeline,
		)

	def get_timeline(
		self,
		*,
		user_id: str | None = None,
		limit: int | None = None,
		status: str | None = None,
		knowledge_tag: str | None = None,
		has_solution: bool | None = None,
	) -> MistakeTimelineData:
		reports = self._select_reports(
			user_id=user_id,
			status=status,
			knowledge_tag=knowledge_tag,
			has_solution=has_solution,
		)
		if limit is not None and limit >= 0:
			reports = reports[:limit]

		items = [self._to_timeline_item(report) for report in reports]
		group_map: dict[str, list[MistakeTimelineItem]] = {
			"今天": [],
			"昨天": [],
			"更早": [],
		}
		for item in items:
			group_map[self._timeline_label(item.created_at)].append(item)

		groups = [
			MistakeTimelineGroup(label=label, items=group_items)
			for label, group_items in group_map.items()
			if group_items
		]
		return MistakeTimelineData(groups=groups, count=len(items))

	def _load_existing_data(self) -> None:
		for report in _list_stored_reports():
			self._reports[report.report_id] = self._with_solution_metadata(report)
		if not self._persist_runtime_state:
			return
		for session in _list_stored_sessions():
			self._redo_sessions[session.session_id] = session
		for rec in _list_stored_recommendations():
			self._recommendations[rec.recommendation_id] = rec
		for dialogue in _list_stored_dialogue_sessions():
			self._dialogue_sessions[dialogue.session_id] = dialogue
		for report in self._reports.values():
			if report.user_id:
				profile = load_user_profile(report.user_id)
				if profile is not None:
					self._user_profiles[report.user_id] = profile

	def _get_recommendation(self, recommendation_id: str) -> MistakeRecommendationData | None:
		rec = self._recommendations.get(recommendation_id)
		if rec is not None:
			return rec
		if not self._persist_runtime_state:
			return None
		stored = load_recommendation(recommendation_id)
		if stored is not None:
			self._recommendations[recommendation_id] = stored
		return stored

	def _select_reports(
		self,
		*,
		user_id: str | None,
		status: str | None = None,
		knowledge_tag: str | None = None,
		has_solution: bool | None = None,
	) -> list[MistakeReportData]:
		reports = list(self._reports.values())
		if user_id:
			reports = [report for report in reports if report.user_id == user_id]
		if status:
			reports = [report for report in reports if report.status == status]
		if knowledge_tag and knowledge_tag.strip():
			tag = knowledge_tag.strip()
			reports = [report for report in reports if tag in report.knowledge_tags]
		if has_solution is not None:
			reports = [report for report in reports if report.has_solution is has_solution]
		reports.sort(key=lambda item: item.created_at, reverse=True)
		return reports

	def _normalize_report(self, report: MistakeReportPayload) -> MistakeReportPayload:
		knowledge_tags = [item.strip() for item in report.knowledge_tags if item.strip()]
		thinking_chain = [
			MistakeThinkingNode(
				node_id=node.node_id.strip(),
				content=node.content.strip(),
				status=node.status,
				parent_id=node.parent_id.strip() if node.parent_id else None,
				error_history=[item.strip() for item in node.error_history if item.strip()],
			)
			for node in report.thinking_chain
			if node.node_id.strip() and node.content.strip()
		]
		error_profile = [
			MistakeErrorProfileItem(
				error_type=item.error_type.strip(),
				detail=item.detail.strip(),
			)
			for item in report.error_profile
			if item.error_type.strip()
		]
		solution = self._normalize_solution(report.solution)

		return MistakeReportPayload(
			problem=report.problem,
			knowledge_tags=knowledge_tags,
			thinking_chain=thinking_chain,
			error_profile=error_profile,
			independence_evaluation=report.independence_evaluation,
			solution=solution,
		)

	def _build_report_title(self, custom_title: str | None, report: MistakeReportPayload) -> str:
		if custom_title and custom_title.strip():
			return custom_title.strip()
		problem = report.problem.raw_problem.strip()
		return self._build_problem_preview(problem, limit=18)

	def _build_problem_preview(self, problem_text: str, *, limit: int = 30) -> str:
		cleaned = " ".join(problem_text.split())
		if len(cleaned) <= limit:
			return cleaned
		return f"{cleaned[:limit].rstrip()}…"

	def _normalize_solution(self, solution: str | None) -> str | None:
		if solution is None:
			return None
		cleaned = "\n".join(line.rstrip() for line in solution.strip().splitlines()).strip()
		return cleaned or None

	def _has_solution(self, solution: str | None) -> bool:
		return bool(solution and solution.strip())

	def _build_solution_preview(self, solution: str | None, *, limit: int = 48) -> str:
		if not self._has_solution(solution):
			return ""
		cleaned = " ".join(solution.split())
		if len(cleaned) <= limit:
			return cleaned
		return f"{cleaned[:limit].rstrip()}…"

	def _with_solution_metadata(self, report: MistakeReportData) -> MistakeReportData:
		solution = self._normalize_solution(report.report.solution)
		has_solution = self._has_solution(solution)
		solution_preview = self._build_solution_preview(solution)
		if (
			report.report.solution == solution
			and report.has_solution == has_solution
			and report.solution_preview == solution_preview
		):
			return report
		updated_report = report.model_copy(
			update={
				"has_solution": has_solution,
				"solution_preview": solution_preview,
				"report": report.report.model_copy(update={"solution": solution}),
			}
		)
		save_report(updated_report)
		return updated_report

	def _extract_primary_error_type(self, error_profile: list[MistakeErrorProfileItem]) -> str:
		if error_profile:
			return error_profile[0].error_type
		return "待分析"

	def _parse_datetime(self, value: str) -> datetime:
		parsed = datetime.fromisoformat(value)
		if parsed.tzinfo is None:
			return parsed.replace(tzinfo=timezone.utc)
		return parsed.astimezone(timezone.utc)

	def _timeline_label(self, created_at: str) -> str:
		created_date = self._parse_datetime(created_at).date()
		today = datetime.now(timezone.utc).date()
		if created_date == today:
			return "今天"
		if created_date == today - timedelta(days=1):
			return "昨天"
		return "更早"

	def _to_summary(self, report: MistakeReportData) -> MistakeReportSummary:
		return MistakeReportSummary(**report.model_dump(exclude={"report"}))

	def _to_timeline_item(self, report: MistakeReportData) -> MistakeTimelineItem:
		return MistakeTimelineItem(
			report_id=report.report_id,
			title=report.report_title,
			status=report.status,
			primary_error_type=report.primary_error_type,
			knowledge_tags=report.knowledge_tags,
			independence_level=report.independence_level,
			has_solution=report.has_solution,
			solution_preview=report.solution_preview,
			created_at=report.created_at,
		)

	def _build_lecture_sections(self, report: MistakeReportData) -> list[MistakeLectureSection]:
		problem = report.report.problem
		sections: list[MistakeLectureSection] = [
			MistakeLectureSection(title="题目回顾", content=problem.raw_problem, kind="summary")
		]
		if report.knowledge_tags:
			sections.append(
				MistakeLectureSection(
					title="涉及知识点",
					content="、".join(report.knowledge_tags),
					kind="knowledge",
				)
			)
		if report.report.thinking_chain:
			sections.append(
				MistakeLectureSection(
					title="关键思路回放",
					content="\n".join(
						f"{index + 1}. {node.content}"
						for index, node in enumerate(report.report.thinking_chain[:5])
					),
					kind="thinking",
				)
			)
		if report.report.error_profile:
			error_lines = []
			for item in report.report.error_profile[:3]:
				error_lines.append(f"{item.error_type}：{item.detail}" if item.detail else item.error_type)
			sections.append(
				MistakeLectureSection(
					title="本题易错点",
					content="\n".join(error_lines),
					kind="error",
				)
			)
		if report.has_solution and report.report.solution:
			sections.append(
				MistakeLectureSection(
					title="题解讲义",
					content=report.report.solution,
					kind="solution",
				)
			)
		sections.append(
			MistakeLectureSection(
				title="下次遇到要先想什么",
				content=self._build_next_time_suggestion(report),
				kind="suggestion",
			)
		)
		return sections

	def _build_review_steps(self, report: MistakeReportData) -> list[MistakeReviewStep]:
		problem = report.report.problem
		steps: list[MistakeReviewStep] = [
			MistakeReviewStep(
				step_no=1,
				title="先看清题目在问什么",
				content=problem.goal or self._build_problem_preview(problem.raw_problem, limit=40),
				status="done",
			)
		]
		if report.knowledge_tags:
			steps.append(
				MistakeReviewStep(
					step_no=2,
					title="先匹配知识点",
					content="、".join(report.knowledge_tags[:3]),
					status="done",
				)
			)
		steps.append(
			MistakeReviewStep(
				step_no=len(steps) + 1,
				title="重点盯住这一步",
				content=self._build_focus_step_content(report),
				status="focus",
			)
		)
		if problem.answer:
			steps.append(
				MistakeReviewStep(
					step_no=len(steps) + 1,
					title="最后自检答案",
					content=problem.answer,
					status="done",
				)
			)
		return steps

	def _build_key_takeaways(self, report: MistakeReportData) -> list[str]:
		takeaways: list[str] = []
		if report.primary_error_type and report.primary_error_type != "待分析":
			takeaways.append(f"这题主要卡在：{report.primary_error_type}")
		if report.report.independence_evaluation.level:
			takeaways.append(f"独立完成度：{report.report.independence_evaluation.level}")
		if report.report.problem.goal:
			takeaways.append(f"先明确目标：{report.report.problem.goal}")
		if report.knowledge_tags:
			takeaways.append(f"优先回顾知识点：{'、'.join(report.knowledge_tags[:3])}")
		if not takeaways:
			takeaways.append("先读题，再找条件和目标，最后检查答案是否符合题意。")
		return takeaways[:4]

	def _build_focus_step_content(self, report: MistakeReportData) -> str:
		if report.report.error_profile:
			item = report.report.error_profile[0]
			return f"{item.error_type}：{item.detail}" if item.detail else item.error_type
		if report.report.thinking_chain:
			return report.report.thinking_chain[-1].content
		return "把已知条件和要求的问题对应起来，再开始列式。"

	def _build_next_time_suggestion(self, report: MistakeReportData) -> str:
		parts: list[str] = []
		if report.knowledge_tags:
			parts.append(f"先判断这题属于{'、'.join(report.knowledge_tags[:2])}哪一类")
		if report.primary_error_type and report.primary_error_type != "待分析":
			parts.append(f"特别留意\u201c{report.primary_error_type}\u201d这类错误")
		if report.report.problem.answer:
			parts.append("做完后记得把结果代回题意检查")
		if not parts:
			parts.append("先圈出已知条件，再确认题目要你求什么")
		return "；".join(parts) + "。"

	def _build_plan_item(self, report: MistakeReportData) -> MistakePlanItem:
		action = "review_lecture" if report.has_solution else "redo_problem"
		reason_parts = []
		if report.primary_error_type and report.primary_error_type != "待分析":
			reason_parts.append(f"主要错因：{report.primary_error_type}")
		if report.knowledge_tags:
			reason_parts.append(f"涉及知识点：{'、'.join(report.knowledge_tags[:2])}")
		if report.has_solution:
			reason_parts.append("有题解可先看讲义加深理解")
		else:
			reason_parts.append("无题解建议直接重做巩固")
		return MistakePlanItem(
			report_id=report.report_id,
			title=report.report_title,
			primary_error_type=report.primary_error_type,
			knowledge_tags=report.knowledge_tags,
			has_solution=report.has_solution,
			action=action,
			reason="；".join(reason_parts) + "。",
		)

	def _build_plan_focus(self, focus_knowledge_tags: list[str], reports: list[MistakeReportData]) -> str:
		if focus_knowledge_tags:
			return f"今日主攻：{'、'.join(focus_knowledge_tags[:2])}"
		if reports:
			return f"优先巩固：{reports[0].primary_error_type or '待分析'}相关错题"
		return "暂无待巩固错题，继续保持！"

	def _build_plan_summary(self, reports: list[MistakeReportData], focus_knowledge_tags: list[str]) -> str:
		if not reports:
			return "当前没有待巩固错题，做得好！"
		parts = [f"共 {len(reports)} 道待巩固"]
		if focus_knowledge_tags:
			parts.append(f"重点知识点：{'、'.join(focus_knowledge_tags)}")
		with_solution_count = sum(1 for report in reports if report.has_solution)
		if with_solution_count:
			parts.append(f"其中 {with_solution_count} 道有题解可参考")
		return "；".join(parts) + "。"

	def get_user_profile(self, user_id: str) -> MistakeUserProfileData | None:
		profile = self._user_profiles.get(user_id)
		if profile is not None:
			return profile
		if not self._persist_runtime_state:
			return None
		stored = load_user_profile(user_id)
		if stored is not None:
			self._user_profiles[user_id] = stored
		return stored

	def build_user_profile(self, user_id: str) -> MistakeUserProfileData:
		existing = self.get_user_profile(user_id)
		if existing is not None:
			return existing
		reports = self._select_reports(user_id=user_id)
		profile = self._build_profile_from_reports(user_id, reports)
		self._user_profiles[user_id] = profile
		if self._persist_runtime_state:
			save_user_profile(profile)
		return profile

	def _update_user_profile_on_ingest(self, user_id: str, report: MistakeReportData) -> None:
		if not user_id:
			return
		existing = self.get_user_profile(user_id)
		if existing is None:
			profile = self._build_profile_from_reports(user_id, self._select_reports(user_id=user_id))
		else:
			profile = self._incremental_update_profile(existing, report)
		self._user_profiles[user_id] = profile
		if self._persist_runtime_state:
			save_user_profile(profile)

	def _build_profile_from_reports(self, user_id: str, reports: list[MistakeReportData]) -> MistakeUserProfileData:
		total = len(reports)
		pending_count = sum(1 for r in reports if r.status == "pending")
		mastered_count = sum(1 for r in reports if r.status == "mastered")

		tag_counter: Counter[str] = Counter()
		error_counter: Counter[str] = Counter()
		weak_points_map: dict[str, MistakeUserProfileWeakPoint] = {}
		independence_levels: list[str] = []

		for r in reports:
			for tag in r.knowledge_tags:
				if tag.strip():
					tag_counter[tag] += 1
			if r.primary_error_type and r.primary_error_type != "待分析":
				error_counter[r.primary_error_type] += 1
			if r.independence_level:
				independence_levels.append(r.independence_level)

			for tag in r.knowledge_tags:
				tag_stripped = tag.strip()
				if not tag_stripped:
					continue
				if tag_stripped not in weak_points_map:
					weak_points_map[tag_stripped] = MistakeUserProfileWeakPoint(
						knowledge_tag=tag_stripped,
						error_types=[],
						frequency=0,
						latest_report_ids=[],
					)
				wp = weak_points_map[tag_stripped]
				wp.frequency += 1
				if r.primary_error_type and r.primary_error_type != "待分析" and r.primary_error_type not in wp.error_types:
					wp.error_types.append(r.primary_error_type)
				if r.report_id not in wp.latest_report_ids:
					wp.latest_report_ids.append(r.report_id)
					if len(wp.latest_report_ids) > 5:
						wp.latest_report_ids = wp.latest_report_ids[-5:]

		weak_knowledge_tags = [tag for tag, _ in tag_counter.most_common(10)]
		weak_points = sorted(weak_points_map.values(), key=lambda wp: wp.frequency, reverse=True)
		error_type_distribution = dict(error_counter.most_common())

		independence_summary = self._build_independence_summary(independence_levels)
		learning_style_hints = self._build_learning_style_hints(weak_points, error_counter)

		return MistakeUserProfileData(
			user_id=user_id,
			total_reports=total,
			pending_count=pending_count,
			mastered_count=mastered_count,
			weak_knowledge_tags=weak_knowledge_tags,
			weak_points=weak_points,
			error_type_distribution=error_type_distribution,
			independence_summary=independence_summary,
			learning_style_hints=learning_style_hints,
			updated_at=datetime.now(timezone.utc).isoformat(),
		)

	def _incremental_update_profile(self, existing: MistakeUserProfileData, new_report: MistakeReportData) -> MistakeUserProfileData:
		has_new_error = False
		new_error_types: list[str] = []
		for ep in new_report.report.error_profile:
			if ep.error_type.strip() and ep.error_type.strip() not in existing.error_type_distribution:
				has_new_error = True
				new_error_types.append(ep.error_type.strip())

		for tag in new_report.knowledge_tags:
			tag_stripped = tag.strip()
			if not tag_stripped:
				continue
			if tag_stripped not in existing.weak_knowledge_tags:
				has_new_error = True

		if not has_new_error and new_report.primary_error_type in existing.error_type_distribution:
			updated_profile = existing.model_copy(update={
				"total_reports": existing.total_reports + 1,
				"pending_count": existing.pending_count + (1 if new_report.status == "pending" else 0),
				"mastered_count": existing.mastered_count + (1 if new_report.status == "mastered" else 0),
				"updated_at": datetime.now(timezone.utc).isoformat(),
			})
			return updated_profile

		result = self._multi_agent.update_user_profile(
			existing_profile=existing.model_dump(),
			new_report_payload=new_report.report.model_dump(),
		)
		updated_profile_data = result.get("profile", existing.model_dump())
		if not isinstance(updated_profile_data, dict):
			updated_profile_data = existing.model_dump()

		try:
			profile = MistakeUserProfileData.model_validate(updated_profile_data)
			profile = profile.model_copy(update={
				"user_id": existing.user_id,
				"total_reports": existing.total_reports + 1,
				"pending_count": existing.pending_count + (1 if new_report.status == "pending" else 0),
				"mastered_count": existing.mastered_count + (1 if new_report.status == "mastered" else 0),
				"updated_at": datetime.now(timezone.utc).isoformat(),
			})
			return profile
		except Exception:
			return self._build_profile_from_reports(existing.user_id, self._select_reports(user_id=existing.user_id))

	def _build_independence_summary(self, levels: list[str]) -> str:
		if not levels:
			return "暂无数据"
		high = sum(1 for lv in levels if lv in ("高", "独立完成", "high"))
		mid = sum(1 for lv in levels if lv in ("中", "部分独立", "medium"))
		low = sum(1 for lv in levels if lv in ("低", "需要帮助", "low"))
		total = len(levels)
		if high / total >= 0.6:
			return f"独立完成度较高（{high}/{total}次独立完成），但仍有薄弱环节需巩固"
		if mid / total >= 0.5:
			return f"独立完成度中等（{mid}/{total}次部分独立），建议加强关键步骤的练习"
		return f"独立完成度偏低（{low}/{total}次需要帮助），建议从基础概念开始复习"

	def _build_learning_style_hints(self, weak_points: list[MistakeUserProfileWeakPoint], error_counter: Counter[str]) -> list[str]:
		hints: list[str] = []
		if weak_points and weak_points[0].frequency >= 3:
			hints.append(f"「{weak_points[0].knowledge_tag}」反复出错，建议从基础概念重新梳理")
		common_errors = error_counter.most_common(3)
		for error_type, count in common_errors:
			if count >= 2:
				hints.append(f"「{error_type}」类错误频繁出现，注意区分易混淆概念")
		if len(weak_points) > 3:
			hints.append("薄弱知识点较多，建议按优先级逐个突破，不要同时攻太多")
		if not hints:
			hints.append("继续保持良好的学习习惯，定期回顾错题")
		return hints[:5]

	def start_dialogue_session(self, request: MistakeDialogueSessionRequest) -> MistakeDialogueSessionData:
		report = self.get_report(request.report_id)
		if request.user_id and report.user_id != request.user_id:
			raise ValueError(f"无权访问该错题报告：{request.report_id}")

		effective_user_id = request.user_id or report.user_id
		similar_question = None
		if effective_user_id:
			try:
				rec_request = MistakeRecommendationGenerateRequest(
					report_id=request.report_id,
					user_id=effective_user_id,
					recommendation_type="similar",
				)
				similar_question = self.generate_recommendation(rec_request)
			except Exception:
				similar_question = None

		report_payload = report.report.model_dump()
		similar_dict = similar_question.model_dump() if similar_question else None

		listener_result = self._multi_agent.dialogue_listener_turn(
			report_payload=report_payload,
			similar_question=similar_dict,
			dialogue_history=None,
		)

		now = datetime.now(timezone.utc).isoformat()
		messages = [
			MistakeDialogueMessage(
				role="assistant",
				content=listener_result["reply"],
				timestamp=now,
			)
		]

		session = MistakeDialogueSessionData(
			session_id=f"dlg_{uuid4().hex[:12]}",
			report_id=report.report_id,
			user_id=effective_user_id,
			report_title=report.report_title,
			problem_text=report.report.problem.raw_problem,
			is_completed=False,
			mastery_verdict="in_progress",
			mastery_detail="",
			messages=messages,
			similar_question=similar_question,
			agent_traces=[
				MistakeAgentTrace(agent="listener", status="success", summary=f"开场引导：{listener_result['topic_focus']}"),
			],
			created_at=now,
			updated_at=now,
		)
		self._dialogue_sessions[session.session_id] = session.model_copy(deep=True)
		if self._persist_runtime_state:
			save_dialogue_session(session)
		return session

	def get_dialogue_session(self, session_id: str) -> MistakeDialogueSessionData:
		session = self._dialogue_sessions.get(session_id)
		if session is not None:
			return session
		if not self._persist_runtime_state:
			raise KeyError(f"对话会话不存在：{session_id}")
		stored = load_dialogue_session(session_id)
		if stored is None:
			raise KeyError(f"对话会话不存在：{session_id}")
		self._dialogue_sessions[session_id] = stored
		return stored

	def advance_dialogue_session(self, session_id: str, request: MistakeDialogueSessionTurnRequest) -> MistakeDialogueSessionData:
		session = self._dialogue_sessions.get(session_id)
		if session is None:
			if not self._persist_runtime_state:
				raise KeyError(f"对话会话不存在：{session_id}")
			stored = load_dialogue_session(session_id)
			if stored is None:
				raise KeyError(f"对话会话不存在：{session_id}")
			session = stored
			self._dialogue_sessions[session_id] = session
		if session.is_completed:
			return session

		report = self.get_report(session.report_id)
		report_payload = report.report.model_dump()
		similar_dict = session.similar_question.model_dump() if session.similar_question else None

		now = datetime.now(timezone.utc).isoformat()
		history = list(session.messages)
		history.append(
			MistakeDialogueMessage(
				role="user",
				content=request.message.strip(),
				timestamp=now,
			)
		)

		dialogue_history = [{"role": msg.role, "content": msg.content} for msg in history]

		listener_result = self._multi_agent.dialogue_listener_turn(
			report_payload=report_payload,
			similar_question=similar_dict,
			dialogue_history=dialogue_history,
		)

		history.append(
			MistakeDialogueMessage(
				role="assistant",
				content=listener_result["reply"],
				timestamp=now,
			)
		)

		is_completed = not listener_result["should_continue"]
		mastery_verdict: str = session.mastery_verdict
		mastery_detail = session.mastery_detail

		if is_completed or listener_result["mastery_signal"] >= 0.8 or (len(history) >= 10 and listener_result["mastery_signal"] >= 0.6):
			judgment = self._multi_agent.judge_dialogue_mastery(
				report_payload=report_payload,
				dialogue_history=dialogue_history,
				similar_question=similar_dict,
			)
			mastery_verdict = judgment["verdict"]
			mastery_detail = judgment["detail"]
			if mastery_verdict != "in_progress":
				is_completed = True
			if mastery_verdict == "mastered" and report.status == "pending":
				self.update_status(session.report_id, MistakeReportStatusUpdateRequest(status="mastered"))

		agent_traces = list(session.agent_traces)
		agent_traces.append(
			MistakeAgentTrace(
				agent="listener",
				status="success",
				summary=f"追问：{listener_result['topic_focus']}" if listener_result["is_probing"] else f"回应：{listener_result['topic_focus']}",
			)
		)

		updated = session.model_copy(update={
			"messages": history,
			"is_completed": is_completed,
			"mastery_verdict": mastery_verdict,
			"mastery_detail": mastery_detail,
			"agent_traces": agent_traces,
			"updated_at": now,
		})
		self._dialogue_sessions[session_id] = updated
		if self._persist_runtime_state:
			save_dialogue_session(updated)
		return updated


__all__ = ["MistakeService"]
