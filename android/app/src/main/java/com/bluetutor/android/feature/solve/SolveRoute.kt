package com.bluetutor.android.feature.solve

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.solve.data.GuideApiClient
import com.bluetutor.android.feature.solve.component.SolveTeacherIllustration
import com.bluetutor.android.feature.solve.data.SolveOcrApiClient
import com.bluetutor.android.ui.theme.BluetutorSpacing
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.launch
import java.io.File

@Suppress("DEPRECATION")
@Composable
fun SolveRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState = remember { solveRouteMockUiState() }
    var guideState by remember { mutableStateOf(SolveGuideConversationState()) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var pendingEditorImageType by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun applyRecognizedText(questionText: String) {
        if (questionText.isBlank()) {
            guideState = guideState.copy(
                isRecognizingImage = false,
                error = "图片识别结果为空，请重试或手动输入题目",
            )
            return
        }
        guideState = guideState.copy(
            problemText = questionText,
            studentInput = "",
            sessionId = null,
            messages = emptyList(),
            isSubmitting = false,
            isSolved = false,
            isRecognizingImage = false,
            error = null,
        )
    }

    fun recognizeImageToProblemText(imageBase64: String, imageType: String) {
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
        uiState = uiState,
        guideState = guideState,
        onTakePhoto = ::startTakePhotoFlow,
        onPickFromAlbum = { albumLauncher.launch("image/*") },
        onManualInput = { guideState = guideState.copy(error = null) },
        onProblemTextChange = {
            guideState = guideState.copy(problemText = it, error = null)
        },
        onStudentInputChange = {
            guideState = guideState.copy(studentInput = it, error = null)
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
                        guideState = guideState.copy(
                            isSubmitting = false,
                            sessionId = sessionId,
                            studentInput = "",
                            messages = emptyList(),
                            isSolved = false,
                        )
                    } catch (e: Exception) {
                        guideState = guideState.copy(isSubmitting = false, error = e.message)
                    }
                }
            }
        },
        onSendTurn = {
            val sessionId = guideState.sessionId
            val studentInput = guideState.studentInput.trim()
            if (sessionId == null) {
                guideState = guideState.copy(error = "请先创建解题会话")
            } else if (studentInput.isBlank()) {
                guideState = guideState.copy(error = "请先输入你的思路")
            } else {
                scope.launch {
                    val pendingStudentMessage = SolveChatMessage(role = "student", content = studentInput)
                    guideState = guideState.copy(isSubmitting = true, error = null)
                    try {
                        val turn = GuideApiClient.runTurn(sessionId, studentInput)
                        guideState = guideState.copy(
                            isSubmitting = false,
                            studentInput = "",
                            messages = guideState.messages + pendingStudentMessage + SolveChatMessage(
                                role = "tutor",
                                content = turn.question,
                            ),
                            isSolved = turn.isSolved,
                        )
                    } catch (e: Exception) {
                        guideState = guideState.copy(isSubmitting = false, error = e.message)
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun SolveScreen(
    uiState: SolveRouteUiState,
    guideState: SolveGuideConversationState,
    onTakePhoto: () -> Unit,
    onPickFromAlbum: () -> Unit,
    onManualInput: () -> Unit,
    onProblemTextChange: (String) -> Unit,
    onStudentInputChange: (String) -> Unit,
    onStartGuide: () -> Unit,
    onSendTurn: () -> Unit,
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

            GuideTrialSection(
                state = guideState,
                onProblemTextChange = onProblemTextChange,
                onStudentInputChange = onStudentInputChange,
                onStartGuide = onStartGuide,
                onSendTurn = onSendTurn,
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = BluetutorSpacing.screenHorizontal)
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFEAF6FD), Color(0xFFF6FBFF)),
                    ),
                    RoundedCornerShape(28.dp),
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "体验分步引导",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A3550),
                fontWeight = FontWeight.ExtraBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.guides.forEach { guide ->
                    SolveGuideCard(
                        guide = guide,
                        modifier = Modifier.weight(1f),
                    )
                }
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
                text = "🦉 小提示",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF38ABDA),
                fontWeight = FontWeight.ExtraBold,
            )

            uiState.tips.forEach { tip ->
                SolveTipRow(tip = tip)
            }
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
private fun SolveGuideCard(
    guide: SolveGuideUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = guide.emoji, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = guide.title,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF2A4A60),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
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

@Composable
private fun GuideTrialSection(
    state: SolveGuideConversationState,
    onProblemTextChange: (String) -> Unit,
    onStudentInputChange: (String) -> Unit,
    onStartGuide: () -> Unit,
    onSendTurn: () -> Unit,
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
            text = "已接通体验版",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF38ABDA),
            fontWeight = FontWeight.ExtraBold,
        )

        Text(
            text = if (state.sessionId == null) {
                "先手动输入一道题，创建 Guide 会话后再发送你的思路。"
            } else {
                "会话已创建，你可以继续输入自己的思路，让 AI 老师顺着你的想法引导。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4B6578),
        )

        if (state.sessionId != null) {
            Text(
                text = "会话 ID：${state.sessionId.take(8)}...",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7A96A8),
            )
        }

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
        }

        OutlinedTextField(
            value = state.problemText,
            onValueChange = onProblemTextChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.sessionId == null && !state.isSubmitting && !state.isRecognizingImage,
            label = { Text("题目") },
            placeholder = { Text("例如：小明有12个苹果，吃了3个，还剩几个？") },
            minLines = 3,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38ABDA),
                unfocusedBorderColor = Color(0xFFD7E8F2),
            ),
        )

        if (state.sessionId == null) {
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
        } else {
            OutlinedTextField(
                value = state.studentInput,
                onValueChange = onStudentInputChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && !state.isSolved && !state.isRecognizingImage,
                label = { Text("我的思路") },
                placeholder = { Text("例如：我先想到 12 减 3") },
                minLines = 2,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38ABDA),
                    unfocusedBorderColor = Color(0xFFD7E8F2),
                ),
            )

            Button(
                onClick = onSendTurn,
                enabled = !state.isSubmitting && !state.isSolved && !state.isRecognizingImage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(if (state.isSolved) "已完成本题" else "发送思路")
                }
            }
        }

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB42318),
            )
        }

        if (state.messages.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.messages.forEach { message ->
                    SolveMessageBubble(message = message)
                }
            }
        }

        if (state.isSolved) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDDF7E5), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "当前题目已被判定为已解决，可以继续补做报告/题解接线。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF166534),
                )
            }
        }
    }
}

@Composable
private fun SolveMessageBubble(message: SolveChatMessage) {
    val isStudent = message.role == "student"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isStudent) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .background(
                    if (isStudent) Color(0xFFDDF4FF) else Color(0xFFF3F8FB),
                    RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isStudent) Color(0xFF0B4F70) else Color(0xFF2B465A),
            )
        }
    }
}

private data class SolveRouteUiState(
    val entries: List<SolveEntryUiModel>,
    val guides: List<SolveGuideUiModel>,
    val tips: List<SolveTipUiModel>,
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

private data class SolveGuideUiModel(
    val emoji: String,
    val title: String,
)

private data class SolveTipUiModel(
    val emoji: String,
    val text: String,
)

private data class SolveGuideConversationState(
    val problemText: String = "",
    val studentInput: String = "",
    val sessionId: String? = null,
    val messages: List<SolveChatMessage> = emptyList(),
    val isSubmitting: Boolean = false,
    val isRecognizingImage: Boolean = false,
    val isSolved: Boolean = false,
    val error: String? = null,
)

private data class SolveChatMessage(
    val role: String,
    val content: String,
)

private fun solveRouteMockUiState(): SolveRouteUiState = SolveRouteUiState(
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
    guides = listOf(
        SolveGuideUiModel("🏃", "追及问题"),
        SolveGuideUiModel("🐔🐰", "鸡兔同笼"),
    ),
    tips = listOf(
        SolveTipUiModel("📸", "尽量拍清楚题干和图形，字迹越清晰越好哦"),
        SolveTipUiModel("💡", "我不会直接给答案，会一步步帮你理解！"),
        SolveTipUiModel("🧩", "做完还能推荐相似题，帮你举一反三"),
    ),
)

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