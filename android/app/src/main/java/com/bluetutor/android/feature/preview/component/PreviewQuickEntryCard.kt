package com.bluetutor.android.feature.preview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.preview.PreviewQuickEntryTone
import com.bluetutor.android.feature.preview.PreviewQuickEntryUiModel
import com.bluetutor.android.ui.theme.BlueTutorAccentContainer
import com.bluetutor.android.ui.theme.BlueTutorBackgroundSoft
import com.bluetutor.android.ui.theme.BlueTutorSecondaryContainer
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun PreviewQuickEntryCard(
    entry: PreviewQuickEntryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = when (entry.tone) {
        PreviewQuickEntryTone.Sky -> Brush.linearGradient(
            colors = listOf(BlueTutorBackgroundSoft, BlueTutorSecondaryContainer),
        )

        PreviewQuickEntryTone.Warm -> Brush.linearGradient(
            colors = listOf(BlueTutorAccentContainer, Color(0xFFFFF1C7)),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(brush, RoundedCornerShape(BluetutorRadius.xl))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm),
    ) {
        Text(
            text = entry.emoji,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = entry.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}