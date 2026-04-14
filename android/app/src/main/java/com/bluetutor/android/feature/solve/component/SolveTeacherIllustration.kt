package com.bluetutor.android.feature.solve.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun SolveTeacherIllustration(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "solve_teacher_transition")
    val floatY = transition.animateFloat(
        initialValue = 6f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "solve_teacher_float",
    )
    val warmGlowScale = transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "solve_teacher_glow",
    )
    val leftSparkleAlpha = transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "solve_left_sparkle",
    )
    val rightSparkleScale = transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "solve_right_sparkle",
    )
    val topSparkleAlpha = transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "solve_top_sparkle",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 22.dp, y = 12.dp)
                .size(width = 132.dp, height = 170.dp)
                .graphicsLayer(
                    scaleX = warmGlowScale.value,
                    scaleY = warmGlowScale.value,
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xA8FFF0AE),
                            Color(0x46FFF7D2),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-6).dp, y = (-2).dp)
                .size(width = 156.dp, height = 186.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3C92E2A6),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/owl_teach.svg")
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = "BlueTutor owl teacher scene",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(0.96f)
                .graphicsLayer(translationY = floatY.value),
        )

        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFFFE27A).copy(alpha = leftSparkleAlpha.value),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 28.dp)
                .size(20.dp),
        )

        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = Color(0xFFFFD861).copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 30.dp, y = 48.dp)
                .size(26.dp)
                .graphicsLayer(
                    scaleX = rightSparkleScale.value,
                    scaleY = rightSparkleScale.value,
                ),
        )

        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFFFD87B).copy(alpha = topSparkleAlpha.value),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = 22.dp)
                .size(18.dp),
        )

        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFFFECA0).copy(alpha = 0.96f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = 8.dp)
                .size(24.dp)
                .graphicsLayer(
                    scaleX = rightSparkleScale.value,
                    scaleY = rightSparkleScale.value,
                ),
        )

        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = Color(0xFFFFE27A).copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp, y = (-14).dp)
                .size(18.dp),
        )
    }
}