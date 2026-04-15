package com.bluetutor.android.feature.preview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.core.designsystem.component.BtPrimaryButton
import com.bluetutor.android.core.designsystem.component.BtProgressBar
import com.bluetutor.android.core.designsystem.component.BtTagChip
import com.bluetutor.android.feature.preview.PreviewRecommendedLessonUiModel
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color

@Composable
fun PreviewRecommendedLessonCard(
    lesson: PreviewRecommendedLessonUiModel,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val progressBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF7DD3F7), Color(0xFF38ABDA)),
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(274.dp),
        shape = RoundedCornerShape(BluetutorRadius.xl),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(BluetutorSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BtTagChip(
                    text = lesson.tag,
                    tone = com.bluetutor.android.core.designsystem.component.BtTagChipTone.Primary,
                )
                Text(
                    text = lesson.grade,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            BtProgressBar(
                progress = lesson.masteryPercent / 100f,
                label = "掌握度",
                valueText = "${lesson.masteryPercent}%",
                fillBrush = progressBrush,
                trackColor = colorScheme.primaryContainer.copy(alpha = 0.55f),
            )

            Spacer(modifier = Modifier.weight(1f))

            BtPrimaryButton(
                text = "开始预习",
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}