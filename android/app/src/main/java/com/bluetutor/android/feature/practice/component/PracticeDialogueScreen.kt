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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.practice.PracticeDestination
import com.bluetutor.android.feature.practice.PracticeDialogueState
import com.bluetutor.android.feature.practice.data.MistakeDialogueMessageResult
import com.bluetutor.android.feature.practice.data.MistakeDialogueSessionResult
import com.bluetutor.android.feature.practice.data.MistakesApiClient
import com.bluetutor.android.feature.practice.masteryVerdictDisplayName
import com.bluetutor.android.ui.theme.BluetutorGradients
import kotlinx.coroutines.launch

@Composable
fun PracticeDialogueScreen(
    reportId: String,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(PracticeDialogueState()) }

    LaunchedEffect(reportId) {
        onBottomBarVisibilityChange(false)
        try {
            val session = MistakesApiClient.startDialogueSession(reportId)
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
            title = "讲给AI听",
            onBack = onBack,
            action = {
                if (state.session != null) {
                    Text(
                        text = masteryVerdictDisplayName(state.session!!.masteryVerdict),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (state.session!!.masteryVerdict) {
                            "mastered" -> Color(0xFF065F46)
                            "not_mastered" -> Color(0xFF9D174D)
                            else -> Color(0xFF92400E)
                        },
                        modifier = Modifier
                            .background(
                                when (state.session!!.masteryVerdict) {
                                    "mastered" -> Color(0xFFD1FAE5)
                                    "not_mastered" -> Color(0xFFFCE7F3)
                                    else -> Color(0xFFFEF3C7)
                                },
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            },
        )

        when {
            state.isLoading && state.session == null -> LoadingScreen(modifier = Modifier.weight(1f))
            state.error != null && state.session == null -> ErrorScreen(
                message = state.error ?: "启动对话失败",
                onRetry = {
                    scope.launch {
                        state = state.copy(isLoading = true, error = null)
                        try {
                            val session = MistakesApiClient.startDialogueSession(reportId)
                            state = state.copy(isLoading = false, session = session)
                        } catch (e: Exception) {
                            state = state.copy(isLoading = false, error = e.message)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            state.session != null -> {
                DialogueContent(
                    state = state,
                    onSendMessage = { message ->
                        scope.launch {
                            state = state.copy(isSending = true, inputText = "")
                            try {
                                val updated = MistakesApiClient.advanceDialogueSession(
                                    state.session!!.sessionId,
                                    message,
                                )
                                state = state.copy(isSending = false, session = updated)
                            } catch (e: Exception) {
                                state = state.copy(isSending = false, error = e.message)
                            }
                        }
                    },
                    onUpdateInput = { state = state.copy(inputText = it) },
                    onBack = onBack,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun PracticeDialogueSessionScreen(
    sessionId: String,
    onNavigate: (PracticeDestination) -> Unit,
    onBack: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(PracticeDialogueState()) }

    LaunchedEffect(sessionId) {
        onBottomBarVisibilityChange(false)
        try {
            val session = MistakesApiClient.advanceDialogueSession(sessionId, "")
            state = state.copy(isLoading = false, session = session)
        } catch (e: Exception) {
            try {
                val session = MistakesApiClient.startDialogueSession(sessionId)
                state = state.copy(isLoading = false, session = session)
            } catch (e2: Exception) {
                state = state.copy(isLoading = false, error = e2.message)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground()),
    ) {
        TopBar(title = "讲给AI听", onBack = onBack)

        when {
            state.isLoading && state.session == null -> LoadingScreen(modifier = Modifier.weight(1f))
            state.error != null && state.session == null -> ErrorScreen(
                message = state.error ?: "加载失败",
                onRetry = { onBack() },
                modifier = Modifier.weight(1f),
            )
            state.session != null -> {
                DialogueContent(
                    state = state,
                    onSendMessage = { message ->
                        scope.launch {
                            state = state.copy(isSending = true, inputText = "")
                            try {
                                val updated = MistakesApiClient.advanceDialogueSession(
                                    state.session!!.sessionId,
                                    message,
                                )
                                state = state.copy(isSending = false, session = updated)
                            } catch (e: Exception) {
                                state = state.copy(isSending = false, error = e.message)
                            }
                        }
                    },
                    onUpdateInput = { state = state.copy(inputText = it) },
                    onBack = onBack,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogueContent(
    state: PracticeDialogueState,
    onSendMessage: (String) -> Unit,
    onUpdateInput: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session!!
    val listState = rememberLazyListState()

    LaunchedEffect(session.messages.size) {
        if (session.messages.isNotEmpty()) {
            listState.animateScrollToItem(session.messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.8f))
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = session.problemText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8AA8B8),
                maxLines = 2,
            )
        }

        if (session.isCompleted) {
            DialogueCompletedBanner(
                session = session,
                onBack = onBack,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(session.messages) { message ->
                ChatBubble(message = message)
            }

            if (state.isSending) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF38ABDA),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "AI 老师正在思考...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8AA8B8),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        if (!session.isCompleted) {
            ChatInputBar(
                inputText = state.inputText,
                isSending = state.isSending,
                onSend = onSendMessage,
                onUpdate = onUpdateInput,
            )
        }
    }
}

@Composable
private fun ChatBubble(message: MistakeDialogueMessageResult) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF38ABDA), Color(0xFF7DD3F7))),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🦉", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isUser) Color(0xFF38ABDA) else Color.White,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else Color(0xFF2A4A60),
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFEEF7FF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🙋", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isSending: Boolean,
    onSend: (String) -> Unit,
    onUpdate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .imePadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onUpdate,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(text = "讲讲你的思路...", color = Color(0xFFB0C4D4))
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38ABDA),
                unfocusedBorderColor = Color(0xFFE8F2FA),
                focusedContainerColor = Color(0xFFF9FCFF),
                unfocusedContainerColor = Color(0xFFF9FCFF),
            ),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (inputText.isNotBlank() && !isSending) {
                        onSend(inputText.trim())
                    }
                },
            ),
            enabled = !isSending,
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .background(
                    brush = if (inputText.isNotBlank() && !isSending)
                        Brush.horizontalGradient(listOf(Color(0xFF7DD3F7), Color(0xFF38ABDA)))
                    else
                        Brush.horizontalGradient(listOf(Color(0xFFD8EAF4), Color(0xFFD8EAF4))),
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable(enabled = inputText.isNotBlank() && !isSending) {
                    onSend(inputText.trim())
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isSending) {
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

@Composable
private fun DialogueCompletedBanner(
    session: MistakeDialogueSessionResult,
    onBack: () -> Unit,
) {
    val isMastered = session.masteryVerdict == "mastered"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isMastered) Color(0xFFD1FAE5) else Color(0xFFFCE7F3),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isMastered) "🎉 讲解通过！你已经掌握了这道题" else "💪 还需要继续巩固",
            style = MaterialTheme.typography.labelLarge,
            color = if (isMastered) Color(0xFF065F46) else Color(0xFF9D174D),
            fontWeight = FontWeight.Bold,
        )
        if (session.masteryDetail.isNotEmpty()) {
            Text(
                text = session.masteryDetail,
                style = MaterialTheme.typography.bodySmall,
                color = if (isMastered) Color(0xFF065F46) else Color(0xFF9D174D),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}


