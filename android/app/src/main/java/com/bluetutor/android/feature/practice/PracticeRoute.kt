package com.bluetutor.android.feature.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorSpacing
import androidx.compose.material3.Text
import com.bluetutor.android.feature.practice.component.PracticeMascotIllustration
import com.bluetutor.android.feature.practice.component.PracticeRecommendationCard
import com.bluetutor.android.feature.practice.component.PracticeStatCard

@Composable
fun PracticeRoute(modifier: Modifier = Modifier) {
    PracticeScreen(
        uiState = practiceRouteMockUiState(),
        modifier = modifier,
    )
}

@Composable
fun PracticeScreen(
    uiState: PracticeUiState,
    modifier: Modifier = Modifier,
) {
    val recommendedCount = uiState.recommendedPractices.size.coerceAtLeast(1)
    val practicePagerState = rememberPagerState(
        initialPage = loopingPagerInitialPage(recommendedCount),
        pageCount = {
            if (uiState.recommendedPractices.size > 1) Int.MAX_VALUE else recommendedCount
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground())
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 20.dp,
                vertical = 22.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF1A9ED0),
                            Color(0xFF38ABDA),
                            Color(0xFF4DBDE8),
                            Color(0xFF60C8F2),
                        ),
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
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
                        text = "今日错题 ${uiState.todayMistakesCount}道 待巩固",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.22f), androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "继续加油！",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                        )
                        Text(
                            text = "💪",
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
                uiState.stats.forEach { item ->
                    PracticeStatCard(item = item, modifier = Modifier.weight(1f))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF38ABDA), Color(0xFF7DD3F7)),
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                )
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
                            .background(Color.White.copy(alpha = 0.22f), androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
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
                            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
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
                        .background(Color.White.copy(alpha = 0.14f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🎯", style = MaterialTheme.typography.displaySmall)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(title = "薄弱知识点", action = "查看全部 →")

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.weakTopics.forEach { item ->
                    PracticeWeakTopicCard(item = item)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "为你推荐 ✨",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1A3550),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "智能选题",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8BA4B8),
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

            if (uiState.recommendedPractices.isNotEmpty()) {
                HorizontalPager(
                    state = practicePagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(208.dp),
                    pageSpacing = 0.dp,
                    userScrollEnabled = uiState.recommendedPractices.size > 1,
                ) { page ->
                    val actualIndex = page.mod(uiState.recommendedPractices.size)
                    PracticeRecommendationCard(
                        item = uiState.recommendedPractices[actualIndex],
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (uiState.recommendedPractices.size > 1) {
                    CarouselIndicatorRow(
                        currentIndex = practicePagerState.currentPage.mod(uiState.recommendedPractices.size),
                        totalCount = uiState.recommendedPractices.size,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .shadow(8.dp, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF7DD3F7), Color(0xFF38ABDA))),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "再做一题 →",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .shadow(6.dp, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "错题本",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1A3550),
                    fontWeight = FontWeight.ExtraBold,
                )
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

private fun practiceRouteMockUiState(): PracticeUiState = PracticeUiState(
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

private fun loopingPagerInitialPage(itemCount: Int): Int {
    if (itemCount <= 1) return 0
    val midpoint = Int.MAX_VALUE / 2
    return midpoint - midpoint % itemCount
}

@Composable
private fun CarouselIndicatorRow(
    currentIndex: Int,
    totalCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalCount) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == currentIndex) 18.dp else 7.dp, height = 7.dp)
                        .background(
                            color = if (index == currentIndex) Color(0xFF38ABDA) else Color(0xFFD8EAF4),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun PracticeWeakTopicCard(
    item: PracticeWeakTopicUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF9FCFF), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
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
                    .background(item.chipBackground, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
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
private fun SectionHeader(
    title: String,
    action: String,
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
        )
    }
}