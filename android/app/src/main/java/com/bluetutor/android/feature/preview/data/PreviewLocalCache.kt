package com.bluetutor.android.feature.preview.data

import android.content.Context
import com.bluetutor.android.feature.preview.PreviewChatSessionUiModel
import com.bluetutor.android.feature.preview.PreviewConversationMessageUiModel
import com.bluetutor.android.feature.preview.PreviewConversationRole
import com.bluetutor.android.feature.preview.PreviewHandoutBlockType
import com.bluetutor.android.feature.preview.PreviewHandoutBlockUiModel
import com.bluetutor.android.feature.preview.PreviewHandoutUiModel
import com.bluetutor.android.feature.preview.PreviewHistoryEntryUiModel
import com.bluetutor.android.feature.preview.PreviewKnowledgePointUiModel
import com.bluetutor.android.feature.preview.PreviewQuickTopicUiModel
import com.bluetutor.android.feature.preview.PreviewTopicSource
import org.json.JSONArray
import org.json.JSONObject

data class PreviewTopicCacheSnapshot(
    val topic: PreviewQuickTopicUiModel?,
    val summary: String,
    val knowledgePoints: List<PreviewKnowledgePointUiModel>,
    val selectedKnowledgePointIds: Set<String>,
    val chatSessions: List<PreviewChatSessionUiModel>,
    val activeChatSessionId: String?,
    val updatedAtMillis: Long,
)

object PreviewLocalCache {
    private const val preferencesName = "preview_local_cache_v4"
    private const val historyKey = "preview_history"

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
                topic = root.optJSONObject("topic")?.let(::readTopic),
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
        topic: PreviewQuickTopicUiModel,
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
            .put("topic", writeTopic(topic))
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

    fun readSavedTopic(context: Context, topicId: Int): PreviewQuickTopicUiModel? {
        return readTopicCache(context, topicId)?.topic
    }

    fun readHistory(context: Context): List<PreviewHistoryEntryUiModel> {
        val rawValue = context
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(historyKey, null)
            ?: return emptyList()

        return runCatching {
            val itemsJson = JSONArray(rawValue)
            buildList {
                for (index in 0 until itemsJson.length()) {
                    val item = itemsJson.optJSONObject(index) ?: continue
                    add(readHistoryEntry(item))
                }
            }.sortedByDescending { it.updatedAtMillis }
        }.getOrElse { emptyList() }
    }

    fun upsertHistoryEntry(
        context: Context,
        entry: PreviewHistoryEntryUiModel,
    ) {
        val nextEntries = buildList {
            add(entry)
            readHistory(context)
                .filterNot { it.id == entry.id }
                .forEach(::add)
        }.take(30)

        val json = JSONArray().apply {
            nextEntries.forEach { put(writeHistoryEntry(it)) }
        }
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(historyKey, json.toString())
            .apply()
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

    private fun readTopic(item: JSONObject): PreviewQuickTopicUiModel {
        val handoutJson = item.optJSONObject("handout") ?: JSONObject()
        val blocksJson = handoutJson.optJSONArray("blocks") ?: JSONArray()
        return PreviewQuickTopicUiModel(
            id = item.optInt("id"),
            emoji = item.optString("emoji").ifBlank { "📘" },
            label = item.optString("label").ifBlank { "文档预习" },
            grade = item.optString("grade").ifBlank { "上传文档" },
            intro = item.optString("intro"),
            seedContent = item.optString("seedContent"),
            source = topicSourceFromValue(item.optString("source")),
            originalDocumentUri = item.optString("originalDocumentUri").trim().ifEmpty { null },
            originalDocumentMimeType = item.optString("originalDocumentMimeType").trim().ifEmpty { null },
            originalFileName = item.optString("originalFileName").trim().ifEmpty { null },
            handout = PreviewHandoutUiModel(
                articleTitle = handoutJson.optString("articleTitle"),
                articleSubtitle = handoutJson.optString("articleSubtitle"),
                introduction = handoutJson.optString("introduction"),
                footerPrompt = handoutJson.optString("footerPrompt"),
                blocks = buildList {
                    for (index in 0 until blocksJson.length()) {
                        val block = blocksJson.optJSONObject(index) ?: continue
                        add(
                            PreviewHandoutBlockUiModel(
                                id = block.optString("id"),
                                type = handoutBlockTypeFromValue(block.optString("type")),
                                title = block.optString("title"),
                                text = block.optString("text"),
                                supportingText = block.optString("supportingText"),
                                sectionTitle = block.optString("sectionTitle"),
                            ),
                        )
                    }
                },
            ),
        )
    }

    private fun writeTopic(topic: PreviewQuickTopicUiModel): JSONObject {
        return JSONObject()
            .put("id", topic.id)
            .put("emoji", topic.emoji)
            .put("label", topic.label)
            .put("grade", topic.grade)
            .put("intro", topic.intro)
            .put("seedContent", topic.seedContent)
            .put("source", topicSourceValue(topic.source))
            .put("originalDocumentUri", topic.originalDocumentUri)
            .put("originalDocumentMimeType", topic.originalDocumentMimeType)
            .put("originalFileName", topic.originalFileName)
            .put(
                "handout",
                JSONObject()
                    .put("articleTitle", topic.handout.articleTitle)
                    .put("articleSubtitle", topic.handout.articleSubtitle)
                    .put("introduction", topic.handout.introduction)
                    .put("footerPrompt", topic.handout.footerPrompt)
                    .put(
                        "blocks",
                        JSONArray().apply {
                            topic.handout.blocks.forEach { block ->
                                put(
                                    JSONObject()
                                        .put("id", block.id)
                                        .put("type", handoutBlockTypeValue(block.type))
                                        .put("title", block.title)
                                        .put("text", block.text)
                                        .put("supportingText", block.supportingText)
                                        .put("sectionTitle", block.sectionTitle),
                                )
                            }
                        },
                    ),
            )
    }

    private fun readHistoryEntry(item: JSONObject): PreviewHistoryEntryUiModel {
        return PreviewHistoryEntryUiModel(
            id = item.optString("id"),
            topicId = item.optInt("topicId"),
            title = item.optString("title"),
            subtitle = item.optString("subtitle"),
            tag = item.optString("tag"),
            source = topicSourceFromValue(item.optString("source")),
            updatedAtMillis = item.optLong("updatedAtMillis", 0L),
            isInProgress = item.optBoolean("isInProgress", false),
        )
    }

    private fun writeHistoryEntry(entry: PreviewHistoryEntryUiModel): JSONObject {
        return JSONObject()
            .put("id", entry.id)
            .put("topicId", entry.topicId)
            .put("title", entry.title)
            .put("subtitle", entry.subtitle)
            .put("tag", entry.tag)
            .put("source", topicSourceValue(entry.source))
            .put("updatedAtMillis", entry.updatedAtMillis)
            .put("isInProgress", entry.isInProgress)
    }

    private fun topicKey(topicId: Int): String = "topic_$topicId"

    private fun topicSourceValue(source: PreviewTopicSource): String {
        return when (source) {
            PreviewTopicSource.QuickTopic -> "quick_topic"
            PreviewTopicSource.UploadedDocument -> "uploaded_document"
        }
    }

    private fun topicSourceFromValue(value: String): PreviewTopicSource {
        return if (value == "uploaded_document") {
            PreviewTopicSource.UploadedDocument
        } else {
            PreviewTopicSource.QuickTopic
        }
    }

    private fun handoutBlockTypeValue(type: PreviewHandoutBlockType): String {
        return when (type) {
            PreviewHandoutBlockType.SectionHeading -> "section_heading"
            PreviewHandoutBlockType.Paragraph -> "paragraph"
            PreviewHandoutBlockType.Formula -> "formula"
            PreviewHandoutBlockType.ThinkingPrompt -> "thinking_prompt"
            PreviewHandoutBlockType.Note -> "note"
        }
    }

    private fun handoutBlockTypeFromValue(value: String): PreviewHandoutBlockType {
        return when (value) {
            "section_heading" -> PreviewHandoutBlockType.SectionHeading
            "formula" -> PreviewHandoutBlockType.Formula
            "thinking_prompt" -> PreviewHandoutBlockType.ThinkingPrompt
            "note" -> PreviewHandoutBlockType.Note
            else -> PreviewHandoutBlockType.Paragraph
        }
    }

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