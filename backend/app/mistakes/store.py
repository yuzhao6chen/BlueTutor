from __future__ import annotations

import json
import logging
from pathlib import Path

from .schema import MistakeDialogueSessionData, MistakeRedoSessionData, MistakeRecommendationData, MistakeReportData, MistakeUserProfileData

logger = logging.getLogger(__name__)

_REPORTS_DIR = Path(__file__).parent / "data" / "reports"
_REPORTS_INDEX = _REPORTS_DIR / "index.json"

_REDO_DIR = Path(__file__).parent / "data" / "redo_sessions"
_REDO_INDEX = _REDO_DIR / "index.json"

_REC_DIR = Path(__file__).parent / "data" / "recommendations"
_REC_INDEX = _REC_DIR / "index.json"

_PROFILE_DIR = Path(__file__).parent / "data" / "profiles"
_PROFILE_INDEX = _PROFILE_DIR / "index.json"

_DIALOGUE_DIR = Path(__file__).parent / "data" / "dialogue_sessions"
_DIALOGUE_INDEX = _DIALOGUE_DIR / "index.json"


def _ensure_dir(path: Path) -> None:
	path.mkdir(parents=True, exist_ok=True)


def _read_json_index(index_file: Path) -> dict[str, dict]:
	if not index_file.exists():
		return {}
	try:
		with index_file.open("r", encoding="utf-8") as file:
			data = json.load(file)
		return data if isinstance(data, dict) else {}
	except (OSError, ValueError) as exc:
		logger.error("读取索引失败 [%s]：%s", index_file, exc)
		return {}


def _write_json_index(index_file: Path, index: dict[str, dict]) -> None:
	_ensure_dir(index_file.parent)
	with index_file.open("w", encoding="utf-8") as file:
		json.dump(index, file, ensure_ascii=False, indent=2)


def save_report(report: MistakeReportData) -> None:
	_ensure_dir(_REPORTS_DIR)
	report_path = _REPORTS_DIR / f"{report.report_id}.json"
	with report_path.open("w", encoding="utf-8") as file:
		json.dump(report.model_dump(), file, ensure_ascii=False, indent=2)

	index = _read_json_index(_REPORTS_INDEX)
	index[report.report_id] = {
		"report_id": report.report_id,
		"user_id": report.user_id,
		"report_title": report.report_title,
		"status": report.status,
		"primary_error_type": report.primary_error_type,
		"independence_level": report.independence_level,
		"created_at": report.created_at,
		"problem_preview": report.problem_preview,
		"knowledge_tags": report.knowledge_tags,
		"has_solution": report.has_solution,
		"solution_preview": report.solution_preview,
	}
	_write_json_index(_REPORTS_INDEX, index)


def load_report(report_id: str) -> MistakeReportData | None:
	report_path = _REPORTS_DIR / f"{report_id}.json"
	if not report_path.exists():
		return None
	try:
		with report_path.open("r", encoding="utf-8") as file:
			data = json.load(file)
		return MistakeReportData.model_validate(data)
	except (OSError, ValueError) as exc:
		logger.error("读取错题报告失败 [%s]：%s", report_id, exc)
		return None


def list_reports() -> list[MistakeReportData]:
	index = _read_json_index(_REPORTS_INDEX)
	reports: list[MistakeReportData] = []
	for report_id in index.keys():
		report = load_report(report_id)
		if report is not None:
			reports.append(report)
	reports.sort(key=lambda item: item.created_at, reverse=True)
	return reports


def save_redo_session(session: MistakeRedoSessionData) -> None:
	_ensure_dir(_REDO_DIR)
	session_path = _REDO_DIR / f"{session.session_id}.json"
	with session_path.open("w", encoding="utf-8") as file:
		json.dump(session.model_dump(), file, ensure_ascii=False, indent=2)

	index = _read_json_index(_REDO_INDEX)
	index[session.session_id] = {
		"session_id": session.session_id,
		"session_type": session.session_type,
		"report_id": session.report_id,
		"recommendation_id": session.recommendation_id,
		"user_id": session.user_id,
		"report_title": session.report_title,
		"stage": session.stage,
		"is_completed": session.is_completed,
		"updated_at": session.updated_at,
	}
	_write_json_index(_REDO_INDEX, index)


def load_redo_session(session_id: str) -> MistakeRedoSessionData | None:
	session_path = _REDO_DIR / f"{session_id}.json"
	if not session_path.exists():
		return None
	try:
		with session_path.open("r", encoding="utf-8") as file:
			data = json.load(file)
		return MistakeRedoSessionData.model_validate(data)
	except (OSError, ValueError) as exc:
		logger.error("读取重做会话失败 [%s]：%s", session_id, exc)
		return None


def list_redo_sessions() -> list[MistakeRedoSessionData]:
	index = _read_json_index(_REDO_INDEX)
	sessions: list[MistakeRedoSessionData] = []
	for session_id in index.keys():
		session = load_redo_session(session_id)
		if session is not None:
			sessions.append(session)
	sessions.sort(key=lambda item: item.updated_at, reverse=True)
	return sessions


def save_recommendation(rec: MistakeRecommendationData) -> None:
	_ensure_dir(_REC_DIR)
	rec_path = _REC_DIR / f"{rec.recommendation_id}.json"
	with rec_path.open("w", encoding="utf-8") as file:
		json.dump(rec.model_dump(), file, ensure_ascii=False, indent=2)

	index = _read_json_index(_REC_INDEX)
	index[rec.recommendation_id] = {
		"recommendation_id": rec.recommendation_id,
		"origin_report_id": rec.origin_report_id,
		"user_id": "",
		"recommendation_type": rec.recommendation_type,
		"title": rec.title,
		"difficulty": rec.difficulty,
		"generated_at": rec.generated_at,
	}
	_write_json_index(_REC_INDEX, index)


def load_recommendation(recommendation_id: str) -> MistakeRecommendationData | None:
	rec_path = _REC_DIR / f"{recommendation_id}.json"
	if not rec_path.exists():
		return None
	try:
		with rec_path.open("r", encoding="utf-8") as file:
			data = json.load(file)
		return MistakeRecommendationData.model_validate(data)
	except (OSError, ValueError) as exc:
		logger.error("读取推荐题失败 [%s]：%s", recommendation_id, exc)
		return None


def list_recommendations() -> list[MistakeRecommendationData]:
	index = _read_json_index(_REC_INDEX)
	recommendations: list[MistakeRecommendationData] = []
	for rec_id in index.keys():
		rec = load_recommendation(rec_id)
		if rec is not None:
			recommendations.append(rec)
	recommendations.sort(key=lambda item: item.generated_at, reverse=True)
	return recommendations


def save_user_profile(profile: MistakeUserProfileData) -> None:
	_ensure_dir(_PROFILE_DIR)
	profile_path = _PROFILE_DIR / f"{profile.user_id}.json"
	with profile_path.open("w", encoding="utf-8") as file:
		json.dump(profile.model_dump(), file, ensure_ascii=False, indent=2)

	index = _read_json_index(_PROFILE_INDEX)
	index[profile.user_id] = {
		"user_id": profile.user_id,
		"total_reports": profile.total_reports,
		"pending_count": profile.pending_count,
		"mastered_count": profile.mastered_count,
		"updated_at": profile.updated_at,
	}
	_write_json_index(_PROFILE_INDEX, index)


def load_user_profile(user_id: str) -> MistakeUserProfileData | None:
	profile_path = _PROFILE_DIR / f"{user_id}.json"
	if not profile_path.exists():
		return None
	try:
		with profile_path.open("r", encoding="utf-8") as file:
			data = json.load(file)
		return MistakeUserProfileData.model_validate(data)
	except (OSError, ValueError) as exc:
		logger.error("读取用户画像失败 [%s]：%s", user_id, exc)
		return None


def save_dialogue_session(session: MistakeDialogueSessionData) -> None:
	_ensure_dir(_DIALOGUE_DIR)
	session_path = _DIALOGUE_DIR / f"{session.session_id}.json"
	with session_path.open("w", encoding="utf-8") as file:
		json.dump(session.model_dump(), file, ensure_ascii=False, indent=2)

	index = _read_json_index(_DIALOGUE_INDEX)
	index[session.session_id] = {
		"session_id": session.session_id,
		"report_id": session.report_id,
		"user_id": session.user_id,
		"report_title": session.report_title,
		"is_completed": session.is_completed,
		"mastery_verdict": session.mastery_verdict,
		"updated_at": session.updated_at,
	}
	_write_json_index(_DIALOGUE_INDEX, index)


def load_dialogue_session(session_id: str) -> MistakeDialogueSessionData | None:
	session_path = _DIALOGUE_DIR / f"{session_id}.json"
	if not session_path.exists():
		return None
	try:
		with session_path.open("r", encoding="utf-8") as file:
			data = json.load(file)
		return MistakeDialogueSessionData.model_validate(data)
	except (OSError, ValueError) as exc:
		logger.error("读取对话会话失败 [%s]：%s", session_id, exc)
		return None


def list_dialogue_sessions() -> list[MistakeDialogueSessionData]:
	index = _read_json_index(_DIALOGUE_INDEX)
	sessions: list[MistakeDialogueSessionData] = []
	for session_id in index.keys():
		session = load_dialogue_session(session_id)
		if session is not None:
			sessions.append(session)
	sessions.sort(key=lambda item: item.updated_at, reverse=True)
	return sessions


__all__ = [
	"save_report",
	"load_report",
	"list_reports",
	"save_redo_session",
	"load_redo_session",
	"list_redo_sessions",
	"save_recommendation",
	"load_recommendation",
	"list_recommendations",
	"save_user_profile",
	"load_user_profile",
	"save_dialogue_session",
	"load_dialogue_session",
	"list_dialogue_sessions",
]
