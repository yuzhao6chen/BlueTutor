package com.bluetutor.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun BtPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(BluetutorRadius.lg)

    Row(
        modifier = modifier
            .clip(shape)
            .alpha(if (enabled) 1f else 0.55f)
            .background(BluetutorGradients.primaryAction())
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = BluetutorSpacing.lg, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.let {
            it()
            Box(modifier = Modifier.padding(end = BluetutorSpacing.sm))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        trailingContent?.let {
            Box(modifier = Modifier.padding(start = BluetutorSpacing.sm))
            it()
        }
    }
}

@Composable
fun BtSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(BluetutorRadius.lg)

    Box(
        modifier = modifier
            .clip(shape)
            .alpha(if (enabled) 1f else 0.55f)
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = BluetutorSpacing.lg, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
        )
    }
}