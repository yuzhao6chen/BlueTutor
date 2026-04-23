package com.bluetutor.android.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BlueTutorPrimary,
    onPrimary = BlueTutorDarkBackground,
    secondary = BlueTutorSecondary,
    onSecondary = BlueTutorDarkBackground,
    tertiary = BlueTutorAccent,
    onTertiary = BlueTutorAccentContainerText,
    background = BlueTutorDarkBackground,
    onBackground = BlueTutorDarkTextPrimary,
    surface = BlueTutorDarkSurface,
    onSurface = BlueTutorDarkTextPrimary,
    surfaceVariant = BlueTutorDarkSurfaceRaised,
    onSurfaceVariant = BlueTutorDarkTextSecondary,
    outline = BlueTutorDarkOutline,
    outlineVariant = BlueTutorDarkOutline,
    primaryContainer = BlueTutorPrimaryStrong,
    onPrimaryContainer = BlueTutorSurface,
    secondaryContainer = BlueTutorDarkSurfaceRaised,
    onSecondaryContainer = BlueTutorDarkTextPrimary,
    tertiaryContainer = BlueTutorAccentContainerText,
    onTertiaryContainer = BlueTutorSurface,
    error = BlueTutorError,
    onError = BlueTutorSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = BlueTutorPrimaryStrong,
    onPrimary = BlueTutorSurface,
    secondary = BlueTutorSecondary,
    onSecondary = BlueTutorPrimaryContainerText,
    tertiary = BlueTutorAccent,
    onTertiary = BlueTutorAccentContainerText,
    background = BlueTutorBackground,
    onBackground = BlueTutorTextPrimary,
    surface = BlueTutorSurface,
    onSurface = BlueTutorTextPrimary,
    surfaceVariant = BlueTutorBackgroundSoft,
    onSurfaceVariant = BlueTutorTextSecondary,
    outline = BlueTutorOutline,
    outlineVariant = BlueTutorOutlineSoft,
    primaryContainer = BlueTutorPrimaryContainer,
    onPrimaryContainer = BlueTutorPrimaryContainerText,
    secondaryContainer = BlueTutorSecondaryContainer,
    onSecondaryContainer = BlueTutorSecondaryContainerText,
    tertiaryContainer = BlueTutorAccentContainer,
    onTertiaryContainer = BlueTutorAccentContainerText,
    error = BlueTutorError,
    onError = BlueTutorSurface,
)

@Composable
fun BluetutorAndroidTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BluetutorShapes,
        content = content
    )
}