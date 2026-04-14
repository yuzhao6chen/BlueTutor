package com.bluetutor.android.feature.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bluetutor.android.core.designsystem.component.BtHeroCard
import com.bluetutor.android.core.designsystem.component.BtOwlIllustration
import com.bluetutor.android.core.designsystem.component.BtProgressBar
import com.bluetutor.android.core.designsystem.component.BtSectionTitle
import com.bluetutor.android.core.designsystem.component.BtUploadStatusCard
import com.bluetutor.android.core.designsystem.component.BtUploadStatusCardState
import com.bluetutor.android.feature.preview.component.PreviewQuickEntryCard
import com.bluetutor.android.feature.preview.component.PreviewQuickTopicCard
import com.bluetutor.android.feature.preview.component.PreviewRecommendedLessonCard
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorSpacing
import kotlinx.coroutines.delay

@Composable
fun PreviewRoute(modifier: Modifier = Modifier) {
    var selectedTopicId by rememberSaveable { mutableStateOf(3) }
    var uploadStage by remember { mutableStateOf(PreviewUploadStage.Idle) }
    var uploadedFileName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uploadStage) {
        if (uploadStage == PreviewUploadStage.Processing) {
            delay(1400)
            uploadStage = PreviewUploadStage.Success
        }
    }

    val uiState = remember(uploadStage, uploadedFileName) {
        previewMockUiState(
            uploadStage = uploadStage,
            uploadedFileName = uploadedFileName,
        )
    }

    PreviewScreen(
        uiState = uiState,
        selectedTopicId = selectedTopicId,
        onSelectTopic = { selectedTopicId = it },
        onPrimaryUploadAction = {
            when (uploadStage) {
                PreviewUploadStage.Idle -> {
                    uploadedFileName = "六年级下册行程问题讲义.pdf"
                    uploadStage = PreviewUploadStage.Processing
                }

                PreviewUploadStage.Processing -> Unit
                PreviewUploadStage.Success -> Unit
            }
        },
        onSecondaryUploadAction = {
            uploadedFileName = null
            uploadStage = PreviewUploadStage.Idle
        },
        modifier = modifier,
    )
}

@Composable
fun PreviewScreen(
    uiState: PreviewUiState,
    selectedTopicId: Int,
    onSelectTopic: (Int) -> Unit,
    onPrimaryUploadAction: () -> Unit,
    onSecondaryUploadAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lessonCount = uiState.recommendedLessons.size.coerceAtLeast(1)
    val previewPagerState = rememberPagerState(
        initialPage = previewLoopingInitialPage(lessonCount),
        pageCount = {
            if (uiState.recommendedLessons.size > 1) Int.MAX_VALUE else lessonCount
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground())
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = BluetutorSpacing.screenHorizontal,
                vertical = BluetutorSpacing.screenVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sectionGap),
    ) {
        BtHeroCard(
            badgeText = "🔥 连续学习 ${uiState.streakDays} 天",
            kicker = "Hi，${uiState.userName} 👋",
            title = "今天想先\n预习哪一课？",
            subtitle = "本周已预习 ${uiState.weeklyPreviewedLessons} 课",
            trailingSize = 148.dp,
            trailingContent = {
                BtOwlIllustration(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 2.dp, bottom = 2.dp),
                )
            },
            footer = {
                BtProgressBar(
                    progress = uiState.weeklyGoalCurrent.toFloat() / uiState.weeklyGoalTarget.toFloat(),
                    label = "本周目标",
                    valueText = "${uiState.weeklyGoalCurrent} / ${uiState.weeklyGoalTarget} 天",
                    textColor = MaterialTheme.colorScheme.surface,
                )
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
            BtSectionTitle(
                title = "快捷预习",
                actionText = "全部知识点 →",
                onActionClick = {},
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm),
            ) {
                uiState.quickTopics.forEach { topic ->
                    PreviewQuickTopicCard(
                        topic = topic,
                        selected = topic.id == selectedTopicId,
                        onClick = { onSelectTopic(topic.id) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
            BtSectionTitle(
                title = "上传讲义",
                modifier = Modifier.fillMaxWidth(),
            )

            BtUploadStatusCard(
                title = uiState.uploadCard.title,
                description = uiState.uploadCard.description,
                state = when (uiState.uploadCard.stage) {
                    PreviewUploadStage.Idle -> BtUploadStatusCardState.Idle
                    PreviewUploadStage.Processing -> BtUploadStatusCardState.Processing
                    PreviewUploadStage.Success -> BtUploadStatusCardState.Success
                },
                onClick = onPrimaryUploadAction,
                formats = listOf("PDF", "Word", "图片"),
                helperText = uiState.uploadCard.helperText,
                fileName = uiState.uploadCard.fileName,
                leadingEmoji = when (uiState.uploadCard.stage) {
                    PreviewUploadStage.Idle -> "📄"
                    PreviewUploadStage.Processing -> "✨"
                    PreviewUploadStage.Success -> "✅"
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
        ) {
            uiState.quickEntries.forEach { entry ->
                PreviewQuickEntryCard(
                    entry = entry,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
            BtSectionTitle(
                title = "今日推荐",
                actionText = "查看全部",
                onActionClick = {},
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.recommendedLessons.isNotEmpty()) {
                HorizontalPager(
                    state = previewPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(286.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    pageSpacing = 12.dp,
                    userScrollEnabled = uiState.recommendedLessons.size > 1,
                ) { page ->
                    val actualIndex = page.mod(uiState.recommendedLessons.size)
                    PreviewRecommendedLessonCard(
                        lesson = uiState.recommendedLessons[actualIndex],
                        onActionClick = {},
                    )
                }

                if (uiState.recommendedLessons.size > 1) {
                    PreviewCarouselIndicator(
                        currentIndex = previewPagerState.currentPage.mod(uiState.recommendedLessons.size),
                        totalCount = uiState.recommendedLessons.size,
                    )
                }
            }
        }
    }
}

private fun previewLoopingInitialPage(itemCount: Int): Int {
    if (itemCount <= 1) return 0
    val midpoint = Int.MAX_VALUE / 2
    return midpoint - midpoint % itemCount
}

@Composable
private fun PreviewCarouselIndicator(
    currentIndex: Int,
    totalCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs),
        ) {
            repeat(totalCount) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == currentIndex) 18.dp else 7.dp, height = 7.dp)
                        .background(
                            color = if (index == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}