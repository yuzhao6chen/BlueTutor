package com.bluetutor.android.feature.practice

import androidx.compose.ui.graphics.Color
import com.bluetutor.android.feature.practice.data.MistakeDialogueSessionResult
import com.bluetutor.android.feature.practice.data.MistakeHomeSummaryResult
import com.bluetutor.android.feature.practice.data.MistakeLectureResult
import com.bluetutor.android.feature.practice.data.MistakeRecommendationResult
import com.bluetutor.android.feature.practice.data.MistakeRecommendationSubmitResult
import com.bluetutor.android.feature.practice.data.MistakeRedoSessionResult
import com.bluetutor.android.feature.practice.data.MistakeTimelineGroup

sealed class PracticeDestination {
    data object Home : PracticeDestination()

    data class Timeline(
        val initialStatus: String? = null,
        val initialKnowledgeTag: String? = null,
    ) : PracticeDestination()

    data class Detail(val reportId: String) : PracticeDestination()

    data class Redo(val reportId: String) : PracticeDestination()

    data class Dialogue(val reportId: String) : PracticeDestination()

    data class Recommendation(
        val reportId: String,
        val recommendationType: String,
    ) : PracticeDestination()

    data class RecommendationPractice(val recommendationId: String) : PracticeDestination()
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
    val filterStatus: String? = null,
    val timelineGroups: List<MistakeTimelineGroup> = emptyList(),
)

data class PracticeDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val lecture: MistakeLectureResult? = null,
)

data class PracticeRecommendationState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val recommendation: MistakeRecommendationResult? = null,
    val selectedOptionId: String? = null,
    val submitResult: MistakeRecommendationSubmitResult? = null,
)

data class PracticeDialogueState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val session: MistakeDialogueSessionResult? = null,
    val inputText: String = "",
)

data class PracticeRedoState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val session: MistakeRedoSessionResult? = null,
    val selectedOptionId: String? = null,
    val freeTextAnswer: String = "",
    val showHint: Boolean = false,
)

val weakTopicEmojis = mapOf(
    "行程应用题" to "🚂",
    "几何图形" to "📐",
    "数量关系" to "🔢",
    "分数运算" to "➗",
    "方程" to "🧮",
    "数据统计" to "📊",
)

val weakTopicColors = listOf(
    Color(0xFFDBEAFE) to Color(0xFF1D4ED8),
    Color(0xFFFCE7F3) to Color(0xFF9D174D),
    Color(0xFFEDE9FE) to Color(0xFF6D28D9),
    Color(0xFFD1FAE5) to Color(0xFF065F46),
    Color(0xFFFEF3C7) to Color(0xFF92400E),
)

fun resultDisplayName(result: String): String {
    return when (result.lowercase()) {
        "correct" -> "回答正确"
        "partial", "partially_correct" -> "部分正确"
        "incorrect", "wrong" -> "继续思考"
        else -> "已作答"
    }
}

fun resultColor(result: String): Color {
    return when (result.lowercase()) {
        "correct" -> Color(0xFF10B981)
        "partial", "partially_correct" -> Color(0xFFF59E0B)
        "incorrect", "wrong" -> Color(0xFFEF4444)
        else -> Color(0xFF38ABDA)
    }
}

fun stageDisplayName(stage: String): String {
    return when (stage.lowercase()) {
        "understand", "understanding" -> "理解题意"
        "analyze", "analysis" -> "分析关系"
        "solve", "solving" -> "尝试解题"
        "review", "reflection" -> "复盘巩固"
        else -> "重做练习"
    }
}

fun stageProgress(stage: String): Float {
    return when (stage.lowercase()) {
        "understand", "understanding" -> 0.25f
        "analyze", "analysis" -> 0.5f
        "solve", "solving" -> 0.75f
        "review", "reflection" -> 1f
        else -> 0.2f
    }
}

fun difficultyDisplayName(difficulty: String): String {
    return when (difficulty.lowercase()) {
        "easy" -> "简单"
        "medium" -> "中等"
        "hard" -> "稍难"
        else -> difficulty.ifBlank { "常规" }
    }
}

fun difficultyColor(difficulty: String): Pair<Color, Color> {
    return when (difficulty.lowercase()) {
        "easy" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "medium" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        "hard" -> Color(0xFFFCE7F3) to Color(0xFF9D174D)
        else -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
    }
}

fun masteryVerdictDisplayName(verdict: String): String {
    return when (verdict.lowercase()) {
        "mastered" -> "已掌握"
        "not_mastered" -> "未掌握"
        else -> "待判断"
    }
}