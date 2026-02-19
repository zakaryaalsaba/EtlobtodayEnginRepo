package com.mnsf.resturantandroid.util

import android.os.Build
import android.util.Log
import com.mnsf.resturantandroid.BuildConfig

object DeviceHelper {
    private const val TAG = "DeviceHelper"
    
    // Default IPs - can be overridden via BuildConfig
    private const val DEFAULT_EMULATOR_IP = "10.0.2.2"
    private const val DEFAULT_PHYSICAL_DEVICE_IP = "192.168.1.107"
    private const val DEFAULT_PORT = "3000"
    private const val DEFAULT_API_PATH = "/api/"
    
    /**
     * Detects if the app is running on an Android emulator.
     * Uses multiple heuristics for reliable detection.
     */
    fun isEmulator(): Boolean {
        return try {
            // Check build properties that are common in emulators
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
            
            Log.d(TAG, "Device detection - isEmulator: $isEmulator, MODEL: ${Build.MODEL}, MANUFACTURER: ${Build.MANUFACTURER}, PRODUCT: ${Build.PRODUCT}")
            isEmulator
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting emulator", e)
            false // Default to physical device if detection fails
        }
    }
    
    /**
     * Gets the appropriate API base URL based on device type.
     * 
     * Priority:
     * 1. If BuildConfig.API_BASE_URL is set in local.properties (not default), use it for both devices
     * 2. Otherwise, auto-detect:
     *    - Emulator: 10.0.2.2 (maps to host machine's localhost)
     *    - Physical device: 192.168.1.107 (your computer's LAN IP - update if your IP changes)
     */
    fun getApiBaseUrl(): String {
        val buildConfigUrl = BuildConfig.API_BASE_URL.trim()
        
        // Check if BuildConfig has a custom URL (not the default emulator one)
        // If local.properties has API_BASE_URL set, it will override auto-detection
        val isCustomUrl = buildConfigUrl.isNotEmpty() && 
                         !buildConfigUrl.contains("10.0.2.2") &&
                         buildConfigUrl.startsWith("http")
        
        val baseUrl = if (isCustomUrl) {
            // Use BuildConfig URL if explicitly set in local.properties
            Log.d(TAG, "Using BuildConfig.API_BASE_URL from local.properties: $buildConfigUrl")
            buildConfigUrl
        } else {
            // Auto-detect based on device type
            val ip = if (isEmulator()) {
                DEFAULT_EMULATOR_IP
            } else {
                DEFAULT_PHYSICAL_DEVICE_IP
            }
            val url = "http://$ip:$DEFAULT_PORT$DEFAULT_API_PATH"
            Log.d(TAG, "Auto-detected API base URL: $url")
            url
        }
        
        Log.d(TAG, "Final API base URL: $baseUrl (device: ${if (isEmulator()) "emulator" else "physical device"})")
        return baseUrl
    }
}
