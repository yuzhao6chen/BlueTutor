package com.bluetutor.android.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.profile.component.ProfileAbilityBar
import com.bluetutor.android.feature.profile.component.ProfileOwlAvatar
import com.bluetutor.android.feature.profile.component.ProfileRecentActivityItem
import com.bluetutor.android.feature.profile.component.ProfileSettingItem
import com.bluetutor.android.feature.profile.component.ProfileStatOverviewCard
import com.bluetutor.android.feature.profile.component.ProfileWeekCheckInCard
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun ProfileRoute(modifier: Modifier = Modifier) {
    ProfileScreen(
        uiState = profileMockUiState(),
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BluetutorGradients.pageBackground())
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = BluetutorSpacing.screenHorizontal,
                vertical = BluetutorSpacing.screenVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.sectionGap),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEEF9FF),
                                Color(0xFFC8EDFB),
                                Color(0xFF7ED3F4),
                                Color(0xFF38ABDA),
                                Color(0xFF1DA8DA),
                            ),
                        ),
                        shape = RoundedCornerShape(32.dp),
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                ProfileOwlAvatar()

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.xs),
                ) {
                    Text(
                        text = uiState.userName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF0B4F70),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(Color(0x2E38ABDA), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = uiState.gradeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF0B4F70),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0x47FFE600), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "🔥 连续 ${uiState.streakDays} 天",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF7A5000),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "设置",
                        tint = Color(0xFF0B4F70),
                    )
                }
            }

            ProfileWeekCheckInCard(weekDays = uiState.weekDays)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                ProfileStatOverviewCard(item = uiState.stats[0], modifier = Modifier.weight(1f))
                ProfileStatOverviewCard(item = uiState.stats[1], modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                ProfileStatOverviewCard(item = uiState.stats[2], modifier = Modifier.weight(1f))
                ProfileStatOverviewCard(item = uiState.stats[3], modifier = Modifier.weight(1f))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
        ) {
            ProfileSectionHeader(title = "我的能力画像")

            uiState.abilities.forEach { ability ->
                ProfileAbilityBar(item = ability)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
            ) {
                ProfileInsightCard(
                    title = uiState.strengthNote.title,
                    description = uiState.strengthNote.description,
                    containerColor = uiState.strengthNote.containerColor,
                    contentColor = uiState.strengthNote.contentColor,
                    modifier = Modifier.weight(1f),
                )
                ProfileInsightCard(
                    title = uiState.improvementNote.title,
                    description = uiState.improvementNote.description,
                    containerColor = uiState.improvementNote.containerColor,
                    contentColor = uiState.improvementNote.contentColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            ProfileSectionHeader(title = "最近学习")
            uiState.recentActivities.forEachIndexed { index, item ->
                ProfileRecentActivityItem(
                    item = item,
                    showDivider = index < uiState.recentActivities.lastIndex,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
        ) {
            ProfileSectionHeader(title = "设置")
            uiState.settings.forEach { item ->
                ProfileSettingItem(item = item)
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A3550),
            fontWeight = FontWeight.ExtraBold,
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.18f)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF7ED3F4), Color(0xFF38ABDA)),
                    ),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
    }
}

@Composable
private fun ProfileInsightCard(
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}