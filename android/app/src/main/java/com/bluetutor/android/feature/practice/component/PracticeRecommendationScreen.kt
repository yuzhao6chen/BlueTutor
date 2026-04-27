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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.practice.PracticeDestination
import com.bluetutor.android.feature.practice.PracticeRecommendationState
import com.bluetutor.android.feature.practice.PracticeRedoState
import com.bluetutor.android.feature.practice.data.MistakeRecommendationOptionResult
import com.bluetutor.android.feature.practice.data.MistakesApiClient
import com.bluetutor.android.feature.practice.data.PracticeLocalCache
import com.bluetutor.android.feature.practice.data.withRecoveredReport
import com.bluetutor.android.feature.practice.difficultyColor
import com.bluetutor.android.feature.practice.difficultyDisplayName
import com.bluetutor.android.ui.theme.BluetutorGradients
import kotlinx.coroutines.launch

@Composable
fun PracticeRecommendationScreen(
    reportId: String,
    recommendationType: String,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(context, reportId, recommendationType) {
        val cachedRecommendation = PracticeLocalCache.readRecommendation(context, reportId, recommendationType)
        mutableStateOf(
            PracticeRecommendationState(
                isLoading = cachedRecommendation == null,
                recommendation = cachedRecommendation,
            ),
        )
    }

    LaunchedEffect(reportId, recommendationType) {
        onBottomBarVisibilityChange(false)
        try {
            val rec = withRecoveredReport(context, reportId) { resolvedReportId ->
                MistakesApiClient.generateRecommendation(resolvedReportId, recommendationType = recommendationType)
            }
            PracticeLocalCache.saveRecommendation(context, reportId, recommendationType, rec)
            state = state.copy(isLoading = false, recommendation = rec)
        } catch (e: Exception) {
            state = state.copy(isLoading = false, error = e.message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground()),
    ) {
        TopBar(
            title = if (recommendationType == "similar") "相似题" else "变式题",
            onBack = onBack,
        )

        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.weight(1f))
            state.error != null && state.recommendation == null -> ErrorScreen(
                message = state.error ?: "生成推荐题失败",
                onRetry = {
                    scope.launch {
                        state = state.copy(isLoading = true, error = null)
                        try {
                            val rec = withRecoveredReport(context, reportId) { resolvedReportId ->
                                MistakesApiClient.generateRecommendation(resolvedReportId, recommendationType = recommendationType)
                            }
                            PracticeLocalCache.saveRecommendation(context, reportId, recommendationType, rec)
                            state = state.copy(isLoading = false, recommendation = rec)
                        } catch (e: Exception) {
                            state = state.copy(isLoading = false, error = e.message)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            state.recommendation != null -> RecommendationContent(
                state = state,
                onAnswerSelected = { optionId ->
                    scope.launch {
                        state = state.copy(isSubmitting = true, selectedOptionId = optionId)
                        try {
                            val result = MistakesApiClient.submitRecommendationAnswer(
                                state.recommendation!!.recommendationId,
                                optionId,
                            )
                            state = state.copy(isSubmitting = false, submitResult = result)
                        } catch (e: Exception) {
                            state = state.copy(isSubmitting = false, error = e.message)
                        }
                    }
                },
                onStartRedo = {
                    onNavigate(PracticeDestination.RecommendationPractice(state.recommendation!!.recommendationId))
                },
                onBack = onBack,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PracticeRecommendationPracticeScreen(
    recommendationId: String,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(context, recommendationId) {
        val cachedSession = PracticeLocalCache.readRedoSessionByRecommendationId(context, recommendationId)
        mutableStateOf(
            PracticeRedoState(
                isLoading = cachedSession == null,
                session = cachedSession,
            ),
        )
    }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(recommendationId) {
        onBottomBarVisibilityChange(false)
        if (!started) {
            started = true
            try {
                val session = MistakesApiClient.startRecommendationRedo(recommendationId)
                PracticeLocalCache.saveRedoSession(context, session)
                state = state.copy(isLoading = false, session = session)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground()),
    ) {
        TopBar(title = "推荐题练习", onBack = onBack)

        when {
            state.isLoading && state.session == null -> LoadingScreen(modifier = Modifier.weight(1f))
            state.error != null && state.session == null -> ErrorScreen(
                message = state.error ?: "启动练习失败",
                onRetry = {
                    scope.launch {
                        state = state.copy(isLoading = true, error = null)
                        try {
                            val session = MistakesApiClient.startRecommendationRedo(recommendationId)
                            PracticeLocalCache.saveRedoSession(context, session)
                            state = state.copy(isLoading = false, session = session)
                        } catch (e: Exception) {
                            state = state.copy(isLoading = false, error = e.message)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            state.session != null -> {
                if (state.session!!.isCompleted) {
                    RedoCompletedSection(
                        session = state.session!!,
                        onBack = onBack,
                        onMarkMastered = {
                            scope.launch {
                                try {
                                    MistakesApiClient.updateReportStatus(state.session!!.reportId, "mastered")
                                    onBack()
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    RedoInProgressSection(
                        state = state,
                        onAnswerSelected = { answer ->
                            scope.launch {
                                state = state.copy(isSubmitting = true, selectedOptionId = null, freeTextAnswer = "")
                                try {
                                    val updated = MistakesApiClient.advanceRedoSession(
                                        state.session!!.sessionId,
                                        answer,
                                    )
                                    PracticeLocalCache.saveRedoSession(context, updated)
                                    state = state.copy(isSubmitting = false, session = updated, showHint = false)
                                } catch (e: Exception) {
                                    state = state.copy(isSubmitting = false, error = e.message)
                                }
                            }
                        },
                        onToggleHint = { state = state.copy(showHint = !state.showHint) },
                        onUpdateFreeText = { state = state.copy(freeTextAnswer = it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationContent(
    state: PracticeRecommendationState,
    onAnswerSelected: (String) -> Unit,
    onStartRedo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rec = state.recommendation!!
    val submitResult = state.submitResult

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val typeLabel = if (rec.recommendationType == "similar") "相似题" else "变式题"
                val (diffBg, diffText) = difficultyColor(rec.difficulty)
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1D4ED8),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Text(
                    text = difficultyDisplayName(rec.difficulty),
                    style = MaterialTheme.typography.labelSmall,
                    color = diffText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(diffBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Text(
                text = rec.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FCFF), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = rec.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2A4A60),
                )
            }

            if (rec.knowledgeTags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rec.knowledgeTags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier
                                .background(Color(0xFFDBEAFE), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        if (submitResult == null) {
            if (rec.options.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "选择答案",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1A3550),
                        fontWeight = FontWeight.SemiBold,
                    )
                    rec.options.forEach { option ->
                        val isSelected = state.selectedOptionId == option.id
                        OptionButton(
                            option = option,
                            isSelected = isSelected,
                            isEnabled = !state.isSubmitting,
                            onClick = { onAnswerSelected(option.id) },
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "这道题没有选项，可以直接开始练习",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8AA8B8),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF4A90E2), Color(0xFF1D67C7))),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .clickable(onClick = onStartRedo)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "开始练习 →",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (submitResult.isCorrect) Color(0xFFD1FAE5) else Color(0xFFFCE7F3),
                        RoundedCornerShape(24.dp),
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (submitResult.isCorrect) "🎉 回答正确！" else "❌ 回答不正确",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (submitResult.isCorrect) Color(0xFF065F46) else Color(0xFF9D174D),
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = submitResult.feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (submitResult.isCorrect) Color(0xFF065F46) else Color(0xFF9D174D),
                )

                if (!submitResult.isCorrect) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF4A90E2), Color(0xFF1D67C7))),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable(onClick = onStartRedo)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "开始练习 →",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .clickable(onClick = onBack)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "返回",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF38ABDA),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "返回",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF38ABDA),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (rec.whyRecommended.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "💡 推荐理由",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1A3550),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = rec.whyRecommended,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8AA8B8),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OptionButton(
    option: MistakeRecommendationOptionResult,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFF38ABDA).copy(alpha = 0.1f) else Color(0xFFF9FCFF),
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (isSelected) Color(0xFF38ABDA) else Color(0xFFE8F2FA),
                    RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = option.id.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else Color(0xFF38ABDA),
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = option.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color(0xFF38ABDA) else Color(0xFF2A4A60),
        )
    }
}
