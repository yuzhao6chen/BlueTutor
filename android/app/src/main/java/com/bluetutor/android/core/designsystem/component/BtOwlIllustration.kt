package com.bluetutor.android.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
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
fun BtOwlIllustration(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "bt_owl_transition")
    val floatY = transition.animateFloat(
        initialValue = 4f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bt_owl_float",
    )
    val glowScale = transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bt_owl_glow",
    )
    val leftStarAlpha = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bt_left_star_alpha",
    )
    val rightStarScale = transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bt_right_star_scale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .graphicsLayer(
                    scaleX = glowScale.value,
                    scaleY = glowScale.value,
                    alpha = 0.92f,
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x73FFF4B8),
                            Color(0x45C9F0FF),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/owl.svg")
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = "BlueTutor owl",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    translationY = floatY.value,
                    scaleX = 1.03f,
                    scaleY = 1.03f,
                ),
        )

        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFFFE27A).copy(alpha = leftStarAlpha.value),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 0.dp, y = 8.dp)
                .size(22.dp),
        )

        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 20.dp)
                .size(18.dp)
                .graphicsLayer(
                    scaleX = rightStarScale.value,
                    scaleY = rightStarScale.value,
                ),
        )

        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFFDF1A5).copy(alpha = 0.72f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp, y = (-6).dp)
                .size(16.dp),
        )
    }
}