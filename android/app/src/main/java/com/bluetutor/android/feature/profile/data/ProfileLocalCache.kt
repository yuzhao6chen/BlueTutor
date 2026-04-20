package com.bluetutor.android.feature.profile.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

data class ProfileLocalSnapshot(
    val userName: String = "小明同学",
    val gradeLabel: String = "六年级",
    val avatarPath: String? = null,
    val checkedInDates: Set<String> = defaultCheckedInDates(),
    val reminderEnabled: Boolean = true,
    val motionEffectsEnabled: Boolean = true,
)

object ProfileLocalCache {
    private const val preferencesName = "profile_local_cache_v1"
    private const val profileKey = "profile_state"

    fun readProfile(context: Context): ProfileLocalSnapshot? {
        val rawValue = context
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(profileKey, null)
            ?: return null

        return runCatching {
            val root = JSONObject(rawValue)
            val checkedInDatesJson = root.optJSONArray("checkedInDates") ?: JSONArray()
            val avatarPath = root.optString("avatarPath").trim().ifEmpty { null }

            ProfileLocalSnapshot(
                userName = root.optString("userName").ifBlank { "小明同学" },
                gradeLabel = root.optString("gradeLabel").ifBlank { "六年级" },
                avatarPath = avatarPath?.takeIf { File(it).exists() },
                checkedInDates = buildSet {
                    for (index in 0 until checkedInDatesJson.length()) {
                        val value = checkedInDatesJson.optString(index).trim()
                        if (value.isNotEmpty()) {
                            add(value)
                        }
                    }
                }.ifEmpty { defaultCheckedInDates() },
                reminderEnabled = root.optBoolean("reminderEnabled", true),
                motionEffectsEnabled = root.optBoolean("motionEffectsEnabled", true),
            )
        }.getOrNull()
    }

    fun saveProfile(context: Context, snapshot: ProfileLocalSnapshot) {
        val root = JSONObject()
            .put("userName", snapshot.userName)
            .put("gradeLabel", snapshot.gradeLabel)
            .put("avatarPath", snapshot.avatarPath)
            .put("reminderEnabled", snapshot.reminderEnabled)
            .put("motionEffectsEnabled", snapshot.motionEffectsEnabled)
            .put(
                "checkedInDates",
                JSONArray().apply {
                    snapshot.checkedInDates.sorted().forEach { put(it) }
                },
            )

        context
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(profileKey, root.toString())
            .apply()
    }

    fun importAvatar(context: Context, sourceUri: Uri): String? {
        val avatarDir = File(context.filesDir, "profile").apply { mkdirs() }
        avatarDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }

        val extension = resolveExtension(context, sourceUri)
        val targetFile = File(avatarDir, "avatar.$extension")

        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    fun deleteAvatar(avatarPath: String?) {
        if (avatarPath.isNullOrBlank()) {
            return
        }
        runCatching {
            File(avatarPath).takeIf { it.exists() }?.delete()
        }
    }

    fun resetProfile(context: Context, snapshot: ProfileLocalSnapshot?) {
        deleteAvatar(snapshot?.avatarPath)
        context
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .remove(profileKey)
            .apply()
    }

    private fun resolveExtension(context: Context, sourceUri: Uri): String {
        val mimeType = context.contentResolver.getType(sourceUri)
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"
    }
}

private fun defaultCheckedInDates(): Set<String> {
    val today = LocalDate.now()
    return setOf(
        today.minusDays(2).toString(),
        today.minusDays(1).toString(),
    )
}