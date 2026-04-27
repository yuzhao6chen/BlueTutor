package com.bluetutor.android.feature.solve

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.util.TypedValue
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import com.bluetutor.android.core.designsystem.component.BtSectionTitle
import com.bluetutor.android.feature.practice.data.MistakesApiClient
import com.bluetutor.android.feature.solve.data.GuideApiClient
import com.bluetutor.android.feature.solve.data.GuideDialogueMessage
import com.bluetutor.android.feature.solve.data.GuideReportResult
import com.bluetutor.android.feature.solve.data.GuideThinkingNodeResult
import com.bluetutor.android.feature.solve.data.GuideVisualizationResult
import com.bluetutor.android.feature.solve.data.SolveHistoryEntryUiModel
import com.bluetutor.android.feature.solve.data.SolveLocalCache
import com.bluetutor.android.feature.solve.data.SolveSessionCacheSnapshot
import com.bluetutor.android.feature.solve.component.SolveTeacherIllustration
import com.bluetutor.android.feature.solve.data.SolveOcrApiClient
import com.bluetutor.android.ui.theme.BluetutorSpacing
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.html.HtmlPlugin
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
@Composable
fun SolveRoute(
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var recentHistory by remember { mutableStateOf(SolveLocalCache.readHistory(context)) }
    val uiState = solveRouteMockUiState(recentHistory)
    var guideState by remember { mutableStateOf(SolveGuideConversationState()) }
    var reportState by remember { mutableStateOf(SolveArtifactState<GuideReportResult>()) }
    var solutionState by remember { mutableStateOf(SolveArtifactState<String>()) }
    var visualizationState by remember { mutableStateOf(SolveArtifactState<GuideVisualizationResult>()) }
    var destination by remember { mutableStateOf(SolveDestination.Home) }
    var resultMenuExpanded by remember { mutableStateOf(false) }
    var showSolvedCompletionDialog by remember { mutableStateOf(false) }
    var solvedPromptSessionId by remember { mutableStateOf<String?>(null) }
    var mistakesSyncState by remember { mutableStateOf(SolveMistakeSyncState()) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var pendingEditorImageType by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(destination) {
        onBottomBarVisibilityChange(destination == SolveDestination.Home)
    }

    DisposableEffect(Unit) {
        onDispose {
            onBottomBarVisibilityChange(true)
        }
    }

    fun refreshRecentHistory() {
        recentHistory = SolveLocalCache.readHistory(context)
    }

    fun buildHistoryEntry(
        state: SolveGuideConversationState,
        updatedAtMillis: Long,
    ): SolveHistoryEntryUiModel? {
        val sessionId = state.sessionId ?: return null
        val title = state.problemText.trim().ifBlank { "最近引导解题" }
        val latestTutorPrompt = state.dialogueHistory.lastOrNull { it.role == "tutor" }?.content?.trim().orEmpty()
        val subtitle = when {
            state.isSolved -> "已完成，可继续回看完整思路和工作台记录。"
            state.draftInput.isNotBlank() -> "上次输入：${state.draftInput.trim()}"
            latestTutorPrompt.isNotBlank() -> latestTutorPrompt
            state.goal.isNotBlank() -> "目标：${state.goal}"
            else -> "继续回到工作台，把这道题一步步做完。"
        }
        return SolveHistoryEntryUiModel(
            sessionId = sessionId,
            title = title,
            subtitle = subtitle,
            statusTag = if (state.isSolved) "已完成" else "进行中",
            updatedAtMillis = updatedAtMillis,
            isSolved = state.isSolved,
        )
    }

    fun persistGuideSessionSnapshot(state: SolveGuideConversationState) {
        val sessionId = state.sessionId ?: return
        val updatedAtMillis = System.currentTimeMillis()
        SolveLocalCache.saveSessionCache(
            context = context,
            snapshot = SolveSessionCacheSnapshot(
                sessionId = sessionId,
                problemText = state.problemText,
                parsedKnownConditions = state.parsedKnownConditions,
                goal = state.goal,
                referenceAnswer = state.referenceAnswer,
                dialogueHistory = state.dialogueHistory,
                thinkingTree = state.thinkingTree,
                currentStuckNodeId = state.currentStuckNodeId,
                stuckCount = state.stuckCount,
                lastUpdatedNodeId = state.lastUpdatedNodeId,
                isSolved = state.isSolved,
                draftInput = state.draftInput,
                updatedAtMillis = updatedAtMillis,
            ),
        )
        buildHistoryEntry(state, updatedAtMillis)?.let { entry ->
            SolveLocalCache.upsertHistoryEntry(context, entry)
            refreshRecentHistory()
        }
    }

    fun restoreGuideState(snapshot: SolveSessionCacheSnapshot): SolveGuideConversationState {
        return SolveGuideConversationState(
            problemText = snapshot.problemText,
            sessionId = snapshot.sessionId,
            isSubmitting = false,
            isRecognizingImage = false,
            error = null,
            parsedKnownConditions = snapshot.parsedKnownConditions,
            goal = snapshot.goal,
            referenceAnswer = snapshot.referenceAnswer,
            dialogueHistory = snapshot.dialogueHistory,
            thinkingTree = snapshot.thinkingTree,
            currentStuckNodeId = snapshot.currentStuckNodeId,
            stuckCount = snapshot.stuckCount,
            lastUpdatedNodeId = snapshot.lastUpdatedNodeId,
            isSolved = snapshot.isSolved,
            draftInput = snapshot.draftInput,
            isWorkbenchLoading = false,
            streamingTutorMarkdown = "",
        )
    }

    fun shouldRecoverGuideSession(error: Throwable): Boolean {
        return error.message.orEmpty().contains("会话不存在")
    }

    suspend fun recreateGuideSessionFromSnapshot(snapshot: SolveSessionCacheSnapshot): String {
        val rebuiltSessionId = GuideApiClient.createSession(snapshot.problemText)
        snapshot.dialogueHistory
            .filter { it.role == "student" && it.content.isNotBlank() }
            .forEach { message ->
                GuideApiClient.runTurn(rebuiltSessionId, message.content)
            }
        return rebuiltSessionId
    }

    fun clearArtifactStates() {
        reportState = SolveArtifactState()
        solutionState = SolveArtifactState()
        visualizationState = SolveArtifactState()
        resultMenuExpanded = false
        showSolvedCompletionDialog = false
        solvedPromptSessionId = null
        mistakesSyncState = SolveMistakeSyncState()
    }

    fun clearWorkbenchState(problemText: String) {
        clearArtifactStates()
        guideState = guideState.copy(
            problemText = problemText,
            sessionId = null,
            isSubmitting = false,
            isRecognizingImage = false,
            error = null,
            parsedKnownConditions = emptyList(),
            goal = "",
            referenceAnswer = "",
            dialogueHistory = emptyList(),
            thinkingTree = emptyMap(),
            currentStuckNodeId = null,
            stuckCount = 0,
            lastUpdatedNodeId = null,
            isSolved = false,
            draftInput = "",
            isWorkbenchLoading = false,
            streamingTutorMarkdown = "",
        )
    }

    fun loadReport(sessionId: String, forceRefresh: Boolean = false) {
        if (reportState.isLoading) return
        if (reportState.data != null && !forceRefresh) return
        scope.launch {
            reportState = reportState.copy(isLoading = true, error = null)
            try {
                val report = GuideApiClient.generateReport(sessionId)
                reportState = SolveArtifactState(data = report)
                if (!report.solutionMarkdown.isNullOrBlank() && solutionState.data.isNullOrBlank()) {
                    solutionState = SolveArtifactState(data = report.solutionMarkdown)
                }
            } catch (e: Exception) {
                reportState = SolveArtifactState(error = e.message ?: "讲题报告加载失败，请稍后重试")
            }
        }
    }

    fun loadSolution(sessionId: String, forceRefresh: Boolean = false) {
        if (solutionState.isLoading) return
        if (!forceRefresh && !solutionState.data.isNullOrBlank()) return
        scope.launch {
            solutionState = solutionState.copy(isLoading = true, error = null)
            try {
                val solution = if (forceRefresh) {
                    GuideApiClient.generateSolution(sessionId)
                } else {
                    GuideApiClient.getOrGenerateSolution(sessionId)
                }
                solutionState = SolveArtifactState(data = solution)
            } catch (e: Exception) {
                solutionState = SolveArtifactState(error = e.message ?: "题解加载失败，请稍后重试")
            }
        }
    }

    fun loadVisualization(sessionId: String, forceRefresh: Boolean = false) {
        if (visualizationState.isLoading) return
        if (visualizationState.data != null && !forceRefresh) return
        scope.launch {
            visualizationState = visualizationState.copy(isLoading = true, error = null)
            try {
                if (solutionState.data.isNullOrBlank()) {
                    val solution = GuideApiClient.getOrGenerateSolution(sessionId)
                    solutionState = SolveArtifactState(data = solution)
                }
                val visualization = if (forceRefresh) {
                    GuideApiClient.generateVisualization(sessionId)
                } else {
                    GuideApiClient.getOrGenerateVisualization(sessionId)
                }
                visualizationState = SolveArtifactState(data = visualization)
            } catch (e: Exception) {
                visualizationState = SolveArtifactState(error = e.message ?: "可视化加载失败，请稍后重试")
            }
        }
    }

    fun syncSolvedSessionToMistakes(sessionId: String) {
        if (mistakesSyncState.isSyncing || mistakesSyncState.syncedSessionId == sessionId) return
        scope.launch {
            mistakesSyncState = mistakesSyncState.copy(
                syncedSessionId = sessionId,
                isSyncing = true,
                error = null,
            )
            try {
                val report = reportState.data ?: GuideApiClient.generateReport(sessionId).also {
                    reportState = SolveArtifactState(data = it)
                    if (!it.solutionMarkdown.isNullOrBlank() && solutionState.data.isNullOrBlank()) {
                        solutionState = SolveArtifactState(data = it.solutionMarkdown)
                    }
                }
                val ingestResult = MistakesApiClient.ingestGuideReport(
                    sourceSessionId = sessionId,
                    rawProblem = report.rawProblem.ifBlank { guideState.problemText },
                    knownConditions = report.knownConditions,
                    goal = report.goal,
                    answer = report.answer,
                    knowledgeTags = report.knowledgeTags,
                    thinkingChain = report.thinkingChain,
                    errorProfileMarkdown = report.errorProfileMarkdown,
                    independenceMarkdown = report.independenceMarkdown,
                    solution = report.solutionMarkdown,
                    reportTitle = report.rawProblem.take(18).ifBlank { "引导解题完成记录" },
                )
                mistakesSyncState = SolveMistakeSyncState(
                    syncedSessionId = sessionId,
                    syncedReportId = ingestResult.reportId,
                    syncedReportTitle = ingestResult.reportTitle,
                    isSyncing = false,
                    error = null,
                )
            } catch (e: Exception) {
                mistakesSyncState = SolveMistakeSyncState(
                    syncedSessionId = sessionId,
                    isSyncing = false,
                    error = e.message ?: "同步到错题本失败",
                )
            }
        }
    }

    fun openSolvedDestination(target: SolveDestination) {
        val sessionId = guideState.sessionId ?: return
        if (!guideState.isSolved) return
        destination = target
        when (target) {
            SolveDestination.Report -> loadReport(sessionId)
            SolveDestination.Solution -> loadSolution(sessionId)
            SolveDestination.Visualization -> loadVisualization(sessionId)
            else -> Unit
        }
    }

    LaunchedEffect(guideState.sessionId, guideState.isSolved) {
        val sessionId = guideState.sessionId
        if (guideState.isSolved && sessionId != null && solvedPromptSessionId != sessionId) {
            solvedPromptSessionId = sessionId
            showSolvedCompletionDialog = true
            syncSolvedSessionToMistakes(sessionId)
        }
    }

    suspend fun refreshWorkbench(sessionId: String, preserveDraft: Boolean = true, allowRecovery: Boolean = true) {
        val draft = if (preserveDraft) guideState.draftInput else ""
        guideState = guideState.copy(isWorkbenchLoading = true, error = null, draftInput = draft)
        try {
            val detail = GuideApiClient.getSessionDetail(sessionId)
            val thinkingTree = GuideApiClient.getThinkingTree(sessionId)
            val nextState = guideState.copy(
                problemText = detail.rawProblem,
                sessionId = detail.sessionId,
                parsedKnownConditions = detail.parsedProblem.knownConditions,
                goal = detail.parsedProblem.goal,
                referenceAnswer = detail.parsedProblem.answer,
                dialogueHistory = detail.dialogueHistory,
                thinkingTree = thinkingTree,
                currentStuckNodeId = detail.currentStuckNodeId,
                stuckCount = detail.stuckCount,
                lastUpdatedNodeId = detail.lastUpdatedNodeId,
                isSolved = detail.isSolved,
                draftInput = draft,
                isSubmitting = false,
                isWorkbenchLoading = false,
                streamingTutorMarkdown = "",
                error = null,
            )
            guideState = nextState
            persistGuideSessionSnapshot(nextState)
        } catch (e: Exception) {
            guideState = guideState.copy(
                isSubmitting = false,
                isWorkbenchLoading = false,
                error = e.message ?: "加载解题工作台失败，请稍后重试",
            )
            if (allowRecovery && shouldRecoverGuideSession(e)) {
                val snapshot = SolveLocalCache.readSessionCache(context, sessionId)
                if (snapshot != null) {
                    try {
                        val rebuiltSessionId = recreateGuideSessionFromSnapshot(snapshot)
                        val rebuiltState = restoreGuideState(snapshot).copy(
                            sessionId = rebuiltSessionId,
                            draftInput = draft,
                            isWorkbenchLoading = true,
                        )
                        guideState = rebuiltState
                        refreshWorkbench(rebuiltSessionId, preserveDraft = true, allowRecovery = false)
                        return
                    } catch (_: Exception) {
                        // Fall through to the original error below if rebuilding also fails.
                    }
                }
            }
            guideState = guideState.copy(
                isSubmitting = false,
                isWorkbenchLoading = false,
                error = e.message ?: "加载解题工作台失败，请稍后重试",
            )
        }
    }

    fun openHistoryEntry(entry: SolveHistoryEntryUiModel) {
        clearArtifactStates()
        val snapshot = SolveLocalCache.readSessionCache(context, entry.sessionId)
        guideState = snapshot?.let(::restoreGuideState) ?: SolveGuideConversationState(
            problemText = entry.title,
            sessionId = entry.sessionId,
            isWorkbenchLoading = true,
        )
        destination = SolveDestination.Workbench
        scope.launch {
            refreshWorkbench(entry.sessionId, preserveDraft = snapshot != null)
        }
    }

    fun applyRecognizedText(questionText: String) {
        if (questionText.isBlank()) {
            clearWorkbenchState(guideState.problemText)
            guideState = guideState.copy(
                isRecognizingImage = false,
                error = "图片识别结果为空，请重试或手动输入题目",
            )
            destination = SolveDestination.ProblemConfirm
            return
        }
        clearWorkbenchState(questionText)
        guideState = guideState.copy(isRecognizingImage = false)
        destination = SolveDestination.ProblemConfirm
    }

    fun recognizeImageToProblemText(imageBase64: String, imageType: String) {
        destination = SolveDestination.ProblemConfirm
        scope.launch {
            guideState = guideState.copy(isRecognizingImage = true, error = null)
            try {
                val result = SolveOcrApiClient.recognizeQuestionText(
                    imageBase64 = imageBase64,
                    imageType = imageType,
                )
                applyRecognizedText(result.questionText)
            } catch (e: Exception) {
                guideState = guideState.copy(
                    isRecognizingImage = false,
                    error = e.message ?: "图片识别失败，请稍后重试",
                )
                destination = SolveDestination.ProblemConfirm
            }
        }
    }

    val editorLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract(),
    ) { result ->
        when {
            result.isSuccessful -> {
                val outputUri = result.uriContent
                val imageType = pendingEditorImageType ?: "screenshot"
                pendingEditorImageType = null
                if (outputUri == null) {
                    guideState = guideState.copy(error = "编辑结果为空，请重试")
                    return@rememberLauncherForActivityResult
                }
                val base64 = readImageBase64FromUri(context, outputUri)
                if (base64 == null) {
                    guideState = guideState.copy(error = "读取编辑后图片失败，请重试")
                    return@rememberLauncherForActivityResult
                }
                recognizeImageToProblemText(base64, imageType)
            }
            result == CropImage.CancelledResult -> {
                pendingEditorImageType = null
            }
            else -> {
                pendingEditorImageType = null
                val message = result.error?.message ?: "图片编辑失败，请重试"
                guideState = guideState.copy(error = message)
            }
        }
    }

    fun launchFullScreenEditor(sourceUri: Uri, imageType: String) {
        val isScreenshot = imageType == "screenshot"
        val outputUri = createTempImageUri(
            context = context,
            prefix = "solve_cropped",
            suffix = if (isScreenshot) ".png" else ".jpg",
        )
        if (outputUri == null) {
            guideState = guideState.copy(error = "无法创建编辑临时文件，请重试")
            return
        }

        pendingEditorImageType = imageType
        editorLauncher.launch(
            CropImageContractOptions(
                uri = sourceUri,
                cropImageOptions = CropImageOptions(
                    customOutputUri = outputUri,
                    outputCompressFormat = if (isScreenshot) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                    outputCompressQuality = 100,
                    guidelines = CropImageView.Guidelines.ON_TOUCH,
                    allowRotation = true,
                    allowCounterRotation = true,
                    allowFlipping = false,
                    showCropOverlay = true,
                    fixAspectRatio = false,
                    activityTitle = "编辑题目图片",
                    cropMenuCropButtonTitle = "完成",
                    activityMenuIconColor = android.graphics.Color.parseColor("#0B4F70"),
                ),
            ),
        )
    }

    val albumLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        val selectedUri = uri ?: return@rememberLauncherForActivityResult
        launchFullScreenEditor(selectedUri, "screenshot")
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val capturedUri = pendingCaptureUri
        pendingCaptureUri = null
        if (!success || capturedUri == null) {
            guideState = guideState.copy(error = "拍照失败，请重试")
            return@rememberLauncherForActivityResult
        }
        launchFullScreenEditor(capturedUri, "photo")
    }

    fun startTakePhotoFlow() {
        val outputUri = createTempImageUri(context, "solve_capture", ".jpg")
        if (outputUri == null) {
            guideState = guideState.copy(error = "无法创建拍照文件，请重试")
            return
        }
        pendingCaptureUri = outputUri
        cameraLauncher.launch(outputUri)
    }

    SolveScreen(
        destination = destination,
        uiState = uiState,
        guideState = guideState,
        reportState = reportState,
        solutionState = solutionState,
        visualizationState = visualizationState,
        resultMenuExpanded = resultMenuExpanded,
        onBackFromProblemConfirm = { destination = SolveDestination.Home },
        onBackFromWorkbench = { destination = SolveDestination.ProblemConfirm },
        onBackFromResult = { destination = SolveDestination.Workbench },
        onBackFromHistory = { destination = SolveDestination.Home },
        onTakePhoto = ::startTakePhotoFlow,
        onPickFromAlbum = { albumLauncher.launch("image/*") },
        onManualInput = {
            clearWorkbenchState(guideState.problemText)
            destination = SolveDestination.ProblemConfirm
        },
        onProblemTextChange = {
            clearWorkbenchState(it)
        },
        onWorkbenchInputChange = {
            val nextState = guideState.copy(draftInput = it, error = null)
            guideState = nextState
            persistGuideSessionSnapshot(nextState)
        },
        onWorkbenchQuickAction = {
            val nextState = guideState.copy(draftInput = it, error = null)
            guideState = nextState
            persistGuideSessionSnapshot(nextState)
        },
        onStartGuide = {
            val problemText = guideState.problemText.trim()
            if (problemText.isBlank()) {
                guideState = guideState.copy(error = "请先输入题目")
            } else {
                scope.launch {
                    guideState = guideState.copy(isSubmitting = true, error = null)
                    try {
                        val sessionId = GuideApiClient.createSession(problemText)
                        val nextState = guideState.copy(
                            isSubmitting = false,
                            sessionId = sessionId,
                            isWorkbenchLoading = true,
                        )
                        guideState = nextState
                        persistGuideSessionSnapshot(nextState)
                        destination = SolveDestination.Workbench
                        refreshWorkbench(sessionId, preserveDraft = false)
                    } catch (e: Exception) {
                        guideState = guideState.copy(isSubmitting = false, error = e.message)
                    }
                }
            }
        },
        onSendTurn = {
            val sessionId = guideState.sessionId
            val draft = guideState.draftInput.trim()
            if (sessionId == null) {
                guideState = guideState.copy(error = "请先开始引导")
            } else if (draft.isBlank()) {
                guideState = guideState.copy(error = "请先输入你的想法")
            } else {
                scope.launch {
                    guideState = guideState.copy(
                        isSubmitting = true,
                        error = null,
                        streamingTutorMarkdown = "",
                    )
                    try {
                        GuideApiClient.runTurnStream(
                            sessionId = sessionId,
                            studentInput = draft,
                            onRetry = {
                                guideState = guideState.copy(streamingTutorMarkdown = "")
                            },
                            onToken = { token ->
                                guideState = guideState.copy(
                                    streamingTutorMarkdown = guideState.streamingTutorMarkdown + token,
                                )
                            },
                        )
                        guideState = guideState.copy(isSubmitting = false, draftInput = "")
                        refreshWorkbench(sessionId, preserveDraft = false)
                    } catch (e: Exception) {
                        guideState = guideState.copy(
                            isSubmitting = false,
                            error = e.message ?: "发送失败，请稍后重试",
                        )
                    }
                }
            }
        },
        onRefreshWorkbench = {
            val sessionId = guideState.sessionId ?: return@SolveScreen
            scope.launch {
                refreshWorkbench(sessionId)
            }
        },
        onResultMenuExpandedChange = { resultMenuExpanded = it },
        onOpenSolvedDestination = ::openSolvedDestination,
        onReloadReport = {
            guideState.sessionId?.let { sessionId -> loadReport(sessionId, forceRefresh = true) }
        },
        onReloadSolution = {
            guideState.sessionId?.let { sessionId -> loadSolution(sessionId, forceRefresh = true) }
        },
        onReloadVisualization = {
            guideState.sessionId?.let { sessionId -> loadVisualization(sessionId, forceRefresh = true) }
        },
        onOpenHistory = { destination = SolveDestination.History },
        onOpenRecentHistoryEntry = ::openHistoryEntry,
        historyEntries = recentHistory,
        modifier = modifier,
    )

    if (showSolvedCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showSolvedCompletionDialog = false },
            title = {
                Text(
                    text = "本题已完成",
                    color = Color(0xFF1A3550),
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "可以继续停留在解题工作台，也可以点击右上角查看讲题报告、题解和可视化。",
                        color = Color(0xFF446273),
                    )
                    Text(
                        text = when {
                            mistakesSyncState.isSyncing -> "正在同步到错题本，稍后可以在错题本中继续查看。"
                            !mistakesSyncState.syncedReportId.isNullOrBlank() -> "已同步到错题本，可在错题本中继续回看与巩固。"
                            !mistakesSyncState.error.isNullOrBlank() -> "错题本同步暂时失败，但不影响继续查看结果页。"
                            else -> "完成后会自动同步到错题本。"
                        },
                        color = Color(0xFF2B5B76),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showSolvedCompletionDialog = false
                        resultMenuExpanded = true
                    },
                ) {
                    Text("查看结果")
                }
            },
            dismissButton = {
                Button(onClick = { showSolvedCompletionDialog = false }) {
                    Text("稍后")
                }
            },
        )
    }
}

@Composable
private fun SolveScreen(
    destination: SolveDestination,
    uiState: SolveRouteUiState,
    guideState: SolveGuideConversationState,
    reportState: SolveArtifactState<GuideReportResult>,
    solutionState: SolveArtifactState<String>,
    visualizationState: SolveArtifactState<GuideVisualizationResult>,
    resultMenuExpanded: Boolean,
    onBackFromProblemConfirm: () -> Unit,
    onBackFromWorkbench: () -> Unit,
    onBackFromResult: () -> Unit,
    onBackFromHistory: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromAlbum: () -> Unit,
    onManualInput: () -> Unit,
    onProblemTextChange: (String) -> Unit,
    onWorkbenchInputChange: (String) -> Unit,
    onWorkbenchQuickAction: (String) -> Unit,
    onStartGuide: () -> Unit,
    onSendTurn: () -> Unit,
    onRefreshWorkbench: () -> Unit,
    onResultMenuExpandedChange: (Boolean) -> Unit,
    onOpenSolvedDestination: (SolveDestination) -> Unit,
    onReloadReport: () -> Unit,
    onReloadSolution: () -> Unit,
    onReloadVisualization: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRecentHistoryEntry: (SolveHistoryEntryUiModel) -> Unit,
    historyEntries: List<SolveHistoryEntryUiModel>,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        SolveDestination.Home -> SolveAcquireScreen(
            uiState = uiState,
            onTakePhoto = onTakePhoto,
            onPickFromAlbum = onPickFromAlbum,
            onManualInput = onManualInput,
            onOpenHistory = onOpenHistory,
            onOpenRecentHistoryEntry = onOpenRecentHistoryEntry,
            modifier = modifier,
        )

        SolveDestination.ProblemConfirm -> SolveProblemConfirmScreen(
            state = guideState,
            onBack = onBackFromProblemConfirm,
            onProblemTextChange = onProblemTextChange,
            onStartGuide = onStartGuide,
            modifier = modifier,
        )

        SolveDestination.Workbench -> SolveWorkbenchScreen(
            state = guideState,
            onBack = onBackFromWorkbench,
            onDraftInputChange = onWorkbenchInputChange,
            onQuickAction = onWorkbenchQuickAction,
            onSendTurn = onSendTurn,
            onRefresh = onRefreshWorkbench,
            resultMenuExpanded = resultMenuExpanded,
            onResultMenuExpandedChange = onResultMenuExpandedChange,
            onOpenSolvedDestination = onOpenSolvedDestination,
            modifier = modifier,
        )

        SolveDestination.Report -> SolveReportScreen(
            state = guideState,
            artifactState = reportState,
            resultMenuExpanded = resultMenuExpanded,
            onBack = onBackFromResult,
            onResultMenuExpandedChange = onResultMenuExpandedChange,
            onOpenSolvedDestination = onOpenSolvedDestination,
            onReload = onReloadReport,
            modifier = modifier,
        )

        SolveDestination.Solution -> SolveSolutionScreen(
            state = guideState,
            artifactState = solutionState,
            visualizationState = visualizationState,
            resultMenuExpanded = resultMenuExpanded,
            onBack = onBackFromResult,
            onResultMenuExpandedChange = onResultMenuExpandedChange,
            onOpenSolvedDestination = onOpenSolvedDestination,
            onReload = onReloadSolution,
            modifier = modifier,
        )

        SolveDestination.Visualization -> SolveVisualizationScreen(
            state = guideState,
            artifactState = visualizationState,
            resultMenuExpanded = resultMenuExpanded,
            onBack = onBackFromResult,
            onResultMenuExpandedChange = onResultMenuExpandedChange,
            onOpenSolvedDestination = onOpenSolvedDestination,
            onReload = onReloadVisualization,
            modifier = modifier,
        )

        SolveDestination.History -> SolveHistoryScreen(
            entries = historyEntries,
            onBack = onBackFromHistory,
            onOpenEntry = onOpenRecentHistoryEntry,
            modifier = modifier,
        )
    }
}

@Composable
private fun SolveAcquireScreen(
    uiState: SolveRouteUiState,
    onTakePhoto: () -> Unit,
    onPickFromAlbum: () -> Unit,
    onManualInput: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRecentHistoryEntry: (SolveHistoryEntryUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD6EFFC),
                        Color(0xFFEAF6FF),
                        Color(0xFFF6FBFF),
                        Color.White,
                    ),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SolveHeroSection()

        Column(
            modifier = Modifier.padding(horizontal = BluetutorSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.entries.forEach { entry ->
                SolveEntryCard(
                    entry = entry,
                    onClick = {
                        when (entry.action) {
                            SolveEntryAction.Camera -> onTakePhoto()
                            SolveEntryAction.Album -> onPickFromAlbum()
                            SolveEntryAction.Manual -> onManualInput()
                        }
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(28.dp))
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "小提示",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF38ABDA),
                fontWeight = FontWeight.ExtraBold,
            )

            uiState.tips.forEach { tip ->
                SolveTipRow(tip = tip)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = BluetutorSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BtSectionTitle(
                title = "最近学习",
                actionText = "查看全部",
                onActionClick = onOpenHistory,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.recentHistory.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "还没有最近学习",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1A3550),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "开始一次引导解题后，这里会保留最近两条记录，方便你随时回到解题工作台。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B8398),
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.recentHistory.take(2).forEach { entry ->
                        SolveRecentHistoryRow(
                            entry = entry,
                            onClick = { onOpenRecentHistoryEntry(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolveRecentHistoryRow(
    entry: SolveHistoryEntryUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.95f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.statusTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.isSolved) Color(0xFF0F766E) else Color(0xFF1D4ED8),
                    modifier = Modifier
                        .background(
                            if (entry.isSolved) Color(0xFFCCFBF1) else Color(0xFFDBEAFE),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Text(
                    text = formatSolveHistoryTime(entry.updatedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7A96A8),
                )
            }

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = entry.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B8398),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SolveHeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F7DB3),
                        Color(0xFF1B93C6),
                        Color(0xFF38ABDA),
                        Color(0xFF6BC9EE),
                        Color(0xFFA9E2F8),
                        Color(0xFFDDF5FF),
                    ),
                ),
                shape = RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 0.dp)
                .size(138.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 8.dp)
                .size(96.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 28.dp, start = 18.dp, end = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "引导式解题",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xB3155576),
                fontWeight = FontWeight.ExtraBold,
            )

            SolveTeacherIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp),
            )

            Text(
                text = "把题目发给我",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF0B4F70),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "我带你一步步把它搞定 🎯",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xB30B4F70),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SolveEntryCard(
    entry: SolveEntryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .background(entry.background, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(entry.leadingContainerColor, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = entry.iconTint,
                modifier = Modifier.size(28.dp),
            )

            if (entry.highlightEmoji != null) {
                Text(
                    text = entry.highlightEmoji,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 6.dp, top = 4.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = entry.titleColor,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                color = entry.descriptionColor,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(entry.arrowBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = entry.arrowTint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SolveTipRow(tip: SolveTipUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFFEAF6FD), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = tip.emoji, style = MaterialTheme.typography.titleMedium)
        }

        Text(
            text = tip.text,
            modifier = Modifier.padding(top = 7.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF3A5A78),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveProblemConfirmScreen(
    state: SolveGuideConversationState,
    onBack: () -> Unit,
    onProblemTextChange: (String) -> Unit,
    onStartGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "题目确认",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEAF6FF),
                            Color(0xFFF6FBFF),
                            Color.White,
                        ),
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(28.dp))
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "识别结果可直接修改。确认题目后点击“开始引导”。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B6578),
                )

                if (state.isRecognizingImage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF38ABDA),
                        )
                        Text(
                            text = "正在识别图片中的题目...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B6578),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEAF6FD), RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = if (state.problemText.isBlank()) {
                                "图片已上传，正在解析题目内容，请稍候..."
                            } else {
                                "正在更新最新识别结果，你也可以先提前检查和修改文本。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2B5B76),
                        )
                    }
                }

                OutlinedTextField(
                    value = state.problemText,
                    onValueChange = onProblemTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting && !state.isRecognizingImage,
                    label = { Text("题目") },
                    placeholder = { Text("例如：小明有12个苹果，吃了3个，还剩几个？") },
                    minLines = 8,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38ABDA),
                        unfocusedBorderColor = Color(0xFFD7E8F2),
                    ),
                )

                Button(
                    onClick = onStartGuide,
                    enabled = !state.isSubmitting && !state.isRecognizingImage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text("开始引导")
                    }
                }

                if (state.error != null) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB42318),
                    )
                }

                if (state.sessionId != null) {
                    Text(
                        text = "再次点击“开始引导”会基于当前题目重新创建会话。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B6578),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SolveWorkbenchScreen(
    state: SolveGuideConversationState,
    onBack: () -> Unit,
    onDraftInputChange: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    onSendTurn: () -> Unit,
    onRefresh: () -> Unit,
    resultMenuExpanded: Boolean,
    onResultMenuExpandedChange: (Boolean) -> Unit,
    onOpenSolvedDestination: (SolveDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isProblemExpanded by remember(state.sessionId, state.problemText) { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val pagerScope = rememberCoroutineScope()
    val currentPageTitle = when (pagerState.currentPage) {
        0 -> "思路树"
        2 -> "线索与进展"
        else -> "解题工作台"
    }
    val currentBackDescription = if (pagerState.currentPage == 1) "返回题目确认" else "返回解题工作台"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = currentPageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage == 1) {
                                onBack()
                            } else {
                                pagerScope.launch { pagerState.animateScrollToPage(1) }
                            }
                        },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = currentBackDescription,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                actions = {
                    if (state.isSolved) {
                        SolveResultMenuAction(
                            expanded = resultMenuExpanded,
                            onExpandedChange = onResultMenuExpandedChange,
                            onOpenSolvedDestination = onOpenSolvedDestination,
                        )
                    } else {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "刷新工作台",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEAF6FF),
                            Color(0xFFF7FBFF),
                            Color.White,
                        ),
                    ),
                )
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .padding(top = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            ) {
                WorkbenchProblemCard(
                    state = state,
                    isExpanded = isProblemExpanded,
                    onToggleExpand = { isProblemExpanded = !isProblemExpanded },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                pageSpacing = 12.dp,
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp, bottom = 18.dp),
                ) {
                    when (page) {
                        0 -> SolveThinkingTreePageContent(state = state)
                        1 -> SolveWorkbenchMainPage(
                            state = state,
                            onDraftInputChange = onDraftInputChange,
                            onQuickAction = onQuickAction,
                            onSendTurn = onSendTurn,
                        )
                        else -> SolveProgressPageContent(state = state)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveHistoryScreen(
    entries: List<SolveHistoryEntryUiModel>,
    onBack: () -> Unit,
    onOpenEntry: (SolveHistoryEntryUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "全部引导历史",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEAF6FF),
                            Color(0xFFF7FBFF),
                            Color.White,
                        ),
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (entries.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.94f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "还没有引导历史",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A3550),
                        )
                        Text(
                            text = "完成一次引导解题后，这里会自动记录历史，你可以随时回到对应的解题工作台。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B8398),
                        )
                    }
                }
            } else {
                entries.forEach { entry ->
                    SolveRecentHistoryRow(
                        entry = entry,
                        onClick = { onOpenEntry(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkbenchProblemCard(
    state: SolveGuideConversationState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .background(Color.White, RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 12.dp else 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "题目压缩卡",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = if (isExpanded) "收起" else "展开",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF1F8CC5),
                modifier = Modifier.clickable(onClick = onToggleExpand),
            )
        }

        Text(
            text = state.problemText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF2A4A60),
            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "左右滑动可查看思路树与线索",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7A96A8),
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .background(Color(0xFFF4FAFD), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "左滑/右滑",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2B5B76),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        if (isExpanded && state.parsedKnownConditions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "已知条件",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF4B6578),
                    fontWeight = FontWeight.ExtraBold,
                )
                state.parsedKnownConditions.take(if (isExpanded) state.parsedKnownConditions.size else 2).forEach { condition ->
                    Text(
                        text = "• $condition",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B6578),
                    )
                }
            }
        }

        if (isExpanded && state.goal.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEAF6FD), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "目标：${state.goal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2B5B76),
                )
            }
        }
    }
}

@Composable
private fun SolveWorkbenchMainPage(
    state: SolveGuideConversationState,
    onDraftInputChange: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    onSendTurn: () -> Unit,
) {
    val latestTutorQuestion = state.dialogueHistory.lastOrNull { it.role == "tutor" }?.content
    val currentFocus = state.currentStuckNodeId
        ?.let { state.thinkingTree[it]?.content }
        ?: state.lastUpdatedNodeId?.let { state.thinkingTree[it]?.content }
        ?: state.goal.ifBlank { "先找到这道题的突破口" }
    val teacherPrompt = when {
        state.streamingTutorMarkdown.isNotBlank() -> state.streamingTutorMarkdown
        state.isSolved -> "这道题已经走通了，你可以回看整条思路并准备生成后续结果。"
        !latestTutorQuestion.isNullOrBlank() -> latestTutorQuestion
        else -> "先说说你准备从哪里开始，我会顺着你的思路继续引导。"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WorkbenchGuideCard(
            currentFocus = currentFocus,
            teacherPrompt = teacherPrompt,
            isSolved = state.isSolved,
            isLoading = state.isWorkbenchLoading,
        )

        WorkbenchInputCard(
            draftInput = state.draftInput,
            isSolved = state.isSolved,
            isSubmitting = state.isSubmitting,
            isLoading = state.isWorkbenchLoading,
            onDraftInputChange = onDraftInputChange,
            onQuickAction = onQuickAction,
            onSendTurn = onSendTurn,
        )

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB42318),
            )
        }
    }
}

@Composable
private fun WorkbenchGuideCard(
    currentFocus: String,
    teacherPrompt: String,
    isSolved: Boolean,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF8FCFF), Color(0xFFEAF6FF)),
                ),
                RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "当前引导",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.ExtraBold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "当前焦点",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF4B6578),
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = currentFocus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A3550),
                    fontWeight = FontWeight.SemiBold,
                )
                MarkdownText(
                    markdown = teacherPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF35566F),
                )
                if (isLoading || isSolved) {
                    Text(
                        text = if (isSolved) "当前题目已完成，可继续查看思路树或线索进展。" else "正在同步最新引导内容...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSolved) Color(0xFF166534) else Color(0xFF7A96A8),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkbenchInputCard(
    draftInput: String,
    isSolved: Boolean,
    isSubmitting: Boolean,
    isLoading: Boolean,
    onDraftInputChange: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    onSendTurn: () -> Unit,
) {
    val quickActions = listOf(
        "卡住了" to "我卡住了，不知道下一步该怎么开始",
        "给提示" to "给我一点提示，但先别直接给答案",
        "换思路" to "我想换一种思路重新试试",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .background(Color.White, RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "我的想法",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.ExtraBold,
        )

        OutlinedTextField(
            value = draftInput,
            onValueChange = onDraftInputChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting && !isLoading,
            label = { Text("输入你此刻的思路") },
            placeholder = { Text("例如：我想先找已知条件，再判断第一步应该算什么") },
            minLines = 4,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38ABDA),
                unfocusedBorderColor = Color(0xFFD7E8F2),
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickActions.forEach { (label, fullText) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF4FAFD), RoundedCornerShape(999.dp))
                        .clickable(enabled = !isSubmitting && !isLoading) { onQuickAction(fullText) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF35566F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Button(
            onClick = onSendTurn,
            enabled = !isSubmitting && !isLoading && !isSolved,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Text(if (isSolved) "本题已完成" else "继续引导")
            }
        }
    }
}

@Composable
private fun WorkbenchMetricPill(label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF2B5B76),
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SolveThinkingTreePageContent(state: SolveGuideConversationState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WorkbenchInfoCard(
            title = "返回提示",
            body = "向右滑动可回到解题工作台，也可以点击左上角返回。",
            showShadow = false,
        )

        WorkbenchTreeSummaryCard(state = state, showShadow = false)

        if (state.thinkingTree.isEmpty()) {
            WorkbenchEmptyCard(
                text = "当前还没有形成可展示的思路树，先在工作台里输入第一步想法。",
                showShadow = false,
            )
        } else {
            RenderThinkingTree(
                nodeId = "n0",
                depth = 0,
                tree = state.thinkingTree,
                currentStuckNodeId = state.currentStuckNodeId,
            )
        }
    }
}

@Composable
private fun SolveProgressPageContent(state: SolveGuideConversationState) {
    val latestMessages = state.dialogueHistory.takeLast(6)
    val correctClues = state.thinkingTree.values
        .filter { it.status == "correct" && it.content.isNotBlank() }
        .map { it.content }
        .distinct()
        .take(4)
    val riskNodes = state.thinkingTree.values
        .filter { it.status == "incorrect" || it.status == "stuck" }
        .take(4)
    val currentFocus = state.currentStuckNodeId
        ?.let { state.thinkingTree[it]?.content }
        ?: state.lastUpdatedNodeId?.let { state.thinkingTree[it]?.content }
        ?: "先把第一步说清楚"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WorkbenchInfoCard(
            title = "返回提示",
            body = "向左滑动可回到解题工作台，也可以点击左上角返回。",
            showShadow = false,
        )

        WorkbenchInfoCard(
            title = "当前卡点",
            body = currentFocus,
            footer = "已引导 ${state.stuckCount} 次${if (state.isSolved) "，当前题目已完成" else ""}",
            showShadow = false,
        )

        WorkbenchInfoCard(
            title = "已发现线索",
            body = if (correctClues.isEmpty()) {
                "还没有稳定的正确路径，先在工作台输入你的第一步想法。"
            } else {
                correctClues.joinToString(separator = "\n") { "• $it" }
            },
            showShadow = false,
        )

        WorkbenchInfoCard(
            title = "风险与误区",
            body = if (riskNodes.isEmpty()) {
                "当前还没有明显的错误分支，继续尝试即可。"
            } else {
                riskNodes.joinToString(separator = "\n") { node ->
                    val history = node.errorHistory.takeIf { it.isNotEmpty() }?.joinToString("；") ?: "等待进一步判断"
                    "• ${node.content}（${history}）"
                }
            },
            showShadow = false,
        )

        WorkbenchInfoCard(
            title = "最近对话",
            body = "",
            showShadow = false,
            bodyContent = {
                if (latestMessages.isEmpty()) {
                    Text(
                        text = "还没有对话记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF446273),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        latestMessages.forEachIndexed { index, message ->
                            val roleLabel = if (message.role == "student") "我" else "老师"
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = roleLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (message.role == "student") Color(0xFF2B5B76) else Color(0xFF1F8CC5),
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                MarkdownText(
                                    markdown = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF446273),
                                )
                            }
                            if (index != latestMessages.lastIndex) {
                                HorizontalDivider(color = Color(0xFFE6F1F7))
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun MarkdownText(
    markdown: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val resolvedFontSize = if (style.fontSize.isSpecified) style.fontSize else MaterialTheme.typography.bodyMedium.fontSize
    val resolvedLineHeight = if (style.lineHeight.isSpecified) style.lineHeight else resolvedFontSize * 1.45f
    val textSizePx = with(density) { resolvedFontSize.toPx() }
    val lineHeightPx = with(density) { resolvedLineHeight.toPx() }
    val lineHeightExtra = (lineHeightPx - textSizePx).coerceAtLeast(0f)
    val markwon = remember(context, textSizePx) {
        buildSafeMarkwon(
            context = context.applicationContext,
            textSizePx = textSizePx,
        )
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextIsSelectable(false)
                includeFontPadding = false
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedFontSize.value)
            textView.setTextColor(color.toArgb())
            textView.setLineSpacing(lineHeightExtra, 1f)
            val safeMarkdown = markdown.ifBlank { " " }
            runCatching {
                markwon.setMarkdown(textView, safeMarkdown)
            }.getOrElse {
                textView.text = safeMarkdown
            }
        },
    )
}

private fun buildSafeMarkwon(
    context: Context,
    textSizePx: Float,
): Markwon {
    return runCatching {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(
                JLatexMathPlugin.create(textSizePx) { builder ->
                    builder.inlinesEnabled(true)
                },
            )
            .build()
    }.getOrElse {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .build()
    }
}

@Composable
private fun SolveResultMenuAction(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenSolvedDestination: (SolveDestination) -> Unit,
) {
    Box {
        IconButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "查看结果页面",
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text("讲题报告") },
                onClick = {
                    onExpandedChange(false)
                    onOpenSolvedDestination(SolveDestination.Report)
                },
            )
            DropdownMenuItem(
                text = { Text("题解") },
                onClick = {
                    onExpandedChange(false)
                    onOpenSolvedDestination(SolveDestination.Solution)
                },
            )
            DropdownMenuItem(
                text = { Text("可视化") },
                onClick = {
                    onExpandedChange(false)
                    onOpenSolvedDestination(SolveDestination.Visualization)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveReportScreen(
    state: SolveGuideConversationState,
    artifactState: SolveArtifactState<GuideReportResult>,
    resultMenuExpanded: Boolean,
    onBack: () -> Unit,
    onResultMenuExpandedChange: (Boolean) -> Unit,
    onOpenSolvedDestination: (SolveDestination) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SolveResultScreenScaffold(
        title = "讲题报告",
        resultMenuExpanded = resultMenuExpanded,
        onBack = onBack,
        onResultMenuExpandedChange = onResultMenuExpandedChange,
        onOpenSolvedDestination = onOpenSolvedDestination,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                artifactState.isLoading -> {
                    SolveArtifactLoadingCard(
                        title = "正在生成讲题报告",
                        description = "会整理题目、思路链路、误区和独立完成情况。",
                    )
                }

                !artifactState.error.isNullOrBlank() -> {
                    SolveArtifactErrorCard(
                        title = "讲题报告暂时不可用",
                        message = artifactState.error,
                        actionLabel = "重新生成",
                        onAction = onReload,
                    )
                }

                artifactState.data == null -> {
                    SolveArtifactLoadingCard(
                        title = "正在整理讲题报告",
                        description = "首次进入会自动请求生成。",
                    )
                }

                else -> {
                    val report = artifactState.data
                    SolveResultHeroCard(
                        eyebrow = "完成复盘",
                        title = state.problemText.ifBlank { report.rawProblem },
                        summary = "已整理 ${report.thinkingChain.size} 步思路、${report.knowledgeTags.size} 个知识点标签。",
                    )
                    WorkbenchInfoCard(
                        title = "题目与目标",
                        bodyContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MarkdownText(
                                    markdown = report.rawProblem.ifBlank { state.problemText },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF35566F),
                                )
                                if (report.goal.isNotBlank()) {
                                    MarkdownText(
                                        markdown = "- 目标：${report.goal}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF35566F),
                                    )
                                }
                                if (report.answer.isNotBlank()) {
                                    MarkdownText(
                                        markdown = "- 参考答案：${report.answer}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF35566F),
                                    )
                                }
                            }
                        },
                    )
                    if (report.knowledgeTags.isNotEmpty()) {
                        WorkbenchInfoCard(
                            title = "知识点标签",
                            body = "",
                            bodyContent = {
                                FlowChipRow(items = report.knowledgeTags)
                            },
                        )
                    }
                    WorkbenchInfoCard(
                        title = "思路链路",
                        body = "",
                        bodyContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                report.thinkingChain.forEach { item ->
                                    SolveThinkingChainCard(item = item)
                                }
                            }
                        },
                    )
                    WorkbenchInfoCard(
                        title = "主要误区",
                        body = "",
                        bodyContent = {
                            MarkdownText(
                                markdown = report.errorProfileMarkdown.ifBlank { "本次没有识别到明确误区。" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF35566F),
                            )
                        },
                    )
                    WorkbenchInfoCard(
                        title = "独立完成情况",
                        body = "",
                        bodyContent = {
                            MarkdownText(
                                markdown = report.independenceMarkdown.ifBlank { "当前没有独立完成情况分析。" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF35566F),
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveSolutionScreen(
    state: SolveGuideConversationState,
    artifactState: SolveArtifactState<String>,
    visualizationState: SolveArtifactState<GuideVisualizationResult>,
    resultMenuExpanded: Boolean,
    onBack: () -> Unit,
    onResultMenuExpandedChange: (Boolean) -> Unit,
    onOpenSolvedDestination: (SolveDestination) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SolveResultScreenScaffold(
        title = "题解",
        resultMenuExpanded = resultMenuExpanded,
        onBack = onBack,
        onResultMenuExpandedChange = onResultMenuExpandedChange,
        onOpenSolvedDestination = onOpenSolvedDestination,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                artifactState.isLoading -> {
                    SolveArtifactLoadingCard(
                        title = "正在生成题解",
                        description = "会把完整解法整理成适合回看的步骤版说明。",
                    )
                }

                !artifactState.error.isNullOrBlank() -> {
                    SolveArtifactErrorCard(
                        title = "题解暂时不可用",
                        message = artifactState.error,
                        actionLabel = "重新生成",
                        onAction = onReload,
                    )
                }

                artifactState.data.isNullOrBlank() -> {
                    SolveArtifactLoadingCard(
                        title = "正在准备题解",
                        description = "首次进入会自动请求生成。",
                    )
                }

                else -> {
                    SolveResultHeroCard(
                        eyebrow = "完整回看",
                        title = "这道题的完整题解已整理好",
                        summary = "返回工作台后仍可继续查看思路树和线索进展。",
                    )
                    WorkbenchInfoCard(
                        title = "题解正文",
                        body = "",
                        bodyContent = {
                            SolutionMarkdownCard(
                                markdown = artifactState.data,
                                visualization = visualizationState.data,
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveVisualizationScreen(
    state: SolveGuideConversationState,
    artifactState: SolveArtifactState<GuideVisualizationResult>,
    resultMenuExpanded: Boolean,
    onBack: () -> Unit,
    onResultMenuExpandedChange: (Boolean) -> Unit,
    onOpenSolvedDestination: (SolveDestination) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SolveResultScreenScaffold(
        title = "可视化",
        resultMenuExpanded = resultMenuExpanded,
        onBack = onBack,
        onResultMenuExpandedChange = onResultMenuExpandedChange,
        onOpenSolvedDestination = onOpenSolvedDestination,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                artifactState.isLoading -> {
                    SolveArtifactLoadingCard(
                        title = "正在生成可视化",
                        description = "会把关键解题步骤转换成可回看的结构图，通常需要 20 到 60 秒，请保持页面停留。",
                    )
                }

                !artifactState.error.isNullOrBlank() -> {
                    SolveArtifactErrorCard(
                        title = "可视化暂时不可用",
                        message = buildString {
                            append(artifactState.error)
                            append("\n\n可视化生成通常比普通结果更耗时，如果后端仍在生成，可稍等后再次进入或点击重试。")
                        },
                        actionLabel = "继续重试",
                        onAction = onReload,
                    )
                }

                artifactState.data == null -> {
                    SolveArtifactLoadingCard(
                        title = "正在准备可视化",
                        description = "首次进入会自动请求生成。",
                    )
                }

                else -> {
                    val visualization = artifactState.data
                    SolveResultHeroCard(
                        eyebrow = "步骤图解",
                        title = "${state.problemText.ifBlank { "当前题目" }} 的可视化",
                        summary = "题型：${visualization.problemType}，共 ${visualization.visuals.size} 张步骤图。",
                    )
                    visualization.visuals.forEach { visual ->
                        WorkbenchInfoCard(
                            title = visual.title,
                            body = "",
                            footer = visual.stepId,
                            bodyContent = {
                                SolveSvgCard(svg = visual.svg)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveResultScreenScaffold(
    title: String,
    resultMenuExpanded: Boolean,
    onBack: () -> Unit,
    onResultMenuExpandedChange: (Boolean) -> Unit,
    onOpenSolvedDestination: (SolveDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回解题工作台",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                actions = {
                    SolveResultMenuAction(
                        expanded = resultMenuExpanded,
                        onExpandedChange = onResultMenuExpandedChange,
                        onOpenSolvedDestination = onOpenSolvedDestination,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        content(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEAF6FF),
                            Color(0xFFF7FBFF),
                            Color.White,
                        ),
                    ),
                )
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .padding(top = 18.dp, bottom = 40.dp),
        )
    }
}

@Composable
private fun SolveResultHeroCard(
    eyebrow: String,
    title: String,
    summary: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFEDF8FF), Color(0xFFDFF3FF)),
                ),
                RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF1F8CC5),
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF173A55),
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF446273),
        )
    }
}

@Composable
private fun SolveArtifactLoadingCard(
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color(0xFF1F8CC5),
            strokeWidth = 3.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF446273),
            )
        }
    }
}

@Composable
private fun SolveArtifactErrorCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFB42318),
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6C4B4B),
        )
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun FlowChipRow(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF2FAFF), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2B5B76),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                repeat(3 - rowItems.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SolveThinkingChainCard(item: com.bluetutor.android.feature.solve.data.GuideReportChainItem) {
    val statusLabel = when (item.status) {
        "correct" -> "正确"
        "incorrect" -> "错误"
        "stuck" -> "卡住"
        "abandoned" -> "放弃"
        else -> item.status.ifBlank { "过程" }
    }
    val statusColor = when (item.status) {
        "correct" -> Color(0xFF2F9E62)
        "incorrect" -> Color(0xFFD97706)
        "stuck" -> Color(0xFF1F8CC5)
        else -> Color(0xFF68879B)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FCFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.nodeId.ifBlank { "步骤" },
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.ExtraBold,
            )
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Text(
            text = item.content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF35566F),
        )
        if (item.errorHistory.isNotEmpty()) {
            MarkdownText(
                markdown = item.errorHistory.joinToString(separator = "\n") { "- $it" },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B5A3C),
            )
        }
    }
}

@Composable
private fun SolutionMarkdownCard(
    markdown: String,
    visualization: GuideVisualizationResult?,
) {
    val visualTitleMap = visualization?.visuals?.associateBy({ it.stepId }, { it.title }).orEmpty()
    val normalized = markdown
        .replace(Regex("\\[VISUAL:([^\\]]+)\\]"), "\n[VISUAL:$1]\n")
        .replace("\r\n", "\n")
    val lines = normalized.split("\n")
    val buffer = mutableListOf<String>()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        @Composable
        fun flushBuffer() {
            if (buffer.isNotEmpty()) {
                MarkdownText(
                    markdown = buffer.joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF35566F),
                )
                buffer.clear()
            }
        }

        lines.forEach { rawLine ->
            val match = Regex("^\\[VISUAL:([^\\]]+)\\]$").matchEntire(rawLine.trim())
            if (match != null) {
                flushBuffer()
                val stepId = match.groupValues[1]
                val visualTitle = visualTitleMap[stepId] ?: stepId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3FAFF), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "对应可视化",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF1F8CC5),
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = visualTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF35566F),
                        )
                    }
                }
            } else {
                buffer += rawLine
            }
        }
        flushBuffer()
    }
}

@Composable
private fun SolveSvgCard(svg: String) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                null,
                """
                <html>
                <head>
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
                  <style>
                    body { margin: 0; padding: 0; background: #ffffff; }
                    .wrap { display: flex; align-items: center; justify-content: center; min-height: 100vh; }
                    svg { max-width: 100%; height: auto; }
                  </style>
                </head>
                <body>
                  <div class=\"wrap\">$svg</div>
                </body>
                </html>
                """.trimIndent(),
                "text/html",
                "utf-8",
                null,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveThinkingTreeScreen(
    state: SolveGuideConversationState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "思路树",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回工作台",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FBFF))
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WorkbenchTreeSummaryCard(state = state)

            if (state.thinkingTree.isEmpty()) {
                WorkbenchEmptyCard(text = "当前还没有形成可展示的思路树，先在工作台里输入第一步想法。")
            } else {
                RenderThinkingTree(
                    nodeId = "n0",
                    depth = 0,
                    tree = state.thinkingTree,
                    currentStuckNodeId = state.currentStuckNodeId,
                )
            }
        }
    }
}

@Composable
private fun WorkbenchTreeSummaryCard(
    state: SolveGuideConversationState,
    showShadow: Boolean = true,
) {
    val correctCount = state.thinkingTree.values.count { it.status == "correct" }
    val incorrectCount = state.thinkingTree.values.count { it.status == "incorrect" }
    val stuckCount = state.thinkingTree.values.count { it.status == "stuck" }
    val cardModifier = if (showShadow) {
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
    } else {
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
    }

    Column(
        modifier = cardModifier
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "思路树概览",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.ExtraBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkbenchMetricPill(label = "正确 $correctCount")
            WorkbenchMetricPill(label = "错误 $incorrectCount")
            WorkbenchMetricPill(label = "卡住 $stuckCount")
        }
    }
}

@Composable
private fun RenderThinkingTree(
    nodeId: String,
    depth: Int,
    tree: Map<String, GuideThinkingNodeResult>,
    currentStuckNodeId: String?,
) {
    val node = tree[nodeId] ?: return
    val backgroundColor = when (node.status) {
        "correct" -> Color(0xFFE9F9EF)
        "incorrect" -> Color(0xFFFFF1EB)
        "abandoned" -> Color(0xFFF4F6F8)
        else -> Color(0xFFEAF6FD)
    }
    val borderColor = when {
        node.nodeId == currentStuckNodeId -> Color(0xFF1F8CC5)
        node.status == "correct" -> Color(0xFF2F9E62)
        node.status == "incorrect" -> Color(0xFFD97706)
        node.status == "abandoned" -> Color(0xFF94A3B8)
        else -> Color(0xFF60A5FA)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = node.nodeId,
                    style = MaterialTheme.typography.labelLarge,
                    color = borderColor,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = node.status,
                    style = MaterialTheme.typography.labelLarge,
                    color = borderColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                text = node.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2A4A60),
            )
            if (node.errorHistory.isNotEmpty()) {
                Text(
                    text = "错误记录：${node.errorHistory.joinToString("；")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A4B2F),
                )
            }
        }

        node.children.forEach { childId ->
            RenderThinkingTree(
                nodeId = childId,
                depth = depth + 1,
                tree = tree,
                currentStuckNodeId = currentStuckNodeId,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveProgressScreen(
    state: SolveGuideConversationState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestMessages = state.dialogueHistory.takeLast(6)
    val correctClues = state.thinkingTree.values
        .filter { it.status == "correct" && it.content.isNotBlank() }
        .map { it.content }
        .distinct()
        .take(4)
    val riskNodes = state.thinkingTree.values
        .filter { it.status == "incorrect" || it.status == "stuck" }
        .take(4)
    val currentFocus = state.currentStuckNodeId
        ?.let { state.thinkingTree[it]?.content }
        ?: state.lastUpdatedNodeId?.let { state.thinkingTree[it]?.content }
        ?: "先把第一步说清楚"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "线索与进展",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回工作台",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FBFF))
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WorkbenchInfoCard(
                title = "当前卡点",
                body = currentFocus,
                footer = "已引导 ${state.stuckCount} 次${if (state.isSolved) "，当前题目已完成" else ""}",
            )

            WorkbenchInfoCard(
                title = "已发现线索",
                body = if (correctClues.isEmpty()) {
                    "还没有稳定的正确路径，先在工作台输入你的第一步想法。"
                } else {
                    correctClues.joinToString(separator = "\n") { "• $it" }
                },
            )

            WorkbenchInfoCard(
                title = "风险与误区",
                body = if (riskNodes.isEmpty()) {
                    "当前还没有明显的错误分支，继续尝试即可。"
                } else {
                    riskNodes.joinToString(separator = "\n") { node ->
                        val history = node.errorHistory.takeIf { it.isNotEmpty() }?.joinToString("；") ?: "等待进一步判断"
                        "• ${node.content}（${history}）"
                    }
                },
            )

            WorkbenchInfoCard(
                title = "最近对话",
                body = if (latestMessages.isEmpty()) {
                    "还没有对话记录。"
                } else {
                    latestMessages.joinToString(separator = "\n\n") { message ->
                        val roleLabel = if (message.role == "student") "我" else "老师"
                        "$roleLabel：${message.content}"
                    }
                },
            )
        }
    }
}

@Composable
private fun WorkbenchInfoCard(
    title: String,
    body: String = "",
    footer: String? = null,
    showShadow: Boolean = true,
    bodyContent: @Composable (() -> Unit)? = null,
) {
    val cardModifier = if (showShadow) {
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
    } else {
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
    }
    Column(
        modifier = cardModifier
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.ExtraBold,
        )
        if (bodyContent != null) {
            bodyContent()
        } else {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF35566F),
            )
        }
        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A96A8),
            )
        }
    }
}

@Composable
private fun WorkbenchEmptyCard(
    text: String,
    showShadow: Boolean = true,
) {
    val cardModifier = if (showShadow) {
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
    } else {
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
    }
    Box(
        modifier = cardModifier
            .padding(18.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4B6578),
        )
    }
}

private enum class SolveDestination {
    Home,
    ProblemConfirm,
    Workbench,
    Report,
    Solution,
    Visualization,
    History,
}

private data class SolveArtifactState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

private data class SolveMistakeSyncState(
    val syncedSessionId: String? = null,
    val syncedReportId: String? = null,
    val syncedReportTitle: String? = null,
    val isSyncing: Boolean = false,
    val error: String? = null,
)

private data class SolveRouteUiState(
    val entries: List<SolveEntryUiModel>,
    val tips: List<SolveTipUiModel>,
    val recentHistory: List<SolveHistoryEntryUiModel>,
)

private data class SolveEntryUiModel(
    val action: SolveEntryAction,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val background: Brush,
    val leadingContainerColor: Color,
    val arrowBackground: Color,
    val arrowTint: Color,
    val titleColor: Color,
    val descriptionColor: Color,
    val highlightEmoji: String? = null,
)

private enum class SolveEntryAction {
    Camera,
    Album,
    Manual,
}

private data class SolveTipUiModel(
    val emoji: String,
    val text: String,
)

private data class SolveGuideConversationState(
    val problemText: String = "",
    val sessionId: String? = null,
    val isSubmitting: Boolean = false,
    val isRecognizingImage: Boolean = false,
    val error: String? = null,
    val parsedKnownConditions: List<String> = emptyList(),
    val goal: String = "",
    val referenceAnswer: String = "",
    val dialogueHistory: List<GuideDialogueMessage> = emptyList(),
    val thinkingTree: Map<String, GuideThinkingNodeResult> = emptyMap(),
    val currentStuckNodeId: String? = null,
    val stuckCount: Int = 0,
    val lastUpdatedNodeId: String? = null,
    val isSolved: Boolean = false,
    val draftInput: String = "",
    val isWorkbenchLoading: Boolean = false,
    val streamingTutorMarkdown: String = "",
)

private fun solveRouteMockUiState(
    recentHistory: List<SolveHistoryEntryUiModel> = emptyList(),
): SolveRouteUiState = SolveRouteUiState(
    entries = listOf(
        SolveEntryUiModel(
            action = SolveEntryAction.Camera,
            title = "拍照上传",
            description = "拍清楚题目，我带你一步步想 💬",
            icon = Icons.Rounded.PhotoCamera,
            iconTint = Color.White,
            background = Brush.linearGradient(
                listOf(Color(0xFF1DA8DA), Color(0xFF38ABDA), Color(0xFF7ED3F4), Color(0xFFC8EDFB)),
            ),
            leadingContainerColor = Color.White.copy(alpha = 0.24f),
            arrowBackground = Color.White.copy(alpha = 0.32f),
            arrowTint = Color(0xFF0B4F70),
            titleColor = Color(0xFF0B4F70),
            descriptionColor = Color(0xB30B4F70),
            highlightEmoji = "⭐",
        ),
        SolveEntryUiModel(
            action = SolveEntryAction.Album,
            title = "从相册导入",
            description = "选一张题目图片",
            icon = Icons.Rounded.PhotoLibrary,
            iconTint = Color(0xFF62A6D8),
            background = Brush.linearGradient(
                listOf(Color(0xFFF0ECFC), Color(0xFFE2D9F8)),
            ),
            leadingContainerColor = Color.White.copy(alpha = 0.58f),
            arrowBackground = Color.White.copy(alpha = 0.52f),
            arrowTint = Color(0xFF4A7A9A),
            titleColor = Color(0xFF1A3550),
            descriptionColor = Color(0xFF7A96A8),
        ),
        SolveEntryUiModel(
            action = SolveEntryAction.Manual,
            title = "手动输入题目",
            description = "自己打出来也行 ✏️",
            icon = Icons.Rounded.EditNote,
            iconTint = Color(0xFFF0B245),
            background = Brush.linearGradient(
                listOf(Color(0xFFFFFBEB), Color(0xFFFFF3CC)),
            ),
            leadingContainerColor = Color.White.copy(alpha = 0.58f),
            arrowBackground = Color.White.copy(alpha = 0.62f),
            arrowTint = Color(0xFF4A7A9A),
            titleColor = Color(0xFF1A3550),
            descriptionColor = Color(0xFF7A96A8),
        ),
    ),
    tips = listOf(
        SolveTipUiModel("📸", "尽量拍清楚题干和图形，字迹越清晰越好哦"),
        SolveTipUiModel("💡", "我不会直接给答案，会一步步帮你理解！"),
        SolveTipUiModel("🧩", "做完还能推荐相似题，帮你举一反三"),
    ),
    recentHistory = recentHistory,
)

private fun formatSolveHistoryTime(updatedAtMillis: Long): String {
    if (updatedAtMillis <= 0L) return "刚刚"
    val now = System.currentTimeMillis()
    val diff = now - updatedAtMillis
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
        diff < 24 * 3_600_000L -> "${diff / 3_600_000L} 小时前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(updatedAtMillis))
    }
}

private fun readImageBase64FromUri(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.isEmpty()) {
                null
            } else {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }
    }.getOrNull()
}

private fun createTempImageUri(context: Context, prefix: String, suffix: String): Uri? {
    return runCatching {
        val cacheDir = File(context.cacheDir, "solve-images").apply { mkdirs() }
        val imageFile = File.createTempFile(prefix, suffix, cacheDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
    }.getOrNull()
}