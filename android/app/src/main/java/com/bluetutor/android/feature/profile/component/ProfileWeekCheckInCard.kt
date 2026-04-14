package com.bluetutor.android.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.profile.ProfileWeekDayUiModel
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun ProfileWeekCheckInCard(
    weekDays: List<ProfileWeekDayUiModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.32f), RoundedCornerShape(BluetutorRadius.lg))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
    ) {
        Text(
            text = "本周打卡",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xCC0B4F70),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weekDays.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = if (item.studied) Color.White.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.studied) {
                            Text(
                                text = "⭐",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0x730B4F70),
                            )
                        }
                    }

                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xB30B4F70),
                    )
                }
            }
        }
    }
}