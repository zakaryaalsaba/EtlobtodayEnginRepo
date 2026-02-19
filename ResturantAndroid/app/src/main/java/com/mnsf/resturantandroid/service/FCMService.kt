package com.mnsf.resturantandroid.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Note: This service will only work if Firebase is properly configured with google-services.json
class FCMService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "FCMService"
        var deviceToken: String? = null
            set(value) {
                field = value
            }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        deviceToken = token
        // Token will be sent to backend when user logs in or registers
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            val type = remoteMessage.data["type"]
            val orderId = remoteMessage.data["order_id"]
            val orderNumber = remoteMessage.data["order_number"]
            val status = remoteMessage.data["status"]
            
            // Handle order update notification
            if (type == "order_update" && orderId != null && orderNumber != null && status != null) {
                // Show notification or update UI
                // You can use NotificationManager to show a notification
            }
        }
        
        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
            // Show notification
        }
    }
}

