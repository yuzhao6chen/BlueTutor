package com.bluetutor.android.feature.solve.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GuideApiException(message: String) : IOException(message)

data class GuideTurnResult(
    val question: String,
    val isSolved: Boolean,
)

data class GuideParsedProblem(
    val knownConditions: List<String>,
    val goal: String,
    val answer: String,
)

data class GuideDialogueMessage(
    val role: String,
    val content: String,
)

data class GuideSessionDetailResult(
    val sessionId: String,
    val rawProblem: String,
    val parsedProblem: GuideParsedProblem,
    val dialogueHistory: List<GuideDialogueMessage>,
    val currentStuckNodeId: String?,
    val stuckCount: Int,
    val lastUpdatedNodeId: String?,
    val isSolved: Boolean,
)

data class GuideThinkingNodeResult(
    val nodeId: String,
    val content: String,
    val status: String,
    val parentId: String?,
    val errorHistory: List<String>,
    val children: List<String>,
)

data class GuideReportChainItem(
    val nodeId: String,
    val content: String,
    val status: String,
    val parentId: String?,
    val errorHistory: List<String>,
)

data class GuideReportResult(
    val rawProblem: String,
    val knownConditions: List<String>,
    val goal: String,
    val answer: String,
    val knowledgeTags: List<String>,
    val thinkingChain: List<GuideReportChainItem>,
    val errorProfileMarkdown: String,
    val independenceMarkdown: String,
    val solutionMarkdown: String?,
)

data class GuideVisualizationItem(
    val stepId: String,
    val title: String,
    val svg: String,
)

data class GuideVisualizationResult(
    val problemType: String,
    val visuals: List<GuideVisualizationItem>,
)

object GuideApiClient {
    private const val emulatorBaseUrl = "http://10.0.2.2:8000"
    private const val lanBaseUrl = "http://10.1.2.120:8000"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedBaseUrl: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun createSession(problemText: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("problem_text", problemText)
        val root = postJson("/api/guide/sessions", payload)
        val data = root.optJSONObject("data")
            ?: throw GuideApiException("引导解题创建会话返回缺少 data 字段")
        data.optString("session_id").ifBlank {
            throw GuideApiException("引导解题创建会话返回缺少 session_id")
        }
    }

    suspend fun runTurn(sessionId: String, studentInput: String): GuideTurnResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("student_input", studentInput)
        val root = postJson("/api/guide/sessions/$sessionId/turns", payload)
        val data = root.optJSONObject("data")
            ?: throw GuideApiException("引导解题轮次返回缺少 data 字段")
        GuideTurnResult(
            question = data.optString("question"),
            isSolved = data.optBoolean("is_solved"),
        )
    }

    suspend fun runTurnStream(
        sessionId: String,
        studentInput: String,
        onRetry: suspend () -> Unit,
        onToken: suspend (String) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("student_input", studentInput)
        var finalSolved = false

        streamPostSse(
            path = "/api/guide/sessions/$sessionId/turns/stream",
            payload = payload,
        ) { event, data ->
            when (event) {
                "retry" -> withContext(Dispatchers.Main) { onRetry() }
                "token" -> {
                    val token = data.optString("token")
                    if (token.isNotEmpty()) {
                        withContext(Dispatchers.Main) { onToken(token) }
                    }
                }
                "done" -> {
                    finalSolved = data.optBoolean("is_solved", false)
                }
                "error" -> {
                    val message = data.optString("message").ifBlank { "流式引导失败，请稍后重试" }
                    throw GuideApiException(message)
                }
            }
        }

        finalSolved
    }

    suspend fun getSessionDetail(sessionId: String): GuideSessionDetailResult = withContext(Dispatchers.IO) {
        val root = getJson("/api/guide/sessions/$sessionId")
        val parsedProblemRoot = root.optJSONObject("parsed_problem")
        val dialogueHistoryRoot = root.optJSONArray("dialogue_history")

        GuideSessionDetailResult(
            sessionId = root.optString("session_id").ifBlank {
                throw GuideApiException("引导解题会话详情缺少 session_id")
            },
            rawProblem = root.optString("raw_problem"),
            parsedProblem = GuideParsedProblem(
                knownConditions = buildList {
                    val conditions = parsedProblemRoot?.optJSONArray("known_conditions")
                    if (conditions != null) {
                        for (index in 0 until conditions.length()) {
                            val item = conditions.optString(index).trim()
                            if (item.isNotEmpty()) add(item)
                        }
                    }
                },
                goal = parsedProblemRoot?.optString("goal").orEmpty(),
                answer = parsedProblemRoot?.optString("answer").orEmpty(),
            ),
            dialogueHistory = buildList {
                if (dialogueHistoryRoot != null) {
                    for (index in 0 until dialogueHistoryRoot.length()) {
                        val item = dialogueHistoryRoot.optJSONObject(index) ?: continue
                        add(
                            GuideDialogueMessage(
                                role = item.optString("role"),
                                content = item.optString("content"),
                            ),
                        )
                    }
                }
            },
            currentStuckNodeId = root.optString("current_stuck_node_id").ifBlank { null },
            stuckCount = root.optInt("stuck_count", 0),
            lastUpdatedNodeId = root.optString("last_updated_node_id").ifBlank { null },
            isSolved = root.optBoolean("is_solved", false),
        )
    }

    suspend fun getThinkingTree(sessionId: String): Map<String, GuideThinkingNodeResult> = withContext(Dispatchers.IO) {
        val root = getJson("/api/guide/sessions/$sessionId/thinking-tree")
        buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val node = root.optJSONObject(key) ?: continue
                put(
                    key,
                    GuideThinkingNodeResult(
                        nodeId = node.optString("node_id", key),
                        content = node.optString("content"),
                        status = node.optString("status"),
                        parentId = node.optString("parent_id").ifBlank { null },
                        errorHistory = buildList {
                            val history = node.optJSONArray("error_history")
                            if (history != null) {
                                for (index in 0 until history.length()) {
                                    val item = history.optString(index).trim()
                                    if (item.isNotEmpty()) add(item)
                                }
                            }
                        },
                        children = buildList {
                            val children = node.optJSONArray("children")
                            if (children != null) {
                                for (index in 0 until children.length()) {
                                    val item = children.optString(index).trim()
                                    if (item.isNotEmpty()) add(item)
                                }
                            }
                        },
                    ),
                )
            }
        }
    }

    suspend fun generateReport(sessionId: String): GuideReportResult = withContext(Dispatchers.IO) {
        val root = postJson("/api/guide/sessions/$sessionId/report", JSONObject())
        parseReport(root)
    }

    suspend fun getSolution(sessionId: String): String = withContext(Dispatchers.IO) {
        val root = getJson("/api/guide/sessions/$sessionId/solution")
        val data = root.optJSONObject("data")
            ?: throw GuideApiException("题解接口返回缺少 data 字段")
        nullableString(data, "solution")
            ?: throw GuideApiException("题解接口返回为空，请重新生成")
    }

    suspend fun generateSolution(sessionId: String): String = withContext(Dispatchers.IO) {
        val root = postJson("/api/guide/sessions/$sessionId/solution", JSONObject())
        val data = root.optJSONObject("data")
            ?: throw GuideApiException("题解生成接口返回缺少 data 字段")
        nullableString(data, "solution")
            ?: throw GuideApiException("题解生成结果为空，请稍后重试")
    }

    suspend fun getOrGenerateSolution(sessionId: String): String = withContext(Dispatchers.IO) {
        try {
            getSolution(sessionId)
        } catch (error: GuideApiException) {
            if (shouldGenerateOnDemand(error)) {
                generateSolution(sessionId)
            } else {
                throw error
            }
        }
    }

    suspend fun getVisualization(sessionId: String): GuideVisualizationResult = withContext(Dispatchers.IO) {
        val root = getJson("/api/guide/sessions/$sessionId/visualization")
        val data = root.optJSONObject("data")
            ?: throw GuideApiException("可视化接口返回缺少 data 字段")
        parseVisualization(data)
    }

    suspend fun generateVisualization(sessionId: String): GuideVisualizationResult = withContext(Dispatchers.IO) {
        val root = postJson("/api/guide/sessions/$sessionId/visualization", JSONObject())
        val data = root.optJSONObject("data")
            ?: throw GuideApiException("可视化生成接口返回缺少 data 字段")
        parseVisualization(data)
    }

    suspend fun getOrGenerateVisualization(sessionId: String): GuideVisualizationResult = withContext(Dispatchers.IO) {
        try {
            getVisualization(sessionId)
        } catch (error: GuideApiException) {
            if (shouldGenerateOnDemand(error)) {
                runWithRetry(
                    maxAttempts = 2,
                    retryDelayMs = 1500,
                ) {
                    generateVisualization(sessionId)
                }
            } else {
                throw error
            }
        }
    }

    private suspend fun <T> runWithRetry(
        maxAttempts: Int,
        retryDelayMs: Long,
        block: suspend () -> T,
    ): T {
        var lastError: GuideApiException? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (error: GuideApiException) {
                lastError = error
                if (attempt < maxAttempts - 1 && shouldRetryTransient(error)) {
                    delay(retryDelayMs)
                } else {
                    throw error
                }
            }
        }
        throw lastError ?: GuideApiException("引导解题服务暂时不可用")
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
                        throw GuideApiException("引导解题服务返回空响应")
                    }

                    val root = JSONObject(body)
                    val code = root.optInt("code", -1)
                    val isWrappedSuccess = !root.has("code") || code == 0 || code == 200
                    if (!response.isSuccessful || !isWrappedSuccess) {
                        val message = root.optString("message").ifBlank {
                            "引导解题服务请求失败（HTTP ${response.code}）"
                        }
                        throw GuideApiException(message)
                    }

                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: GuideApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }

        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastNetworkError?.message?.takeIf { it.isNotBlank() }
        throw GuideApiException(
            buildString {
                append("暂时无法连接引导解题服务。已尝试地址：")
                append(attempted)
                append("。请确认后端已在 0.0.0.0:8000 启动。")
                if (detail != null) {
                    append("\n")
                    append(detail)
                }
            },
        )
    }

    private fun parseReport(root: JSONObject): GuideReportResult {
        val problem = root.optJSONObject("problem") ?: JSONObject()
        val thinkingChain = root.optJSONArray("thinking_chain") ?: JSONArray()
        return GuideReportResult(
            rawProblem = problem.optString("raw_problem"),
            knownConditions = jsonArrayToStringList(problem.optJSONArray("known_conditions")),
            goal = problem.optString("goal"),
            answer = problem.optString("answer"),
            knowledgeTags = jsonArrayToStringList(root.optJSONArray("knowledge_tags")),
            thinkingChain = buildList {
                for (index in 0 until thinkingChain.length()) {
                    val item = thinkingChain.optJSONObject(index) ?: continue
                    add(
                        GuideReportChainItem(
                            nodeId = item.optString("node_id"),
                            content = item.optString("content"),
                            status = item.optString("status"),
                            parentId = item.optString("parent_id").ifBlank { null },
                            errorHistory = jsonArrayToStringList(item.optJSONArray("error_history")),
                        ),
                    )
                }
            },
            errorProfileMarkdown = formatJsonValue(root.opt("error_profile")),
            independenceMarkdown = formatJsonValue(root.opt("independence_evaluation")),
            solutionMarkdown = nullableString(root, "solution"),
        )
    }

    private fun parseVisualization(root: JSONObject): GuideVisualizationResult {
        val visuals = root.optJSONArray("visuals") ?: JSONArray()
        return GuideVisualizationResult(
            problemType = root.optString("problem_type").ifBlank { "unknown" },
            visuals = buildList {
                for (index in 0 until visuals.length()) {
                    val item = visuals.optJSONObject(index) ?: continue
                    add(
                        GuideVisualizationItem(
                            stepId = item.optString("step_id").ifBlank { "step_${index + 1}" },
                            title = item.optString("title").ifBlank { "步骤 ${index + 1}" },
                            svg = item.optString("svg"),
                        ),
                    )
                }
            },
        )
    }

    private fun jsonArrayToStringList(array: JSONArray?): List<String> = buildList {
        if (array == null) return@buildList
        for (index in 0 until array.length()) {
            val value = array.opt(index)
            val formatted = formatJsonValue(value).trim()
            if (formatted.isNotEmpty()) add(formatted)
        }
    }

    private fun nullableString(root: JSONObject, key: String): String? {
        if (!root.has(key) || root.isNull(key)) return null
        return root.optString(key)
            .trim()
            .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun shouldGenerateOnDemand(error: GuideApiException): Boolean {
        val message = error.message.orEmpty()
        return message.contains("尚未生成") ||
            message.contains("返回为空") ||
            message.contains("缺少 solution") ||
            message.contains("缺少 data 字段")
    }

    private fun shouldRetryTransient(error: GuideApiException): Boolean {
        val message = error.message.orEmpty()
        return message.contains("暂时无法连接") ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true)
    }

    private fun formatJsonValue(value: Any?, depth: Int = 0): String {
        if (value == null || value == JSONObject.NULL) return ""
        val indent = "  ".repeat(depth)
        return when (value) {
            is JSONObject -> {
                val parts = mutableListOf<String>()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    val formattedChild = formatJsonValue(child, depth + 1).trim()
                    if (formattedChild.isEmpty()) continue
                    if (child is JSONObject || child is JSONArray) {
                        parts += "$indent- $key\n$formattedChild"
                    } else {
                        parts += "$indent- $key：$formattedChild"
                    }
                }
                parts.joinToString("\n")
            }
            is JSONArray -> {
                val parts = mutableListOf<String>()
                for (index in 0 until value.length()) {
                    val child = value.opt(index)
                    val formattedChild = formatJsonValue(child, depth + 1).trim()
                    if (formattedChild.isEmpty()) continue
                    if (child is JSONObject || child is JSONArray) {
                        parts += "$indent-\n$formattedChild"
                    } else {
                        parts += "$indent- $formattedChild"
                    }
                }
                parts.joinToString("\n")
            }
            else -> value.toString()
        }
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
                        throw GuideApiException("引导解题服务返回空响应")
                    }

                    val root = JSONObject(body)
                    if (!response.isSuccessful) {
                        val message = root.optString("message").ifBlank {
                            "引导解题服务请求失败（HTTP ${response.code}）"
                        }
                        throw GuideApiException(message)
                    }

                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: GuideApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }

        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastNetworkError?.message?.takeIf { it.isNotBlank() }
        throw GuideApiException(
            buildString {
                append("暂时无法连接引导解题服务。已尝试地址：")
                append(attempted)
                append("。请确认后端已在 0.0.0.0:8000 启动。")
                if (detail != null) {
                    append("\n")
                    append(detail)
                }
            },
        )
    }

    private suspend fun streamPostSse(
        path: String,
        payload: JSONObject,
        onEvent: suspend (event: String, data: JSONObject) -> Unit,
    ) {
        var lastNetworkError: IOException? = null

        for (baseUrl in candidateBaseUrls()) {
            val request = Request.Builder()
                .url(baseUrl + path)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .addHeader("Accept", "text/event-stream")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val root = body.takeIf { it.isNotBlank() }?.let(::JSONObject)
                        val message = root?.optString("message").orEmpty().ifBlank {
                            "引导解题服务请求失败（HTTP ${response.code}）"
                        }
                        throw GuideApiException(message)
                    }

                    cachedBaseUrl = baseUrl
                    val reader = response.body?.charStream()?.buffered()
                        ?: throw GuideApiException("引导解题流式响应为空")

                    var currentEvent = "message"
                    val currentData = StringBuilder()

                    reader.use { bufferedReader ->
                        while (true) {
                            val line = bufferedReader.readLine() ?: break
                            when {
                                line.startsWith("event:") -> {
                                    currentEvent = line.removePrefix("event:").trim().ifBlank { "message" }
                                }

                                line.startsWith("data:") -> {
                                    if (currentData.isNotEmpty()) currentData.append('\n')
                                    currentData.append(line.removePrefix("data:").trim())
                                }

                                line.isBlank() -> {
                                    val payloadJson = currentData.toString().trim().ifBlank { "{}" }
                                    onEvent(currentEvent, JSONObject(payloadJson))
                                    currentEvent = "message"
                                    currentData.clear()
                                }
                            }
                        }
                    }

                    if (currentData.isNotEmpty()) {
                        onEvent(currentEvent, JSONObject(currentData.toString().trim()))
                    }
                    return
                }
            } catch (error: GuideApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }

        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastNetworkError?.message?.takeIf { it.isNotBlank() }
        throw GuideApiException(
            buildString {
                append("暂时无法连接引导解题服务。已尝试地址：")
                append(attempted)
                append("。请确认后端已在 0.0.0.0:8000 启动。")
                if (detail != null) {
                    append("\n")
                    append(detail)
                }
            },
        )
    }

    private fun candidateBaseUrls(): List<String> {
        val ordered = mutableListOf<String>()
        cachedBaseUrl?.let(ordered::add)
        ordered.add(emulatorBaseUrl)
        ordered.add(lanBaseUrl)
        return ordered.distinct()
    }
}