from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Query, status
from fastapi.responses import JSONResponse

from ..mistakes.schema import (
	ApiResponse,
	MistakeDailyPlanResponse,
	MistakeDialogueSessionRequest,
	MistakeDialogueSessionResponse,
	MistakeDialogueSessionTurnRequest,
	MistakeHomeSummaryResponse,
	MistakeLectureResponse,
	MistakeRecommendationGenerateRequest,
	MistakeRecommendationResponse,
	MistakeRecommendationStartRedoRequest,
	MistakeRecommendationSubmitRequest,
	MistakeRecommendationSubmitResponse,
	MistakeRedoSessionRequest,
	MistakeRedoSessionResponse,
	MistakeRedoSessionTurnRequest,
	MistakeReportDetailResponse,
	MistakeReportIngestRequest,
	MistakeReportIngestResponse,
	MistakeReportListResponse,
	MistakeReportStatusUpdateRequest,
	MistakeStatsResponse,
	MistakeTimelineResponse,
	MistakeUserProfileResponse,
)
from ..mistakes.service import MistakeService


mistakes_router = APIRouter(prefix="/api/mistakes", tags=["mistakes"])
_mistake_service: MistakeService | None = None


def get_mistake_service() -> MistakeService:
	global _mistake_service
	if _mistake_service is None:
		_mistake_service = MistakeService()
	return _mistake_service


@mistakes_router.get("/health")
def mistakes_health() -> ApiResponse[dict[str, str]]:
	return ApiResponse(data={"status": "ok", "module": "mistakes"})


@mistakes_router.post("/report/ingest", response_model=MistakeReportIngestResponse)
def ingest_mistake_report(request: MistakeReportIngestRequest) -> MistakeReportIngestResponse | JSONResponse:
	try:
		data = get_mistake_service().ingest_report(request)
		return MistakeReportIngestResponse(data=data)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4101,
			message=str(exc),
		)


@mistakes_router.get("/reports", response_model=MistakeReportListResponse)
def list_mistake_reports(
	user_id: str | None = Query(default=None),
	status: str | None = Query(default=None),
	knowledge_tag: str | None = Query(default=None),
	has_solution: bool | None = Query(default=None),
) -> MistakeReportListResponse:
	data = get_mistake_service().list_reports(
		user_id=user_id,
		status=status,
		knowledge_tag=knowledge_tag,
		has_solution=has_solution,
	)
	return MistakeReportListResponse(data=data)


@mistakes_router.get("/reports/{report_id}", response_model=MistakeReportDetailResponse)
def get_mistake_report(report_id: str) -> MistakeReportDetailResponse | JSONResponse:
	try:
		data = get_mistake_service().get_report(report_id)
		return MistakeReportDetailResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4104,
			message=str(exc),
		)


@mistakes_router.get("/reports/{report_id}/lecture", response_model=MistakeLectureResponse)
def get_mistake_report_lecture(report_id: str) -> MistakeLectureResponse | JSONResponse:
	try:
		data = get_mistake_service().get_lecture(report_id)
		return MistakeLectureResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4106,
			message=str(exc),
		)


@mistakes_router.patch("/reports/{report_id}/status", response_model=MistakeReportDetailResponse)
def update_mistake_report_status(
	report_id: str,
	request: MistakeReportStatusUpdateRequest,
) -> MistakeReportDetailResponse | JSONResponse:
	try:
		data = get_mistake_service().update_status(report_id, request)
		return MistakeReportDetailResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4105,
			message=str(exc),
		)


@mistakes_router.post("/redo-sessions/start", response_model=MistakeRedoSessionResponse)
def start_mistake_redo_session(
	request: MistakeRedoSessionRequest,
) -> MistakeRedoSessionResponse | JSONResponse:
	try:
		data = get_mistake_service().start_redo_session(request)
		return MistakeRedoSessionResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4109,
			message=str(exc),
		)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4110,
			message=str(exc),
		)


@mistakes_router.get("/redo-sessions/{session_id}", response_model=MistakeRedoSessionResponse)
def get_mistake_redo_session(session_id: str) -> MistakeRedoSessionResponse | JSONResponse:
	try:
		data = get_mistake_service().get_redo_session(session_id)
		return MistakeRedoSessionResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4116,
			message=str(exc),
		)


@mistakes_router.post("/redo-sessions/{session_id}/turn", response_model=MistakeRedoSessionResponse)
def advance_mistake_redo_session(
	session_id: str,
	request: MistakeRedoSessionTurnRequest,
) -> MistakeRedoSessionResponse | JSONResponse:
	try:
		data = get_mistake_service().advance_redo_session(session_id, request)
		return MistakeRedoSessionResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4111,
			message=str(exc),
		)


@mistakes_router.post("/recommendations/{recommendation_id}/start-redo", response_model=MistakeRedoSessionResponse)
def start_recommendation_redo_session(
	recommendation_id: str,
	request: MistakeRecommendationStartRedoRequest,
) -> MistakeRedoSessionResponse | JSONResponse:
	try:
		data = get_mistake_service().start_redo_session_from_recommendation(recommendation_id, request)
		return MistakeRedoSessionResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4112,
			message=str(exc),
		)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4113,
			message=str(exc),
		)


@mistakes_router.post("/recommendations/{recommendation_id}/submit", response_model=MistakeRecommendationSubmitResponse)
def submit_recommendation_answer(
	recommendation_id: str,
	request: MistakeRecommendationSubmitRequest,
) -> MistakeRecommendationSubmitResponse | JSONResponse:
	try:
		data = get_mistake_service().submit_recommendation_answer(recommendation_id, request)
		return MistakeRecommendationSubmitResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4114,
			message=str(exc),
		)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4115,
			message=str(exc),
		)


@mistakes_router.post("/recommendations/generate", response_model=MistakeRecommendationResponse)
def generate_mistake_recommendation(
	request: MistakeRecommendationGenerateRequest,
) -> MistakeRecommendationResponse | JSONResponse:
	try:
		data = get_mistake_service().generate_recommendation(request)
		return MistakeRecommendationResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4107,
			message=str(exc),
		)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4108,
			message=str(exc),
		)


@mistakes_router.get("/stats", response_model=MistakeStatsResponse)
def get_mistake_stats(user_id: str | None = Query(default=None)) -> MistakeStatsResponse:
	data = get_mistake_service().get_stats(user_id=user_id)
	return MistakeStatsResponse(data=data)


@mistakes_router.get("/timeline", response_model=MistakeTimelineResponse)
def get_mistake_timeline(
	user_id: str | None = Query(default=None),
	limit: int | None = Query(default=None, ge=1),
	status: str | None = Query(default=None),
	knowledge_tag: str | None = Query(default=None),
	has_solution: bool | None = Query(default=None),
) -> MistakeTimelineResponse:
	data = get_mistake_service().get_timeline(
		user_id=user_id,
		limit=limit,
		status=status,
		knowledge_tag=knowledge_tag,
		has_solution=has_solution,
	)
	return MistakeTimelineResponse(data=data)


@mistakes_router.get("/daily-plan", response_model=MistakeDailyPlanResponse)
def get_mistake_daily_plan(
	user_id: str | None = Query(default=None),
	limit: int = Query(default=3, ge=1, le=10),
) -> MistakeDailyPlanResponse:
	data = get_mistake_service().get_daily_plan(user_id=user_id, limit=limit)
	return MistakeDailyPlanResponse(data=data)


@mistakes_router.get("/home-summary", response_model=MistakeHomeSummaryResponse)
def get_mistake_home_summary(user_id: str | None = Query(default=None)) -> MistakeHomeSummaryResponse:
	data = get_mistake_service().get_home_summary(user_id=user_id)
	return MistakeHomeSummaryResponse(data=data)


@mistakes_router.get("/profiles/{user_id}", response_model=MistakeUserProfileResponse)
def get_user_profile(user_id: str) -> MistakeUserProfileResponse | JSONResponse:
	try:
		data = get_mistake_service().build_user_profile(user_id)
		return MistakeUserProfileResponse(data=data)
	except Exception as exc:
		return _error_response(
			status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
			code=4120,
			message=str(exc),
		)


@mistakes_router.post("/dialogue-sessions/start", response_model=MistakeDialogueSessionResponse)
def start_dialogue_session(
	request: MistakeDialogueSessionRequest,
) -> MistakeDialogueSessionResponse | JSONResponse:
	try:
		data = get_mistake_service().start_dialogue_session(request)
		return MistakeDialogueSessionResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4121,
			message=str(exc),
		)
	except ValueError as exc:
		return _error_response(
			status_code=status.HTTP_400_BAD_REQUEST,
			code=4122,
			message=str(exc),
		)


@mistakes_router.get("/dialogue-sessions/{session_id}", response_model=MistakeDialogueSessionResponse)
def get_dialogue_session(session_id: str) -> MistakeDialogueSessionResponse | JSONResponse:
	try:
		data = get_mistake_service().get_dialogue_session(session_id)
		return MistakeDialogueSessionResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4123,
			message=str(exc),
		)


@mistakes_router.post("/dialogue-sessions/{session_id}/turn", response_model=MistakeDialogueSessionResponse)
def advance_dialogue_session(
	session_id: str,
	request: MistakeDialogueSessionTurnRequest,
) -> MistakeDialogueSessionResponse | JSONResponse:
	try:
		data = get_mistake_service().advance_dialogue_session(session_id, request)
		return MistakeDialogueSessionResponse(data=data)
	except KeyError as exc:
		return _error_response(
			status_code=status.HTTP_404_NOT_FOUND,
			code=4124,
			message=str(exc),
		)


def _error_response(*, status_code: int, code: int, message: str) -> JSONResponse:
	payload: dict[str, Any] = {
		"code": code,
		"message": message,
		"data": None,
	}
	return JSONResponse(status_code=status_code, content=payload)


__all__ = ["mistakes_router", "get_mistake_service"]
