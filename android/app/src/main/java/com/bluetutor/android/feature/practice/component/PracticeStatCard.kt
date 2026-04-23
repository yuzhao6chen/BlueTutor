package com.bluetutor.android.feature.practice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.bluetutor.android.feature.practice.PracticeStatUiModel

@Composable
fun PracticeStatCard(
    item: PracticeStatUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
            .defaultMinSize(minHeight = 124.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    brush = Brush.linearGradient(listOf(Color(0xFFF2FBFF), Color(0xFFD8F0FA))),
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.emoji,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF5A9DBF),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = item.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0E567A),
            )

            Text(
                text = item.unit,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7AAFC8),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}