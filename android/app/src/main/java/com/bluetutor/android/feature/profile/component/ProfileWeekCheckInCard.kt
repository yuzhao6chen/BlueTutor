package com.bluetutor.android.feature.profile.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.profile.ProfileWeekDayUiModel
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun ProfileWeekCheckInCard(
    weekDays: List<ProfileWeekDayUiModel>,
    streakDays: Int,
    todayCheckedIn: Boolean,
    celebrationActive: Boolean,
    modifier: Modifier = Modifier,
    onCheckInClick: () -> Unit = {},
) {
    val buttonScale = animateFloatAsState(
        targetValue = if (celebrationActive) 1.08f else 1f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "check-in-scale",
    )
    val buttonRotation = animateFloatAsState(
        targetValue = if (celebrationActive) 2.2f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "check-in-rotation",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.32f), RoundedCornerShape(BluetutorRadius.lg))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "本周打卡",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xCC0B4F70),
                )
                AnimatedContent(targetState = streakDays, label = "streak-days") { targetDays ->
                    Text(
                        text = if (targetDays > 0) "已连续点亮 $targetDays 天" else "今天开始养成打卡节奏",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF0B4F70),
                    )
                }
            }

            Box(contentAlignment = Alignment.TopCenter) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = celebrationActive,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(320)) { it / 3 },
                    exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 3 },
                ) {
                    Row(
                        modifier = Modifier.offset(y = (-24).dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = "✨")
                        Text(text = "🌟")
                        Text(text = "🎉")
                    }
                }

                Surface(
                    modifier = Modifier
                        .scale(buttonScale.value)
                        .graphicsLayer { rotationZ = buttonRotation.value }
                        .clickable(enabled = !todayCheckedIn, onClick = onCheckInClick),
                    color = if (todayCheckedIn) Color.White.copy(alpha = 0.9f) else Color(0xFF0B4F70),
                    shape = RoundedCornerShape(999.dp),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = if (todayCheckedIn) "今日已打卡" else "立即打卡",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (todayCheckedIn) Color(0xFF0B4F70) else Color.White,
                    )
                }
            }
        }

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
                                color = when {
                                    item.studied -> Color.White.copy(alpha = 0.96f)
                                    item.isToday -> Color.White.copy(alpha = 0.42f)
                                    else -> Color.White.copy(alpha = 0.25f)
                                },
                                shape = CircleShape,
                            )
                            .then(
                                if (item.isToday) {
                                    Modifier.background(Color.Transparent, CircleShape)
                                } else {
                                    Modifier
                                },
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
                        color = if (item.isToday) Color(0xFF0B4F70) else Color(0xB30B4F70),
                    )
                }
            }
        }

        AnimatedContent(targetState = todayCheckedIn, label = "check-in-hint") { checkedIn ->
            Text(
                text = if (checkedIn) {
                    "今天这颗星已经点亮，明天继续来打卡。"
                } else {
                    "先点亮今天的星星，再去做一节预习或练习。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD60B4F70),
            )
        }
    }
}