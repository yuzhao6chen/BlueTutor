package com.bluetutor.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object BluetutorGradients {
    fun pageBackground(): Brush = Brush.verticalGradient(
        colors = listOf(
            BlueTutorBackgroundSoft,
            BlueTutorBackground,
            BlueTutorSurface,
        ),
    )

    fun primaryAction(): Brush = Brush.horizontalGradient(
        colors = listOf(
            BlueTutorSecondary,
            BlueTutorPrimary,
            BlueTutorPrimaryStrong,
        ),
    )

    fun heroCard(): Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1DA8DA),
            Color(0xFF38ABDA),
            Color(0xFF7ED3F4),
            Color(0xFFC8EDFB),
        ),
    )

    fun warmProgress(): Brush = Brush.horizontalGradient(
        colors = listOf(
            BlueTutorAccent,
            Color(0xFFFFD54F),
            Color(0xFFFFB800),
        ),
    )
}