package com.bluetutor.android.feature.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bluetutor.android.feature.profile.data.ProfileLocalSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class ProfileUiState(
    val userName: String,
    val gradeLabel: String,
    val avatarPath: String?,
    val streakDays: Int,
    val todayCheckedIn: Boolean,
    val weekDays: List<ProfileWeekDayUiModel>,
    val stats: List<ProfileStatUiModel>,
    val abilities: List<ProfileAbilityUiModel>,
    val strengthNote: ProfileInsightUiModel,
    val improvementNote: ProfileInsightUiModel,
    val recentActivities: List<ProfileRecentActivityUiModel>,
)

data class ProfileWeekDayUiModel(
    val label: String,
    val studied: Boolean,
    val isToday: Boolean,
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

fun profileUiState(snapshot: ProfileLocalSnapshot, today: LocalDate = LocalDate.now()): ProfileUiState {
    val checkedInDates = snapshot.checkedInDates.mapNotNull { value ->
        runCatching { LocalDate.parse(value) }.getOrNull()
    }.toSet()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val todayCheckedIn = checkedInDates.contains(today)
    val streakAnchor = when {
        checkedInDates.contains(today) -> today
        checkedInDates.contains(today.minusDays(1)) -> today.minusDays(1)
        else -> checkedInDates.maxOrNull()
    }

    return ProfileUiState(
        userName = snapshot.userName,
        gradeLabel = snapshot.gradeLabel,
        avatarPath = snapshot.avatarPath,
        streakDays = calculateStreakDays(checkedInDates, streakAnchor),
        todayCheckedIn = todayCheckedIn,
        weekDays = buildList {
            repeat(7) { index ->
                val date = weekStart.plusDays(index.toLong())
                add(
                    ProfileWeekDayUiModel(
                        label = dayOfWeekLabel(date.dayOfWeek),
                        studied = checkedInDates.contains(date),
                        isToday = date == today,
                    ),
                )
            }
        },
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
    )
}

fun profileSettingsItems(): List<ProfileSettingUiModel> = listOf(
    ProfileSettingUiModel(Icons.Rounded.Edit, "修改昵称", "支持在本地保存昵称"),
    ProfileSettingUiModel(Icons.Rounded.Image, "上传头像", "从本地相册挑选一张头像"),
    ProfileSettingUiModel(Icons.Rounded.HelpOutline, "帮助与反馈", "查看反馈方式"),
    ProfileSettingUiModel(Icons.Rounded.Refresh, "重置本地资料", "清空昵称、头像和打卡缓存"),
)

private fun calculateStreakDays(
    checkedInDates: Set<LocalDate>,
    streakAnchor: LocalDate?,
): Int {
    var streak = 0
    var cursor = streakAnchor ?: return 0
    while (checkedInDates.contains(cursor)) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun dayOfWeekLabel(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "一"
        DayOfWeek.TUESDAY -> "二"
        DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"
        DayOfWeek.FRIDAY -> "五"
        DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }
}