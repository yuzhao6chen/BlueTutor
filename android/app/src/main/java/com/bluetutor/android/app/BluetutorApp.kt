package com.bluetutor.android.app

import androidx.compose.runtime.Composable
import com.bluetutor.android.navigation.MainScaffold
import com.bluetutor.android.ui.theme.BluetutorAndroidTheme

@Composable
fun BluetutorApp() {
    BluetutorAndroidTheme(
        darkTheme = false,
        dynamicColor = false,
    ) {
        MainScaffold()
    }
}