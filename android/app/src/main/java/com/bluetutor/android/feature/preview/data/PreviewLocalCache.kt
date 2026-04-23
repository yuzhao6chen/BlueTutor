package com.bluetutor.android.feature.preview.data

import android.content.Context
import com.bluetutor.android.feature.preview.PreviewChatSessionUiModel
import com.bluetutor.android.feature.preview.PreviewConversationMessageUiModel
import com.bluetutor.android.feature.preview.PreviewConversationRole
import com.bluetutor.android.feature.preview.PreviewKnowledgePointUiModel
import org.json.JSONArray
import org.json.JSONObject

data class PreviewTopicCacheSnapshot(
    val summary: String,
    val knowledgePoints: List<PreviewKnowledgePointUiModel>,
    val selectedKnowledgePointIds: Set<String>,
    val chatSessions: List<PreviewChatSessionUiModel>,
    val activeChatSessionId: String?,
    val updatedAtMillis: Long,
)

object PreviewLocalCache {
    private const val preferencesName = "preview_local_cache_v3"

    fun readTopicCache(context: Context, topicId: Int): PreviewTopicCacheSnapshot? {
        val rawValue = context
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(topicKey(topicId), null)
            ?: return null

        return runCatching {
            val root = JSONObject(rawValue)
            val knowledgePointsJson = root.optJSONArray("knowledgePoints") ?: JSONArray()
            val selectedIdsJson = root.optJSONArray("selectedKnowledgePointIds") ?: JSONArray()
            val sessionsJson = root.optJSONArray("chatSessions") ?: JSONArray()
            val activeChatSessionId = root.optString("activeChatSessionId").trim().ifEmpty { null }

            PreviewTopicCacheSnapshot(
                summary = root.optString("summary"),
                knowledgePoints = buildList {
                    for (index in 0 until knowledgePointsJson.length()) {
                        val item = knowledgePointsJson.optJSONObject(index) ?: continue
                        add(
                            PreviewKnowledgePointUiModel(
                                id = item.optString("id"),
                                title = item.optString("title"),
                                description = item.optString("description"),
                                confidence = item.optDouble("confidence", 0.0).toFloat(),
                            ),
                        )
                    }
                },
                selectedKnowledgePointIds = buildSet {
                    for (index in 0 until selectedIdsJson.length()) {
                        val value = selectedIdsJson.optString(index).trim()
                        if (value.isNotEmpty()) {
                            add(value)
                        }
                    }
                },
                chatSessions = buildList {
                    for (index in 0 until sessionsJson.length()) {
                        val item = sessionsJson.optJSONObject(index) ?: continue
                        add(readChatSession(item))
                    }
                },
                activeChatSessionId = activeChatSessionId,
                updatedAtMillis = root.optLong("updatedAtMillis", 0L),
            )
        }.getOrNull()
    }

    fun saveTopicCache(
        context: Context,
        topicId: Int,
        summary: String,
        knowledgePoints: List<PreviewKnowledgePointUiModel>,
        selectedKnowledgePointIds: Set<String>,
        chatSessions: List<PreviewChatSessionUiModel>,
        activeChatSessionId: String?,
    ) {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        if (summary.isBlank() && knowledgePoints.isEmpty() && chatSessions.isEmpty()) {
            preferences.edit().remove(topicKey(topicId)).apply()
            return
        }

        val root = JSONObject()
            .put("summary", summary)
            .put("activeChatSessionId", activeChatSessionId)
            .put("updatedAtMillis", System.currentTimeMillis())
            .put(
                "knowledgePoints",
                JSONArray().apply {
                    knowledgePoints.forEach { item ->
                        put(
                            JSONObject()
                                .put("id", item.id)
                                .put("title", item.title)
                                .put("description", item.description)
                                .put("confidence", item.confidence.toDouble()),
                        )
                    }
                },
            )
            .put(
                "selectedKnowledgePointIds",
                JSONArray().apply {
                    selectedKnowledgePointIds.forEach { put(it) }
                },
            )
            .put(
                "chatSessions",
                JSONArray().apply {
                    chatSessions
                        .sortedByDescending { it.updatedAtMillis }
                        .take(8)
                        .forEach { session ->
                            put(writeChatSession(session))
                        }
                },
            )

        preferences.edit().putString(topicKey(topicId), root.toString()).apply()
    }

    private fun readChatSession(item: JSONObject): PreviewChatSessionUiModel {
        val messagesJson = item.optJSONArray("messages") ?: JSONArray()
        return PreviewChatSessionUiModel(
            id = item.optString("id").ifBlank { "session_default" },
            title = item.optString("title").ifBlank { "最近会话" },
            sourceExcerpt = item.optString("sourceExcerpt").trim().ifEmpty { null },
            sourceSectionTitle = item.optString("sourceSectionTitle").trim().ifEmpty { null },
            createdAtMillis = item.optLong("createdAtMillis", 0L),
            updatedAtMillis = item.optLong("updatedAtMillis", 0L),
            messages = buildList {
                for (index in 0 until messagesJson.length()) {
                    val messageItem = messagesJson.optJSONObject(index) ?: continue
                    add(
                        PreviewConversationMessageUiModel(
                            id = messageItem.optString("id"),
                            role = roleFromValue(messageItem.optString("role")),
                            text = messageItem.optString("text"),
                            followUpQuestions = buildList {
                                val questionsJson = messageItem.optJSONArray("followUpQuestions") ?: JSONArray()
                                for (questionIndex in 0 until questionsJson.length()) {
                                    val question = questionsJson.optString(questionIndex).trim()
                                    if (question.isNotEmpty()) {
                                        add(question)
                                    }
                                }
                            },
                        ),
                    )
                }
            },
        )
    }

    private fun writeChatSession(session: PreviewChatSessionUiModel): JSONObject {
        return JSONObject()
            .put("id", session.id)
            .put("title", session.title)
            .put("sourceExcerpt", session.sourceExcerpt)
            .put("sourceSectionTitle", session.sourceSectionTitle)
            .put("createdAtMillis", session.createdAtMillis)
            .put("updatedAtMillis", session.updatedAtMillis)
            .put(
                "messages",
                JSONArray().apply {
                    session.messages
                        .filterNot { message ->
                            message.role == PreviewConversationRole.Assistant &&
                                message.text == "…" &&
                                message.followUpQuestions.isEmpty()
                        }
                        .takeLast(20)
                        .forEach { message ->
                        put(
                            JSONObject()
                                .put("id", message.id)
                                .put("role", roleValue(message.role))
                                .put("text", message.text)
                                .put(
                                    "followUpQuestions",
                                    JSONArray().apply {
                                        message.followUpQuestions.forEach { put(it) }
                                    },
                                ),
                        )
                    }
                },
            )
    }

    private fun topicKey(topicId: Int): String = "topic_$topicId"

    private fun roleValue(role: PreviewConversationRole): String {
        return when (role) {
            PreviewConversationRole.User -> "user"
            PreviewConversationRole.Assistant -> "assistant"
        }
    }

    private fun roleFromValue(value: String): PreviewConversationRole {
        return if (value == "user") {
            PreviewConversationRole.User
        } else {
            PreviewConversationRole.Assistant
        }
    }
}