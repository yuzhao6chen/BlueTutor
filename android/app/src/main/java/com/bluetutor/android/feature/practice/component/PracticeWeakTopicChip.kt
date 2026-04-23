package com.bluetutor.android.feature.practice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.practice.PracticeWeakTopicUiModel

@Composable
fun PracticeWeakTopicChip(
    item: PracticeWeakTopicUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF9FCFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = item.emoji, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${item.count} 题",
                style = MaterialTheme.typography.labelSmall,
                color = item.chipTextColor,
                modifier = Modifier
                    .background(item.chipBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        Text(
            text = item.name,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF2A4A60),
        )
    }
}