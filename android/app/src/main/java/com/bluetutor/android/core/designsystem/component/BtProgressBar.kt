package com.bluetutor.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun BtProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    valueText: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trackColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
    fillBrush: Brush = BluetutorGradients.warmProgress(),
    height: Dp = 10.dp,
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm),
    ) {
        if (label != null || valueText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                    )
                }

                if (valueText != null) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(BluetutorRadius.pill))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeProgress)
                    .height(height)
                    .clip(RoundedCornerShape(BluetutorRadius.pill))
                    .background(fillBrush),
            )
        }
    }
}
