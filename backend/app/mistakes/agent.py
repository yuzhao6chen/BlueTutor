from __future__ import annotations

import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from .prompt import (
    build_dialogue_listener_prompt,
    build_dialogue_mastery_prompt,
    build_problem_analysis_prompt,
    build_profile_aware_analysis_prompt,
    build_profile_aware_variant_prompt,
    build_profile_update_prompt,
    build_quality_review_prompt,
    build_redo_evaluation_prompt,
    build_redo_hint_prompt,
    build_redo_option_prompt,
    build_redo_plan_prompt,
    build_variant_generation_prompt,
)
from .schema import MistakeAgentTrace, MistakeRecommendationData, MistakeRecommendationOption, MistakeRedoOption

DEFAULT_ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
DEFAULT_BASE_URL = "https://api-ai.vivo.com.cn/vivogpt/completions"
DEFAULT_MODEL_NAME = "vivo-BlueLM-TB"
VALID_STAGES = {"understand_problem", "identify_first_step", "solve", "final_check", "completed"}
VALID_MODES = {"single_choice", "free_text"}
VALID_RESULTS = {"correct", "partial", "incorrect"}

_STAGE_ORDER = ["understand_problem", "identify_first_step", "solve", "final_check", "completed"]


def _next_stage(current: str) -> str:
    idx = _STAGE_ORDER.index(current) if current in _STAGE_ORDER else 0
    return _STAGE_ORDER[min(idx + 1, len(_STAGE_ORDER) - 1)]


class MistakeMultiAgent:
    def __init__(self, *, base_url: str | None = None, api_key: str | None = None, app_id: str | None = None, model_name: str | None = None, timeout_seconds: int | None = None, max_retries: int | None = None, env_path: Path | None = None) -> None:
        self.env_path = env_path or DEFAULT_ENV_PATH
        _load_env_file(self.env_path)
        self.base_url = (
            base_url
            or os.getenv("MISTAKES_LLM_BASE_URL")
            or os.getenv("LLM_BASE_URL")
            or DEFAULT_BASE_URL
        )
        self.api_key = (
            api_key
            or os.getenv("VIVO_APP_KEY")
            or os.getenv("MISTAKES_LLM_API_KEY")
            or os.getenv("LLM_API_KEY")
        )
        self.app_id = (
            app_id
            or os.getenv("VIVO_APP_ID")
            or os.getenv("MISTAKES_LLM_APP_ID")
            or os.getenv("LLM_APP_ID")
        )
        self.model_name = (
            model_name
            or os.getenv("MISTAKES_LLM_MODEL_NAME")
            or os.getenv("LLM_MODEL_NAME")
            or DEFAULT_MODEL_NAME
        )
        self.timeout_seconds = timeout_seconds or int(
            os.getenv("MISTAKES_LLM_TIMEOUT_SECONDS")
            or os.getenv("LLM_TIMEOUT_SECONDS")
            or "60"
        )
        self.max_retries = max_retries or int(
            os.getenv("MISTAKES_LLM_MAX_RETRIES")
            or os.getenv("LLM_MAX_RETRIES")
            or "1"
        )

    def generate_recommendation(self, *, report_payload: dict[str, Any], origin_report_id: str, recommendation_type: str) -> MistakeRecommendationData:
        traces: list[MistakeAgentTrace] = []
        analysis = self._call(build_problem_analysis_prompt(report_payload, recommendation_type), self._fallback_analysis(report_payload))
        traces.append(MistakeAgentTrace(agent="problem-analyzer", status="success", summary=str(analysis.get("recommendation_goal") or "分析完成")))
        candidate = self._call(build_variant_generation_prompt(analysis, report_payload, recommendation_type), self._fallback_candidate(report_payload, analysis, recommendation_type))
        traces.append(MistakeAgentTrace(agent="variant-generator", status="success", summary=str(candidate.get("title") or "生成候选题")))
        reviewed = self._call(build_quality_review_prompt(candidate, analysis), {"normalized_result": self._normalize_candidate(candidate)})
        final_data = reviewed.get("normalized_result") if isinstance(reviewed.get("normalized_result"), dict) else candidate
        final_data = self._normalize_candidate(final_data)
        traces.append(MistakeAgentTrace(agent="quality-reviewer", status="success", summary="质量审核完成"))
        return MistakeRecommendationData(
            recommendation_id=f"rec_{origin_report_id[-8:]}_{recommendation_type}",
            origin_report_id=origin_report_id,
            recommendation_type=recommendation_type,
            title=str(final_data.get("title") or "推荐题"),
            difficulty=self._normalize_difficulty(final_data.get("difficulty")),
            question=str(final_data.get("question") or ""),
            options=[MistakeRecommendationOption(id=item["id"], text=item["text"]) for item in final_data.get("options", [])],
            correct_option_id=str(final_data.get("correct_option_id") or ""),
            answer=str(final_data.get("answer") or ""),
            explanation=str(final_data.get("explanation") or ""),
            knowledge_tags=[str(item).strip() for item in final_data.get("knowledge_tags", []) if str(item).strip()],
            why_recommended=str(final_data.get("why_recommended") or ""),
            agent_traces=traces,
            generated_at=datetime.now(timezone.utc).isoformat(),
        )

    def plan_redo_turn(
        self,
        *,
        report_payload: dict[str, Any],
        stage: str,
        history: list[dict[str, Any]],
        hint_level: int = 1,
    ) -> dict[str, Any]:
        traces: list[MistakeAgentTrace] = []
        planning = self._normalize_redo_plan(self._call(build_redo_plan_prompt(report_payload, stage, history), self._fallback_redo_plan(stage)))
        traces.append(MistakeAgentTrace(agent="pedagogy-planner", status="success", summary=str(planning.get("teaching_goal") or "规划引导步骤")))
        options: list[MistakeRedoOption] = []
        correct_option_id = ""
        if planning["interaction_mode"] == "single_choice":
            option_result = self._normalize_redo_options(self._call(build_redo_option_prompt(report_payload, planning), self._fallback_redo_options(planning["stage"])))
            options = [MistakeRedoOption(id=item["id"], text=item["text"]) for item in option_result["options"]]
            correct_option_id = option_result["correct_option_id"]
            traces.append(MistakeAgentTrace(agent="option-generator", status="success", summary="生成选项"))
        hint_payload = self._call(build_redo_hint_prompt(report_payload, planning, history, hint_level=hint_level), {"hint": self._fallback_redo_hint(planning["stage"], hint_level)})
        hint = str(hint_payload.get("hint") or self._fallback_redo_hint(planning["stage"], hint_level)).strip()
        traces.append(MistakeAgentTrace(agent="hint-generator", status="success", summary=f"生成提示（等级{hint_level}）"))
        return {"planning": planning, "options": options, "correct_option_id": correct_option_id, "hint": hint, "agent_traces": traces}

    def evaluate_redo_turn(
        self,
        *,
        report_payload: dict[str, Any],
        planning: dict[str, Any],
        answer: str,
        correct_option_id: str = "",
        consecutive_correct: int = 0,
        required_consecutive_correct: int = 1,
    ) -> dict[str, Any]:
        traces: list[MistakeAgentTrace] = []
        result = self._normalize_redo_evaluation(
            self._call(
                build_redo_evaluation_prompt(report_payload, planning, answer, consecutive_correct, required_consecutive_correct),
                self._fallback_redo_evaluation(report_payload, planning, answer, correct_option_id, consecutive_correct, required_consecutive_correct),
            )
        )
        traces.append(MistakeAgentTrace(agent="response-evaluator", status="success", summary=f"评估结果：{result['result']}"))
        result["agent_traces"] = traces
        return result

    def _call(self, prompt: str, fallback: dict[str, Any]) -> dict[str, Any]:
        if not self.api_key:
            return fallback
        for _ in range(self.max_retries + 1):
            try:
                return self._parse(self._send(prompt))
            except Exception:
                continue
        return fallback

    def _send(self, prompt: str) -> str:
        payload = {"model": self.model_name, "messages": [{"role": "user", "content": prompt}], "temperature": 0.3, "stream": False}
        headers = {"Content-Type": "application/json"}
        if self.app_id:
            headers["X-App-Id"] = self.app_id
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        request = Request(url=self.base_url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
        try:
            with urlopen(request, timeout=self.timeout_seconds) as response:
                body = response.read().decode("utf-8")
        except HTTPError as exc:
            raise RuntimeError(exc.read().decode("utf-8", errors="ignore")) from exc
        except URLError as exc:
            raise RuntimeError(str(exc.reason)) from exc
        data = json.loads(body)
        if isinstance(data, dict) and "choices" in data:
            return data["choices"][0]["message"]["content"]
        if isinstance(data, dict) and isinstance(data.get("data"), dict) and "content" in data["data"]:
            return str(data["data"]["content"])
        raise RuntimeError("Unexpected LLM response structure")

    def _parse(self, raw: str) -> dict[str, Any]:
        text = raw.strip()
        if text.startswith("```"):
            text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.IGNORECASE)
            text = re.sub(r"\s*```$", "", text)
        try:
            parsed = json.loads(text)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", text, re.DOTALL)
            if not match:
                raise ValueError("No valid JSON object found")
            parsed = json.loads(match.group(0))
        if not isinstance(parsed, dict):
            raise ValueError("JSON root must be object")
        return parsed

    def _fallback_analysis(self, report_payload: dict[str, Any]) -> dict[str, Any]:
        tags = report_payload.get("knowledge_tags", []) or []
        errors = report_payload.get("error_profile", []) or []
        primary = errors[0]["error_type"] if errors else "未分类"
        return {"problem_type": tags[0] if tags else "综合", "knowledge_tags": tags[:3], "method_pattern": "根据已知条件逐步推导", "error_focus": primary, "variant_axes": ["数值变化", "情境变化"], "recommendation_goal": f"针对{primary}生成练习题"}

    def _fallback_candidate(self, report_payload: dict[str, Any], analysis: dict[str, Any], recommendation_type: str) -> dict[str, Any]:
        tags = analysis.get("knowledge_tags") or report_payload.get("knowledge_tags") or ["综合"]
        return {"title": "相似练习题" if recommendation_type == "similar" else "变式练习题", "difficulty": "easy", "question": f"与原题相关的{tags[0]}练习题", "options": [{"id": "A", "text": "正确答案选项"}, {"id": "B", "text": "常见错误选项"}, {"id": "C", "text": "计算偏差选项"}, {"id": "D", "text": "概念混淆选项"}], "correct_option_id": "A", "answer": "根据题意计算得出", "explanation": "根据已知条件逐步推导即可", "knowledge_tags": tags[:3], "why_recommended": f"针对{analysis.get('error_focus', '薄弱点')}进行针对性练习"}

    def _fallback_redo_plan(self, stage: str) -> dict[str, Any]:
        mapping = {
            "understand_problem": {"stage": "understand_problem", "interaction_mode": "single_choice", "teaching_goal": "理解题意", "prompt": "请仔细读题，选出你对题目的理解"},
            "identify_first_step": {"stage": "identify_first_step", "interaction_mode": "single_choice", "teaching_goal": "确定第一步", "prompt": "你觉得第一步应该先算什么"},
            "solve": {"stage": "solve", "interaction_mode": "free_text", "teaching_goal": "完整解答", "prompt": "请写出你的完整解题过程"},
            "final_check": {"stage": "final_check", "interaction_mode": "free_text", "teaching_goal": "检查验证", "prompt": "请检查你的答案是否合理，可以代入验证"},
        }
        return mapping.get(stage, {"stage": "completed", "interaction_mode": "free_text", "teaching_goal": "完成", "prompt": "已完成全部步骤"})

    def _fallback_redo_options(self, stage: str) -> dict[str, Any]:
        if stage == "understand_problem":
            return {"options": [{"id": "A", "text": "求追及时间"}, {"id": "B", "text": "求路程差"}, {"id": "C", "text": "求速度和"}, {"id": "D", "text": "求相遇时间"}], "correct_option_id": "A"}
        return {"options": [{"id": "A", "text": "先算速度差"}, {"id": "B", "text": "先算总路程"}, {"id": "C", "text": "先算时间"}, {"id": "D", "text": "先算速度和"}], "correct_option_id": "A"}

    def _fallback_redo_hint(self, stage: str, hint_level: int = 1) -> str:
        hints = {
            "understand_problem": {1: "仔细看题目问的是什么", 2: "题目问的是时间，想想需要知道哪些条件", 3: "需要知道路程差和速度差才能求时间"},
            "identify_first_step": {1: "想想追及问题的关键量是什么", 2: "追及问题要先算速度差", 3: "速度差 = 快的人速度 - 慢的人速度"},
            "solve": {1: "把已知条件列出来，找关系", 2: "先算小红先走的路程，再算速度差", 3: "路程差 = 40×5=200，速度差 = 60-40=20"},
            "final_check": {1: "把答案代回原题检验", 2: "用追及时间验证两人走的路程", 3: "小明走60×10=600，小红走40×(5+10)=600，相等则正确"},
        }
        stage_hints = hints.get(stage, {1: "再仔细想想", 2: "回顾一下已知条件", 3: "看看题解中的关键步骤"})
        return stage_hints.get(hint_level, stage_hints[1])

    def _fallback_redo_evaluation(
        self,
        report_payload: dict[str, Any],
        planning: dict[str, Any],
        answer: str,
        correct_option_id: str,
        consecutive_correct: int = 0,
        required_consecutive_correct: int = 1,
    ) -> dict[str, Any]:
        stage = str(planning.get("stage") or "understand_problem")
        reference_answer = str((report_payload.get("problem") or {}).get("answer") or "").strip()
        new_consecutive = consecutive_correct

        if planning.get("interaction_mode") == "single_choice":
            ok = answer.strip().upper() == correct_option_id.upper()
            if ok:
                new_consecutive = consecutive_correct + 1
            else:
                new_consecutive = 0
            can_complete = ok and new_consecutive >= required_consecutive_correct
            next_stage = "completed" if can_complete else (_next_stage(stage) if ok else stage)
            return {
                "result": "correct" if ok else "incorrect",
                "feedback": "回答正确，继续加油！" if ok else "不太对，再想想看",
                "next_stage": next_stage,
                "should_offer_hint": not ok,
                "can_clear_mistake": can_complete,
                "consecutive_correct": new_consecutive,
            }

        is_correct, is_partial = self._evaluate_free_text(answer, reference_answer, stage)
        if is_correct:
            new_consecutive = consecutive_correct + 1
        elif is_partial:
            new_consecutive = consecutive_correct
        else:
            new_consecutive = 0

        can_complete = is_correct and new_consecutive >= required_consecutive_correct

        if is_correct:
            next_stage = "completed" if can_complete else _next_stage(stage)
            return {
                "result": "correct",
                "feedback": "回答正确，你掌握得很好！" if can_complete else "回答正确，继续下一步",
                "next_stage": next_stage,
                "should_offer_hint": False,
                "can_clear_mistake": can_complete,
                "consecutive_correct": new_consecutive,
            }
        if is_partial:
            return {
                "result": "partial",
                "feedback": "思路有部分正确，但还需要完善",
                "next_stage": stage,
                "should_offer_hint": True,
                "can_clear_mistake": False,
                "consecutive_correct": new_consecutive,
            }
        return {
            "result": "incorrect",
            "feedback": "再仔细想想，可以参考提示",
            "next_stage": stage,
            "should_offer_hint": True,
            "can_clear_mistake": False,
            "consecutive_correct": new_consecutive,
        }

    def _evaluate_free_text(self, answer: str, reference_answer: str, stage: str) -> tuple[bool, bool]:
        answer_clean = answer.strip()
        if not answer_clean:
            return False, False

        if not reference_answer:
            if len(answer_clean) >= 4:
                return True, False
            return False, False

        ref_nums = self._extract_numbers(reference_answer)
        ans_nums = self._extract_numbers(answer_clean)

        if ref_nums and ans_nums:
            for rn in ref_nums:
                for an in ans_nums:
                    if abs(rn - an) < 0.01:
                        return True, False
            for rn in ref_nums:
                for an in ans_nums:
                    if abs(rn - an) < max(1.0, abs(rn) * 0.1):
                        return False, True

        if reference_answer in answer_clean or answer_clean in reference_answer:
            return True, False

        ref_chars = set(reference_answer)
        ans_chars = set(answer_clean)
        if ref_chars and ans_chars:
            overlap = len(ref_chars & ans_chars) / max(len(ref_chars), 1)
            if overlap > 0.6:
                return True, False
            if overlap > 0.3:
                return False, True

        if len(answer_clean) >= 4 and stage in ("solve", "final_check"):
            return False, True

        return False, False

    def _extract_numbers(self, text: str) -> list[float]:
        numbers: list[float] = []
        for match in re.finditer(r"[-+]?\d*\.?\d+", text):
            try:
                numbers.append(float(match.group()))
            except ValueError:
                continue
        return numbers

    def _normalize_candidate(self, candidate: dict[str, Any]) -> dict[str, Any]:
        options = self._normalize_options(candidate.get("options"))
        correct = str(candidate.get("correct_option_id") or options[0]["id"]).strip()[:1]
        if correct not in {item["id"] for item in options}:
            correct = options[0]["id"]
        return {"title": str(candidate.get("title") or "推荐题").strip(), "difficulty": self._normalize_difficulty(candidate.get("difficulty")), "question": str(candidate.get("question") or "请完成以下练习"), "options": options, "correct_option_id": correct, "answer": str(candidate.get("answer") or "").strip(), "explanation": str(candidate.get("explanation") or "").strip(), "knowledge_tags": [str(item).strip() for item in candidate.get("knowledge_tags", []) if str(item).strip()], "why_recommended": str(candidate.get("why_recommended") or "针对薄弱点进行练习")}

    def _normalize_redo_plan(self, value: dict[str, Any]) -> dict[str, Any]:
        stage = str(value.get("stage") or "understand_problem")
        mode = str(value.get("interaction_mode") or "single_choice")
        if stage not in VALID_STAGES:
            stage = "understand_problem"
        if mode not in VALID_MODES:
            mode = "single_choice"
        return {"stage": stage, "interaction_mode": mode, "teaching_goal": str(value.get("teaching_goal") or "引导学生思考"), "prompt": str(value.get("prompt") or "请回答以下问题")}

    def _normalize_redo_options(self, value: dict[str, Any]) -> dict[str, Any]:
        options = self._normalize_options(value.get("options"))
        correct = str(value.get("correct_option_id") or options[0]["id"]).strip()[:1]
        if correct not in {item["id"] for item in options}:
            correct = options[0]["id"]
        return {"options": options, "correct_option_id": correct}

    def _normalize_redo_evaluation(self, value: dict[str, Any]) -> dict[str, Any]:
        result = str(value.get("result") or "incorrect")
        next_stage = str(value.get("next_stage") or "understand_problem")
        if result not in VALID_RESULTS:
            result = "incorrect"
        if next_stage not in VALID_STAGES:
            next_stage = "understand_problem"
        return {
            "result": result,
            "feedback": str(value.get("feedback") or "继续加油"),
            "next_stage": next_stage,
            "should_offer_hint": bool(value.get("should_offer_hint", False)),
            "can_clear_mistake": bool(value.get("can_clear_mistake", False)),
            "consecutive_correct": int(value.get("consecutive_correct") or 0),
        }

    def _normalize_options(self, raw_options: Any) -> list[dict[str, str]]:
        options = raw_options if isinstance(raw_options, list) else []
        cleaned: list[dict[str, str]] = []
        for index, item in enumerate(options[:4]):
            if not isinstance(item, dict):
                continue
            text = str(item.get("text") or "").strip()
            if not text:
                continue
            cleaned.append({"id": str(item.get("id") or chr(65 + index)).strip()[:1], "text": text})
        while len(cleaned) < 4:
            cleaned.append({"id": chr(65 + len(cleaned)), "text": f"选项{len(cleaned) + 1}"})
        return cleaned

    def _normalize_difficulty(self, value: Any) -> str:
        text = str(value or "medium").strip().lower()
        return text if text in {"easy", "medium", "hard"} else "medium"

    def generate_profile_aware_recommendation(
        self,
        *,
        report_payload: dict[str, Any],
        origin_report_id: str,
        recommendation_type: str,
        user_profile: dict[str, Any] | None = None,
    ) -> MistakeRecommendationData:
        traces: list[MistakeAgentTrace] = []
        if user_profile:
            analysis = self._call(
                build_profile_aware_analysis_prompt(report_payload, recommendation_type, user_profile),
                self._fallback_analysis(report_payload),
            )
            traces.append(MistakeAgentTrace(agent="profile-aware-analyzer", status="success", summary=str(analysis.get("recommendation_goal") or "分析完成")))
            candidate = self._call(
                build_profile_aware_variant_prompt(analysis, report_payload, recommendation_type, user_profile),
                self._fallback_candidate(report_payload, analysis, recommendation_type),
            )
            traces.append(MistakeAgentTrace(agent="profile-aware-generator", status="success", summary=str(candidate.get("title") or "生成候选题")))
        else:
            analysis = self._call(
                build_problem_analysis_prompt(report_payload, recommendation_type),
                self._fallback_analysis(report_payload),
            )
            traces.append(MistakeAgentTrace(agent="problem-analyzer", status="success", summary=str(analysis.get("recommendation_goal") or "分析完成")))
            candidate = self._call(
                build_variant_generation_prompt(analysis, report_payload, recommendation_type),
                self._fallback_candidate(report_payload, analysis, recommendation_type),
            )
            traces.append(MistakeAgentTrace(agent="variant-generator", status="success", summary=str(candidate.get("title") or "生成候选题")))
        reviewed = self._call(build_quality_review_prompt(candidate, analysis), {"normalized_result": self._normalize_candidate(candidate)})
        final_data = reviewed.get("normalized_result") if isinstance(reviewed.get("normalized_result"), dict) else candidate
        final_data = self._normalize_candidate(final_data)
        traces.append(MistakeAgentTrace(agent="quality-reviewer", status="success", summary="质量审核完成"))
        return MistakeRecommendationData(
            recommendation_id=f"rec_{origin_report_id[-8:]}_{recommendation_type}",
            origin_report_id=origin_report_id,
            recommendation_type=recommendation_type,
            title=str(final_data.get("title") or "推荐题"),
            difficulty=self._normalize_difficulty(final_data.get("difficulty")),
            question=str(final_data.get("question") or ""),
            options=[MistakeRecommendationOption(id=item["id"], text=item["text"]) for item in final_data.get("options", [])],
            correct_option_id=str(final_data.get("correct_option_id") or ""),
            answer=str(final_data.get("answer") or ""),
            explanation=str(final_data.get("explanation") or ""),
            knowledge_tags=[str(item).strip() for item in final_data.get("knowledge_tags", []) if str(item).strip()],
            why_recommended=str(final_data.get("why_recommended") or ""),
            agent_traces=traces,
            generated_at=datetime.now(timezone.utc).isoformat(),
        )

    def dialogue_listener_turn(
        self,
        *,
        report_payload: dict[str, Any],
        similar_question: dict[str, Any] | None = None,
        dialogue_history: list[dict[str, Any]] | None = None,
    ) -> dict[str, Any]:
        result = self._call(
            build_dialogue_listener_prompt(report_payload, similar_question, dialogue_history),
            self._fallback_dialogue_listener(dialogue_history),
        )
        reply = str(result.get("reply") or "请继续讲解你的思路。").strip()
        is_probing = bool(result.get("is_probing", False))
        topic_focus = str(result.get("topic_focus") or "").strip()
        should_continue = bool(result.get("should_continue", True))
        mastery_signal = float(result.get("mastery_signal") or 0.5)
        mastery_signal = max(0.0, min(1.0, mastery_signal))
        return {
            "reply": reply,
            "is_probing": is_probing,
            "topic_focus": topic_focus,
            "should_continue": should_continue,
            "mastery_signal": mastery_signal,
        }

    def judge_dialogue_mastery(
        self,
        *,
        report_payload: dict[str, Any],
        dialogue_history: list[dict[str, Any]],
        similar_question: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        result = self._call(
            build_dialogue_mastery_prompt(report_payload, dialogue_history, similar_question),
            self._fallback_dialogue_mastery(dialogue_history),
        )
        verdict = str(result.get("verdict") or "in_progress")
        if verdict not in {"mastered", "not_mastered", "in_progress"}:
            verdict = "in_progress"
        return {
            "verdict": verdict,
            "detail": str(result.get("detail") or "").strip(),
            "weak_aspects": [str(item).strip() for item in result.get("weak_aspects", []) if str(item).strip()],
            "confidence": max(0.0, min(1.0, float(result.get("confidence") or 0.5))),
        }

    def update_user_profile(
        self,
        *,
        existing_profile: dict[str, Any],
        new_report_payload: dict[str, Any],
    ) -> dict[str, Any]:
        result = self._call(
            build_profile_update_prompt(existing_profile, new_report_payload),
            {"updated": False, "profile": existing_profile},
        )
        updated = bool(result.get("updated", False))
        profile = result.get("profile")
        if not isinstance(profile, dict):
            profile = existing_profile
        return {"updated": updated, "profile": profile}

    def _fallback_dialogue_listener(self, dialogue_history: list[dict[str, Any]] | None = None) -> dict[str, Any]:
        if not dialogue_history or len(dialogue_history) < 2:
            return {
                "reply": "好的，请开始讲解这道题的解题思路吧。",
                "is_probing": False,
                "topic_focus": "整体思路",
                "should_continue": True,
                "mastery_signal": 0.3,
            }
        last_user_msg = ""
        for msg in reversed(dialogue_history or []):
            if msg.get("role") == "user":
                last_user_msg = str(msg.get("content") or "")
                break
        if len(last_user_msg) < 10:
            return {
                "reply": "能再详细说说吗？你刚才提到的这一点，具体是怎么推导的？",
                "is_probing": True,
                "topic_focus": "详细推导",
                "should_continue": True,
                "mastery_signal": 0.4,
            }
        return {
            "reply": "听起来有道理，那你能说说为什么这一步是这样的吗？",
            "is_probing": True,
            "topic_focus": "关键步骤",
            "should_continue": True,
            "mastery_signal": 0.5,
        }

    def _fallback_dialogue_mastery(self, dialogue_history: list[dict[str, Any]]) -> dict[str, Any]:
        user_msgs = [msg for msg in dialogue_history if msg.get("role") == "user"]
        if len(user_msgs) < 3:
            return {
                "verdict": "in_progress",
                "detail": "对话轮次不足，无法判定",
                "weak_aspects": [],
                "confidence": 0.3,
            }
        total_len = sum(len(str(msg.get("content") or "")) for msg in user_msgs)
        if total_len > 100:
            return {
                "verdict": "mastered",
                "detail": "学生讲解较为充分，逻辑基本清晰",
                "weak_aspects": [],
                "confidence": 0.6,
            }
        return {
            "verdict": "not_mastered",
            "detail": "学生讲解不够充分，可能未完全掌握",
            "weak_aspects": ["讲解不够详细"],
            "confidence": 0.5,
        }


def _load_env_file(env_path: Path) -> None:
    if not env_path.exists():
        return
    for line in env_path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


__all__ = ["MistakeMultiAgent"]
