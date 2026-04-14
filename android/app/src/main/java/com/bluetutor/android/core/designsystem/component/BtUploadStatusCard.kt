package com.bluetutor.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

enum class BtUploadStatusCardState {
    Idle,
    Processing,
    Success,
}

@Composable
fun BtUploadStatusCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    state: BtUploadStatusCardState = BtUploadStatusCardState.Idle,
    onClick: (() -> Unit)? = null,
    fileName: String? = null,
    helperText: String? = null,
    formats: List<String> = emptyList(),
    leadingEmoji: String = "📄",
    primaryActionText: String? = null,
    primaryActionEnabled: Boolean = true,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    secondaryActionEnabled: Boolean = true,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(BluetutorRadius.xl)
    val stateStyle = when (state) {
        BtUploadStatusCardState.Idle -> UploadCardStateStyle(
            containerColor = Color.White,
            borderColor = Color(0xFFD8EAF4),
            chipTone = BtTagChipTone.Secondary,
            chipText = "待上传",
        )
        BtUploadStatusCardState.Processing -> UploadCardStateStyle(
            containerColor = Color(0x1FF59E0B),
            borderColor = Color(0x40F59E0B),
            chipTone = BtTagChipTone.Warning,
            chipText = "分析中",
        )
        BtUploadStatusCardState.Success -> UploadCardStateStyle(
            containerColor = Color(0x1F10B981),
            borderColor = Color(0x4010B981),
            chipTone = BtTagChipTone.Success,
            chipText = "已上传",
        )
    }

    Column(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .background(stateStyle.containerColor, shape)
            .border(1.dp, stateStyle.borderColor, shape)
            .padding(BluetutorSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = leadingEmoji,
                    style = MaterialTheme.typography.headlineMedium,
                )
                BtTagChip(
                    text = stateStyle.chipText,
                    tone = stateStyle.chipTone,
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (formats.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm),
            ) {
                formats.forEach { format ->
                    BtTagChip(
                        text = format,
                        tone = BtTagChipTone.Neutral,
                    )
                }
            }
        }

        if (fileName != null || helperText != null) {
            Column(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(BluetutorRadius.lg))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs),
            ) {
                if (fileName != null) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (helperText != null) {
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (primaryActionText != null && onPrimaryAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                BtPrimaryButton(
                    text = primaryActionText,
                    onClick = onPrimaryAction,
                    enabled = primaryActionEnabled,
                    modifier = Modifier.weight(1f),
                )

                if (secondaryActionText != null && onSecondaryAction != null) {
                    BtSecondaryButton(
                        text = secondaryActionText,
                        onClick = onSecondaryAction,
                        enabled = secondaryActionEnabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private data class UploadCardStateStyle(
    val containerColor: Color,
    val borderColor: Color,
    val chipTone: BtTagChipTone,
    val chipText: String,
)