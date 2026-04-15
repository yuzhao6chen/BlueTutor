package com.bluetutor.android.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.profile.ProfileStatUiModel
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun ProfileStatOverviewCard(
    item: ProfileStatUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(BluetutorRadius.xl))
            .defaultMinSize(minHeight = 124.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFEAF6FF), Color(0xFFCBEAFB)),
                    ),
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.emoji,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF7A96A8),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = item.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A3550),
            )

            Text(
                text = item.unit,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A96A8),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}