package com.bluetutor.android.core.network

import android.content.Context
import android.os.Build
import org.json.JSONObject

object BackendEndpointConfig {
    private const val configAssetName = "backend_endpoints.json"
    private const val templateAssetName = "backend_endpoints.template.json"

    private val emulatorFallbackBaseUrls = listOf(
        "http://10.0.2.2:8000",
        "http://10.1.2.120:8000",
    )
    private val deviceFallbackBaseUrls = listOf(
        "http://10.1.2.120:8000",
        "http://10.0.2.2:8000",
    )

    @Volatile
    private var configuredBaseUrls: List<String> = emptyList()

    fun initialize(context: Context) {
        configuredBaseUrls = loadConfiguredBaseUrls(context).ifEmpty { defaultBaseUrls() }
    }

    fun candidateBaseUrls(cachedBaseUrl: String?): List<String> {
        val configured = configuredBaseUrls.ifEmpty { defaultBaseUrls() }
        return buildList {
            cachedBaseUrl
                ?.trim()
                ?.removeSuffix("/")
                ?.takeIf { it.isNotEmpty() }
                ?.let(::add)

            configured.forEach { baseUrl ->
                if (!contains(baseUrl)) {
                    add(baseUrl)
                }
            }
        }
    }

    private fun loadConfiguredBaseUrls(context: Context): List<String> {
        val directConfig = readAssetBaseUrls(context, configAssetName)
        if (directConfig.isNotEmpty()) {
            return directConfig
        }
        return readAssetBaseUrls(context, templateAssetName)
    }

    private fun readAssetBaseUrls(context: Context, assetName: String): List<String> {
        return runCatching {
            context.assets.open(assetName).bufferedReader().use { reader ->
                parseBaseUrls(reader.readText())
            }
        }.getOrDefault(emptyList())
    }

    private fun parseBaseUrls(rawJson: String): List<String> {
        val root = JSONObject(rawJson)
        val baseUrls = root.optJSONArray("base_urls") ?: return emptyList()
        return buildList {
            for (index in 0 until baseUrls.length()) {
                val rawValue = baseUrls.optString(index).trim().removeSuffix("/")
                if (rawValue.isNotEmpty() && !contains(rawValue)) {
                    add(rawValue)
                }
            }
        }
    }

    private fun defaultBaseUrls(): List<String> {
        return if (isProbablyEmulator()) {
            emulatorFallbackBaseUrls
        } else {
            deviceFallbackBaseUrls
        }
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            Build.PRODUCT.contains("sdk")
    }
}