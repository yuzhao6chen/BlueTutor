package com.bluetutor.android.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.bluetutor.android.core.designsystem.component.BtGradientCard
import com.bluetutor.android.core.designsystem.component.BtPrimaryButton
import com.bluetutor.android.core.designsystem.component.BtProgressBar
import com.bluetutor.android.core.designsystem.component.BtSectionTitle
import com.bluetutor.android.core.designsystem.component.BtSecondaryButton
import com.bluetutor.android.core.designsystem.component.BtTagChip
import com.bluetutor.android.core.designsystem.component.BtTagChipTone
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun BluetutorPlaceholderScreen(
    title: String,
    subtitle: String,
    hint: String,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground())
            .padding(
                horizontal = BluetutorSpacing.screenHorizontal,
                vertical = BluetutorSpacing.screenVertical,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BtTagChip(
                text = "阶段 2 · Design System",
                tone = BtTagChipTone.Primary,
                leadingText = "✨",
            )
            Spacer(modifier = Modifier.height(BluetutorSpacing.lg))
            Text(
                text = emoji,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(BluetutorSpacing.lg))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(BluetutorSpacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(BluetutorSpacing.xl))
            BtGradientCard(
                modifier = Modifier.fillMaxWidth(),
                brush = BluetutorGradients.heroCard(),
            ) {
                BtTagChip(
                    text = "主应用壳已切换",
                    tone = BtTagChipTone.Accent,
                    leadingText = "🛠",
                )

                Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm)) {
                    Text(
                        text = "第二阶段的业务组件已经开始抽离",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.surface.copy(alpha = 0.92f),
                    )

                    BtProgressBar(
                        progress = 2f / 7f,
                        label = "迁移总进度",
                        valueText = "第 2 阶段 / 7",
                        textColor = colorScheme.surface,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
                    BtPrimaryButton(
                        text = "开始迁移真实页面",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                    BtSecondaryButton(
                        text = "查看计划",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(BluetutorSpacing.xl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                BtSectionTitle(
                    title = "已沉淀的通用组件",
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm)) {
                    BtTagChip(text = "主按钮", tone = BtTagChipTone.Secondary)
                    BtTagChip(text = "标签", tone = BtTagChipTone.Primary)
                    BtTagChip(text = "渐变卡片", tone = BtTagChipTone.Warning)
                }
            }
        }
    }
}