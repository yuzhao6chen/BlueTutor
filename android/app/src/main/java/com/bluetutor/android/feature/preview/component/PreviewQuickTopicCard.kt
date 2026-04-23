package com.bluetutor.android.feature.preview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.preview.PreviewQuickTopicUiModel
import com.bluetutor.android.ui.theme.BlueTutorOutline
import com.bluetutor.android.ui.theme.BlueTutorPrimaryContainer
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun PreviewQuickTopicCard(
    topic: PreviewQuickTopicUiModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(BluetutorRadius.lg)
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .background(
                if (selected) BlueTutorPrimaryContainer else colorScheme.surface,
                shape,
            )
            .border(
                width = 1.dp,
                color = if (selected) colorScheme.primary.copy(alpha = 0.25f) else BlueTutorOutline,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = 92.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs),
    ) {
        Text(
            text = topic.emoji,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = topic.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) colorScheme.primary else colorScheme.onSurface,
        )
    }
}