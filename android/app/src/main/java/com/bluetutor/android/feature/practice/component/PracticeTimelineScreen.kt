package com.bluetutor.android.feature.practice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import com.bluetutor.android.feature.practice.PracticeDestination
import com.bluetutor.android.feature.practice.PracticeTimelineState
import com.bluetutor.android.feature.practice.data.MistakeTimelineGroup
import com.bluetutor.android.feature.practice.data.MistakeTimelineItem
import com.bluetutor.android.feature.practice.data.MistakesApiClient
import com.bluetutor.android.feature.practice.data.PracticeLocalCache
import com.bluetutor.android.ui.theme.BluetutorGradients
import kotlinx.coroutines.launch

@Composable
fun PracticeTimelineScreen(
    initialStatus: String? = null,
    initialKnowledgeTag: String? = null,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember {
        mutableIntStateOf(
            when (initialStatus) {
                "pending" -> 1
                "mastered" -> 2
                else -> 0
            },
        )
    }
    var state by remember(context, initialStatus, initialKnowledgeTag) {
        val cachedGroups = PracticeLocalCache.readTimeline(
            context,
            status = initialStatus,
            knowledgeTag = initialKnowledgeTag,
        ).orEmpty()
        mutableStateOf(
            PracticeTimelineState(
                isLoading = cachedGroups.isEmpty(),
                filterStatus = initialStatus,
                timelineGroups = cachedGroups,
            ),
        )
    }
    val tabs = listOf("全部", "待巩固", "已巩固")

    LaunchedEffect(selectedTab, initialKnowledgeTag) {
        onBottomBarVisibilityChange(false)
        val filter = when (selectedTab) {
            1 -> "pending"
            2 -> "mastered"
            else -> null
        }
        val cachedGroups = PracticeLocalCache.readTimeline(
            context,
            status = filter,
            knowledgeTag = initialKnowledgeTag,
        )
        state = state.copy(
            isLoading = cachedGroups.isNullOrEmpty(),
            filterStatus = filter,
            timelineGroups = cachedGroups ?: state.timelineGroups,
            error = null,
        )
        try {
            val groups = MistakesApiClient.getTimeline(status = filter, knowledgeTag = initialKnowledgeTag)
            PracticeLocalCache.saveTimeline(
                context,
                status = filter,
                knowledgeTag = initialKnowledgeTag,
                groups = groups,
            )
            state = state.copy(isLoading = false, timelineGroups = groups)
        } catch (e: Exception) {
            state = state.copy(isLoading = false, error = e.message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground()),
    ) {
        TopBar(title = "错题本", onBack = onBack)

        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF1A3550),
            indicator = {
                Box(
                    Modifier
                        .tabIndicatorOffset(selectedTab)
                        .height(3.dp)
                        .fillMaxWidth()
                        .background(Color(0xFF38ABDA), RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    selectedContentColor = Color(0xFF38ABDA),
                    unselectedContentColor = Color(0xFF8AA8B8),
                )
            }
        }

        if (state.isLoading) {
            LoadingScreen(modifier = Modifier.weight(1f))
        } else if (state.error != null && state.timelineGroups.isEmpty()) {
            ErrorScreen(
                message = state.error ?: "加载失败",
                onRetry = {
                    scope.launch {
                        state = state.copy(isLoading = true, error = null)
                        try {
                            val groups = MistakesApiClient.getTimeline(status = state.filterStatus, knowledgeTag = initialKnowledgeTag)
                            PracticeLocalCache.saveTimeline(
                                context,
                                status = state.filterStatus,
                                knowledgeTag = initialKnowledgeTag,
                                groups = groups,
                            )
                            state = state.copy(isLoading = false, timelineGroups = groups)
                        } catch (e: Exception) {
                            state = state.copy(isLoading = false, error = e.message)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
        } else if (state.timelineGroups.isEmpty()) {
            EmptyTimelineScreen(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.timelineGroups.forEach { group ->
                    if (group.items.isNotEmpty()) {
                        item {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8AA8B8),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(group.items, key = { it.reportId }) { item ->
                            TimelineCard(
                                item = item,
                                onClick = { onNavigate(PracticeDestination.Detail(item.reportId)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(
    item: MistakeTimelineItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (item.status == "pending") Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (item.status == "pending") "📝" else "✅",
                style = MaterialTheme.typography.titleMedium,
            )
        }

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
                    color = if (item.status == "pending") Color(0xFF92400E) else Color(0xFF065F46),
                    modifier = Modifier
                        .background(
                            if (item.status == "pending") Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                            RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                if (item.primaryErrorType.isNotEmpty() && item.primaryErrorType != "待分析") {
                    Text(
                        text = item.primaryErrorType,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8AA8B8),
                    )
                }
                if (item.knowledgeTags.isNotEmpty()) {
                    Text(
                        text = item.knowledgeTags.first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38ABD8),
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFD8EAF4),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyTimelineScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "🎉", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "暂无错题记录",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF8AA8B8),
            )
            Text(
                text = "继续保持，你做得很棒！",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0C4D4),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        expandedHeight = 52.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF1A3550),
                    )
                }
            }
        },
        actions = {
            if (action != null) {
                action()
            } else {
                Spacer(modifier = Modifier.width(38.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White,
        ),
    )
}

private suspend fun loadTimeline(
    state: () -> PracticeTimelineState,
    onUpdate: (PracticeTimelineState) -> Unit,
    filterStatus: String?,
) {
    onUpdate(state().copy(isLoading = true, error = null))
    try {
        val groups = MistakesApiClient.getTimeline(status = filterStatus)
        onUpdate(state().copy(isLoading = false, timelineGroups = groups))
    } catch (e: Exception) {
        onUpdate(state().copy(isLoading = false, error = e.message))
    }
}
