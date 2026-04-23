package com.bluetutor.android.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.bluetutor.android.R
import com.bluetutor.android.navigation.MainScaffold
import com.bluetutor.android.ui.theme.BluetutorAndroidTheme
import kotlinx.coroutines.delay

@Composable
fun BluetutorApp() {
    BluetutorAndroidTheme(
        darkTheme = false,
        dynamicColor = false,
    ) {
        var showLaunchScreen by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            delay(1_000)
            showLaunchScreen = false
        }

        if (showLaunchScreen) {
            BluetutorLaunchScreen()
        } else {
            MainScaffold()
        }
    }
}

@Composable
private fun BluetutorLaunchScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.bt_splash_full),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .fillMaxHeight(0.38f),
        )
    }
}