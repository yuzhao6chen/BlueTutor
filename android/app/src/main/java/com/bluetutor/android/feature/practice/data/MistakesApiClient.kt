package com.bluetutor.android.feature.practice.data

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MistakesApiException(message: String) : IOException(message)

data class MistakeHomeSummaryResult(
    val todayPendingCount: Int,
    val pendingReviewCount: Int,
    val completedThisWeekCount: Int,
    val masteredErrorTypesCount: Int,
    val weakKnowledgeTags: List<MistakeWeakTagItem>,
    val recentTimeline: List<MistakeTimelineGroup>,
)

data class MistakeWeakTagItem(
    val tag: String,
    val count: Int,
)

data class MistakeTimelineGroup(
    val label: String,
    val items: List<MistakeTimelineItem>,
)

data class MistakeTimelineItem(
    val reportId: String,
    val title: String,
    val status: String,
    val primaryErrorType: String,
    val knowledgeTags: List<String>,
    val independenceLevel: String,
    val hasSolution: Boolean,
    val solutionPreview: String,
    val createdAt: String,
)

data class MistakeReportDetailResult(
    val reportId: String,
    val userId: String,
    val reportTitle: String,
    val status: String,
    val knowledgeTags: List<String>,
    val primaryErrorType: String,
    val independenceLevel: String,
    val hasSolution: Boolean,
    val solutionPreview: String,
    val createdAt: String,
    val problemPreview: String,
    val problem: MistakeProblemResult,
    val thinkingChain: List<MistakeThinkingNodeResult>,
    val errorProfile: List<MistakeErrorProfileResult>,
    val independenceEvaluation: MistakeIndependenceResult,
    val solution: String?,
)

data class MistakeProblemResult(
    val rawProblem: String,
    val knownConditions: List<String>,
    val goal: String,
    val answer: String,
)

data class MistakeThinkingNodeResult(
    val nodeId: String,
    val content: String,
    val status: String,
    val parentId: String?,
    val errorHistory: List<String>,
)

data class MistakeErrorProfileResult(
    val errorType: String,
    val detail: String,
)

data class MistakeIndependenceResult(
    val level: String,
    val detail: String,
)

data class MistakeLectureResult(
    val reportId: String,
    val reportTitle: String,
    val status: String,
    val problemText: String,
    val answer: String,
    val knowledgeTags: List<String>,
    val primaryErrorType: String,
    val independenceLevel: String,
    val hasSolution: Boolean,
    val solutionPreview: String,
    val lectureSections: List<MistakeLectureSectionResult>,
    val reviewSteps: List<MistakeReviewStepResult>,
    val keyTakeaways: List<String>,
    val createdAt: String,
)

data class MistakeLectureSectionResult(
    val title: String,
    val content: String,
    val kind: String,
)

data class MistakeReviewStepResult(
    val stepNo: Int,
    val title: String,
    val content: String,
    val status: String,
)

data class MistakeRedoSessionResult(
    val sessionId: String,
    val reportId: String,
    val userId: String?,
    val reportTitle: String,
    val problemText: String,
    val stage: String,
    val turnCount: Int,
    val currentPrompt: String,
    val interactionMode: String,
    val options: List<MistakeRedoOptionResult>,
    val hint: String,
    val hintLevel: Int,
    val maxHintLevel: Int,
    val lastFeedback: String,
    val isCompleted: Boolean,
    val canClearMistake: Boolean,
    val consecutiveCorrect: Int,
    val requiredConsecutiveCorrect: Int,
    val sessionType: String,
    val recommendationId: String?,
    val history: List<MistakeRedoTurnResult>,
    val createdAt: String,
    val updatedAt: String,
)

data class MistakeRedoOptionResult(
    val id: String,
    val text: String,
)

data class MistakeRedoTurnResult(
    val turnNo: Int,
    val prompt: String,
    val interactionMode: String,
    val studentAnswer: String,
    val result: String,
    val feedback: String,
    val hint: String,
    val stage: String,
)

data class MistakeRecommendationResult(
    val recommendationId: String,
    val originReportId: String,
    val recommendationType: String,
    val title: String,
    val difficulty: String,
    val question: String,
    val options: List<MistakeRecommendationOptionResult>,
    val correctOptionId: String,
    val answer: String,
    val explanation: String,
    val knowledgeTags: List<String>,
    val whyRecommended: String,
    val generatedAt: String,
)

data class MistakeRecommendationOptionResult(
    val id: String,
    val text: String,
)

data class MistakeRecommendationSubmitResult(
    val recommendationId: String,
    val isCorrect: Boolean,
    val selectedOptionId: String,
    val correctOptionId: String,
    val feedback: String,
    val shouldCreateMistake: Boolean,
)

data class MistakeDialogueSessionResult(
    val sessionId: String,
    val reportId: String,
    val userId: String?,
    val reportTitle: String,
    val problemText: String,
    val isCompleted: Boolean,
    val masteryVerdict: String,
    val masteryDetail: String,
    val messages: List<MistakeDialogueMessageResult>,
    val similarQuestion: MistakeRecommendationResult?,
    val createdAt: String,
    val updatedAt: String,
)

data class MistakeDialogueMessageResult(
    val role: String,
    val content: String,
    val timestamp: String,
)

data class MistakeDailyPlanResult(
    val userId: String?,
    val generatedAt: String,
    val todayFocus: String,
    val summary: String,
    val focusKnowledgeTags: List<String>,
    val items: List<MistakePlanItemResult>,
    val count: Int,
)

data class MistakePlanItemResult(
    val reportId: String,
    val title: String,
    val primaryErrorType: String,
    val knowledgeTags: List<String>,
    val hasSolution: Boolean,
    val action: String,
    val reason: String,
)

object MistakesApiClient {
    private const val emulatorBaseUrl = "http://10.0.2.2:8000"
    private const val lanBaseUrl = "http://10.1.2.120:8000"
    private const val demoUserId = "android_phase1_user"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedBaseUrl: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getHomeSummary(userId: String? = null): MistakeHomeSummaryResult =
        withContext(Dispatchers.IO) {
            val path = buildPath("/api/mistakes/home-summary", "user_id" to userId)
            val root = getJson(path)
            val data = root.optJSONObject("data")
                ?: throw MistakesApiException("home-summary 接口返回缺少 data 字段")
            parseHomeSummary(data)
        }

    suspend fun getTimeline(
        userId: String? = null,
        limit: Int? = null,
        status: String? = null,
        knowledgeTag: String? = null,
    ): List<MistakeTimelineGroup> = withContext(Dispatchers.IO) {
        val path = buildPath(
            "/api/mistakes/timeline",
            "user_id" to userId,
            "limit" to limit?.toString(),
            "status" to status,
            "knowledge_tag" to knowledgeTag,
        )
        val root = getJson(path)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("timeline 接口返回缺少 data 字段")
        parseTimelineGroups(data)
    }

    suspend fun getReportDetail(reportId: String): MistakeReportDetailResult =
        withContext(Dispatchers.IO) {
            val root = getJson("/api/mistakes/reports/$reportId")
            val data = root.optJSONObject("data")
                ?: throw MistakesApiException("report detail 接口返回缺少 data 字段")
            parseReportDetail(data)
        }

    suspend fun getLecture(reportId: String): MistakeLectureResult =
        withContext(Dispatchers.IO) {
            val root = getJson("/api/mistakes/reports/$reportId/lecture")
            val data = root.optJSONObject("data")
                ?: throw MistakesApiException("lecture 接口返回缺少 data 字段")
            parseLecture(data)
        }

    suspend fun getDailyPlan(userId: String? = null, limit: Int = 3): MistakeDailyPlanResult =
        withContext(Dispatchers.IO) {
            val path = buildPath(
                "/api/mistakes/daily-plan",
                "user_id" to userId,
                "limit" to limit.toString(),
            )
            val root = getJson(path)
            val data = root.optJSONObject("data")
                ?: throw MistakesApiException("daily-plan 接口返回缺少 data 字段")
            parseDailyPlan(data)
        }

    suspend fun startRedoSession(
        reportId: String,
        userId: String? = null,
    ): MistakeRedoSessionResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("report_id", reportId)
        userId?.let { payload.put("user_id", it) }
        val root = postJson("/api/mistakes/redo-sessions/start", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("start redo 接口返回缺少 data 字段")
        parseRedoSession(data)
    }

    suspend fun getRedoSession(sessionId: String): MistakeRedoSessionResult =
        withContext(Dispatchers.IO) {
            val root = getJson("/api/mistakes/redo-sessions/$sessionId")
            val data = root.optJSONObject("data")
                ?: throw MistakesApiException("get redo 接口返回缺少 data 字段")
            parseRedoSession(data)
        }

    suspend fun advanceRedoSession(
        sessionId: String,
        answer: String,
    ): MistakeRedoSessionResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("answer", answer)
        val root = postJson("/api/mistakes/redo-sessions/$sessionId/turn", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("advance redo 接口返回缺少 data 字段")
        parseRedoSession(data)
    }

    suspend fun generateRecommendation(
        reportId: String,
        userId: String? = null,
        recommendationType: String = "variant",
    ): MistakeRecommendationResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("report_id", reportId)
            .put("recommendation_type", recommendationType)
        userId?.let { payload.put("user_id", it) }
        val root = postJson("/api/mistakes/recommendations/generate", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("generate recommendation 接口返回缺少 data 字段")
        parseRecommendation(data)
    }

    suspend fun startRecommendationRedo(
        recommendationId: String,
        userId: String? = null,
    ): MistakeRedoSessionResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
        userId?.let { payload.put("user_id", it) }
        val root = postJson("/api/mistakes/recommendations/$recommendationId/start-redo", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("start recommendation redo 接口返回缺少 data 字段")
        parseRedoSession(data)
    }

    suspend fun submitRecommendationAnswer(
        recommendationId: String,
        answer: String,
        userId: String? = null,
    ): MistakeRecommendationSubmitResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("answer", answer)
        userId?.let { payload.put("user_id", it) }
        val root = postJson("/api/mistakes/recommendations/$recommendationId/submit", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("submit recommendation 接口返回缺少 data 字段")
        parseRecommendationSubmit(data)
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: String,
    ): MistakeReportDetailResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("status", status)
        val root = patchJson("/api/mistakes/reports/$reportId/status", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("update status 接口返回缺少 data 字段")
        parseReportDetail(data)
    }

    suspend fun startDialogueSession(
        reportId: String,
        userId: String? = null,
    ): MistakeDialogueSessionResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("report_id", reportId)
        userId?.let { payload.put("user_id", it) }
        val root = postJson("/api/mistakes/dialogue-sessions/start", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("start dialogue 接口返回缺少 data 字段")
        parseDialogueSession(data)
    }

    suspend fun advanceDialogueSession(
        sessionId: String,
        message: String,
    ): MistakeDialogueSessionResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("message", message)
        val root = postJson("/api/mistakes/dialogue-sessions/$sessionId/turn", payload)
        val data = root.optJSONObject("data")
            ?: throw MistakesApiException("advance dialogue 接口返回缺少 data 字段")
        parseDialogueSession(data)
    }

    private fun getJson(path: String): JSONObject {
        var lastNetworkError: IOException? = null
        for (baseUrl in candidateBaseUrls()) {
            val request = Request.Builder()
                .url(baseUrl + path)
                .get()
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        throw MistakesApiException("错题服务返回空响应")
                    }
                    val root = JSONObject(body)
                    val code = root.optInt("code", -1)
                    if (!response.isSuccessful || code != 0) {
                        val message = root.optString("message").ifBlank {
                            "错题服务请求失败（HTTP ${response.code}）"
                        }
                        throw MistakesApiException(message)
                    }
                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: MistakesApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }
        throw MistakesApiException(buildConnectErrorMessage(lastNetworkError))
    }

    private fun postJson(path: String, payload: JSONObject): JSONObject {
        var lastNetworkError: IOException? = null
        for (baseUrl in candidateBaseUrls()) {
            val request = Request.Builder()
                .url(baseUrl + path)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        throw MistakesApiException("错题服务返回空响应")
                    }
                    val root = JSONObject(body)
                    val code = root.optInt("code", -1)
                    if (!response.isSuccessful || code != 0) {
                        val message = root.optString("message").ifBlank {
                            "错题服务请求失败（HTTP ${response.code}）"
                        }
                        throw MistakesApiException(message)
                    }
                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: MistakesApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }
        throw MistakesApiException(buildConnectErrorMessage(lastNetworkError))
    }

    private fun patchJson(path: String, payload: JSONObject): JSONObject {
        var lastNetworkError: IOException? = null
        for (baseUrl in candidateBaseUrls()) {
            val request = Request.Builder()
                .url(baseUrl + path)
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        throw MistakesApiException("错题服务返回空响应")
                    }
                    val root = JSONObject(body)
                    val code = root.optInt("code", -1)
                    if (!response.isSuccessful || code != 0) {
                        val message = root.optString("message").ifBlank {
                            "错题服务请求失败（HTTP ${response.code}）"
                        }
                        throw MistakesApiException(message)
                    }
                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: MistakesApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }
        throw MistakesApiException(buildConnectErrorMessage(lastNetworkError))
    }

    private fun buildPath(base: String, vararg params: Pair<String, String?>): String {
        val queryParts = params.filter { it.second != null }.map { "${it.first}=${it.second}" }
        return if (queryParts.isEmpty()) base else "$base?${queryParts.joinToString("&")}"
    }

    private fun buildConnectErrorMessage(lastError: IOException?): String {
        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastError?.message?.takeIf { it.isNotBlank() }
        return buildString {
            append("暂时无法连接错题服务。已尝试地址：")
            append(attempted)
            append("。请确认后端已在 0.0.0.0:8000 启动。")
            if (detail != null) {
                append("\n")
                append(detail)
            }
        }
    }

    private fun candidateBaseUrls(): List<String> {
        val preferred = if (isProbablyEmulator()) {
            listOf(emulatorBaseUrl, lanBaseUrl)
        } else {
            listOf(lanBaseUrl)
        }
        val cached = cachedBaseUrl
        return buildList {
            if (!cached.isNullOrBlank()) add(cached)
            preferred.forEach { if (!contains(it)) add(it) }
        }
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            Build.PRODUCT.contains("sdk")
    }

    private fun parseHomeSummary(data: JSONObject): MistakeHomeSummaryResult {
        val weakTagsJson = data.optJSONArray("weak_knowledge_tags") ?: JSONArray()
        val weakTags = buildList {
            for (i in 0 until weakTagsJson.length()) {
                val item = weakTagsJson.optJSONObject(i) ?: continue
                add(MistakeWeakTagItem(tag = item.optString("tag"), count = item.optInt("count")))
            }
        }
        return MistakeHomeSummaryResult(
            todayPendingCount = data.optInt("today_pending_count"),
            pendingReviewCount = data.optInt("pending_review_count"),
            completedThisWeekCount = data.optInt("completed_this_week_count"),
            masteredErrorTypesCount = data.optInt("mastered_error_types_count"),
            weakKnowledgeTags = weakTags,
            recentTimeline = parseTimelineGroups(data),
        )
    }

    private fun parseTimelineGroups(data: JSONObject): List<MistakeTimelineGroup> {
        val groupsJson = data.optJSONArray("groups") ?: data.optJSONArray("recent_timeline") ?: JSONArray()
        return buildList {
            for (i in 0 until groupsJson.length()) {
                val groupObj = groupsJson.optJSONObject(i) ?: continue
                val itemsJson = groupObj.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (j in 0 until itemsJson.length()) {
                        val itemObj = itemsJson.optJSONObject(j) ?: continue
                        add(parseTimelineItem(itemObj))
                    }
                }
                add(MistakeTimelineGroup(label = groupObj.optString("label"), items = items))
            }
        }
    }

    private fun parseTimelineItem(obj: JSONObject): MistakeTimelineItem {
        val tagsJson = obj.optJSONArray("knowledge_tags") ?: JSONArray()
        val tags = buildList {
            for (i in 0 until tagsJson.length()) add(tagsJson.optString(i))
        }
        return MistakeTimelineItem(
            reportId = obj.optString("report_id"),
            title = obj.optString("title"),
            status = obj.optString("status"),
            primaryErrorType = obj.optString("primary_error_type"),
            knowledgeTags = tags,
            independenceLevel = obj.optString("independence_level"),
            hasSolution = obj.optBoolean("has_solution"),
            solutionPreview = obj.optString("solution_preview"),
            createdAt = obj.optString("created_at"),
        )
    }

    private fun parseReportDetail(data: JSONObject): MistakeReportDetailResult {
        val reportObj = data.optJSONObject("report") ?: JSONObject()
        val problemObj = reportObj.optJSONObject("problem") ?: JSONObject()
        val conditionsJson = problemObj.optJSONArray("known_conditions") ?: JSONArray()
        val conditions = buildList {
            for (i in 0 until conditionsJson.length()) add(conditionsJson.optString(i))
        }
        val chainJson = reportObj.optJSONArray("thinking_chain") ?: JSONArray()
        val chain = buildList {
            for (i in 0 until chainJson.length()) {
                val nodeObj = chainJson.optJSONObject(i) ?: continue
                val historyJson = nodeObj.optJSONArray("error_history") ?: JSONArray()
                val history = buildList {
                    for (j in 0 until historyJson.length()) add(historyJson.optString(j))
                }
                add(
                    MistakeThinkingNodeResult(
                        nodeId = nodeObj.optString("node_id"),
                        content = nodeObj.optString("content"),
                        status = nodeObj.optString("status"),
                        parentId = nodeObj.optString("parent_id").ifBlank { null },
                        errorHistory = history,
                    ),
                )
            }
        }
        val errorJson = reportObj.optJSONArray("error_profile") ?: JSONArray()
        val errorProfile = buildList {
            for (i in 0 until errorJson.length()) {
                val itemObj = errorJson.optJSONObject(i) ?: continue
                add(MistakeErrorProfileResult(errorType = itemObj.optString("error_type"), detail = itemObj.optString("detail")))
            }
        }
        val indepObj = reportObj.optJSONObject("independence_evaluation") ?: JSONObject()
        val tagsJson = data.optJSONArray("knowledge_tags") ?: JSONArray()
        val tags = buildList {
            for (i in 0 until tagsJson.length()) add(tagsJson.optString(i))
        }
        val solution = reportObj.optString("solution").ifBlank { null }
        return MistakeReportDetailResult(
            reportId = data.optString("report_id"),
            userId = data.optString("user_id"),
            reportTitle = data.optString("report_title"),
            status = data.optString("status"),
            knowledgeTags = tags,
            primaryErrorType = data.optString("primary_error_type"),
            independenceLevel = data.optString("independence_level"),
            hasSolution = data.optBoolean("has_solution"),
            solutionPreview = data.optString("solution_preview"),
            createdAt = data.optString("created_at"),
            problemPreview = data.optString("problem_preview"),
            problem = MistakeProblemResult(
                rawProblem = problemObj.optString("raw_problem"),
                knownConditions = conditions,
                goal = problemObj.optString("goal"),
                answer = problemObj.optString("answer"),
            ),
            thinkingChain = chain,
            errorProfile = errorProfile,
            independenceEvaluation = MistakeIndependenceResult(
                level = indepObj.optString("level"),
                detail = indepObj.optString("detail"),
            ),
            solution = solution,
        )
    }

    private fun parseLecture(data: JSONObject): MistakeLectureResult {
        val sectionsJson = data.optJSONArray("lecture_sections") ?: JSONArray()
        val sections = buildList {
            for (i in 0 until sectionsJson.length()) {
                val obj = sectionsJson.optJSONObject(i) ?: continue
                add(MistakeLectureSectionResult(title = obj.optString("title"), content = obj.optString("content"), kind = obj.optString("kind")))
            }
        }
        val stepsJson = data.optJSONArray("review_steps") ?: JSONArray()
        val steps = buildList {
            for (i in 0 until stepsJson.length()) {
                val obj = stepsJson.optJSONObject(i) ?: continue
                add(MistakeReviewStepResult(stepNo = obj.optInt("step_no"), title = obj.optString("title"), content = obj.optString("content"), status = obj.optString("status")))
            }
        }
        val takeawaysJson = data.optJSONArray("key_takeaways") ?: JSONArray()
        val takeaways = buildList {
            for (i in 0 until takeawaysJson.length()) add(takeawaysJson.optString(i))
        }
        val tagsJson = data.optJSONArray("knowledge_tags") ?: JSONArray()
        val tags = buildList {
            for (i in 0 until tagsJson.length()) add(tagsJson.optString(i))
        }
        return MistakeLectureResult(
            reportId = data.optString("report_id"),
            reportTitle = data.optString("report_title"),
            status = data.optString("status"),
            problemText = data.optString("problem_text"),
            answer = data.optString("answer"),
            knowledgeTags = tags,
            primaryErrorType = data.optString("primary_error_type"),
            independenceLevel = data.optString("independence_level"),
            hasSolution = data.optBoolean("has_solution"),
            solutionPreview = data.optString("solution_preview"),
            lectureSections = sections,
            reviewSteps = steps,
            keyTakeaways = takeaways,
            createdAt = data.optString("created_at"),
        )
    }

    private fun parseRedoSession(data: JSONObject): MistakeRedoSessionResult {
        val optionsJson = data.optJSONArray("options") ?: JSONArray()
        val options = buildList {
            for (i in 0 until optionsJson.length()) {
                val obj = optionsJson.optJSONObject(i) ?: continue
                add(MistakeRedoOptionResult(id = obj.optString("id"), text = obj.optString("text")))
            }
        }
        val historyJson = data.optJSONArray("history") ?: JSONArray()
        val history = buildList {
            for (i in 0 until historyJson.length()) {
                val obj = historyJson.optJSONObject(i) ?: continue
                add(
                    MistakeRedoTurnResult(
                        turnNo = obj.optInt("turn_no"),
                        prompt = obj.optString("prompt"),
                        interactionMode = obj.optString("interaction_mode"),
                        studentAnswer = obj.optString("student_answer"),
                        result = obj.optString("result"),
                        feedback = obj.optString("feedback"),
                        hint = obj.optString("hint"),
                        stage = obj.optString("stage"),
                    ),
                )
            }
        }
        return MistakeRedoSessionResult(
            sessionId = data.optString("session_id"),
            reportId = data.optString("report_id"),
            userId = data.optString("user_id").ifBlank { null },
            reportTitle = data.optString("report_title"),
            problemText = data.optString("problem_text"),
            stage = data.optString("stage"),
            turnCount = data.optInt("turn_count"),
            currentPrompt = data.optString("current_prompt"),
            interactionMode = data.optString("interaction_mode"),
            options = options,
            hint = data.optString("hint"),
            hintLevel = data.optInt("hint_level"),
            maxHintLevel = data.optInt("max_hint_level"),
            lastFeedback = data.optString("last_feedback"),
            isCompleted = data.optBoolean("is_completed"),
            canClearMistake = data.optBoolean("can_clear_mistake"),
            consecutiveCorrect = data.optInt("consecutive_correct"),
            requiredConsecutiveCorrect = data.optInt("required_consecutive_correct"),
            sessionType = data.optString("session_type"),
            recommendationId = data.optString("recommendation_id").ifBlank { null },
            history = history,
            createdAt = data.optString("created_at"),
            updatedAt = data.optString("updated_at"),
        )
    }

    private fun parseRecommendation(data: JSONObject): MistakeRecommendationResult {
        val optionsJson = data.optJSONArray("options") ?: JSONArray()
        val options = buildList {
            for (i in 0 until optionsJson.length()) {
                val obj = optionsJson.optJSONObject(i) ?: continue
                add(MistakeRecommendationOptionResult(id = obj.optString("id"), text = obj.optString("text")))
            }
        }
        val tagsJson = data.optJSONArray("knowledge_tags") ?: JSONArray()
        val tags = buildList {
            for (i in 0 until tagsJson.length()) add(tagsJson.optString(i))
        }
        return MistakeRecommendationResult(
            recommendationId = data.optString("recommendation_id"),
            originReportId = data.optString("origin_report_id"),
            recommendationType = data.optString("recommendation_type"),
            title = data.optString("title"),
            difficulty = data.optString("difficulty"),
            question = data.optString("question"),
            options = options,
            correctOptionId = data.optString("correct_option_id"),
            answer = data.optString("answer"),
            explanation = data.optString("explanation"),
            knowledgeTags = tags,
            whyRecommended = data.optString("why_recommended"),
            generatedAt = data.optString("generated_at"),
        )
    }

    private fun parseRecommendationSubmit(data: JSONObject): MistakeRecommendationSubmitResult {
        return MistakeRecommendationSubmitResult(
            recommendationId = data.optString("recommendation_id"),
            isCorrect = data.optBoolean("is_correct"),
            selectedOptionId = data.optString("selected_option_id"),
            correctOptionId = data.optString("correct_option_id"),
            feedback = data.optString("feedback"),
            shouldCreateMistake = data.optBoolean("should_create_mistake"),
        )
    }

    private fun parseDialogueSession(data: JSONObject): MistakeDialogueSessionResult {
        val messagesJson = data.optJSONArray("messages") ?: JSONArray()
        val messages = buildList {
            for (i in 0 until messagesJson.length()) {
                val obj = messagesJson.optJSONObject(i) ?: continue
                add(MistakeDialogueMessageResult(role = obj.optString("role"), content = obj.optString("content"), timestamp = obj.optString("timestamp")))
            }
        }
        val similarObj = data.optJSONObject("similar_question")
        val similarQuestion = similarObj?.let { parseRecommendation(it) }
        return MistakeDialogueSessionResult(
            sessionId = data.optString("session_id"),
            reportId = data.optString("report_id"),
            userId = data.optString("user_id").ifBlank { null },
            reportTitle = data.optString("report_title"),
            problemText = data.optString("problem_text"),
            isCompleted = data.optBoolean("is_completed"),
            masteryVerdict = data.optString("mastery_verdict"),
            masteryDetail = data.optString("mastery_detail"),
            messages = messages,
            similarQuestion = similarQuestion,
            createdAt = data.optString("created_at"),
            updatedAt = data.optString("updated_at"),
        )
    }

    private fun parseDailyPlan(data: JSONObject): MistakeDailyPlanResult {
        val tagsJson = data.optJSONArray("focus_knowledge_tags") ?: JSONArray()
        val tags = buildList {
            for (i in 0 until tagsJson.length()) add(tagsJson.optString(i))
        }
        val itemsJson = data.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until itemsJson.length()) {
                val obj = itemsJson.optJSONObject(i) ?: continue
                val itemTagsJson = obj.optJSONArray("knowledge_tags") ?: JSONArray()
                val itemTags = buildList {
                    for (j in 0 until itemTagsJson.length()) add(itemTagsJson.optString(j))
                }
                add(
                    MistakePlanItemResult(
                        reportId = obj.optString("report_id"),
                        title = obj.optString("title"),
                        primaryErrorType = obj.optString("primary_error_type"),
                        knowledgeTags = itemTags,
                        hasSolution = obj.optBoolean("has_solution"),
                        action = obj.optString("action"),
                        reason = obj.optString("reason"),
                    ),
                )
            }
        }
        return MistakeDailyPlanResult(
            userId = data.optString("user_id").ifBlank { null },
            generatedAt = data.optString("generated_at"),
            todayFocus = data.optString("today_focus"),
            summary = data.optString("summary"),
            focusKnowledgeTags = tags,
            items = items,
            count = data.optInt("count"),
        )
    }
}
