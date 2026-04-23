package com.bluetutor.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val BluetutorShapes = Shapes(
    extraSmall = RoundedCornerShape(BluetutorRadius.sm),
    small = RoundedCornerShape(BluetutorRadius.md),
    medium = RoundedCornerShape(BluetutorRadius.lg),
    large = RoundedCornerShape(BluetutorRadius.xl),
    extraLarge = RoundedCornerShape(BluetutorRadius.hero),
)