package com.bluetutor.android.feature.practice.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.practice.PracticeDestination
import com.bluetutor.android.feature.practice.PracticeHomeState
import com.bluetutor.android.feature.practice.PracticeStatUiModel
import com.bluetutor.android.feature.practice.PracticeWeakTopicUiModel
import com.bluetutor.android.feature.practice.data.MistakeHomeSummaryResult
import com.bluetutor.android.feature.practice.data.MistakeTimelineGroup
import com.bluetutor.android.feature.practice.data.MistakeTimelineItem
import com.bluetutor.android.feature.practice.data.MistakesApiClient
import com.bluetutor.android.feature.practice.weakTopicColors
import com.bluetutor.android.feature.practice.weakTopicEmojis
import com.bluetutor.android.ui.theme.BluetutorGradients
import kotlinx.coroutines.launch

@Composable
fun PracticeHomeScreen(
    onNavigate: (PracticeDestination) -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(PracticeHomeState()) }

    LaunchedEffect(Unit) {
        onBottomBarVisibilityChange(true)
        try {
            val summary = MistakesApiClient.getHomeSummary()
            state = state.copy(isLoading = false, homeSummary = summary, timelineGroups = summary.recentTimeline)
        } catch (e: Exception) {
            state = state.copy(isLoading = false, error = e.message)
        }
    }

    if (state.isLoading) {
        LoadingScreen(modifier = modifier)
        return
    }

    if (state.error != null && state.homeSummary == null) {
        ErrorScreen(
            message = state.error ?: "加载失败",
            onRetry = {
                state = PracticeHomeState()
                scope.launch {
                    try {
                        val summary = MistakesApiClient.getHomeSummary()
                        state = state.copy(isLoading = false, homeSummary = summary, timelineGroups = summary.recentTimeline)
                    } catch (e: Exception) {
                        state = state.copy(isLoading = false, error = e.message)
                    }
                }
            },
            modifier = modifier,
        )
        return
    }

    val summary = state.homeSummary!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HeroCard(
            todayPendingCount = summary.todayPendingCount,
            pendingReviewCount = summary.pendingReviewCount,
            completedThisWeekCount = summary.completedThisWeekCount,
            masteredErrorTypesCount = summary.masteredErrorTypesCount,
            onStatClick = { filterStatus ->
                onNavigate(PracticeDestination.Timeline(initialStatus = filterStatus))
            },
        )

        AiConsolidationCard(onClick = { onNavigate(PracticeDestination.Timeline(initialStatus = "pending")) })

        if (summary.weakKnowledgeTags.isNotEmpty()) {
            WeakTopicsSection(
                weakTags = summary.weakKnowledgeTags,
                onTagClick = { tag ->
                    onNavigate(PracticeDestination.Timeline(initialKnowledgeTag = tag))
                },
                onSeeAll = { onNavigate(PracticeDestination.Timeline()) },
            )
        }

        if (state.timelineGroups.isNotEmpty()) {
            RecentTimelineSection(
                groups = state.timelineGroups,
                onItemClick = { onNavigate(PracticeDestination.Detail(it.reportId)) },
                onSeeAll = { onNavigate(PracticeDestination.Timeline()) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionButton(
                text = "再做一题 →",
                isPrimary = true,
                onClick = { onNavigate(PracticeDestination.Timeline(initialStatus = "pending")) },
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = "错题本",
                isPrimary = false,
                onClick = { onNavigate(PracticeDestination.Timeline()) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroCard(
    todayPendingCount: Int,
    pendingReviewCount: Int,
    completedThisWeekCount: Int,
    masteredErrorTypesCount: Int,
    onStatClick: (String?) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(32.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF1A9ED0),
                        Color(0xFF38ABDA),
                        Color(0xFF4DBDE8),
                        Color(0xFF60C8F2),
                    ),
                ),
                shape = RoundedCornerShape(32.dp),
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "今日错题 ${todayPendingCount}道 待巩固",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val encouragement = when {
                        todayPendingCount == 0 -> "太棒了！"
                        todayPendingCount <= 3 -> "继续加油！"
                        else -> "一起攻克！"
                    }
                    Text(
                        text = encouragement,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Text(
                        text = when {
                            todayPendingCount == 0 -> "🎉"
                            todayPendingCount <= 3 -> "💪"
                            else -> "🔥"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Text(
                    text = "把听懂的变成真的会做",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }

            PracticeMascotIllustration(
                modifier = Modifier.size(118.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val stats = listOf(
                Triple("🧠", "待巩固", pendingReviewCount.toString()) to "pending",
                Triple("🌟", "本周完成", completedThisWeekCount.toString()) to null,
                Triple("🏆", "已突破", masteredErrorTypesCount.toString()) to "mastered",
            )
            stats.forEach { (statData, filter) ->
                PracticeStatCard(
                    item = PracticeStatUiModel(statData.first, statData.second, statData.third, "题"),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onStatClick(filter) },
                )
            }
        }
    }
}

@Composable
private fun AiConsolidationCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFF38ABDA), Color(0xFF7DD3F7))),
                shape = RoundedCornerShape(32.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 4.dp),
        ) {
            Text(
                text = "A+",
                style = MaterialTheme.typography.displaySmall,
                color = Color(0x332A79A6),
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.rotate(-10f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFE55C),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "专属核心功能",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }

                Text(
                    text = "AI 错题巩固",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )

                Text(
                    text = "按时间轴整理错题盲区\nAI 老师手把手带你彻底攻克",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.96f),
                )

                Row(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(999.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "开始今日提分",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF0B4F70),
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF0B4F70),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🎯", style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}

@Composable
private fun WeakTopicsSection(
    weakTags: List<com.bluetutor.android.feature.practice.data.MistakeWeakTagItem>,
    onTagClick: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = "薄弱知识点", action = "查看全部 →", onAction = onSeeAll)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            weakTags.forEachIndexed { index, tag ->
                val emoji = weakTopicEmojis.getOrElse(index) { "📚" }
                val (bgColor, textColor) = weakTopicColors.getOrElse(index) { Color(0xFFE0F2FE) to Color(0xFF075985) }
                PracticeWeakTopicCard(
                    item = PracticeWeakTopicUiModel(
                        id = index,
                        emoji = emoji,
                        name = tag.tag,
                        count = tag.count,
                        chipBackground = bgColor,
                        chipTextColor = textColor,
                    ),
                    onClick = { onTagClick(tag.tag) },
                )
            }
        }
    }
}

@Composable
private fun RecentTimelineSection(
    groups: List<MistakeTimelineGroup>,
    onItemClick: (MistakeTimelineItem) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = "最近错题", action = "查看全部 →", onAction = onSeeAll)

        groups.forEach { group ->
            if (group.items.isNotEmpty()) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8AA8B8),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
                group.items.take(3).forEach { item ->
                    TimelineItemRow(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
private fun TimelineItemRow(
    item: MistakeTimelineItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFFF9FCFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.status == "pending") "待巩固" else "已巩固",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.status == "pending") Color(0xFFF59E0B) else Color(0xFF10B981),
                    modifier = Modifier
                        .background(
                            if (item.status == "pending") Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                            RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Text(
                    text = item.primaryErrorType,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8AA8B8),
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFD8EAF4),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(if (isPrimary) 8.dp else 6.dp, RoundedCornerShape(18.dp))
            .then(
                if (isPrimary) Modifier.background(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF7DD3F7), Color(0xFF38ABDA))),
                    shape = RoundedCornerShape(18.dp),
                ) else Modifier.background(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (isPrimary) Color.White else Color(0xFF1A3550),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            if (!isPrimary) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF1A3550),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
        )
        Text(
            text = action,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF38ABD8),
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

@Composable
private fun PracticeWeakTopicCard(
    item: PracticeWeakTopicUiModel,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF9FCFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = item.emoji, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${item.count}题",
                style = MaterialTheme.typography.labelSmall,
                color = item.chipTextColor,
                modifier = Modifier
                    .background(item.chipBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF2A4A60),
        )
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = Color(0xFF38ABDA),
            strokeWidth = 3.dp,
        )
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "😔", style = MaterialTheme.typography.displaySmall)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8AA8B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF7DD3F7), Color(0xFF38ABDA))),
                        RoundedCornerShape(999.dp),
                    )
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "重试",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}


