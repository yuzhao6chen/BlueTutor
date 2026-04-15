package com.bluetutor.android.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.bluetutor.android.feature.profile.ProfileAbilityUiModel
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun ProfileAbilityBar(
    item: ProfileAbilityUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${item.level}%",
                style = MaterialTheme.typography.labelLarge,
                color = item.startColor,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(BluetutorRadius.pill)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(item.level / 100f)
                    .height(12.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(item.startColor, item.endColor)),
                        shape = RoundedCornerShape(BluetutorRadius.pill),
                    ),
            )
        }
    }
}