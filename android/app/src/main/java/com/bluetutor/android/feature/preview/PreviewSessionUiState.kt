package com.bluetutor.android.feature.preview

data class PreviewKnowledgePointUiModel(
    val id: String,
    val title: String,
    val description: String,
    val confidence: Float,
)

enum class PreviewConversationRole {
    User,
    Assistant,
}

data class PreviewConversationMessageUiModel(
    val id: String,
    val role: PreviewConversationRole,
    val text: String,
    val followUpQuestions: List<String> = emptyList(),
)

data class PreviewChatSessionUiModel(
    val id: String,
    val title: String,
    val sourceExcerpt: String? = null,
    val sourceSectionTitle: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val messages: List<PreviewConversationMessageUiModel> = emptyList(),
)

data class PreviewTopicWorkspaceUiState(
    val topic: PreviewQuickTopicUiModel,
    val isDigestLoading: Boolean = false,
    val isSending: Boolean = false,
    val digestErrorMessage: String? = null,
    val chatErrorMessage: String? = null,
    val aiSummary: String = "",
    val knowledgePoints: List<PreviewKnowledgePointUiModel> = emptyList(),
    val selectedKnowledgePointIds: Set<String> = emptySet(),
    val chatSessions: List<PreviewChatSessionUiModel> = emptyList(),
    val activeChatSessionId: String? = null,
    val draftQuestion: String = "",
)