package com.bluetutor.android.floatingball

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.content.pm.ServiceInfo
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bluetutor.android.MainActivity
import com.bluetutor.android.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class FloatingBallService : Service() {

    companion object {
        private const val TAG = "FloatingBallService"
        private const val CHANNEL_ID = "floating_ball_channel"
        private const val NOTIFICATION_ID = 1001
        private const val COLLAPSE_DELAY_MS = 4000L
        private const val SCREENSHOT_DELAY_MS = 200L
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val GALLERY_CHECK_INTERVAL_MS = 2000L

        private val GALLERY_PACKAGES = setOf(
            "com.google.android.apps.photos",
            "com.android.gallery3d",
            "com.samsung.android.gallery3d",
            "com.huawei.gallery",
            "com.miui.gallery",
            "com.xiaomi.gallery",
            "com.miui.mediaeditor",
            "com.oppo.gallery3d",
            "com.vivo.gallery",
            "com.sec.android.gallery3d",
            "com.android.documentsui",
            "com.tencent.mm",
            "com.xiaomi.mirror",
            "com.coloros.gallery3d",
            "com.hihonor.gallery",
        )
    }

    private var windowManager: WindowManager? = null
    private var ballView: View? = null
    private var dialogView: View? = null
    private var ballParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isDialogShowing = false
    private var isOnLeftSide = true
    private var isProcessingEnter = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var lastTapTime = 0L
    private var singleTapRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var hasNavigated = false

    private var lastForegroundPackage: String? = null
    private var galleryDialogShownForSession = false
    private var galleryCheckRunnable: Runnable? = null

    private val density: Float
        get() = resources.displayMetrics.density

    private val ballSizePx: Int
        get() = (52 * density).toInt()

    private val peekPx: Int
        get() = (16 * density).toInt()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        FloatingBallManager.setServiceRunning(true)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        addFloatingBall()
        startGalleryDetection()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        FloatingBallManager.setServiceRunning(false)
        stopGalleryDetection()
        removeViews()
        mainHandler.removeCallbacksAndMessages(null)
        releaseMediaProjection()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.floating_ball_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            description = getString(R.string.floating_ball_channel_desc)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_ball_notification_title))
            .setContentText(getString(R.string.floating_ball_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addFloatingBall() {
        val params = buildBallParams()
        ballParams = params

        val container = FrameLayout(this)

        val ballImageView = ImageView(this).apply {
            setImageResource(R.drawable.cute_owl_launcher)
            scaleType = ImageView.ScaleType.CENTER_CROP
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }

        container.addView(ballImageView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        container.layoutParams = FrameLayout.LayoutParams(ballSizePx, ballSizePx)
        updateBallAppearance(container, false)

        container.setOnTouchListener { _, event ->
            handleBallTouch(event, params)
        }

        ballView = container
        windowManager?.addView(container, params)
    }

    private fun updateBallAppearance(container: View, expanded: Boolean) {
        container.alpha = if (expanded) 1.0f else 0.35f
        val dp = density
        container.elevation = if (expanded) 10f * dp else 4f * dp
    }

    private fun buildBallParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            ballSizePx,
            ballSizePx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = -(ballSizePx - peekPx)
            y = resources.displayMetrics.heightPixels / 3
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleBallTouch(event: MotionEvent, params: WindowManager.LayoutParams): Boolean {
        val wm = windowManager ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!isDragging && (abs(dx) > ViewConfiguration.get(this).scaledTouchSlop
                            || abs(dy) > ViewConfiguration.get(this).scaledTouchSlop)
                ) {
                    isDragging = true
                    singleTapRunnable?.let { mainHandler.removeCallbacks(it) }
                    singleTapRunnable = null
                }
                if (isDragging) {
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    wm.updateViewLayout(ballView, params)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    snapToEdge(params)
                } else {
                    handleTap(params)
                }
                return true
            }
        }
        return false
    }

    private fun handleTap(params: WindowManager.LayoutParams) {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
            singleTapRunnable?.let { mainHandler.removeCallbacks(it) }
            singleTapRunnable = null
            lastTapTime = 0L
            onDoubleTap()
        } else {
            lastTapTime = now
            singleTapRunnable = Runnable {
                onSingleTap(params)
                singleTapRunnable = null
            }
            mainHandler.postDelayed(singleTapRunnable!!, DOUBLE_TAP_TIMEOUT)
        }
    }

    private fun onSingleTap(params: WindowManager.LayoutParams) {
        if (isDialogShowing) {
            return
        }
        if (isExpanded) {
            collapseBall(params)
        } else {
            expandBall(params)
        }
    }

    private fun expandBall(params: WindowManager.LayoutParams) {
        isExpanded = true
        mainHandler.removeCallbacks(collapseRunnable)

        val container = ballView ?: return
        updateBallAppearance(container, true)

        val targetX = if (isOnLeftSide) 0 else resources.displayMetrics.widthPixels - ballSizePx
        animateBallPosition(params, targetX, expanded = true)
    }

    private fun collapseBall(params: WindowManager.LayoutParams? = null) {
        isExpanded = false
        if (isDialogShowing) hideDialog()

        val p = params ?: ballParams ?: return
        val container = ballView ?: return
        updateBallAppearance(container, false)

        val targetX = if (isOnLeftSide) -(ballSizePx - peekPx) else resources.displayMetrics.widthPixels - peekPx
        animateBallPosition(p, targetX, expanded = false)
    }

    private val collapseRunnable = Runnable {
        val p = ballParams ?: return@Runnable
        collapseBall(p)
    }

    private fun animateBallPosition(
        params: WindowManager.LayoutParams,
        targetX: Int,
        expanded: Boolean = false,
    ) {
        val wm = windowManager ?: return
        val startX = params.x
        val animator = android.animation.ValueAnimator.ofInt(startX, targetX)
        animator.duration = 250
        animator.interpolator = OvershootInterpolator(0.8f)
        animator.addUpdateListener { anim ->
            params.x = anim.animatedValue as Int
            try {
                wm.updateViewLayout(ballView, params)
            } catch (_: Exception) {
            }
        }
        animator.start()

        if (expanded) {
            mainHandler.postDelayed(collapseRunnable, COLLAPSE_DELAY_MS)
        }
    }

    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val screenWidth = resources.displayMetrics.widthPixels
        val midX = screenWidth / 2
        isOnLeftSide = (params.x + ballSizePx / 2) < midX

        if (isExpanded) {
            val targetX = if (isOnLeftSide) 0 else screenWidth - ballSizePx
            animateBallPosition(params, targetX, expanded = true)
        } else {
            val targetX = if (isOnLeftSide) -(ballSizePx - peekPx) else screenWidth - peekPx
            animateBallPosition(params, targetX)
        }
    }

    private fun onDoubleTap() {
        if (isDialogShowing) {
            hideDialog()
        } else {
            if (!isExpanded) {
                val p = ballParams ?: return
                expandBall(p)
            }
            mainHandler.postDelayed({ showDialog(autoTriggered = false) }, 200)
        }
    }

    private fun startGalleryDetection() {
        if (!FloatingBallManager.isGalleryAutoDetectEnabled(this)) return
        if (!FloatingBallManager.hasUsageStatsPermission(this)) {
            Log.w(TAG, "startGalleryDetection: USAGE_STATS permission not granted, gallery detection will not work")
        }
        galleryCheckRunnable = object : Runnable {
            override fun run() {
                if (FloatingBallManager.isGalleryAutoDetectEnabled(this@FloatingBallService)
                    && FloatingBallManager.hasUsageStatsPermission(this@FloatingBallService)
                ) {
                    checkForegroundApp()
                }
                galleryCheckRunnable?.let {
                    mainHandler.postDelayed(it, GALLERY_CHECK_INTERVAL_MS)
                }
            }
        }
        galleryCheckRunnable?.let { mainHandler.postDelayed(it, GALLERY_CHECK_INTERVAL_MS) }
    }

    private fun stopGalleryDetection() {
        galleryCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        galleryCheckRunnable = null
    }

    private fun checkForegroundApp() {
        val currentPkg = getForegroundAppPackage()
        if (currentPkg == null) {
            Log.d(TAG, "checkForegroundApp: getForegroundAppPackage returned null")
            return
        }
        val isGallery = GALLERY_PACKAGES.contains(currentPkg)
        val isOwnApp = currentPkg == packageName
        Log.d(TAG, "checkForegroundApp: currentPkg=$currentPkg, isGallery=$isGallery, isOwnApp=$isOwnApp, isDialogShowing=$isDialogShowing, galleryDialogShownForSession=$galleryDialogShownForSession")

        if (isGallery && !isDialogShowing && !galleryDialogShownForSession) {
            galleryDialogShownForSession = true
            if (!isExpanded) {
                val p = ballParams ?: return
                expandBall(p)
            }
            mainHandler.postDelayed({ showDialog(autoTriggered = true) }, 2000)
        } else if (!isGallery && !isOwnApp) {
            galleryDialogShownForSession = false
        }
    }

    private fun getForegroundAppPackage(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5000
            val events = usm.queryEvents(startTime, endTime)
            var foregroundApp: String? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundApp = event.packageName
                }
            }
            foregroundApp
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showDialog(autoTriggered: Boolean) {
        if (dialogView?.parent != null) return
        isDialogShowing = true
        mainHandler.removeCallbacks(collapseRunnable)

        val bp = ballParams ?: return

        val dialogWidth = (260 * density).toInt()
        val vPad = (14 * density).toInt()
        val hPad = (18 * density).toInt()

        val card = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F6FBFF"))
            val cornerRadius = (20 * density).toFloat()
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
            clipToOutline = true
            elevation = 16f * density
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
        }

        val label = TextView(this).apply {
            text = if (autoTriggered) "🦉 BlueTutor" else "🦉 BlueTutor"
            setTextColor(Color.parseColor("#0B4F70"))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val bottomMarginVal = (8 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = bottomMarginVal }
        }

        val editText = EditText(this).apply {
            hint = if (autoTriggered) "需要我的帮助吗" else getString(R.string.floating_ball_dialog_hint)
            setTextColor(Color.parseColor("#1A3550"))
            textSize = 15f
            setHintTextColor(Color.parseColor("#90A4AE"))
            background = null
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                    || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    onDialogEnter(autoTriggered)
                    true
                } else {
                    false
                }
            }
        }

        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#D8EAF4"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt(),
            )
        }

        contentLayout.addView(label)
        contentLayout.addView(divider)
        contentLayout.addView(editText)
        card.addView(contentLayout)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val dialogParams = WindowManager.LayoutParams(
            dialogWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            val gap = (12 * density).toInt()
            if (isOnLeftSide) {
                x = bp.x + ballSizePx + gap
            } else {
                x = bp.x - dialogWidth - gap
            }
            y = bp.y
            windowAnimations = android.R.style.Animation_Dialog
        }

        dialogView = card
        try {
            windowManager?.addView(card, dialogParams)
        } catch (e: Exception) {
            Log.e(TAG, "showDialog: failed to add dialog view, possibly MIUI background overlay restriction", e)
            isDialogShowing = false
            dialogView = null
            if (isMiui()) {
                openMiuiAppDetailSettings()
            }
            return
        }

        card.alpha = 0f
        card.scaleX = 0.85f
        card.scaleY = 0.85f
        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator(0.7f))
            .start()

        editText.postDelayed({
            editText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 300)
    }

    private fun onDialogEnter(autoTriggered: Boolean) {
        if (!isDialogShowing) return
        if (isProcessingEnter) {
            Log.w(TAG, "onDialogEnter: already processing, ignoring duplicate call")
            return
        }
        isProcessingEnter = true

        val editText = (dialogView as? FrameLayout)
            ?.getChildAt(0)?.let { it as? LinearLayout }
            ?.getChildAt(2) as? EditText

        if (editText != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        }

        // TODO: 恢复截图/相册功能时取消注释以下代码
        // val hasPermission = FloatingBallManager.hasMediaProjectionPermission()
        // if (hasPermission) {
        //     FloatingBallManager.setSkipImageEditor(autoTriggered)
        // } else {
        //     FloatingBallManager.setNeedsAlbumImport(true)
        // }
        hideDialog()
        navigateToApp()
        isProcessingEnter = false
        // if (hasPermission) {
        //     mainHandler.postDelayed({
        //         takeScreenshot()
        //         isProcessingEnter = false
        //     }, 500)
        // } else {
        //     navigateToApp()
        //     isProcessingEnter = false
        // }
    }

    private fun hideDialog() {
        val dialog = dialogView ?: return
        isDialogShowing = false
        dialog.animate()
            .alpha(0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(150)
            .withEndAction {
                removeDialogView()
            }
            .start()
    }

    private fun removeDialogView() {
        dialogView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
        }
        dialogView = null
    }

    private fun takeScreenshot() {
        if (!FloatingBallManager.hasMediaProjectionPermission()) {
            Log.w(TAG, "takeScreenshot: no MediaProjection permission, navigating directly")
            navigateToApp()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "takeScreenshot: failed to update foreground service type", e)
        }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val resultCode = FloatingBallManager.mediaProjectionResultCode
            val resultData = FloatingBallManager.mediaProjectionResultData
            if (resultCode != Activity.RESULT_OK || resultData == null) {
                Log.e(TAG, "takeScreenshot: MediaProjection data is null after permission check")
                FloatingBallManager.mediaProjectionResultCode = Activity.RESULT_CANCELED
                FloatingBallManager.mediaProjectionResultData = null
                navigateToApp()
                return
            }
            mediaProjection = manager.getMediaProjection(resultCode, resultData)
        } catch (e: Exception) {
            Log.e(TAG, "takeScreenshot: failed to get MediaProjection", e)
            FloatingBallManager.mediaProjectionResultCode = Activity.RESULT_CANCELED
            FloatingBallManager.mediaProjectionResultData = null
            navigateToApp()
            return
        }

        mainHandler.postDelayed({
            captureScreen()
        }, SCREENSHOT_DELAY_MS)
    }

    private var captureCompleted = false

    private fun captureScreen() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi

        captureCompleted = false

        try {
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        } catch (e: Exception) {
            Log.e(TAG, "captureScreen: failed to create ImageReader", e)
            releaseMediaProjection()
            navigateToApp()
            return
        }

        try {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "FloatingBallScreenshot",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null, mainHandler,
            )
        } catch (e: Exception) {
            Log.e(TAG, "captureScreen: failed to create VirtualDisplay", e)
            releaseMediaProjection()
            navigateToApp()
            return
        }

        imageReader?.setOnImageAvailableListener({ reader ->
            if (captureCompleted) return@setOnImageAvailableListener
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    captureCompleted = true
                    val bitmap = imageToBitmap(image, width, height)
                    image.close()
                    image = null
                    if (bitmap != null) {
                        val uri = saveBitmapToFile(bitmap)
                        bitmap.recycle()
                        if (uri != null) {
                            Log.d(TAG, "captureScreen: screenshot saved to $uri")
                            FloatingBallManager.setPendingScreenshot(uri)
                        } else {
                            Log.e(TAG, "captureScreen: failed to save bitmap to file")
                        }
                    } else {
                        Log.e(TAG, "captureScreen: failed to convert image to bitmap")
                    }
                    releaseMediaProjection()
                    mainHandler.postDelayed({
                        navigateToApp()
                    }, 300)
                }
            } catch (e: Exception) {
                Log.e(TAG, "captureScreen: error processing captured image", e)
                image?.close()
                if (!captureCompleted) {
                    captureCompleted = true
                    releaseMediaProjection()
                    navigateToApp()
                }
            }
        }, mainHandler)

        mainHandler.postDelayed({
            if (!captureCompleted) {
                Log.w(TAG, "captureScreen: timed out after 3s, releasing")
                captureCompleted = true
                releaseMediaProjection()
                navigateToApp()
            }
        }, 3000)
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return if (rowPadding == 0) {
            bitmap
        } else {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()
            cropped
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri? {
        return try {
            val cacheDir = File(cacheDir, "floating-ball-screenshots")
            cacheDir.mkdirs()
            val file = File.createTempFile("screenshot_", ".png", cacheDir)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file,
            )
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmapToFile: failed to save screenshot", e)
            null
        }
    }

    private fun releaseMediaProjection() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { mediaProjection?.stop() } catch (_: Exception) {}
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } catch (_: Exception) {}
        }
    }

    private fun navigateToApp() {
        if (hasNavigated) return
        hasNavigated = true
        Log.d(TAG, "navigateToApp: navigating back to app with screenshot pending")
        FloatingBallManager.setShouldNavigateToSolve(true)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "navigateToApp: failed to start activity", e)
        }
        mainHandler.postDelayed({ hasNavigated = false }, 3000)
    }

    private fun removeViews() {
        removeDialogView()
        ballView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        ballView = null
    }

    private fun isMiui(): Boolean {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            val prop = method.invoke(null, "ro.miui.ui.version.name") as? String
            prop != null && prop.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    private fun openMiuiAppDetailSettings() {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity",
                )
                putExtra("extra_pkgname", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
