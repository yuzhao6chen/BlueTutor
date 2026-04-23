package com.bluetutor.android.feature.practice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.practice.PracticeRecommendedPracticeUiModel

@Composable
fun PracticeRecommendationCard(
    item: PracticeRecommendedPracticeUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FCFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PracticeSmallPill(item.type, item.typeBackground, item.typeTextColor)
            PracticeSmallPill(item.difficulty, item.difficultyBackground, item.difficultyTextColor)
            Text(
                text = "错 ${item.wrongCount} 次",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA0B4C4),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Text(
                text = item.question,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2A4A60),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color(0xFF4A90E2), Color(0xFF1D67C7)),
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "练习 →",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .background(Color(0xFFEEF7FF), RoundedCornerShape(14.dp))
                    .height(46.dp)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "解析",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF38ABDA),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PracticeSmallPill(
    text: String,
    background: Color,
    content: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}