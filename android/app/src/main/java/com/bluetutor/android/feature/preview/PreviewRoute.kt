package com.bluetutor.android.feature.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
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
import com.bluetutor.android.feature.preview.data.PreviewApiChatMessage
import com.bluetutor.android.feature.preview.data.PreviewApiChatResult
import com.bluetutor.android.feature.preview.data.PreviewApiClient
import com.bluetutor.android.feature.preview.data.PreviewApiKnowledgePoint
import com.bluetutor.android.feature.preview.data.PreviewLocalCache
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PreviewDestination {
    Home,
    Handout,
    Chat,
}

private data class PreviewSelectedExcerptUiState(
    val text: String,
    val sectionTitle: String?,
)

@Composable
fun PreviewRoute(
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var selectedTopicId by rememberSaveable { mutableIntStateOf(3) }
    var uploadStage by remember { mutableStateOf(PreviewUploadStage.Idle) }
    var uploadedFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var workspace by remember { mutableStateOf<PreviewTopicWorkspaceUiState?>(null) }
    var destination by remember { mutableStateOf(PreviewDestination.Home) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uploadStage) {
        if (uploadStage == PreviewUploadStage.Processing) {
            delay(1400)
            uploadStage = PreviewUploadStage.Success
        }
    }

    LaunchedEffect(destination) {
        onBottomBarVisibilityChange(destination == PreviewDestination.Home)
    }

    DisposableEffect(Unit) {
        onDispose {
            onBottomBarVisibilityChange(true)
        }
    }

    val uiState = remember(uploadStage, uploadedFileName) {
        previewMockUiState(
            uploadStage = uploadStage,
            uploadedFileName = uploadedFileName,
        )
    }

    fun persistWorkspace(nextState: PreviewTopicWorkspaceUiState) {
        PreviewLocalCache.saveTopicCache(
            context = context,
            topicId = nextState.topic.id,
            summary = nextState.aiSummary,
            knowledgePoints = nextState.knowledgePoints,
            selectedKnowledgePointIds = nextState.selectedKnowledgePointIds,
            chatSessions = nextState.chatSessions,
            activeChatSessionId = nextState.activeChatSessionId,
        )
    }

    fun buildFreshSession(
        topic: PreviewQuickTopicUiModel,
        sourceExcerpt: String? = null,
        sourceSectionTitle: String? = null,
        seedTitle: String? = null,
    ): PreviewChatSessionUiModel {
        val now = System.currentTimeMillis()
        return PreviewChatSessionUiModel(
            id = "session-$now-${topic.id}",
            title = buildSessionTitle(seedTitle ?: sourceExcerpt ?: "新会话"),
            sourceExcerpt = sourceExcerpt,
            sourceSectionTitle = sourceSectionTitle,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
    }

    fun restoreWorkspace(topic: PreviewQuickTopicUiModel): PreviewTopicWorkspaceUiState {
        val cached = PreviewLocalCache.readTopicCache(context, topic.id)
        val cachedKnowledgePoints = cached?.knowledgePoints.orEmpty()
        val selectedIds = when {
            cached != null && cached.selectedKnowledgePointIds.isNotEmpty() -> cached.selectedKnowledgePointIds
            cachedKnowledgePoints.isNotEmpty() -> cachedKnowledgePoints.map { it.id }.toSet()
            else -> emptySet()
        }
        val sessions = cached?.chatSessions
            ?.sortedByDescending { it.updatedAtMillis }
            ?.ifEmpty { listOf(buildFreshSession(topic)) }
            ?: listOf(buildFreshSession(topic))
        val activeSessionId = cached?.activeChatSessionId
            ?.takeIf { candidate -> sessions.any { it.id == candidate } }
            ?: sessions.first().id

        return PreviewTopicWorkspaceUiState(
            topic = topic,
            aiSummary = cached?.summary.orEmpty(),
            knowledgePoints = cachedKnowledgePoints,
            selectedKnowledgePointIds = selectedIds,
            chatSessions = sessions,
            activeChatSessionId = activeSessionId,
        )
    }

    fun updateWorkspace(transform: (PreviewTopicWorkspaceUiState) -> PreviewTopicWorkspaceUiState) {
        workspace = workspace?.let(transform)
    }

    fun saveWorkspace(transform: (PreviewTopicWorkspaceUiState) -> PreviewTopicWorkspaceUiState) {
        workspace = workspace?.let { current ->
            transform(current).also(::persistWorkspace)
        }
    }

    fun refreshTopicDigest(topic: PreviewQuickTopicUiModel, forceRefresh: Boolean) {
        val current = workspace?.takeIf { it.topic.id == topic.id } ?: restoreWorkspace(topic)
        if (!forceRefresh && current.aiSummary.isNotBlank() && current.knowledgePoints.isNotEmpty()) {
            workspace = current
            return
        }

        workspace = current.copy(
            isDigestLoading = true,
            digestErrorMessage = null,
        )

        scope.launch {
            runCatching {
                PreviewApiClient.fetchQuickTopicPreview(
                    topicId = topic.id,
                    topicTitle = topic.label,
                    seedContent = topic.seedContent,
                )
            }.onSuccess { result ->
                workspace = workspace?.takeIf { it.topic.id == topic.id }?.let { state ->
                    val nextKnowledgePoints = result.knowledgePoints.toUiModels()
                    val nextIds = nextKnowledgePoints.map { it.id }.toSet()
                    state.copy(
                        isDigestLoading = false,
                        digestErrorMessage = null,
                        aiSummary = result.summary,
                        knowledgePoints = nextKnowledgePoints,
                        selectedKnowledgePointIds = nextIds,
                    ).also(::persistWorkspace)
                }
            }.onFailure { error ->
                updateWorkspace { state ->
                    if (state.topic.id != topic.id) {
                        state
                    } else {
                        state.copy(
                            isDigestLoading = false,
                            digestErrorMessage = error.toPreviewErrorMessage(),
                        )
                    }
                }
            }
        }
    }

    fun openQuickTopic(topic: PreviewQuickTopicUiModel) {
        selectedTopicId = topic.id
        val restored = restoreWorkspace(topic)
        workspace = restored
        destination = PreviewDestination.Handout
        if (restored.aiSummary.isBlank() && restored.knowledgePoints.isEmpty()) {
            refreshTopicDigest(topic, forceRefresh = false)
        }
    }

    fun createNewSession(
        topic: PreviewQuickTopicUiModel,
        sourceExcerpt: String? = null,
        sourceSectionTitle: String? = null,
        prefilledQuestion: String? = null,
    ) {
        val current = workspace?.takeIf { it.topic.id == topic.id } ?: restoreWorkspace(topic)
        val newSession = buildFreshSession(
            topic = topic,
            sourceExcerpt = sourceExcerpt,
            sourceSectionTitle = sourceSectionTitle,
            seedTitle = sourceSectionTitle ?: sourceExcerpt,
        )
        val nextState = current.copy(
            chatSessions = listOf(newSession) + current.chatSessions.filterNot { it.id == newSession.id },
            activeChatSessionId = newSession.id,
            draftQuestion = prefilledQuestion.orEmpty(),
            chatErrorMessage = null,
        )
        workspace = nextState
        persistWorkspace(nextState)
    }

    fun openAiChat(
        topic: PreviewQuickTopicUiModel,
        sourceExcerpt: String? = null,
        sourceSectionTitle: String? = null,
        prefilledQuestion: String? = null,
    ) {
        val restored = workspace?.takeIf { it.topic.id == topic.id } ?: restoreWorkspace(topic)
        workspace = restored

        if (!sourceExcerpt.isNullOrBlank()) {
            createNewSession(
                topic = topic,
                sourceExcerpt = sourceExcerpt,
                sourceSectionTitle = sourceSectionTitle,
                prefilledQuestion = prefilledQuestion ?: "请帮我解释这段内容",
            )
        } else if (restored.chatSessions.isEmpty()) {
            createNewSession(topic = topic)
        }

        destination = PreviewDestination.Chat
        val activeState = workspace ?: restored
        if (activeState.aiSummary.isBlank() && activeState.knowledgePoints.isEmpty() && !activeState.isDigestLoading) {
            refreshTopicDigest(topic, forceRefresh = false)
        }
    }

    fun switchChatSession(sessionId: String) {
        saveWorkspace { state ->
            state.copy(
                activeChatSessionId = sessionId,
                draftQuestion = "",
                chatErrorMessage = null,
            )
        }
    }

    fun clearCurrentChatSession() {
        saveWorkspace { state ->
            val activeSession = state.activeChatSession() ?: return@saveWorkspace state
            val remaining = state.chatSessions.filterNot { it.id == activeSession.id }
            val nextSessions = if (remaining.isEmpty()) {
                listOf(buildFreshSession(state.topic))
            } else {
                remaining
            }
            state.copy(
                isSending = false,
                chatErrorMessage = null,
                draftQuestion = "",
                chatSessions = nextSessions,
                activeChatSessionId = nextSessions.first().id,
            )
        }
    }

    fun updateDraftQuestion(value: String) {
        updateWorkspace { state ->
            state.copy(
                draftQuestion = value,
                chatErrorMessage = null,
            )
        }
    }

    fun sendQuestion(prefilledQuestion: String? = null) {
        val state = workspace ?: return
        if (state.isSending) return

        val activeSession = state.activeChatSession() ?: return
        val question = (prefilledQuestion ?: state.draftQuestion).trim()
        if (question.isEmpty()) return

        val history = activeSession.messages.takeLast(8).map { it.toApiMessage() }
        val selectedKnowledgePoints = when {
            state.selectedKnowledgePointIds.isNotEmpty() -> {
                state.knowledgePoints
                    .filter { it.id in state.selectedKnowledgePointIds }
                    .map { it.title }
            }

            state.knowledgePoints.isNotEmpty() -> state.knowledgePoints.take(3).map { it.title }
            !activeSession.sourceSectionTitle.isNullOrBlank() -> listOf(activeSession.sourceSectionTitle)
            else -> emptyList()
        }

        val userMessage = PreviewConversationMessageUiModel(
            id = buildMessageId("user"),
            role = PreviewConversationRole.User,
            text = question,
        )

        val sendingState = state
            .withUpdatedChatSession(activeSession.id) { session ->
                val nextMessages = session.messages + userMessage
                session.copy(
                    title = buildSessionTitle(if (session.messages.isEmpty()) question else session.title),
                    updatedAtMillis = System.currentTimeMillis(),
                    messages = nextMessages,
                )
            }
            .copy(
                draftQuestion = "",
                isSending = true,
                chatErrorMessage = null,
            )

        workspace = sendingState
        persistWorkspace(sendingState)

        scope.launch {
            runCatching {
                PreviewApiClient.sendQuickTopicQuestion(
                    topicId = state.topic.id,
                    topicTitle = state.topic.label,
                    contextText = buildQuickTopicContext(
                        topic = state.topic,
                        aiSummary = state.aiSummary,
                        activeSession = activeSession,
                    ),
                    selectedKnowledgePoints = selectedKnowledgePoints,
                    question = question,
                    history = history,
                )
            }.onSuccess { result ->
                workspace = workspace?.takeIf { it.topic.id == state.topic.id }?.let { current ->
                    current
                        .withUpdatedChatSession(activeSession.id) { session ->
                            session.copy(
                                updatedAtMillis = System.currentTimeMillis(),
                                messages = session.messages + result.toAssistantMessage(),
                            )
                        }
                        .copy(
                            isSending = false,
                            chatErrorMessage = null,
                        )
                        .also(::persistWorkspace)
                }
            }.onFailure { error ->
                workspace = workspace?.takeIf { it.topic.id == state.topic.id }?.let { current ->
                    current
                        .withUpdatedChatSession(activeSession.id) { session ->
                            session.copy(
                                updatedAtMillis = System.currentTimeMillis(),
                                messages = session.messages.dropLast(1),
                            )
                        }
                        .copy(
                            isSending = false,
                            chatErrorMessage = error.toPreviewErrorMessage(),
                            draftQuestion = question,
                        )
                        .also(::persistWorkspace)
                }
            }
        }
    }

    when (destination) {
        PreviewDestination.Home -> PreviewHomeScreen(
            uiState = uiState,
            selectedTopicId = selectedTopicId,
            onSelectTopic = { selectedTopicId = it.id },
            onOpenQuickTopic = ::openQuickTopic,
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

        PreviewDestination.Handout -> {
            val session = workspace
            if (session == null) {
                destination = PreviewDestination.Home
            } else {
                PreviewHandoutScreen(
                    session = session,
                    onBack = { destination = PreviewDestination.Home },
                    onOpenAiChat = { openAiChat(session.topic) },
                    onAskSelectedText = { excerpt, sectionTitle ->
                        openAiChat(
                            topic = session.topic,
                            sourceExcerpt = excerpt,
                            sourceSectionTitle = sectionTitle,
                            prefilledQuestion = "请帮我解释这段内容",
                        )
                    },
                    modifier = modifier,
                )
            }
        }

        PreviewDestination.Chat -> {
            val session = workspace
            if (session == null) {
                destination = PreviewDestination.Home
            } else {
                PreviewAiChatScreen(
                    session = session,
                    onBack = { destination = PreviewDestination.Handout },
                    onDraftQuestionChange = ::updateDraftQuestion,
                    onSendQuestion = { sendQuestion() },
                    onNewSession = { createNewSession(session.topic) },
                    onSelectSession = ::switchChatSession,
                    onClearCurrentSession = ::clearCurrentChatSession,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun PreviewHomeScreen(
    uiState: PreviewUiState,
    selectedTopicId: Int,
    onSelectTopic: (PreviewQuickTopicUiModel) -> Unit,
    onOpenQuickTopic: (PreviewQuickTopicUiModel) -> Unit,
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
                        onClick = {
                            onSelectTopic(topic)
                            onOpenQuickTopic(topic)
                        },
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
                onActionClick = onSecondaryUploadAction,
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
                    val lesson = uiState.recommendedLessons[actualIndex]
                    PreviewRecommendedLessonCard(
                        lesson = lesson,
                        onActionClick = {
                            uiState.quickTopics
                                .firstOrNull { it.id == lesson.topicId }
                                ?.let(onOpenQuickTopic)
                        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewHandoutScreen(
    session: PreviewTopicWorkspaceUiState,
    onBack: () -> Unit,
    onOpenAiChat: () -> Unit,
    onAskSelectedText: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedExcerpt by remember(session.topic.id) { mutableStateOf<PreviewSelectedExcerptUiState?>(null) }
    var selectionResetToken by remember(session.topic.id) { mutableIntStateOf(0) }
    val handout = session.topic.handout

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = session.topic.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            selectedExcerpt?.let { excerpt ->
                PreviewSelectedExcerptBar(
                    excerpt = excerpt.text,
                    onAskAi = {
                        onAskSelectedText(excerpt.text, excerpt.sectionTitle)
                        selectedExcerpt = null
                        selectionResetToken += 1
                    },
                    onDismiss = {
                        selectedExcerpt = null
                        selectionResetToken += 1
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BluetutorGradients.pageBackground())
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sectionGap),
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "${session.topic.emoji} ${handout.articleTitle}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = handout.articleSubtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = session.topic.grade,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = handout.introduction,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            handout.blocks.forEach { block ->
                PreviewArticleBlock(
                    block = block,
                    resetToken = selectionResetToken,
                    onSelectionChanged = { text ->
                        selectedExcerpt = text?.takeIf { it.isNotBlank() }?.let {
                            PreviewSelectedExcerptUiState(
                                text = it,
                                sectionTitle = block.sectionTitle.ifBlank { block.text },
                            )
                        }
                    },
                )
            }

            PreviewAiEntryCard(
                session = session,
                footerPrompt = handout.footerPrompt,
                onOpenAiChat = onOpenAiChat,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewAiChatScreen(
    session: PreviewTopicWorkspaceUiState,
    onBack: () -> Unit,
    onDraftQuestionChange: (String) -> Unit,
    onSendQuestion: () -> Unit,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onClearCurrentSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSession = session.activeChatSession()
    var menuExpanded by remember { mutableStateOf(false) }
    val messageScrollState = rememberScrollState()

    LaunchedEffect(activeSession?.id, activeSession?.messages?.size) {
        messageScrollState.animateScrollTo(messageScrollState.maxValue)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = buildChatTopBarTitle(
                            topicLabel = session.topic.label,
                            sessionTitle = activeSession?.title,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回讲义",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "更多",
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("新开会话") },
                                onClick = {
                                    menuExpanded = false
                                    onNewSession()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("清空当前会话") },
                                onClick = {
                                    menuExpanded = false
                                    onClearCurrentSession()
                                },
                            )

                            if (session.chatSessions.size > 1) {
                                HorizontalDivider()
                                session.chatSessions
                                    .sortedByDescending { it.updatedAtMillis }
                                    .take(6)
                                    .forEach { chatSession ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = chatSession.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onSelectSession(chatSession.id)
                                            },
                                        )
                                    }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            PreviewChatInputBar(
                draftQuestion = session.draftQuestion,
                isSending = session.isSending,
                onDraftQuestionChange = onDraftQuestionChange,
                onSendQuestion = onSendQuestion,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BluetutorGradients.pageBackground())
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .padding(top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            activeSession?.sourceExcerpt?.takeIf { it.isNotBlank() }?.let { excerpt ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = activeSession.sourceSectionTitle ?: "当前引用",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = excerpt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            if (session.chatErrorMessage != null) {
                Text(
                    text = session.chatErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(messageScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    activeSession == null -> PreviewChatEmptyState()
                    activeSession.messages.isEmpty() -> PreviewChatEmptyState(activeSession.sourceExcerpt != null)
                    else -> {
                        activeSession.messages.forEach { message ->
                            PreviewConversationBubble(
                                message = message,
                                onUseFollowUpQuestion = onDraftQuestionChange,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewArticleBlock(
    block: PreviewHandoutBlockUiModel,
    resetToken: Int,
    onSelectionChanged: (String?) -> Unit,
) {
    when (block.type) {
        PreviewHandoutBlockType.SectionHeading -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        PreviewHandoutBlockType.Paragraph -> {
            PreviewSelectableArticleText(
                blockId = block.id,
                text = block.text,
                resetToken = resetToken,
                onSelectionChanged = onSelectionChanged,
            )
        }

        PreviewHandoutBlockType.Formula -> {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (block.title.isNotBlank()) {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    PreviewSelectableArticleText(
                        blockId = block.id,
                        text = block.text,
                        resetToken = resetToken,
                        onSelectionChanged = onSelectionChanged,
                        emphasis = true,
                        bodyColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (block.supportingText.isNotBlank()) {
                        PreviewSelectableArticleText(
                            blockId = "${block.id}_supporting",
                            text = block.supportingText,
                            resetToken = resetToken,
                            onSelectionChanged = onSelectionChanged,
                            bodyColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        )
                    }
                }
            }
        }

        PreviewHandoutBlockType.ThinkingPrompt -> {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.48f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = block.title.ifBlank { "读完先想一想" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    PreviewSelectableArticleText(
                        blockId = block.id,
                        text = block.text,
                        resetToken = resetToken,
                        onSelectionChanged = onSelectionChanged,
                        bodyColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        PreviewHandoutBlockType.Note -> {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.44f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = block.title.ifBlank { "易错提醒" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    PreviewSelectableArticleText(
                        blockId = block.id,
                        text = block.text,
                        resetToken = resetToken,
                        onSelectionChanged = onSelectionChanged,
                        bodyColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSelectableArticleText(
    blockId: String,
    text: String,
    resetToken: Int,
    onSelectionChanged: (String?) -> Unit,
    emphasis: Boolean = false,
    bodyColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    var value by remember(blockId, resetToken, text) {
        mutableStateOf(TextFieldValue(text = text))
    }

    BasicTextField(
        value = value,
        onValueChange = { nextValue ->
            value = nextValue
            val range = nextValue.selection
            val selectedText = if (range.collapsed) {
                null
            } else {
                nextValue.text.substring(range.start, range.end).trim().ifBlank { null }
            }
            onSelectionChanged(selectedText)
        },
        readOnly = true,
        cursorBrush = SolidColor(bodyColor),
        textStyle = if (emphasis) {
            MaterialTheme.typography.titleMedium.copy(
                color = bodyColor,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            MaterialTheme.typography.bodyLarge.copy(color = bodyColor)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PreviewSelectedExcerptBar(
    excerpt: String,
    onAskAi: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = excerpt,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onAskAi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("去问 AI")
                }
            }
        }
    }
}

@Composable
private fun PreviewAiEntryCard(
    session: PreviewTopicWorkspaceUiState,
    footerPrompt: String,
    onOpenAiChat: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = footerPrompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    session.isDigestLoading -> "AI 正在后台准备这节讲义的补充上下文。"
                    session.digestErrorMessage != null -> "离线讲义可直接阅读；需要联网时再进入 AI 对话。"
                    session.aiSummary.isNotBlank() -> "AI 已准备好，可以继续围绕这节讲义追问。"
                    else -> "你可以先完整读完，再带着具体一句话去问 AI。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenAiChat,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text("进入 AI 对话")
            }
        }
    }
}

@Composable
private fun PreviewChatEmptyState(fromSelection: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (fromSelection) "已把讲义里的片段带进来了" else "先问一个具体问题",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (fromSelection) {
                    "可以直接点发送，也可以先改写成你更想问的方式。"
                } else {
                    "例如：这一段为什么要这样理解？这个公式和上一题有什么关系？"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewChatInputBar(
    draftQuestion: String,
    isSending: Boolean,
    onDraftQuestionChange: (String) -> Unit,
    onSendQuestion: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = draftQuestion,
                onValueChange = onDraftQuestionChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("例如：这一步为什么要先通分？") },
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(18.dp),
            )
            Button(
                onClick = onSendQuestion,
                enabled = !isSending,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = "➜",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewConversationBubble(
    message: PreviewConversationMessageUiModel,
    onUseFollowUpQuestion: (String) -> Unit,
) {
    val isUserMessage = message.role == PreviewConversationRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(22.dp),
            color = if (isUserMessage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isUserMessage) 0.dp else 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!isUserMessage) {
                    Text(
                        text = "BlueTutor",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUserMessage) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (!isUserMessage && message.followUpQuestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        message.followUpQuestions.forEach { followUpQuestion ->
                            AssistChip(
                                onClick = { onUseFollowUpQuestion(followUpQuestion) },
                                label = { Text(followUpQuestion) },
                            )
                        }
                    }
                }
            }
        }
    }
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
        Row(horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs)) {
            repeat(totalCount) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == currentIndex) 18.dp else 7.dp, height = 7.dp)
                        .background(
                            color = if (index == currentIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            },
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}

private fun previewLoopingInitialPage(itemCount: Int): Int {
    if (itemCount <= 1) return 0
    val midpoint = Int.MAX_VALUE / 2
    return midpoint - midpoint % itemCount
}

private fun List<PreviewApiKnowledgePoint>.toUiModels(): List<PreviewKnowledgePointUiModel> = map {
    PreviewKnowledgePointUiModel(
        id = it.id,
        title = it.title,
        description = it.description,
        confidence = it.confidence,
    )
}

private fun PreviewApiChatResult.toAssistantMessage(): PreviewConversationMessageUiModel {
    return PreviewConversationMessageUiModel(
        id = buildMessageId("assistant"),
        role = PreviewConversationRole.Assistant,
        text = reply,
        followUpQuestions = followUpQuestions,
    )
}

private fun PreviewConversationMessageUiModel.toApiMessage(): PreviewApiChatMessage {
    return PreviewApiChatMessage(
        role = if (role == PreviewConversationRole.User) "user" else "assistant",
        content = text,
    )
}

private fun PreviewTopicWorkspaceUiState.activeChatSession(): PreviewChatSessionUiModel? {
    return chatSessions.firstOrNull { it.id == activeChatSessionId } ?: chatSessions.firstOrNull()
}

private fun PreviewTopicWorkspaceUiState.withUpdatedChatSession(
    sessionId: String,
    transform: (PreviewChatSessionUiModel) -> PreviewChatSessionUiModel,
): PreviewTopicWorkspaceUiState {
    val nextSessions = chatSessions.map { session ->
        if (session.id == sessionId) transform(session) else session
    }.sortedByDescending { it.updatedAtMillis }

    return copy(
        chatSessions = nextSessions,
        activeChatSessionId = nextSessions.firstOrNull { it.id == sessionId }?.id ?: activeChatSessionId,
    )
}

private fun buildQuickTopicContext(
    topic: PreviewQuickTopicUiModel,
    aiSummary: String,
    activeSession: PreviewChatSessionUiModel?,
): String {
    val handout = topic.handout
    return buildString {
        append("专题：")
        append(topic.label)
        append("\n年级：")
        append(topic.grade)
        append("\n导语：")
        append(topic.intro)
        append("\n讲义标题：")
        append(handout.articleTitle)
        append("\n讲义摘要：")
        append(handout.articleSubtitle)
        append("\n\n离线讲义内容：\n")
        handout.blocks.forEach { block ->
            when (block.type) {
                PreviewHandoutBlockType.SectionHeading -> {
                    append("\n# ")
                    append(block.text)
                    append("\n")
                }

                PreviewHandoutBlockType.Paragraph,
                PreviewHandoutBlockType.Note,
                PreviewHandoutBlockType.ThinkingPrompt,
                PreviewHandoutBlockType.Formula,
                -> {
                    if (block.title.isNotBlank()) {
                        append(block.title)
                        append("：")
                    }
                    append(block.text)
                    append("\n")
                    if (block.supportingText.isNotBlank()) {
                        append(block.supportingText)
                        append("\n")
                    }
                }
            }
        }
        if (!activeSession?.sourceExcerpt.isNullOrBlank()) {
            append("\n当前聚焦片段：")
            append(activeSession?.sourceExcerpt)
            append("\n")
        }
        if (aiSummary.isNotBlank()) {
            append("\nAI 已整理重点：\n")
            append(aiSummary)
        }
        append("\n\n原始种子内容：\n")
        append(topic.seedContent)
    }
}

private fun buildChatTopBarTitle(topicLabel: String, sessionTitle: String?): String {
    val cleanSessionTitle = sessionTitle?.trim().orEmpty()
    return if (cleanSessionTitle.isEmpty()) {
        topicLabel
    } else {
        "$topicLabel · $cleanSessionTitle"
    }
}

private fun buildSessionTitle(seed: String): String {
    val normalized = seed
        .replace("\n", " ")
        .trim()
        .ifBlank { "新会话" }
    return if (normalized.length <= 14) normalized else normalized.take(14) + "..."
}

private fun buildMessageId(prefix: String): String = "$prefix-${System.nanoTime()}"

private fun Throwable.toPreviewErrorMessage(): String {
    val originalMessage = message?.trim().orEmpty()
    return if (originalMessage.isNotEmpty()) {
        originalMessage
    } else {
        "暂时无法连接到本地预习服务，请确认 FastAPI 已启动。"
    }
}