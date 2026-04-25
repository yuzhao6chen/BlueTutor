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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.practice.PracticeDestination
import com.bluetutor.android.feature.practice.PracticeDetailState
import com.bluetutor.android.feature.practice.data.MistakeLectureResult
import com.bluetutor.android.feature.practice.data.MistakeLectureSectionResult
import com.bluetutor.android.feature.practice.data.MistakeReviewStepResult
import com.bluetutor.android.feature.practice.data.MistakesApiClient
import com.bluetutor.android.feature.practice.data.PracticeLocalCache
import com.bluetutor.android.ui.theme.BluetutorGradients
import kotlinx.coroutines.launch

@Composable
fun PracticeDetailScreen(
    reportId: String,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(context, reportId) {
        val cachedLecture = PracticeLocalCache.readLecture(context, reportId)
        mutableStateOf(
            PracticeDetailState(
                isLoading = cachedLecture == null,
                lecture = cachedLecture,
            ),
        )
    }

    LaunchedEffect(reportId) {
        onBottomBarVisibilityChange(false)
        try {
            val lecture = MistakesApiClient.getLecture(reportId)
            PracticeLocalCache.saveLecture(context, lecture)
            state = state.copy(isLoading = false, lecture = lecture)
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
            title = "错题详情",
            onBack = onBack,
            action = {
                if (state.lecture != null) {
                    Text(
                        text = if (state.lecture!!.status == "pending") "待巩固" else "已巩固",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.lecture!!.status == "pending") Color(0xFF92400E) else Color(0xFF065F46),
                        modifier = Modifier
                            .background(
                                if (state.lecture!!.status == "pending") Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            },
        )

        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.weight(1f))
            state.error != null && state.lecture == null -> ErrorScreen(
                message = state.error ?: "加载失败",
                onRetry = {
                    scope.launch {
                        state = state.copy(isLoading = true, error = null)
                        try {
                            val lecture = MistakesApiClient.getLecture(reportId)
                            PracticeLocalCache.saveLecture(context, lecture)
                            state = state.copy(isLoading = false, lecture = lecture)
                        } catch (e: Exception) {
                            state = state.copy(isLoading = false, error = e.message)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            state.lecture != null -> LectureContent(
                lecture = state.lecture!!,
                onRedo = { onNavigate(PracticeDestination.Redo(reportId)) },
                onDialogue = { onNavigate(PracticeDestination.Dialogue(reportId)) },
                onGenerateRecommendation = { type ->
                    onNavigate(PracticeDestination.Recommendation(reportId, type))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LectureContent(
    lecture: MistakeLectureResult,
    onRedo: () -> Unit,
    onDialogue: () -> Unit,
    onGenerateRecommendation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProblemHeader(lecture = lecture)

        if (lecture.reviewSteps.isNotEmpty()) {
            ReviewStepsCard(steps = lecture.reviewSteps)
        }

        lecture.lectureSections.forEach { section ->
            LectureSectionCard(section = section)
        }

        if (lecture.keyTakeaways.isNotEmpty()) {
            KeyTakeawaysCard(takeaways = lecture.keyTakeaways)
        }

        ActionButtonsSection(
            onRedo = onRedo,
            onDialogue = onDialogue,
            onSimilarQuestion = { onGenerateRecommendation("similar") },
            onVariantQuestion = { onGenerateRecommendation("variant") },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProblemHeader(lecture: MistakeLectureResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = lecture.reportTitle,
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
                text = lecture.problemText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2A4A60),
            )
        }

        if (lecture.answer.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD1FAE5).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "💡", style = MaterialTheme.typography.bodyLarge)
                Column {
                    Text(
                        text = "参考答案",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF065F46),
                    )
                    Text(
                        text = lecture.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2A4A60),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (lecture.knowledgeTags.isNotEmpty()) {
                lecture.knowledgeTags.forEach { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier
                            .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (lecture.primaryErrorType.isNotEmpty() && lecture.primaryErrorType != "待分析") {
                Text(
                    text = lecture.primaryErrorType,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF92400E),
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ReviewStepsCard(steps: List<MistakeReviewStepResult>) {
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
            Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "复盘步骤",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.Bold,
            )
        }

        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (step.status == "focus") Color(0xFFF59E0B) else Color(0xFF10B981),
                                RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = step.stepNo.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (step.stepNo < steps.size) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(Color(0xFFE8F2FA)),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (step.status == "focus") Color(0xFFF59E0B) else Color(0xFF1A3550),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = step.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5A7A90),
                    )
                }
            }
        }
    }
}

@Composable
private fun LectureSectionCard(section: MistakeLectureSectionResult) {
    val (icon, bgColor, textColor) = when (section.kind) {
        "summary" -> Triple("📋", Color(0xFFE0F2FE), Color(0xFF075985))
        "knowledge" -> Triple("📚", Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        "thinking" -> Triple("🧠", Color(0xFFEDE9FE), Color(0xFF6D28D9))
        "error" -> Triple("⚠️", Color(0xFFFCE7F3), Color(0xFF9D174D))
        "solution" -> Triple("✏️", Color(0xFFD1FAE5), Color(0xFF065F46))
        "suggestion" -> Triple("💡", Color(0xFFFEF3C7), Color(0xFF92400E))
        else -> Triple("📄", Color(0xFFF0F4F8), Color(0xFF4A6070))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2A4A60),
            )
        }
    }
}

@Composable
private fun KeyTakeawaysCard(takeaways: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "🔑", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "关键要点",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.Bold,
            )
        }

        takeaways.forEach { takeaway ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF38ABDA),
                )
                Text(
                    text = takeaway,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2A4A60),
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onRedo: () -> Unit,
    onDialogue: () -> Unit,
    onSimilarQuestion: () -> Unit,
    onVariantQuestion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "巩固练习",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryActionButton(
                text = "重做此题",
                icon = "🔄",
                onClick = onRedo,
                modifier = Modifier.weight(1f),
            )
            PrimaryActionButton(
                text = "讲给AI听",
                icon = "🗣️",
                onClick = onDialogue,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = "相似题",
                icon = "📋",
                onClick = onSimilarQuestion,
                modifier = Modifier.weight(1f),
            )
            SecondaryActionButton(
                text = "变式题",
                icon = "🔀",
                onClick = onVariantQuestion,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(
                brush = Brush.horizontalGradient(listOf(Color(0xFF4A90E2), Color(0xFF1D67C7))),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(Color(0xFFEEF7FF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, style = MaterialTheme.typography.bodySmall)
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF38ABDA),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
