package com.driver.resturantandroid.util

import android.content.Context
import android.util.Log
import com.driver.resturantandroid.network.RetrofitClient
import com.driver.resturantandroid.repository.DriverRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FCMTokenManager {
    private const val TAG = "FCMTokenManager"
    
    /**
     * Get FCM token and register it with the backend
     */
    fun registerToken(context: Context, token: String?) {
        val sessionManager = SessionManager(context)
        val authToken = sessionManager.getAuthToken()
        
        if (authToken == null) {
            Log.d(TAG, "No auth token, cannot register FCM token")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get FCM token if not provided
                val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()
                
                Log.d(TAG, "========== REGISTERING FCM TOKEN ==========")
                Log.d(TAG, "FCM Token: $fcmToken")
                Log.d(TAG, "Token Length: ${fcmToken.length}")
                Log.d(TAG, "Token (first 50 chars): ${fcmToken.take(50)}...")
                
                // Send to backend
                val request = mapOf(
                    "device_token" to fcmToken,
                    "device_type" to "android"
                )
                
                Log.d(TAG, "Sending token to backend...")
                val response = RetrofitClient.apiService.updateDeviceToken(
                    request,
                    "Bearer $authToken"
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ FCM token registered successfully with backend")
                    Log.d(TAG, "Response: ${response.body()}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Failed to register FCM token: ${response.code()} - ${response.message()}")
                    Log.e(TAG, "Error body: $errorBody")
                }
                Log.d(TAG, "========== END REGISTERING FCM TOKEN ==========")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error registering FCM token", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get FCM token (for initial registration)
     */
    fun getToken(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "========== GETTING FCM TOKEN ==========")
                Log.d(TAG, "Attempting to get FCM token...")
                
                // Check if Google Play Services is available
                try {
                    val googlePlayServicesAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(context)
                    
                    if (googlePlayServicesAvailable != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                        Log.e(TAG, "❌ Google Play Services not available. Error code: $googlePlayServicesAvailable")
                        Log.e(TAG, "This usually means:")
                        Log.e(TAG, "  1. Running on emulator without Google Play Services")
                        Log.e(TAG, "  2. Google Play Services needs to be updated")
                        Log.e(TAG, "  3. Device doesn't support Google Play Services")
                        return@launch
                    } else {
                        Log.d(TAG, "✅ Google Play Services is available")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not check Google Play Services availability", e)
                }
                
                val token = FirebaseMessaging.getInstance().token.await()
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "❌ FCM token is null or blank")
                } else {
                    Log.d(TAG, "✅ FCM Token retrieved successfully")
                    Log.d(TAG, "Token: $token")
                    Log.d(TAG, "Token Length: ${token.length}")
                    Log.d(TAG, "Token (first 50 chars): ${token.take(50)}...")
                    registerToken(context, token)
                }
                Log.d(TAG, "========== END GETTING FCM TOKEN ==========")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting FCM token", e)
                e.printStackTrace()
                
                // Provide helpful error messages
                when {
                    e.message?.contains("SERVICE_NOT_AVAILABLE") == true -> {
                        Log.e(TAG, "❌ Firebase Messaging Service not available")
                        Log.e(TAG, "Possible causes:")
                        Log.e(TAG, "  1. Running on emulator without Google Play Services")
                        Log.e(TAG, "  2. Google Play Services needs to be updated")
                        Log.e(TAG, "  3. Network connectivity issues")
                        Log.e(TAG, "  4. Firebase project configuration issues")
                        Log.e(TAG, "NOTE: If you have a token in the database, notifications might still work!")
                    }
                    e.message?.contains("FirebaseApp") == true || 
                    e.message?.contains("google-services.json") == true -> {
                        Log.w(TAG, "Firebase not configured. Please add google-services.json to app/ directory")
                    }
                    else -> {
                        Log.e(TAG, "Unknown error: ${e.message}")
                    }
                }
            }
        }
    }
}

