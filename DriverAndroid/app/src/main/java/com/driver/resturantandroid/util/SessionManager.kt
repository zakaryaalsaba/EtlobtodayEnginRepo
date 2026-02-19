package com.driver.resturantandroid.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "DriverSession"
        private const val KEY_DRIVER_ID = "driver_id"
        private const val KEY_DRIVER_NAME = "driver_name"
        private const val KEY_DRIVER_EMAIL = "driver_email"
        private const val KEY_DRIVER_PHONE = "driver_phone"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_IS_ONLINE = "is_online"
    }
    
    fun saveDriverSession(driverId: Int, name: String, email: String, token: String) {
        prefs.edit().apply {
            putInt(KEY_DRIVER_ID, driverId)
            putString(KEY_DRIVER_NAME, name)
            putString(KEY_DRIVER_EMAIL, email)
            putString(KEY_AUTH_TOKEN, token)
            apply()
        }
    }
    
    fun getDriverId(): Int = prefs.getInt(KEY_DRIVER_ID, -1)
    fun getDriverName(): String? = prefs.getString(KEY_DRIVER_NAME, null)
    fun getDriverEmail(): String? = prefs.getString(KEY_DRIVER_EMAIL, null)
    fun getDriverPhone(): String? = prefs.getString(KEY_DRIVER_PHONE, null)
    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
    fun isOnline(): Boolean = prefs.getBoolean(KEY_IS_ONLINE, false)
    
    fun setOnlineStatus(isOnline: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ONLINE, isOnline).apply()
    }
    
    fun isLoggedIn(): Boolean = getAuthToken() != null && getDriverId() != -1
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
    
    fun updateDriverName(name: String) {
        prefs.edit().putString(KEY_DRIVER_NAME, name).apply()
    }
    
    fun updateDriverPhone(phone: String?) {
        if (phone != null) {
            prefs.edit().putString(KEY_DRIVER_PHONE, phone).apply()
        } else {
            prefs.edit().remove(KEY_DRIVER_PHONE).apply()
        }
    }
    
    fun saveDriverSessionWithPhone(driverId: Int, name: String, email: String, phone: String?, token: String) {
        prefs.edit().apply {
            putInt(KEY_DRIVER_ID, driverId)
            putString(KEY_DRIVER_NAME, name)
            putString(KEY_DRIVER_EMAIL, email)
            putString(KEY_AUTH_TOKEN, token)
            if (phone != null) {
                putString(KEY_DRIVER_PHONE, phone)
            }
            apply()
        }
    }
}

