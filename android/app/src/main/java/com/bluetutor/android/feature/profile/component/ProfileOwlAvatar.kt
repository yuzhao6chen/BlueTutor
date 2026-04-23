package com.bluetutor.android.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import java.io.File

@Composable
fun ProfileOwlAvatar(
    avatarPath: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    val context = LocalContext.current
    val avatarModel = avatarPath
        ?.takeIf { File(it).exists() }
        ?.let { File(it) }
        ?: "file:///android_asset/owl.svg"
    val hasCustomAvatar = !avatarPath.isNullOrBlank()
    val contentScale = if (hasCustomAvatar) ContentScale.Crop else ContentScale.Fit

    Box(
        modifier = modifier
            .size(size)
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
                .data(avatarModel)
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = "Profile avatar owl",
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hasCustomAvatar) 4.dp else 9.dp)
                .clip(CircleShape),
        )
    }
}