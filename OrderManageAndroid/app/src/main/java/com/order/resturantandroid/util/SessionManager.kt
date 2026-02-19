package com.order.resturantandroid.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "restaurant_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_ADMIN_ID = "admin_id"
        private const val KEY_WEBSITE_ID = "website_id"
        private const val KEY_ADMIN_EMAIL = "admin_email"
        private const val KEY_ADMIN_NAME = "admin_name"
        private const val KEY_RESTAURANT_NAME = "restaurant_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }
    
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    fun saveAdminInfo(adminId: Int, websiteId: Int, email: String, name: String?, restaurantName: String) {
        prefs.edit().apply {
            putInt(KEY_ADMIN_ID, adminId)
            putInt(KEY_WEBSITE_ID, websiteId)
            putString(KEY_ADMIN_EMAIL, email)
            putString(KEY_ADMIN_NAME, name)
            putString(KEY_RESTAURANT_NAME, restaurantName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getAdminId(): Int {
        return prefs.getInt(KEY_ADMIN_ID, -1)
    }
    
    fun getWebsiteId(): Int {
        return prefs.getInt(KEY_WEBSITE_ID, -1)
    }
    
    fun getAdminEmail(): String? {
        return prefs.getString(KEY_ADMIN_EMAIL, null)
    }
    
    fun getAdminName(): String? {
        return prefs.getString(KEY_ADMIN_NAME, null)
    }
    
    fun getRestaurantName(): String? {
        return prefs.getString(KEY_RESTAURANT_NAME, null)
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAuthToken() != null
    }
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}

