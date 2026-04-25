package com.bluetutor.android.feature.practice

import androidx.compose.ui.graphics.Color
import com.bluetutor.android.feature.practice.data.MistakeDialogueSessionResult
import com.bluetutor.android.feature.practice.data.MistakeHomeSummaryResult
import com.bluetutor.android.feature.practice.data.MistakeLectureResult
import com.bluetutor.android.feature.practice.data.MistakeRedoSessionResult
import com.bluetutor.android.feature.practice.data.MistakeRecommendationResult
import com.bluetutor.android.feature.practice.data.MistakeTimelineGroup

data class PracticeUiState(
    val todayMistakesCount: Int,
    val pendingReviewCount: Int,
    val stats: List<PracticeStatUiModel>,
    val weakTopics: List<PracticeWeakTopicUiModel>,
    val recommendedPractices: List<PracticeRecommendedPracticeUiModel>,
)

data class PracticeStatUiModel(
    val emoji: String,
    val label: String,
    val value: String,
    val unit: String,
)

data class PracticeWeakTopicUiModel(
    val id: Int,
    val emoji: String,
    val name: String,
    val count: Int,
    val chipBackground: Color,
    val chipTextColor: Color,
)

data class PracticeRecommendedPracticeUiModel(
    val id: Int,
    val type: String,
    val typeBackground: Color,
    val typeTextColor: Color,
    val difficulty: String,
    val difficultyBackground: Color,
    val difficultyTextColor: Color,
    val question: String,
    val wrongCount: Int,
)

sealed interface PracticeDestination {
    object Home : PracticeDestination
    data class Timeline(
        val initialStatus: String? = null,
        val initialKnowledgeTag: String? = null,
    ) : PracticeDestination
    data class Detail(val reportId: String) : PracticeDestination
    data class Lecture(val reportId: String) : PracticeDestination
    data class Redo(val reportId: String) : PracticeDestination
    data class RedoSession(val sessionId: String) : PracticeDestination
    data class Dialogue(val reportId: String) : PracticeDestination
    data class DialogueSession(val sessionId: String) : PracticeDestination
    data class Recommendation(val reportId: String, val type: String) : PracticeDestination
    data class RecommendationPractice(val recommendationId: String) : PracticeDestination
}

data class PracticeHomeState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val homeSummary: MistakeHomeSummaryResult? = null,
    val timelineGroups: List<MistakeTimelineGroup> = emptyList(),
)

data class PracticeTimelineState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val timelineGroups: List<MistakeTimelineGroup> = emptyList(),
    val filterStatus: String? = null,
)

data class PracticeDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val lecture: MistakeLectureResult? = null,
)

data class PracticeRedoState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val session: MistakeRedoSessionResult? = null,
    val isSubmitting: Boolean = false,
    val selectedOptionId: String? = null,
    val freeTextAnswer: String = "",
    val showHint: Boolean = false,
)

data class PracticeDialogueState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val session: MistakeDialogueSessionResult? = null,
    val isSending: Boolean = false,
    val inputText: String = "",
)

data class PracticeRecommendationState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val recommendation: MistakeRecommendationResult? = null,
    val selectedOptionId: String? = null,
    val isSubmitting: Boolean = false,
    val submitResult: com.bluetutor.android.feature.practice.data.MistakeRecommendationSubmitResult? = null,
)

val weakTopicEmojis = listOf("🚂", "📐", "🔢", "➗", "📊", "📏", "⏰", "💰", "🔄", "🧩")
val weakTopicColors = listOf(
    Color(0xFFDBEAFE) to Color(0xFF1D4ED8),
    Color(0xFFFCE7F3) to Color(0xFF9D174D),
    Color(0xFFEDE9FE) to Color(0xFF6D28D9),
    Color(0xFFD1FAE5) to Color(0xFF065F46),
    Color(0xFFFEF3C7) to Color(0xFF92400E),
    Color(0xFFFFE4E6) to Color(0xFF9F1239),
    Color(0xFFE0F2FE) to Color(0xFF075985),
    Color(0xFFF3E8FF) to Color(0xFF6B21A8),
    Color(0xFFECFDF5) to Color(0xFF064E3B),
    Color(0xFFFDF4FF) to Color(0xFF86198F),
)

fun stageDisplayName(stage: String): String = when (stage) {
    "understand_problem" -> "理解题目"
    "identify_first_step" -> "确定第一步"
    "solve" -> "解题过程"
    "final_check" -> "最终检查"
    "completed" -> "已完成"
    else -> stage
}

fun stageProgress(stage: String): Float = when (stage) {
    "understand_problem" -> 0.25f
    "identify_first_step" -> 0.5f
    "solve" -> 0.75f
    "final_check" -> 1f
    "completed" -> 1f
    else -> 0f
}

fun difficultyDisplayName(difficulty: String): String = when (difficulty) {
    "easy" -> "简单"
    "medium" -> "中等"
    "hard" -> "较难"
    else -> difficulty
}

fun difficultyColor(difficulty: String): Pair<Color, Color> = when (difficulty) {
    "easy" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
    "medium" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
    "hard" -> Color(0xFFFCE7F3) to Color(0xFF9D174D)
    else -> Color(0xFFE0F2FE) to Color(0xFF075985)
}

fun resultDisplayName(result: String): String = when (result) {
    "correct" -> "正确 ✓"
    "partial" -> "部分正确 ○"
    "incorrect" -> "不正确 ✗"
    else -> result
}

fun resultColor(result: String): Color = when (result) {
    "correct" -> Color(0xFF10B981)
    "partial" -> Color(0xFFF59E0B)
    "incorrect" -> Color(0xFFEF4444)
    else -> Color(0xFF8AA8B8)
}

fun masteryVerdictDisplayName(verdict: String): String = when (verdict) {
    "mastered" -> "已掌握"
    "not_mastered" -> "尚未掌握"
    "in_progress" -> "学习中"
    else -> verdict
}
