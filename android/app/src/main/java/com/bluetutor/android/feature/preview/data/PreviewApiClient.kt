package com.bluetutor.android.feature.preview.data

import com.bluetutor.android.core.network.BackendEndpointConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class PreviewApiKnowledgePoint(
    val id: String,
    val title: String,
    val description: String,
    val confidence: Float,
)

data class PreviewApiKnowledgeResult(
    val summary: String,
    val knowledgePoints: List<PreviewApiKnowledgePoint>,
)

data class PreviewApiChatResult(
    val reply: String,
    val followUpQuestions: List<String>,
)

data class PreviewApiChatMessage(
    val role: String,
    val content: String,
)

data class PreviewApiHandoutBlock(
    val id: String,
    val type: String,
    val title: String,
    val text: String,
    val supportingText: String,
    val sectionTitle: String,
)

data class PreviewApiGeneratedHandout(
    val articleTitle: String,
    val articleSubtitle: String,
    val introduction: String,
    val blocks: List<PreviewApiHandoutBlock>,
    val footerPrompt: String,
)

data class PreviewApiDocumentHandoutResult(
    val fileId: String,
    val fileName: String,
    val fileExtension: String,
    val summary: String,
    val knowledgePoints: List<PreviewApiKnowledgePoint>,
    val handout: PreviewApiGeneratedHandout,
    val cacheHit: Boolean,
)

sealed interface PreviewApiChatStreamEvent {
    data class Token(val token: String) : PreviewApiChatStreamEvent

    data class Done(
        val reply: String,
        val followUpQuestions: List<String>,
    ) : PreviewApiChatStreamEvent
}

class PreviewApiException(message: String) : IOException(message)

object PreviewApiClient {
    private const val demoUserId = "android_phase1_user"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    @Volatile
    private var cachedBaseUrl: String? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(240, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchQuickTopicPreview(
        topicId: Int,
        topicTitle: String,
        seedContent: String,
    ): PreviewApiKnowledgeResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("user_id", demoUserId)
            .put("content_text", seedContent)
            .put("source_type", "quick_topic")
            .put("topic_id", topicId.toString())
            .put("topic_title", topicTitle)

        val root = postJson("/api/preview/knowledge-points", payload)
        val data = root.optJSONObject("data")
            ?: throw PreviewApiException("预习接口返回缺少 data 字段")
        val knowledgePointsJson = data.optJSONArray("knowledge_points") ?: JSONArray()
        val knowledgePoints = buildList {
            for (index in 0 until knowledgePointsJson.length()) {
                val item = knowledgePointsJson.optJSONObject(index) ?: continue
                add(
                    PreviewApiKnowledgePoint(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        description = item.optString("description"),
                        confidence = item.optDouble("confidence", 0.0).toFloat(),
                    ),
                )
            }
        }

        PreviewApiKnowledgeResult(
            summary = data.optString("summary"),
            knowledgePoints = knowledgePoints,
        )
    }

    suspend fun sendQuickTopicQuestion(
        topicId: Int,
        topicTitle: String,
        contextText: String,
        selectedKnowledgePoints: List<String>,
        question: String,
        history: List<PreviewApiChatMessage> = emptyList(),
    ): PreviewApiChatResult = withContext(Dispatchers.IO) {
        val payload = buildChatPayload(
            topicId = topicId,
            topicTitle = topicTitle,
            contextText = contextText,
            selectedKnowledgePoints = selectedKnowledgePoints,
            question = question,
            history = history,
        )

        val root = postJson("/api/preview/chat", payload)
        val data = root.optJSONObject("data")
            ?: throw PreviewApiException("预习对话接口返回缺少 data 字段")

        PreviewApiChatResult(
            reply = data.optString("reply"),
            followUpQuestions = parseFollowUpQuestions(data),
        )
    }

    suspend fun uploadDocumentHandout(
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String?,
    ): PreviewApiDocumentHandoutResult = withContext(Dispatchers.IO) {
        val root = postMultipart(
            path = "/api/preview/documents/upload-handout",
            fields = mapOf("user_id" to demoUserId),
            fileFieldName = "file",
            fileName = fileName,
            fileBytes = fileBytes,
            mimeType = mimeType,
        )
        val data = root.optJSONObject("data")
            ?: throw PreviewApiException("讲义上传接口返回缺少 data 字段")
        val knowledgePointsJson = data.optJSONArray("knowledge_points") ?: JSONArray()
        val handoutJson = data.optJSONObject("handout") ?: JSONObject()
        val blocksJson = handoutJson.optJSONArray("blocks") ?: JSONArray()

        PreviewApiDocumentHandoutResult(
            fileId = data.optString("file_id"),
            fileName = data.optString("file_name"),
            fileExtension = data.optString("file_extension"),
            summary = data.optString("summary"),
            knowledgePoints = buildList {
                for (index in 0 until knowledgePointsJson.length()) {
                    val item = knowledgePointsJson.optJSONObject(index) ?: continue
                    add(
                        PreviewApiKnowledgePoint(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            description = item.optString("description"),
                            confidence = item.optDouble("confidence", 0.0).toFloat(),
                        ),
                    )
                }
            },
            handout = PreviewApiGeneratedHandout(
                articleTitle = handoutJson.optString("article_title"),
                articleSubtitle = handoutJson.optString("article_subtitle"),
                introduction = handoutJson.optString("introduction"),
                blocks = buildList {
                    for (index in 0 until blocksJson.length()) {
                        val item = blocksJson.optJSONObject(index) ?: continue
                        add(
                            PreviewApiHandoutBlock(
                                id = item.optString("id"),
                                type = item.optString("type"),
                                title = item.optString("title"),
                                text = item.optString("text"),
                                supportingText = item.optString("supporting_text"),
                                sectionTitle = item.optString("section_title"),
                            ),
                        )
                    }
                },
                footerPrompt = handoutJson.optString("footer_prompt"),
            ),
            cacheHit = data.optBoolean("cache_hit", false),
        )
    }

    fun streamQuickTopicQuestion(
        topicId: Int,
        topicTitle: String,
        contextText: String,
        selectedKnowledgePoints: List<String>,
        question: String,
        history: List<PreviewApiChatMessage> = emptyList(),
    ): Flow<PreviewApiChatStreamEvent> = flow {
        val payload = buildChatPayload(
            topicId = topicId,
            topicTitle = topicTitle,
            contextText = contextText,
            selectedKnowledgePoints = selectedKnowledgePoints,
            question = question,
            history = history,
        )

        streamSse("/api/preview/chat/stream", payload) { eventName, data ->
            when (eventName) {
                "token" -> {
                    val token = data.optString("token")
                    if (token.isNotEmpty()) {
                        emit(PreviewApiChatStreamEvent.Token(token))
                    }
                }

                "done" -> {
                    emit(
                        PreviewApiChatStreamEvent.Done(
                            reply = data.optString("reply"),
                            followUpQuestions = parseFollowUpQuestions(data),
                        ),
                    )
                }

                "error" -> {
                    val message = data.optString("message").ifBlank {
                        "预习流式对话失败"
                    }
                    throw PreviewApiException(message)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

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
                        throw PreviewApiException("预习服务返回空响应")
                    }

                    val root = JSONObject(body)
                    val code = root.optInt("code", -1)
                    if (!response.isSuccessful || code != 0) {
                        val message = root.optString("message").ifBlank {
                            "预习服务请求失败（HTTP ${response.code}）"
                        }
                        throw PreviewApiException(message)
                    }

                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: PreviewApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }

        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastNetworkError?.message?.takeIf { it.isNotBlank() }
        throw PreviewApiException(
            buildString {
                append("暂时无法连接专题预习服务。已尝试地址：")
                append(attempted)
                append("。请确认后端服务已启动且当前网络可达。")
                if (detail != null) {
                    append("\n")
                    append(detail)
                }
            },
        )
    }

    private fun postMultipart(
        path: String,
        fields: Map<String, String>,
        fileFieldName: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String?,
    ): JSONObject {
        var lastNetworkError: IOException? = null

        for (baseUrl in candidateBaseUrls()) {
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .apply {
                    fields.forEach { (key, value) -> addFormDataPart(key, value) }
                    addFormDataPart(
                        fileFieldName,
                        fileName,
                        fileBytes.toRequestBody((mimeType ?: "application/octet-stream").toMediaTypeOrNull()),
                    )
                }
                .build()

            val request = Request.Builder()
                .url(baseUrl + path)
                .post(multipartBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        throw PreviewApiException("预习服务返回空响应")
                    }

                    val root = JSONObject(body)
                    val code = root.optInt("code", -1)
                    if (!response.isSuccessful || code != 0) {
                        val message = root.optString("message").ifBlank {
                            "预习服务请求失败（HTTP ${response.code}）"
                        }
                        throw PreviewApiException(message)
                    }

                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: PreviewApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }

        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastNetworkError?.message?.takeIf { it.isNotBlank() }
        throw PreviewApiException(
            buildString {
                append("暂时无法连接上传讲义服务。已尝试地址：")
                append(attempted)
                append("。请确认后端服务已启动且当前网络可达。")
                if (detail != null) {
                    append("\n")
                    append(detail)
                }
            },
        )
    }

    private suspend fun streamSse(
        path: String,
        payload: JSONObject,
        onEvent: suspend (String, JSONObject) -> Unit,
    ) {
        var lastNetworkError: IOException? = null

        for (baseUrl in candidateBaseUrls()) {
            val request = Request.Builder()
                .url(baseUrl + path)
                .header("Accept", "text/event-stream")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body ?: throw PreviewApiException("预习服务返回空响应")
                    if (!response.isSuccessful) {
                        throw PreviewApiException(parseHttpErrorMessage(response.code, body))
                    }

                    cachedBaseUrl = baseUrl
                    parseSseStream(body, onEvent)
                    return
                }
            } catch (error: PreviewApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }

        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastNetworkError?.message?.takeIf { it.isNotBlank() }
        throw PreviewApiException(
            buildString {
                append("暂时无法连接专题预习服务。已尝试地址：")
                append(attempted)
                append("。请确认后端服务已启动且当前网络可达。")
                if (detail != null) {
                    append("\n")
                    append(detail)
                }
            },
        )
    }

    private suspend fun parseSseStream(
        body: ResponseBody,
        onEvent: suspend (String, JSONObject) -> Unit,
    ) {
        body.source().use { source ->
            var eventName = "message"
            val dataLines = mutableListOf<String>()

            suspend fun flushEvent() {
                if (dataLines.isEmpty()) {
                    eventName = "message"
                    return
                }

                val data = dataLines.joinToString(separator = "\n")
                dataLines.clear()
                if (data.isBlank()) {
                    eventName = "message"
                    return
                }

                val payload = JSONObject(data)
                val currentEventName = eventName
                eventName = "message"
                onEvent(currentEventName, payload)
            }

            while (!source.exhausted()) {
                val rawLine = source.readUtf8Line() ?: break
                if (rawLine.isBlank()) {
                    flushEvent()
                    continue
                }

                when {
                    rawLine.startsWith("event:") -> {
                        eventName = rawLine.substringAfter("event:").trim().ifEmpty { "message" }
                    }

                    rawLine.startsWith("data:") -> {
                        dataLines += rawLine.substringAfter("data:").trim()
                    }
                }
            }

            flushEvent()
        }
    }

    private fun buildChatPayload(
        topicId: Int,
        topicTitle: String,
        contextText: String,
        selectedKnowledgePoints: List<String>,
        question: String,
        history: List<PreviewApiChatMessage>,
    ): JSONObject {
        val knowledgePointsJson = JSONArray()
        selectedKnowledgePoints.forEach { knowledgePointsJson.put(it) }
        val historyJson = JSONArray()
        history.forEach { message ->
            val content = message.content.trim()
            if (content.isNotEmpty()) {
                historyJson.put(
                    JSONObject()
                        .put("role", message.role)
                        .put("content", content),
                )
            }
        }

        return JSONObject()
            .put("user_id", demoUserId)
            .put("text", question)
            .put("context_text", contextText)
            .put("topic_id", topicId.toString())
            .put("topic_title", topicTitle)
            .put("selected_knowledge_points", knowledgePointsJson)
            .put("history", historyJson)
    }

    private fun parseFollowUpQuestions(data: JSONObject): List<String> {
        val followUpQuestionsJson = data.optJSONArray("follow_up_questions") ?: JSONArray()
        return buildList {
            for (index in 0 until followUpQuestionsJson.length()) {
                val item = followUpQuestionsJson.optString(index).trim()
                if (item.isNotEmpty()) {
                    add(item)
                }
            }
        }
    }

    private fun parseHttpErrorMessage(statusCode: Int, body: ResponseBody): String {
        val rawBody = body.string().trim()
        if (rawBody.isBlank()) {
            return "预习服务请求失败（HTTP $statusCode）"
        }

        return runCatching {
            val root = JSONObject(rawBody)
            root.optString("message").ifBlank {
                "预习服务请求失败（HTTP $statusCode）"
            }
        }.getOrElse {
            rawBody
        }
    }

    private fun candidateBaseUrls(): List<String> {
        return BackendEndpointConfig.candidateBaseUrls(cachedBaseUrl)
    }
}