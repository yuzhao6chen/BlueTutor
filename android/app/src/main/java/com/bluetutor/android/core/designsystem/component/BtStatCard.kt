package com.bluetutor.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.ui.theme.BlueTutorOutlineSoft
import com.bluetutor.android.ui.theme.BlueTutorSurface
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun BtStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    iconEmoji: String? = null,
    containerColor: Color = BlueTutorSurface.copy(alpha = 0.9f),
) {
    Column(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(BluetutorRadius.lg))
            .border(1.dp, BlueTutorOutlineSoft, RoundedCornerShape(BluetutorRadius.lg))
            .defaultMinSize(minHeight = 108.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs),
    ) {
        if (iconEmoji != null) {
            Text(
                text = iconEmoji,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = buildString {
                append(value)
                if (unit != null) {
                    append(unit)
                }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}