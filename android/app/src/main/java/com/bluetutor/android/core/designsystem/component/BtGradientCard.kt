package com.bluetutor.android.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bluetutor.android.ui.theme.BlueTutorOutlineSoft
import com.bluetutor.android.ui.theme.BluetutorElevation
import com.bluetutor.android.ui.theme.BluetutorGradients
import com.bluetutor.android.ui.theme.BluetutorRadius
import com.bluetutor.android.ui.theme.BluetutorSpacing

@Composable
fun BtGradientCard(
    modifier: Modifier = Modifier,
    brush: Brush = BluetutorGradients.heroCard(),
    contentColor: Color = Color.White,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(BluetutorSpacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(BluetutorRadius.hero)

    Box(
        modifier = modifier
            .shadow(BluetutorElevation.high, shape)
            .clip(shape)
            .background(brush)
            .then(
                if (border != null) Modifier.border(border, shape)
                else Modifier.border(1.dp, BlueTutorOutlineSoft.copy(alpha = 0.3f), shape)
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(BluetutorSpacing.md),
                content = content,
            )
        }
    }
}