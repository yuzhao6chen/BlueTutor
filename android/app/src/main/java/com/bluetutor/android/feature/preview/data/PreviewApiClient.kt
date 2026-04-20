package com.bluetutor.android.feature.preview.data

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

class PreviewApiException(message: String) : IOException(message)

object PreviewApiClient {
    private const val emulatorBaseUrl = "http://10.0.2.2:8000"
    private const val lanBaseUrl = "http://10.1.2.120:8000"
    private const val demoUserId = "android_phase1_user"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    @Volatile
    private var cachedBaseUrl: String? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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

        val payload = JSONObject()
            .put("user_id", demoUserId)
            .put("text", question)
            .put("context_text", contextText)
            .put("topic_id", topicId.toString())
            .put("topic_title", topicTitle)
            .put("selected_knowledge_points", knowledgePointsJson)
            .put("history", historyJson)

        val root = postJson("/api/preview/chat", payload)
        val data = root.optJSONObject("data")
            ?: throw PreviewApiException("预习对话接口返回缺少 data 字段")
        val followUpQuestionsJson = data.optJSONArray("follow_up_questions") ?: JSONArray()
        val followUpQuestions = buildList {
            for (index in 0 until followUpQuestionsJson.length()) {
                val item = followUpQuestionsJson.optString(index).trim()
                if (item.isNotEmpty()) {
                    add(item)
                }
            }
        }

        PreviewApiChatResult(
            reply = data.optString("reply"),
            followUpQuestions = followUpQuestions,
        )
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
                append("。请确认后端已在 0.0.0.0:8000 启动。")
                if (detail != null) {
                    append("\n")
                    append(detail)
                }
            },
        )
    }

    private fun candidateBaseUrls(): List<String> {
        val preferred = if (isProbablyEmulator()) {
            listOf(emulatorBaseUrl, lanBaseUrl)
        } else {
            listOf(lanBaseUrl, emulatorBaseUrl)
        }
        val cached = cachedBaseUrl
        return buildList {
            if (!cached.isNullOrBlank()) {
                add(cached)
            }
            preferred.forEach { baseUrl ->
                if (!contains(baseUrl)) {
                    add(baseUrl)
                }
            }
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
}