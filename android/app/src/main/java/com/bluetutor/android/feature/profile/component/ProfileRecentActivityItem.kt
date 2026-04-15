package com.bluetutor.android.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.profile.ProfileRecentActivityUiModel
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun ProfileRecentActivityItem(
    item: ProfileRecentActivityUiModel,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFFE4F5FD), Color(0xFFC8E8F8))),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item.emoji, style = MaterialTheme.typography.titleMedium)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2563EB),
                        )
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2A4A60),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = item.timeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94B0C4),
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC4D8E4),
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF0F6FA)),
            )
        }
    }
}