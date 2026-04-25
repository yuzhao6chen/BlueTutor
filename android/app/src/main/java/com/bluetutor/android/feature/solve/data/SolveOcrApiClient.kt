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

class SolveOcrApiException(message: String) : IOException(message)

data class SolveOcrResult(
    val imageId: String,
    val questionText: String,
)

object SolveOcrApiClient {
    private const val emulatorBaseUrl = "http://10.0.2.2:8000"
    private const val lanBaseUrl = "http://10.1.2.120:8000"
    private const val defaultUserId = "android_solve_user"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedBaseUrl: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun recognizeQuestionText(
        imageBase64: String,
        imageType: String = "screenshot",
        userId: String = defaultUserId,
    ): SolveOcrResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("user_id", userId)
            .put("image_type", imageType)
            .put("goals", "做题模块")
            .put("image_base64", imageBase64)
        val root = postJson("/api/shared/ocr", payload)
        val data = root.optJSONObject("data")
            ?: throw SolveOcrApiException("OCR 接口返回缺少 data 字段")
        SolveOcrResult(
            imageId = data.optString("image_id"),
            questionText = data.optString("question_text").trim(),
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
                        throw SolveOcrApiException("OCR 服务返回空响应")
                    }

                    val root = JSONObject(body)
                    val code = root.optInt("code", -1)
                    if (!response.isSuccessful || code != 0) {
                        val detailMessage =
                            root.optJSONArray("detail")
                                ?.optJSONObject(0)
                                ?.optString("msg")
                                ?.takeIf { it.isNotBlank() }
                                ?: root.optString("detail").takeIf { it.isNotBlank() }
                                ?: root.toString().takeIf { it.isNotBlank() }
                        val serverMessage = root.optString("message") ?: ""
                        val message = when {
                            serverMessage.isNotBlank() -> serverMessage
                            !detailMessage.isNullOrBlank() -> detailMessage
                            else -> "OCR 服务请求失败（HTTP ${response.code}）"
                        }
                        throw SolveOcrApiException(message)
                    }

                    cachedBaseUrl = baseUrl
                    return root
                }
            } catch (error: SolveOcrApiException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }

        val attempted = candidateBaseUrls().joinToString(separator = "、")
        val detail = lastNetworkError?.message?.takeIf { it.isNotBlank() }
        throw SolveOcrApiException(
            buildString {
                append("暂时无法连接 OCR 服务。已尝试地址：")
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