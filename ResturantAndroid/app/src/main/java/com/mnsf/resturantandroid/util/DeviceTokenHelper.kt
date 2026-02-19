package com.mnsf.resturantandroid.util

import android.util.Log
import com.mnsf.resturantandroid.network.ApiService
import com.mnsf.resturantandroid.network.DeviceTokenRequest
import com.mnsf.resturantandroid.network.LocationRequest
import com.mnsf.resturantandroid.service.FCMService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object DeviceTokenHelper {
    private const val TAG = "DeviceTokenHelper"
    
    /**
     * Get FCM token and send to backend
     * Note: This will only work if Firebase is properly configured
     */
    fun getAndSendDeviceToken(
        apiService: ApiService,
        customerId: Int,
        token: String,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            try {
                // Get FCM token
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase not configured or error getting token. Push notifications will be disabled.", e)
                    return@launch
                }
                
                Log.d(TAG, "FCM Token obtained: ${fcmToken.take(20)}...")
                FCMService.deviceToken = fcmToken
                
                val request = DeviceTokenRequest(
                    device_token = fcmToken,
                    device_type = "android"
                )
                
                val response = apiService.updateDeviceToken(
                    customerId = customerId,
                    request = request,
                    token = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Device token sent to backend successfully")
                } else {
                    Log.e(TAG, "Failed to send device token: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting/sending FCM token", e)
            }
        }
    }
    
    /**
     * Send location to backend
     */
    fun sendLocation(
        apiService: ApiService,
        customerId: Int,
        token: String,
        latitude: Double,
        longitude: Double,
        address: String? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            try {
                val request = LocationRequest(
                    latitude = latitude,
                    longitude = longitude,
                    address = address
                )
                
                val response = apiService.updateLocation(
                    customerId = customerId,
                    request = request,
                    token = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Location sent to backend successfully")
                } else {
                    Log.e(TAG, "Failed to send location: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending location", e)
            }
        }
    }
}

