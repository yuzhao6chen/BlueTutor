package com.bluetutor.android.feature.practice.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PracticeLocalCache {
    private const val preferencesName = "practice_local_cache_v1"

    fun readHomeSummary(context: Context): MistakeHomeSummaryResult? {
        val raw = readValue(context, homeSummaryKey) ?: return null
        return runCatching {
            parseHomeSummary(JSONObject(raw))
        }.getOrNull()
    }

    fun saveHomeSummary(context: Context, summary: MistakeHomeSummaryResult) {
        writeValue(context, homeSummaryKey, writeHomeSummary(summary).toString())
    }

    fun readTimeline(
        context: Context,
        status: String? = null,
        knowledgeTag: String? = null,
    ): List<MistakeTimelineGroup>? {
        val raw = readValue(context, timelineKey(status, knowledgeTag)) ?: return null
        return runCatching {
            parseTimelineGroups(JSONArray(raw))
        }.getOrNull()
    }

    fun saveTimeline(
        context: Context,
        status: String? = null,
        knowledgeTag: String? = null,
        groups: List<MistakeTimelineGroup>,
    ) {
        writeValue(context, timelineKey(status, knowledgeTag), writeTimelineGroups(groups).toString())
    }

    fun readLecture(context: Context, reportId: String): MistakeLectureResult? {
        val raw = readValue(context, lectureKey(reportId)) ?: return null
        return runCatching {
            parseLecture(JSONObject(raw))
        }.getOrNull()
    }

    fun saveLecture(context: Context, lecture: MistakeLectureResult) {
        writeValue(context, lectureKey(lecture.reportId), writeLecture(lecture).toString())
    }

    fun readRecommendation(
        context: Context,
        reportId: String,
        recommendationType: String,
    ): MistakeRecommendationResult? {
        val raw = readValue(context, recommendationKey(reportId, recommendationType)) ?: return null
        return runCatching {
            parseRecommendation(JSONObject(raw))
        }.getOrNull()
    }

    fun saveRecommendation(
        context: Context,
        reportId: String,
        recommendationType: String,
        recommendation: MistakeRecommendationResult,
    ) {
        writeValue(
            context,
            recommendationKey(reportId, recommendationType),
            writeRecommendation(recommendation).toString(),
        )
    }

    fun readDialogueSessionByReportId(context: Context, reportId: String): MistakeDialogueSessionResult? {
        val raw = readValue(context, dialogueReportKey(reportId)) ?: return null
        return runCatching {
            parseDialogueSession(JSONObject(raw))
        }.getOrNull()
    }

    fun saveDialogueSession(context: Context, session: MistakeDialogueSessionResult) {
        writeValue(context, dialogueReportKey(session.reportId), writeDialogueSession(session).toString())
    }

    fun readRedoSessionByReportId(context: Context, reportId: String): MistakeRedoSessionResult? {
        val raw = readValue(context, redoReportKey(reportId)) ?: return null
        return runCatching {
            parseRedoSession(JSONObject(raw))
        }.getOrNull()
    }

    fun readRedoSessionByRecommendationId(context: Context, recommendationId: String): MistakeRedoSessionResult? {
        val raw = readValue(context, redoRecommendationKey(recommendationId)) ?: return null
        return runCatching {
            parseRedoSession(JSONObject(raw))
        }.getOrNull()
    }

    fun saveRedoSession(context: Context, session: MistakeRedoSessionResult) {
        val raw = writeRedoSession(session).toString()
        writeValue(context, redoReportKey(session.reportId), raw)
        session.recommendationId?.takeIf { it.isNotBlank() }?.let {
            writeValue(context, redoRecommendationKey(it), raw)
        }
    }

    private fun readValue(context: Context, key: String): String? {
        return context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString(key, null)
    }

    private fun writeValue(context: Context, key: String, value: String) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    private fun writeHomeSummary(summary: MistakeHomeSummaryResult): JSONObject {
        return JSONObject()
            .put("todayPendingCount", summary.todayPendingCount)
            .put("pendingReviewCount", summary.pendingReviewCount)
            .put("completedThisWeekCount", summary.completedThisWeekCount)
            .put("masteredErrorTypesCount", summary.masteredErrorTypesCount)
            .put(
                "weakKnowledgeTags",
                JSONArray().apply {
                    summary.weakKnowledgeTags.forEach { item ->
                        put(JSONObject().put("tag", item.tag).put("count", item.count))
                    }
                },
            )
            .put("recentTimeline", writeTimelineGroups(summary.recentTimeline))
    }

    private fun parseHomeSummary(root: JSONObject): MistakeHomeSummaryResult {
        return MistakeHomeSummaryResult(
            todayPendingCount = root.optInt("todayPendingCount", 0),
            pendingReviewCount = root.optInt("pendingReviewCount", 0),
            completedThisWeekCount = root.optInt("completedThisWeekCount", 0),
            masteredErrorTypesCount = root.optInt("masteredErrorTypesCount", 0),
            weakKnowledgeTags = buildList {
                val tags = root.optJSONArray("weakKnowledgeTags") ?: JSONArray()
                for (index in 0 until tags.length()) {
                    val item = tags.optJSONObject(index) ?: continue
                    add(MistakeWeakTagItem(tag = item.optString("tag"), count = item.optInt("count", 0)))
                }
            },
            recentTimeline = parseTimelineGroups(root.optJSONArray("recentTimeline") ?: JSONArray()),
        )
    }

    private fun writeTimelineGroups(groups: List<MistakeTimelineGroup>): JSONArray {
        return JSONArray().apply {
            groups.forEach { group ->
                put(
                    JSONObject()
                        .put("label", group.label)
                        .put(
                            "items",
                            JSONArray().apply {
                                group.items.forEach { item ->
                                    put(
                                        JSONObject()
                                            .put("reportId", item.reportId)
                                            .put("title", item.title)
                                            .put("status", item.status)
                                            .put("primaryErrorType", item.primaryErrorType)
                                            .put("knowledgeTags", JSONArray().apply { item.knowledgeTags.forEach(::put) })
                                            .put("independenceLevel", item.independenceLevel)
                                            .put("hasSolution", item.hasSolution)
                                            .put("solutionPreview", item.solutionPreview)
                                            .put("createdAt", item.createdAt),
                                    )
                                }
                            },
                        ),
                )
            }
        }
    }

    private fun parseTimelineGroups(array: JSONArray): List<MistakeTimelineGroup> {
        return buildList {
            for (groupIndex in 0 until array.length()) {
                val group = array.optJSONObject(groupIndex) ?: continue
                val itemsJson = group.optJSONArray("items") ?: JSONArray()
                add(
                    MistakeTimelineGroup(
                        label = group.optString("label"),
                        items = buildList {
                            for (itemIndex in 0 until itemsJson.length()) {
                                val item = itemsJson.optJSONObject(itemIndex) ?: continue
                                add(
                                    MistakeTimelineItem(
                                        reportId = item.optString("reportId"),
                                        title = item.optString("title"),
                                        status = item.optString("status"),
                                        primaryErrorType = item.optString("primaryErrorType"),
                                        knowledgeTags = readStringList(item.optJSONArray("knowledgeTags")),
                                        independenceLevel = item.optString("independenceLevel"),
                                        hasSolution = item.optBoolean("hasSolution", false),
                                        solutionPreview = item.optString("solutionPreview"),
                                        createdAt = item.optString("createdAt"),
                                    ),
                                )
                            }
                        },
                    ),
                )
            }
        }
    }

    private fun writeLecture(lecture: MistakeLectureResult): JSONObject {
        return JSONObject()
            .put("reportId", lecture.reportId)
            .put("reportTitle", lecture.reportTitle)
            .put("status", lecture.status)
            .put("problemText", lecture.problemText)
            .put("answer", lecture.answer)
            .put("knowledgeTags", JSONArray().apply { lecture.knowledgeTags.forEach(::put) })
            .put("primaryErrorType", lecture.primaryErrorType)
            .put("independenceLevel", lecture.independenceLevel)
            .put("hasSolution", lecture.hasSolution)
            .put("solutionPreview", lecture.solutionPreview)
            .put(
                "lectureSections",
                JSONArray().apply {
                    lecture.lectureSections.forEach { section ->
                        put(JSONObject().put("title", section.title).put("content", section.content).put("kind", section.kind))
                    }
                },
            )
            .put(
                "reviewSteps",
                JSONArray().apply {
                    lecture.reviewSteps.forEach { step ->
                        put(
                            JSONObject()
                                .put("stepNo", step.stepNo)
                                .put("title", step.title)
                                .put("content", step.content)
                                .put("status", step.status),
                        )
                    }
                },
            )
            .put("keyTakeaways", JSONArray().apply { lecture.keyTakeaways.forEach(::put) })
            .put("createdAt", lecture.createdAt)
    }

    private fun parseLecture(root: JSONObject): MistakeLectureResult {
        return MistakeLectureResult(
            reportId = root.optString("reportId"),
            reportTitle = root.optString("reportTitle"),
            status = root.optString("status"),
            problemText = root.optString("problemText"),
            answer = root.optString("answer"),
            knowledgeTags = readStringList(root.optJSONArray("knowledgeTags")),
            primaryErrorType = root.optString("primaryErrorType"),
            independenceLevel = root.optString("independenceLevel"),
            hasSolution = root.optBoolean("hasSolution", false),
            solutionPreview = root.optString("solutionPreview"),
            lectureSections = buildList {
                val sections = root.optJSONArray("lectureSections") ?: JSONArray()
                for (index in 0 until sections.length()) {
                    val item = sections.optJSONObject(index) ?: continue
                    add(
                        MistakeLectureSectionResult(
                            title = item.optString("title"),
                            content = item.optString("content"),
                            kind = item.optString("kind"),
                        ),
                    )
                }
            },
            reviewSteps = buildList {
                val steps = root.optJSONArray("reviewSteps") ?: JSONArray()
                for (index in 0 until steps.length()) {
                    val item = steps.optJSONObject(index) ?: continue
                    add(
                        MistakeReviewStepResult(
                            stepNo = item.optInt("stepNo", index + 1),
                            title = item.optString("title"),
                            content = item.optString("content"),
                            status = item.optString("status"),
                        ),
                    )
                }
            },
            keyTakeaways = readStringList(root.optJSONArray("keyTakeaways")),
            createdAt = root.optString("createdAt"),
        )
    }

    private fun writeRecommendation(recommendation: MistakeRecommendationResult): JSONObject {
        return JSONObject()
            .put("recommendationId", recommendation.recommendationId)
            .put("originReportId", recommendation.originReportId)
            .put("recommendationType", recommendation.recommendationType)
            .put("title", recommendation.title)
            .put("difficulty", recommendation.difficulty)
            .put("question", recommendation.question)
            .put(
                "options",
                JSONArray().apply {
                    recommendation.options.forEach { option ->
                        put(JSONObject().put("id", option.id).put("text", option.text))
                    }
                },
            )
            .put("correctOptionId", recommendation.correctOptionId)
            .put("answer", recommendation.answer)
            .put("explanation", recommendation.explanation)
            .put("knowledgeTags", JSONArray().apply { recommendation.knowledgeTags.forEach(::put) })
            .put("whyRecommended", recommendation.whyRecommended)
            .put("generatedAt", recommendation.generatedAt)
    }

    private fun parseRecommendation(root: JSONObject): MistakeRecommendationResult {
        return MistakeRecommendationResult(
            recommendationId = root.optString("recommendationId"),
            originReportId = root.optString("originReportId"),
            recommendationType = root.optString("recommendationType"),
            title = root.optString("title"),
            difficulty = root.optString("difficulty"),
            question = root.optString("question"),
            options = buildList {
                val options = root.optJSONArray("options") ?: JSONArray()
                for (index in 0 until options.length()) {
                    val item = options.optJSONObject(index) ?: continue
                    add(MistakeRecommendationOptionResult(id = item.optString("id"), text = item.optString("text")))
                }
            },
            correctOptionId = root.optString("correctOptionId"),
            answer = root.optString("answer"),
            explanation = root.optString("explanation"),
            knowledgeTags = readStringList(root.optJSONArray("knowledgeTags")),
            whyRecommended = root.optString("whyRecommended"),
            generatedAt = root.optString("generatedAt"),
        )
    }

    private fun writeDialogueSession(session: MistakeDialogueSessionResult): JSONObject {
        return JSONObject()
            .put("sessionId", session.sessionId)
            .put("reportId", session.reportId)
            .put("userId", session.userId)
            .put("reportTitle", session.reportTitle)
            .put("problemText", session.problemText)
            .put("isCompleted", session.isCompleted)
            .put("masteryVerdict", session.masteryVerdict)
            .put("masteryDetail", session.masteryDetail)
            .put(
                "messages",
                JSONArray().apply {
                    session.messages.forEach { message ->
                        put(
                            JSONObject()
                                .put("role", message.role)
                                .put("content", message.content)
                                .put("timestamp", message.timestamp),
                        )
                    }
                },
            )
            .put("similarQuestion", session.similarQuestion?.let(::writeRecommendation))
            .put("createdAt", session.createdAt)
            .put("updatedAt", session.updatedAt)
    }

    private fun parseDialogueSession(root: JSONObject): MistakeDialogueSessionResult {
        return MistakeDialogueSessionResult(
            sessionId = root.optString("sessionId"),
            reportId = root.optString("reportId"),
            userId = root.optString("userId").ifBlank { null },
            reportTitle = root.optString("reportTitle"),
            problemText = root.optString("problemText"),
            isCompleted = root.optBoolean("isCompleted", false),
            masteryVerdict = root.optString("masteryVerdict"),
            masteryDetail = root.optString("masteryDetail"),
            messages = buildList {
                val messages = root.optJSONArray("messages") ?: JSONArray()
                for (index in 0 until messages.length()) {
                    val item = messages.optJSONObject(index) ?: continue
                    add(
                        MistakeDialogueMessageResult(
                            role = item.optString("role"),
                            content = item.optString("content"),
                            timestamp = item.optString("timestamp"),
                        ),
                    )
                }
            },
            similarQuestion = root.optJSONObject("similarQuestion")?.let(::parseRecommendation),
            createdAt = root.optString("createdAt"),
            updatedAt = root.optString("updatedAt"),
        )
    }

    private fun writeRedoSession(session: MistakeRedoSessionResult): JSONObject {
        return JSONObject()
            .put("sessionId", session.sessionId)
            .put("reportId", session.reportId)
            .put("userId", session.userId)
            .put("reportTitle", session.reportTitle)
            .put("problemText", session.problemText)
            .put("stage", session.stage)
            .put("turnCount", session.turnCount)
            .put("currentPrompt", session.currentPrompt)
            .put("interactionMode", session.interactionMode)
            .put(
                "options",
                JSONArray().apply {
                    session.options.forEach { option ->
                        put(JSONObject().put("id", option.id).put("text", option.text))
                    }
                },
            )
            .put("hint", session.hint)
            .put("hintLevel", session.hintLevel)
            .put("maxHintLevel", session.maxHintLevel)
            .put("lastFeedback", session.lastFeedback)
            .put("isCompleted", session.isCompleted)
            .put("canClearMistake", session.canClearMistake)
            .put("consecutiveCorrect", session.consecutiveCorrect)
            .put("requiredConsecutiveCorrect", session.requiredConsecutiveCorrect)
            .put("sessionType", session.sessionType)
            .put("recommendationId", session.recommendationId)
            .put(
                "history",
                JSONArray().apply {
                    session.history.forEach { turn ->
                        put(
                            JSONObject()
                                .put("turnNo", turn.turnNo)
                                .put("prompt", turn.prompt)
                                .put("interactionMode", turn.interactionMode)
                                .put("studentAnswer", turn.studentAnswer)
                                .put("result", turn.result)
                                .put("feedback", turn.feedback)
                                .put("hint", turn.hint)
                                .put("stage", turn.stage),
                        )
                    }
                },
            )
            .put("createdAt", session.createdAt)
            .put("updatedAt", session.updatedAt)
    }

    private fun parseRedoSession(root: JSONObject): MistakeRedoSessionResult {
        return MistakeRedoSessionResult(
            sessionId = root.optString("sessionId"),
            reportId = root.optString("reportId"),
            userId = root.optString("userId").ifBlank { null },
            reportTitle = root.optString("reportTitle"),
            problemText = root.optString("problemText"),
            stage = root.optString("stage"),
            turnCount = root.optInt("turnCount", 0),
            currentPrompt = root.optString("currentPrompt"),
            interactionMode = root.optString("interactionMode"),
            options = buildList {
                val options = root.optJSONArray("options") ?: JSONArray()
                for (index in 0 until options.length()) {
                    val item = options.optJSONObject(index) ?: continue
                    add(MistakeRedoOptionResult(id = item.optString("id"), text = item.optString("text")))
                }
            },
            hint = root.optString("hint"),
            hintLevel = root.optInt("hintLevel", 0),
            maxHintLevel = root.optInt("maxHintLevel", 0),
            lastFeedback = root.optString("lastFeedback"),
            isCompleted = root.optBoolean("isCompleted", false),
            canClearMistake = root.optBoolean("canClearMistake", false),
            consecutiveCorrect = root.optInt("consecutiveCorrect", 0),
            requiredConsecutiveCorrect = root.optInt("requiredConsecutiveCorrect", 0),
            sessionType = root.optString("sessionType"),
            recommendationId = root.optString("recommendationId").ifBlank { null },
            history = buildList {
                val history = root.optJSONArray("history") ?: JSONArray()
                for (index in 0 until history.length()) {
                    val item = history.optJSONObject(index) ?: continue
                    add(
                        MistakeRedoTurnResult(
                            turnNo = item.optInt("turnNo", index + 1),
                            prompt = item.optString("prompt"),
                            interactionMode = item.optString("interactionMode"),
                            studentAnswer = item.optString("studentAnswer"),
                            result = item.optString("result"),
                            feedback = item.optString("feedback"),
                            hint = item.optString("hint"),
                            stage = item.optString("stage"),
                        ),
                    )
                }
            },
            createdAt = root.optString("createdAt"),
            updatedAt = root.optString("updatedAt"),
        )
    }

    private fun readStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                if (value.isNotEmpty()) {
                    add(value)
                }
            }
        }
    }

    private fun timelineKey(status: String?, knowledgeTag: String?): String {
        return "timeline_${status ?: "all"}_${knowledgeTag ?: "all"}"
    }

    private fun lectureKey(reportId: String): String = "lecture_$reportId"

    private fun recommendationKey(reportId: String, recommendationType: String): String {
        return "recommendation_${reportId}_${recommendationType}"
    }

    private fun dialogueReportKey(reportId: String): String = "dialogue_report_$reportId"

    private fun redoReportKey(reportId: String): String = "redo_report_$reportId"

    private fun redoRecommendationKey(recommendationId: String): String = "redo_rec_$recommendationId"

    private const val homeSummaryKey = "home_summary"
}