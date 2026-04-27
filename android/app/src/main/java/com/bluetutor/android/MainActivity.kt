package com.bluetutor.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bluetutor.android.app.BluetutorApp
import com.bluetutor.android.core.network.BackendEndpointConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        BackendEndpointConfig.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            BluetutorApp()
        }
    }
}