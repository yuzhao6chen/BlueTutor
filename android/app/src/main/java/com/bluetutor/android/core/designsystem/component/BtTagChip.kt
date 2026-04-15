package com.bluetutor.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.ui.theme.BlueTutorAccentContainer
import com.bluetutor.android.ui.theme.BlueTutorAccentContainerText
import com.bluetutor.android.ui.theme.BlueTutorOutline
import com.bluetutor.android.ui.theme.BlueTutorPrimaryContainer
import com.bluetutor.android.ui.theme.BlueTutorPrimaryContainerText
import com.bluetutor.android.ui.theme.BlueTutorSecondaryContainer
import com.bluetutor.android.ui.theme.BlueTutorSecondaryContainerText
import com.bluetutor.android.ui.theme.BlueTutorSuccess
import com.bluetutor.android.ui.theme.BlueTutorSurface
import com.bluetutor.android.ui.theme.BlueTutorWarning
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

enum class BtTagChipTone {
    Primary,
    Secondary,
    Accent,
    Success,
    Warning,
    Neutral,
}

@Composable
fun BtTagChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: BtTagChipTone = BtTagChipTone.Primary,
    leadingText: String? = null,
) {
    val (containerColor, contentColor, borderColor) = when (tone) {
        BtTagChipTone.Primary -> Triple(BlueTutorPrimaryContainer, BlueTutorPrimaryContainerText, Color.Transparent)
        BtTagChipTone.Secondary -> Triple(BlueTutorSecondaryContainer, BlueTutorSecondaryContainerText, Color.Transparent)
        BtTagChipTone.Accent -> Triple(BlueTutorAccentContainer, BlueTutorAccentContainerText, Color.Transparent)
        BtTagChipTone.Success -> Triple(BlueTutorSuccess.copy(alpha = 0.14f), BlueTutorSuccess, Color.Transparent)
        BtTagChipTone.Warning -> Triple(BlueTutorWarning.copy(alpha = 0.14f), BlueTutorWarning, Color.Transparent)
        BtTagChipTone.Neutral -> Triple(BlueTutorSurface, MaterialTheme.colorScheme.onSurfaceVariant, BlueTutorOutline)
    }

    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(BluetutorRadius.pill))
            .border(1.dp, borderColor, RoundedCornerShape(BluetutorRadius.pill))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs),
    ) {
        if (leadingText != null) {
            Text(
                text = leadingText,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}