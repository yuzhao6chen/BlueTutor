package com.bluetutor.android.feature.practice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun PracticeMascotIllustration(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(122.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x99FFF4B0),
                            Color(0x40FFFFFF),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/owl.svg")
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = "Practice owl mascot",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .size(116.dp),
        )
    }
}