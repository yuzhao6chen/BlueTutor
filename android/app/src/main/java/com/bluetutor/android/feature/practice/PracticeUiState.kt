package com.bluetutor.android.feature.practice

import androidx.compose.ui.graphics.Color

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

fun practiceMockUiState(): PracticeUiState = PracticeUiState(
    todayMistakesCount = 9,
    pendingReviewCount = 9,
    stats = listOf(
        PracticeStatUiModel("🧠", "待巩固", "9", "题"),
        PracticeStatUiModel("🌟", "本周完成", "12", "题"),
        PracticeStatUiModel("🏆", "已突破", "5", "类"),
    ),
    weakTopics = listOf(
        PracticeWeakTopicUiModel(1, "🚂", "行程应用题", 3, Color(0xFFDBEAFE), Color(0xFF1D4ED8)),
        PracticeWeakTopicUiModel(2, "📐", "几何图形", 2, Color(0xFFFCE7F3), Color(0xFF9D174D)),
        PracticeWeakTopicUiModel(3, "🔢", "数量关系", 4, Color(0xFFEDE9FE), Color(0xFF6D28D9)),
        PracticeWeakTopicUiModel(4, "➗", "分数运算", 5, Color(0xFFD1FAE5), Color(0xFF065F46)),
    ),
    recommendedPractices = listOf(
        PracticeRecommendedPracticeUiModel(
            id = 1,
            type = "相似题",
            typeBackground = Color(0xFFDBEAFE),
            typeTextColor = Color(0xFF1D4ED8),
            difficulty = "中等",
            difficultyBackground = Color(0xFFFEF3C7),
            difficultyTextColor = Color(0xFF92400E),
            question = "小红从家到公园步行需要 25 分钟，骑车需要 10 分钟。骑车速度是步行的几倍？",
            wrongCount = 2,
        ),
        PracticeRecommendedPracticeUiModel(
            id = 2,
            type = "变式题",
            typeBackground = Color(0xFFEDE9FE),
            typeTextColor = Color(0xFF6D28D9),
            difficulty = "稍难",
            difficultyBackground = Color(0xFFFCE7F3),
            difficultyTextColor = Color(0xFF9D174D),
            question = "已知路程为 3600 米，两种交通方式时间比为 5:2，分别求两种速度。",
            wrongCount = 1,
        ),
    ),
)