package com.bluetutor.android.feature.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ProfileUiState(
    val userName: String,
    val gradeLabel: String,
    val streakDays: Int,
    val weekDays: List<ProfileWeekDayUiModel>,
    val stats: List<ProfileStatUiModel>,
    val abilities: List<ProfileAbilityUiModel>,
    val strengthNote: ProfileInsightUiModel,
    val improvementNote: ProfileInsightUiModel,
    val recentActivities: List<ProfileRecentActivityUiModel>,
    val settings: List<ProfileSettingUiModel>,
)

data class ProfileWeekDayUiModel(
    val label: String,
    val studied: Boolean,
)

data class ProfileStatUiModel(
    val emoji: String,
    val label: String,
    val value: String,
    val unit: String,
)

data class ProfileAbilityUiModel(
    val name: String,
    val level: Int,
    val startColor: Color,
    val endColor: Color,
)

data class ProfileInsightUiModel(
    val title: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color,
)

data class ProfileRecentActivityUiModel(
    val type: String,
    val title: String,
    val timeText: String,
    val emoji: String,
)

data class ProfileSettingUiModel(
    val icon: ImageVector,
    val label: String,
    val description: String,
)

fun profileMockUiState(): ProfileUiState = ProfileUiState(
    userName = "小明同学",
    gradeLabel = "六年级",
    streakDays = 7,
    weekDays = listOf(
        ProfileWeekDayUiModel("一", true),
        ProfileWeekDayUiModel("二", true),
        ProfileWeekDayUiModel("三", true),
        ProfileWeekDayUiModel("四", false),
        ProfileWeekDayUiModel("五", true),
        ProfileWeekDayUiModel("六", false),
        ProfileWeekDayUiModel("日", false),
    ),
    stats = listOf(
        ProfileStatUiModel("📘", "完成题目", "28", "道"),
        ProfileStatUiModel("🔥", "连续学习", "7", "天"),
        ProfileStatUiModel("🎯", "引导完成", "15", "次"),
        ProfileStatUiModel("🏅", "已突破题型", "5", "类"),
    ),
    abilities = listOf(
        ProfileAbilityUiModel("应用题", 85, Color(0xFF60A5FA), Color(0xFF2563EB)),
        ProfileAbilityUiModel("几何图形", 72, Color(0xFF34D399), Color(0xFF059669)),
        ProfileAbilityUiModel("计算", 90, Color(0xFFFCD34D), Color(0xFFF59E0B)),
        ProfileAbilityUiModel("数量关系", 68, Color(0xFFC084FC), Color(0xFF7C3AED)),
    ),
    strengthNote = ProfileInsightUiModel(
        title = "💪 我擅长的",
        description = "计算题、基础应用",
        containerColor = Color(0xFFF0FDF4),
        contentColor = Color(0xFF15803D),
    ),
    improvementNote = ProfileInsightUiModel(
        title = "📌 待加强",
        description = "数量关系、几何",
        containerColor = Color(0xFFFFF7ED),
        contentColor = Color(0xFFC2410C),
    ),
    recentActivities = listOf(
        ProfileRecentActivityUiModel("解题", "行程问题 - 步行与骑车", "2小时前", "🎯"),
        ProfileRecentActivityUiModel("预习", "分数乘除法", "昨天", "📚"),
        ProfileRecentActivityUiModel("练习", "相似题巩固 ×3", "2天前", "✏️"),
        ProfileRecentActivityUiModel("复盘", "错题重做", "3天前", "🔄"),
    ),
    settings = listOf(
        ProfileSettingUiModel(Icons.Rounded.Settings, "学习设置", "目标年级、每日计划"),
        ProfileSettingUiModel(Icons.Rounded.HelpOutline, "帮助与反馈", "遇到问题随时告诉我们"),
    ),
)