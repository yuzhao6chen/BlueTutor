package com.bluetutor.android.feature.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.profile.component.ProfileAbilityBar
import com.bluetutor.android.feature.profile.component.ProfileOwlAvatar
import com.bluetutor.android.feature.profile.component.ProfileRecentActivityItem
import com.bluetutor.android.feature.profile.component.ProfileSettingItem
import com.bluetutor.android.feature.profile.component.ProfileStatOverviewCard
import com.bluetutor.android.feature.profile.component.ProfileWeekCheckInCard
import com.bluetutor.android.feature.profile.data.ProfileLocalCache
import com.bluetutor.android.feature.profile.data.ProfileLocalSnapshot
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(ProfilePage.Overview) }
    var localSnapshot by remember { mutableStateOf(ProfileLocalCache.readProfile(context) ?: ProfileLocalSnapshot()) }
    var celebrationActive by remember { mutableStateOf(false) }
    var showNicknameDialog by rememberSaveable { mutableStateOf(false) }
    var nicknameDraft by rememberSaveable { mutableStateOf(localSnapshot.userName) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    fun persistSnapshot(updatedSnapshot: ProfileLocalSnapshot) {
        localSnapshot = updatedSnapshot
        ProfileLocalCache.saveProfile(context, updatedSnapshot)
    }

    fun updateSnapshot(transform: (ProfileLocalSnapshot) -> ProfileLocalSnapshot) {
        persistSnapshot(transform(localSnapshot))
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        val avatarPath = uri?.let { ProfileLocalCache.importAvatar(context, it) } ?: return@rememberLauncherForActivityResult
        updateSnapshot { it.copy(avatarPath = avatarPath) }
    }

    val uiState = remember(localSnapshot) {
        profileUiState(localSnapshot)
    }

    BackHandler(enabled = page == ProfilePage.Settings) {
        page = ProfilePage.Overview
    }

    BackHandler(enabled = page == ProfilePage.HelpFeedback) {
        page = ProfilePage.Settings
    }

    LaunchedEffect(page) {
        onBottomBarVisibilityChange(page == ProfilePage.Overview)
    }

    DisposableEffect(Unit) {
        onDispose {
            onBottomBarVisibilityChange(true)
        }
    }

    LaunchedEffect(celebrationActive) {
        if (celebrationActive) {
            delay(900)
            celebrationActive = false
        }
    }

    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("修改昵称") },
            text = {
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = { nicknameDraft = it.take(12) },
                    singleLine = true,
                    label = { Text("昵称") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalizedName = nicknameDraft.trim().ifBlank { localSnapshot.userName }
                        updateSnapshot { it.copy(userName = normalizedName) }
                        showNicknameDialog = false
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置本地资料") },
            text = { Text("这会清空昵称、头像、打卡记录和本地设置，静态画像与统计展示不受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ProfileLocalCache.resetProfile(context, localSnapshot)
                        localSnapshot = ProfileLocalSnapshot()
                        nicknameDraft = localSnapshot.userName
                        celebrationActive = false
                        showResetDialog = false
                    },
                ) {
                    Text("确认重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    Crossfade(targetState = page, label = "profile-page") { currentPage ->
        when (currentPage) {
            ProfilePage.Overview -> {
                ProfileScreen(
                    uiState = uiState,
                    celebrationActive = celebrationActive,
                    onCheckInClick = {
                        if (!uiState.todayCheckedIn) {
                            val today = LocalDate.now().toString()
                            updateSnapshot {
                                val nextDates = (it.checkedInDates + today)
                                    .mapNotNull { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
                                    .sorted()
                                    .takeLast(30)
                                    .map { value -> value.toString() }
                                    .toSet()
                                it.copy(checkedInDates = nextDates)
                            }
                            if (localSnapshot.motionEffectsEnabled) {
                                celebrationActive = true
                            }
                        }
                    },
                    onSettingsClick = { page = ProfilePage.Settings },
                    modifier = modifier,
                )
            }

            ProfilePage.Settings -> {
                ProfileSettingsScreen(
                    snapshot = localSnapshot,
                    onBackClick = { page = ProfilePage.Overview },
                    onEditNicknameClick = {
                        nicknameDraft = localSnapshot.userName
                        showNicknameDialog = true
                    },
                    onPickAvatarClick = { avatarPickerLauncher.launch("image/*") },
                    onClearAvatarClick = {
                        ProfileLocalCache.deleteAvatar(localSnapshot.avatarPath)
                        updateSnapshot { it.copy(avatarPath = null) }
                    },
                    onGradeSelected = { gradeLabel ->
                        updateSnapshot { it.copy(gradeLabel = gradeLabel) }
                    },
                    onReminderEnabledChange = { enabled ->
                        updateSnapshot { it.copy(reminderEnabled = enabled) }
                    },
                    onMotionEffectsEnabledChange = { enabled ->
                        updateSnapshot { it.copy(motionEffectsEnabled = enabled) }
                    },
                    onOpenHelpFeedback = { page = ProfilePage.HelpFeedback },
                    onResetProfileClick = { showResetDialog = true },
                    modifier = modifier,
                )
            }

            ProfilePage.HelpFeedback -> {
                ProfileHelpFeedbackScreen(
                    onBackClick = { page = ProfilePage.Settings },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    celebrationActive: Boolean,
    onCheckInClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEEF9FF),
                                Color(0xFFC8EDFB),
                                Color(0xFF7ED3F4),
                                Color(0xFF38ABDA),
                                Color(0xFF1DA8DA),
                            ),
                        ),
                        shape = RoundedCornerShape(32.dp),
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProfileOwlAvatar(avatarPath = uiState.avatarPath)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs),
                    ) {
                        Text(
                            text = uiState.userName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF0B4F70),
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileHeaderChip(text = uiState.gradeLabel, color = Color(0xFF0B4F70), backgroundColor = Color(0x2E38ABDA))
                            ProfileHeaderChip(
                                text = if (uiState.streakDays > 0) "🔥 连续 ${uiState.streakDays} 天" else "🌱 养成中",
                                color = Color(0xFF7A5000),
                                backgroundColor = Color(0x47FFE600),
                            )
                        }
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "设置",
                            tint = Color(0xFF0B4F70),
                        )
                    }
                }

                ProfileWeekCheckInCard(
                    weekDays = uiState.weekDays,
                    streakDays = uiState.streakDays,
                    todayCheckedIn = uiState.todayCheckedIn,
                    celebrationActive = celebrationActive,
                    onCheckInClick = onCheckInClick,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                ProfileStatOverviewCard(item = uiState.stats[0], modifier = Modifier.weight(1f))
                ProfileStatOverviewCard(item = uiState.stats[1], modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                ProfileStatOverviewCard(item = uiState.stats[2], modifier = Modifier.weight(1f))
                ProfileStatOverviewCard(item = uiState.stats[3], modifier = Modifier.weight(1f))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
        ) {
            ProfileSectionHeader(title = "我的能力画像")

            uiState.abilities.forEach { ability ->
                ProfileAbilityBar(item = ability)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                ProfileInsightCard(
                    title = uiState.strengthNote.title,
                    description = uiState.strengthNote.description,
                    containerColor = uiState.strengthNote.containerColor,
                    contentColor = uiState.strengthNote.contentColor,
                    modifier = Modifier.weight(1f),
                )
                ProfileInsightCard(
                    title = uiState.improvementNote.title,
                    description = uiState.improvementNote.description,
                    containerColor = uiState.improvementNote.containerColor,
                    contentColor = uiState.improvementNote.contentColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            ProfileSectionHeader(title = "最近学习")
            uiState.recentActivities.forEachIndexed { index, item ->
                ProfileRecentActivityItem(
                    item = item,
                    showDivider = index < uiState.recentActivities.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingsScreen(
    snapshot: ProfileLocalSnapshot,
    onBackClick: () -> Unit,
    onEditNicknameClick: () -> Unit,
    onPickAvatarClick: () -> Unit,
    onClearAvatarClick: () -> Unit,
    onGradeSelected: (String) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onMotionEffectsEnabledChange: (Boolean) -> Unit,
    onOpenHelpFeedback: () -> Unit,
    onResetProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingsItems = remember { profileSettingsItems() }

    ProfileSubpageScaffold(
        title = "设置",
        onBackClick = onBackClick,
        modifier = modifier,
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
            ProfileSettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
                    ProfileSectionHeader(title = "个人资料")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileOwlAvatar(avatarPath = snapshot.avatarPath, size = 92.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = snapshot.userName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1A3550),
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (snapshot.avatarPath.isNullOrBlank()) {
                                    "当前使用默认猫头鹰头像。"
                                } else {
                                    "当前已使用自定义头像。"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6D8695),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
                        Button(onClick = onPickAvatarClick) {
                            Text("上传头像")
                        }
                        OutlinedButton(
                            onClick = onClearAvatarClick,
                            enabled = !snapshot.avatarPath.isNullOrBlank(),
                        ) {
                            Text("恢复默认")
                        }
                    }

                    ProfileSettingItem(
                        item = ProfileSettingUiModel(
                            icon = Icons.Rounded.Edit,
                            label = settingsItems[0].label,
                            description = snapshot.userName,
                        ),
                        onClick = onEditNicknameClick,
                    )
                    ProfileSettingItem(
                        item = ProfileSettingUiModel(
                            icon = Icons.Rounded.Image,
                            label = settingsItems[1].label,
                            description = settingsItems[1].description,
                        ),
                        onClick = onPickAvatarClick,
                    )
                }
            }

            ProfileSettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
                    ProfileSectionHeader(title = "学习设置")
                    Text(
                        text = "年级",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1A3550),
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm),
                    ) {
                        gradeOptions.forEach { grade ->
                            ProfileChoiceChip(
                                text = grade,
                                selected = grade == snapshot.gradeLabel,
                                onClick = { onGradeSelected(grade) },
                            )
                        }
                    }
                }
            }

            ProfileSettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
                    ProfileSectionHeader(title = "偏好")
                    ProfileSwitchRow(
                        title = "页面动效",
                        description = "关闭后，打卡庆祝和轻微动画会被抑制。",
                        checked = snapshot.motionEffectsEnabled,
                        onCheckedChange = onMotionEffectsEnabledChange,
                    )
                    ProfileSwitchRow(
                        title = "学习提醒",
                        description = "后续可用于提醒你按时学习。",
                        checked = snapshot.reminderEnabled,
                        onCheckedChange = onReminderEnabledChange,
                    )
                }
            }

            ProfileSettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
                    ProfileSectionHeader(title = "更多")
                    ProfileSettingItem(
                        item = ProfileSettingUiModel(
                            icon = Icons.Rounded.HelpOutline,
                            label = settingsItems[2].label,
                            description = settingsItems[2].description,
                        ),
                        onClick = onOpenHelpFeedback,
                    )
                    ProfileSettingItem(
                        item = ProfileSettingUiModel(
                            icon = Icons.Rounded.Refresh,
                            label = settingsItems[3].label,
                            description = settingsItems[3].description,
                        ),
                        onClick = onResetProfileClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHelpFeedbackScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileSubpageScaffold(
        title = "帮助与反馈",
        onBackClick = onBackClick,
        modifier = modifier,
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
            ProfileSettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
                    ProfileSectionHeader(title = "反馈方式")
                    Text(
                        text = "如果你遇到问题，或者希望补充新功能、优化体验，当前直接向下面这个仓库地址提交 issue 即可。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4F6678),
                    )
                    Surface(
                        color = Color(0xFFF7FBFE),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            text = "https://github.com/yuzhao6chen/BlueTutor",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1A3550),
                        )
                    }
                    Text(
                        text = "建议在 issue 里附上页面截图、操作步骤和你期望的效果，会更方便排查。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8AA8B8),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.ExtraBold,
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.18f)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF7ED3F4), Color(0xFF38ABDA)),
                    ),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
    }
}

@Composable
private fun ProfileInsightCard(
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun ProfileHeaderChip(
    text: String,
    color: Color,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(BluetutorRadius.pill))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun ProfileSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSubpageScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
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
        content(innerPadding)
    }
}

@Composable
private fun ProfileChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF1DA8DA) else Color(0xFFF3F9FC),
        shape = RoundedCornerShape(BluetutorRadius.pill),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Color(0xFF1A3550),
        )
    }
}

@Composable
private fun ProfileSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7FBFE), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF1A3550),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8AA8B8),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private enum class ProfilePage {
    Overview,
    Settings,
    HelpFeedback,
}

private val gradeOptions = listOf("一年级", "二年级", "三年级", "四年级", "五年级", "六年级")