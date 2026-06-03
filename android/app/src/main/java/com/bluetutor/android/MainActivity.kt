
package com.bluetutor.android

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bluetutor.android.app.BluetutorApp
import com.bluetutor.android.core.network.BackendEndpointConfig
import com.bluetutor.android.floatingball.FloatingBallManager
import com.bluetutor.android.floatingball.FloatingBallService

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startFloatingBallService()
            if (!FloatingBallManager.hasMediaProjectionPermission()) {
                requestMediaProjectionPermission()
            }
            requestUsageStatsPermissionIfNeeded()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            FloatingBallManager.mediaProjectionResultCode = result.resultCode
            FloatingBallManager.mediaProjectionResultData = result.data
            FloatingBallManager.saveMediaProjection(this)
        }
        startFloatingBallService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        BackendEndpointConfig.initialize(applicationContext)
        FloatingBallManager.restoreMediaProjection(this)
        enableEdgeToEdge()
        setContent {
            BluetutorApp()
        }
    }

    override fun onResume() {
        super.onResume()
        tryStartFloatingBall()
        handlePendingMediaProjectionRequest()
    }

    private fun tryStartFloatingBall() {
        if (!FloatingBallManager.isEnabled(this)) return
        if (FloatingBallManager.isServiceRunning) return
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        startFloatingBallService()
    }

    private fun handlePendingMediaProjectionRequest() {
        if (!FloatingBallManager.consumeNeedsMediaProjection()) return
        if (FloatingBallManager.hasMediaProjectionPermission()) return
        requestMediaProjectionPermission()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestMediaProjectionPermission() {
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
        } catch (_: Exception) {
            startFloatingBallService()
        }
    }

    private fun startFloatingBallService() {
        val intent = Intent(this, FloatingBallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    fun enableFloatingBall() {
        FloatingBallManager.setEnabled(this, true)
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else {
            startFloatingBallService()
            if (!FloatingBallManager.hasMediaProjectionPermission()) {
                requestMediaProjectionPermission()
            }
            requestUsageStatsPermissionIfNeeded()
        }
    }

    private fun requestUsageStatsPermissionIfNeeded() {
        if (!FloatingBallManager.hasUsageStatsPermission(this)) {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun disableFloatingBall() {
        FloatingBallManager.setEnabled(this, false)
        FloatingBallManager.clearMediaProjection()
        FloatingBallManager.clearPersistedMediaProjection(this)
        stopService(Intent(this, FloatingBallService::class.java))
    }
}
