package com.bluetutor.android.floatingball

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

object FloatingBallManager {

    private const val PREFS_NAME = "floating_ball_prefs"
    private const val KEY_ENABLED = "floating_ball_enabled"
    private const val KEY_MP_RESULT_CODE = "mp_result_code"
    private const val KEY_MP_RESULT_DATA = "mp_result_data"
    private const val KEY_GALLERY_AUTO_DETECT = "gallery_auto_detect"

    @Volatile
    private var _isServiceRunning = false
    val isServiceRunning: Boolean get() = _isServiceRunning

    @Volatile
    private var _pendingScreenshotUri: Uri? = null
    val pendingScreenshotUri: Uri? get() = _pendingScreenshotUri

    @Volatile
    private var _shouldNavigateToSolve = false
    val shouldNavigateToSolve: Boolean get() = _shouldNavigateToSolve

    @Volatile
    private var _skipImageEditor = false
    val skipImageEditor: Boolean get() = _skipImageEditor

    @Volatile
    private var _needsMediaProjection = false
    val needsMediaProjection: Boolean get() = _needsMediaProjection

    @Volatile
    private var _needsAlbumImport = false
    val needsAlbumImport: Boolean get() = _needsAlbumImport

    @Volatile
    private var _mediaProjectionResultCode: Int = Activity.RESULT_CANCELED
    var mediaProjectionResultCode: Int
        get() = _mediaProjectionResultCode
        set(value) {
            _mediaProjectionResultCode = value
        }

    @Volatile
    private var _mediaProjectionResultData: Intent? = null
    var mediaProjectionResultData: Intent?
        get() = _mediaProjectionResultData
        set(value) {
            _mediaProjectionResultData = value
        }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun isGalleryAutoDetectEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_GALLERY_AUTO_DETECT, true)
    }

    fun setGalleryAutoDetectEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GALLERY_AUTO_DETECT, enabled)
            .apply()
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning = running
    }

    fun setPendingScreenshot(uri: Uri?) {
        _pendingScreenshotUri = uri
    }

    fun consumePendingScreenshot(): Uri? {
        val uri = _pendingScreenshotUri
        _pendingScreenshotUri = null
        return uri
    }

    fun setShouldNavigateToSolve(navigate: Boolean) {
        _shouldNavigateToSolve = navigate
    }

    fun consumeShouldNavigateToSolve(): Boolean {
        val navigate = _shouldNavigateToSolve
        _shouldNavigateToSolve = false
        return navigate
    }

    fun setSkipImageEditor(skip: Boolean) {
        _skipImageEditor = skip
    }

    fun consumeSkipImageEditor(): Boolean {
        val skip = _skipImageEditor
        _skipImageEditor = false
        return skip
    }

    fun setNeedsMediaProjection(needs: Boolean) {
        _needsMediaProjection = needs
    }

    fun consumeNeedsMediaProjection(): Boolean {
        val needs = _needsMediaProjection
        _needsMediaProjection = false
        return needs
    }

    fun setNeedsAlbumImport(needs: Boolean) {
        _needsAlbumImport = needs
    }

    fun consumeNeedsAlbumImport(): Boolean {
        val needs = _needsAlbumImport
        _needsAlbumImport = false
        return needs
    }

    fun hasMediaProjectionPermission(): Boolean {
        return _mediaProjectionResultCode == Activity.RESULT_OK && _mediaProjectionResultData != null
    }

    fun clearMediaProjection() {
        _mediaProjectionResultCode = Activity.RESULT_CANCELED
        _mediaProjectionResultData = null
        clearPersistedMediaProjection()
    }

    fun saveMediaProjection(context: Context) {
        if (_mediaProjectionResultCode != Activity.RESULT_OK || _mediaProjectionResultData == null) return
        try {
            val intentUri = _mediaProjectionResultData!!.toUri(Intent.URI_INTENT_SCHEME)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_MP_RESULT_CODE, _mediaProjectionResultCode)
                .putString(KEY_MP_RESULT_DATA, intentUri)
                .apply()
        } catch (_: Exception) {}
    }

    fun restoreMediaProjection(context: Context) {
        if (_mediaProjectionResultCode == Activity.RESULT_OK && _mediaProjectionResultData != null) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val resultCode = prefs.getInt(KEY_MP_RESULT_CODE, Activity.RESULT_CANCELED)
            val intentUri = prefs.getString(KEY_MP_RESULT_DATA, null)
            if (resultCode == Activity.RESULT_OK && intentUri != null) {
                val data = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
                _mediaProjectionResultCode = resultCode
                _mediaProjectionResultData = data
            }
        } catch (_: Exception) {}
    }

    private fun clearPersistedMediaProjection() {
        // no-op: we need a context to clear, so we do it lazily or from a context-aware call
    }

    fun clearPersistedMediaProjection(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MP_RESULT_CODE)
            .remove(KEY_MP_RESULT_DATA)
            .apply()
    }
}
