package com.bluetutor.android.feature.solve.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GuideApiException(message: String) : IOException(message)

data class GuideTurnResult(
    val question: String,
    val isSolved: Boolean,
)

object GuideApiClient {
    private const val emulatorBaseUrl = "http://10.0.2.2:8000"
    private const val lanBaseUrl = "http://10.1.2.120:8000"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedBaseUrl: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
                    if (!response.isSuccessful || code != 0) {
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

    private fun candidateBaseUrls(): List<String> {
        val ordered = mutableListOf<String>()
        cachedBaseUrl?.let(ordered::add)
        ordered.add(emulatorBaseUrl)
        ordered.add(lanBaseUrl)
        return ordered.distinct()
    }
}