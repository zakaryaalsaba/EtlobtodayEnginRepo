package com.driver.resturantandroid.util

import android.os.Build
import android.util.Log
import com.driver.resturantandroid.BuildConfig

/**
 * Helps choose the correct API base URL for emulator vs physical device.
 * Same approach as ResturantAndroid: use local.properties (via BuildConfig) or auto-detect.
 */
object DeviceHelper {
    /** Use this tag in Logcat to filter API base URL: adb logcat -s DriverApi */
    private const val TAG = "DriverApi"

    private const val DEFAULT_EMULATOR_IP = "10.0.2.2"
    private const val DEFAULT_PHYSICAL_DEVICE_IP = "192.168.1.107"
    private const val DEFAULT_PORT = "3000"
    private const val DEFAULT_API_PATH = "/api/"

    fun isEmulator(): Boolean {
        return try {
            val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                    || "google_sdk" == Build.PRODUCT
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.HARDWARE.contains("vbox86")
                    || Build.HARDWARE.contains("generic")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("google_sdk")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator"))
            Log.d(TAG, "Device detection - isEmulator: $isEmulator, MODEL: ${Build.MODEL}")
            isEmulator
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting emulator", e)
            false
        }
    }

    /**
     * Returns the API base URL to use.
     * 1. If BuildConfig.API_BASE_URL is set in local.properties (not 10.0.2.2), use it for both.
     * 2. Otherwise: emulator → 10.0.2.2, physical device → 192.168.1.107 (update if your LAN IP differs).
     */
    fun getApiBaseUrl(): String {
        val buildConfigUrl = BuildConfig.API_BASE_URL.trim().trimStart('/')
        val isCustomUrl = buildConfigUrl.isNotEmpty() &&
                !buildConfigUrl.contains("10.0.2.2") &&
                (buildConfigUrl.startsWith("http://") || buildConfigUrl.startsWith("https://"))

        val baseUrl = if (isCustomUrl) {
            Log.d(TAG, "Using BuildConfig.API_BASE_URL from local.properties: $buildConfigUrl")
            buildConfigUrl
        } else {
            val ip = if (isEmulator()) DEFAULT_EMULATOR_IP else DEFAULT_PHYSICAL_DEVICE_IP
            val url = "http://$ip:$DEFAULT_PORT$DEFAULT_API_PATH"
            Log.d(TAG, "Auto-detected API base URL: $url (${if (isEmulator()) "emulator" else "physical device"})")
            url
        }
        val normalized = baseUrl.let { u -> if (!u.endsWith("/")) "$u/" else u }
        Log.d(TAG, "Final API base URL: $normalized")
        return normalized
    }
}
