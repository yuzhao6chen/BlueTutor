package com.bluetutor.android.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
fun ProfileOwlAvatar(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(84.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF5C4),
                        Color(0xFFEAF6FF),
                        Color(0x80C8EDFB),
                    ),
                ),
                shape = CircleShape,
            )
            .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/owl.svg")
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = "Profile avatar owl",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(70.dp)
                .padding(2.dp),
        )
    }
}