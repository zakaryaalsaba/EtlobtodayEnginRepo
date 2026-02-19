package com.mnsf.resturantandroid.utils

import com.mnsf.resturantandroid.BuildConfig

object UrlHelper {
    /**
     * Converts a URL to be compatible with Android emulator or device
     * If the URL contains localhost, it replaces it with the appropriate address
     * based on the API_BASE_URL configuration
     */
    fun convertUrlForAndroid(url: String?): String {
        if (url.isNullOrBlank()) return ""
        
        // Extract base URL from BuildConfig (remove /api/ suffix if present)
        val apiBaseUrl = BuildConfig.API_BASE_URL
        val baseUrl = apiBaseUrl.removeSuffix("/api/").removeSuffix("/")
        
        // Replace localhost URLs with the configured base URL
        return url
            .replace("http://localhost:3000", baseUrl)
            .replace("http://localhost", baseUrl)
            .replace("https://localhost:3000", baseUrl)
            .replace("https://localhost", baseUrl)
    }
}

