package com.bluetutor.android.feature.practice.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.practice.PracticeDestination
import com.bluetutor.android.feature.practice.PracticeRedoState
import com.bluetutor.android.feature.practice.data.MistakeRedoOptionResult
import com.bluetutor.android.feature.practice.data.MistakeRedoSessionResult
import com.bluetutor.android.feature.practice.data.MistakeRedoTurnResult
import com.bluetutor.android.feature.practice.data.MistakesApiClient
import com.bluetutor.android.feature.practice.resultColor
import com.bluetutor.android.feature.practice.resultDisplayName
import com.bluetutor.android.feature.practice.stageDisplayName
import com.bluetutor.android.feature.practice.stageProgress
import com.bluetutor.android.ui.theme.BluetutorGradients
import kotlinx.coroutines.launch

@Composable
fun PracticeRedoScreen(
    reportId: String,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(PracticeRedoState()) }

    LaunchedEffect(reportId) {
        onBottomBarVisibilityChange(false)
        try {
            val session = MistakesApiClient.startRedoSession(reportId)
            state = state.copy(isLoading = false, session = session)
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
            title = "重做练习",
            onBack = onBack,
        )

        when {
            state.isLoading && state.session == null -> LoadingScreen(modifier = Modifier.weight(1f))
            state.error != null && state.session == null -> ErrorScreen(
                message = state.error ?: "启动重做失败",
                onRetry = {
                    scope.launch {
                        state = state.copy(isLoading = true, error = null)
                        try {
                            val session = MistakesApiClient.startRedoSession(reportId)
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
                                    MistakesApiClient.updateReportStatus(reportId, "mastered")
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
                                    state = state.copy(
                                        isSubmitting = false,
                                        session = updated,
                                        showHint = false,
                                    )
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
fun PracticeRedoSessionScreen(
    sessionId: String,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(PracticeRedoState()) }

    LaunchedEffect(sessionId) {
        onBottomBarVisibilityChange(false)
        try {
            val session = MistakesApiClient.getRedoSession(sessionId)
            state = state.copy(isLoading = false, session = session)
        } catch (e: Exception) {
            state = state.copy(isLoading = false, error = e.message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground()),
    ) {
        TopBar(title = "重做练习", onBack = onBack)

        when {
            state.isLoading && state.session == null -> LoadingScreen(modifier = Modifier.weight(1f))
            state.error != null && state.session == null -> ErrorScreen(
                message = state.error ?: "加载失败",
                onRetry = {
                    scope.launch {
                        state = state.copy(isLoading = true, error = null)
                        try {
                            val session = MistakesApiClient.getRedoSession(sessionId)
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
internal fun RedoInProgressSection(
    state: PracticeRedoState,
    onAnswerSelected: (String) -> Unit,
    onToggleHint: () -> Unit,
    onUpdateFreeText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session!!
    val progress = stageProgress(session.stage)

    Column(modifier = modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF38ABDA),
            trackColor = Color(0xFFE8F2FA),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF38ABDA), RoundedCornerShape(4.dp)),
                )
                Text(
                    text = stageDisplayName(session.stage),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF38ABDA),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "第 ${session.turnCount + 1} 轮",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8AA8B8),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                ) {
                    Text(
                        text = session.currentPrompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1A3550),
                    )
                }
            }

            if (session.lastFeedback.isNotEmpty()) {
                item {
                    val feedbackColor = when {
                        session.lastFeedback.contains("正确") || session.lastFeedback.contains("很好") -> Color(0xFF10B981)
                        session.lastFeedback.contains("不完全") || session.lastFeedback.contains("部分") -> Color(0xFFF59E0B)
                        else -> Color(0xFF38ABDA)
                    }
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(feedbackColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                        ) {
                            Text(
                                text = session.lastFeedback,
                                style = MaterialTheme.typography.bodyMedium,
                                color = feedbackColor,
                            )
                        }
                    }
                }
            }

            if (session.hint.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleHint)
                            .background(Color(0xFFFEF3C7).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (state.showHint) "隐藏提示" else "查看提示（等级 ${session.hintLevel}/${session.maxHintLevel}）",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF92400E),
                        )
                    }
                    AnimatedVisibility(visible = state.showHint) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBEB), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                        ) {
                            Text(
                                text = session.hint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF92400E),
                            )
                        }
                    }
                }
            }

            if (session.history.isNotEmpty()) {
                item {
                    Text(
                        text = "作答记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8AA8B8),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(session.history) { turn ->
                    HistoryTurnItem(turn = turn)
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        AnswerInputSection(
            session = session,
            state = state,
            onAnswerSelected = onAnswerSelected,
            onUpdateFreeText = onUpdateFreeText,
        )
    }
}

@Composable
private fun AnswerInputSection(
    session: MistakeRedoSessionResult,
    state: PracticeRedoState,
    onAnswerSelected: (String) -> Unit,
    onUpdateFreeText: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (session.interactionMode == "single_choice" && session.options.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                session.options.forEach { option ->
                    val isSelected = state.selectedOptionId == option.id
                    OptionButton(
                        option = option,
                        isSelected = isSelected,
                        isEnabled = !state.isSubmitting,
                        onClick = {
                            onAnswerSelected(option.id)
                        },
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.freeTextAnswer,
                    onValueChange = onUpdateFreeText,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "输入你的答案...",
                            color = Color(0xFFB0C4D4),
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38ABDA),
                        unfocusedBorderColor = Color(0xFFE8F2FA),
                        focusedContainerColor = Color(0xFFF9FCFF),
                        unfocusedContainerColor = Color(0xFFF9FCFF),
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (state.freeTextAnswer.isNotBlank() && !state.isSubmitting) {
                                onAnswerSelected(state.freeTextAnswer.trim())
                            }
                        },
                    ),
                    enabled = !state.isSubmitting,
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(
                            brush = if (state.freeTextAnswer.isNotBlank() && !state.isSubmitting)
                                Brush.horizontalGradient(listOf(Color(0xFF7DD3F7), Color(0xFF38ABDA)))
                            else
                                Brush.horizontalGradient(listOf(Color(0xFFD8EAF4), Color(0xFFD8EAF4))),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable(enabled = state.freeTextAnswer.isNotBlank() && !state.isSubmitting) {
                            onAnswerSelected(state.freeTextAnswer.trim())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        if (state.isSubmitting) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF38ABDA),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 老师正在批改...",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8AA8B8),
                )
            }
        }
    }
}

@Composable
private fun OptionButton(
    option: MistakeRedoOptionResult,
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

@Composable
private fun HistoryTurnItem(turn: MistakeRedoTurnResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FCFF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(resultColor(turn.result).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${turn.turnNo}",
                style = MaterialTheme.typography.labelSmall,
                color = resultColor(turn.result),
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = resultDisplayName(turn.result),
                style = MaterialTheme.typography.labelSmall,
                color = resultColor(turn.result),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "你的回答：${turn.studentAnswer}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5A7A90),
            )
            if (turn.feedback.isNotEmpty()) {
                Text(
                    text = turn.feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8AA8B8),
                )
            }
        }
    }
}

@Composable
internal fun RedoCompletedSection(
    session: MistakeRedoSessionResult,
    onBack: () -> Unit,
    onMarkMastered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSuccessful = session.canClearMistake

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isSuccessful) "🎉" else "💪",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSuccessful) "太棒了！你已经掌握这道题" else "继续加油，再来一次吧",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSuccessful) "连续答对 ${session.consecutiveCorrect} 轮，可以标记为已巩固" else "重做完成，但还需要继续巩固",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8AA8B8),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (session.history.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "作答回顾",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1A3550),
                    fontWeight = FontWeight.SemiBold,
                )
                session.history.forEach { turn ->
                    HistoryTurnItem(turn = turn)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSuccessful) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(6.dp, RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable(onClick = onMarkMastered)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "标记为已巩固 ✓",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFEEF7FF), RoundedCornerShape(18.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "返回",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF38ABDA),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
