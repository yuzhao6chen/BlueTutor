package com.bluetutor.android.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorSpacing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun BtHeroCard(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    kicker: String? = null,
    subtitle: String? = null,
    trailingSize: Dp = 120.dp,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BtGradientCard(
        modifier = modifier,
        brush = BluetutorGradients.heroCard(),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sm),
            ) {
                if (badgeText != null) {
                    BtTagChip(
                        text = badgeText,
                        tone = BtTagChipTone.Accent,
                    )
                }

                if (kicker != null) {
                    Text(
                        text = kicker,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }

            if (trailingContent != null) {
                Box(
                    modifier = Modifier
                        .size(trailingSize)
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                    content = trailingContent,
                )
            }
        }

        footer?.invoke(this)
    }
}