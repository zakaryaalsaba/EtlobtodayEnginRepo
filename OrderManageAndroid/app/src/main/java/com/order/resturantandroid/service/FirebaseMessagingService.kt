package com.order.resturantandroid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.order.resturantandroid.R
import com.order.resturantandroid.data.remote.DeviceTokenRequest
import com.order.resturantandroid.data.remote.RetrofitClient
import com.order.resturantandroid.ui.orders.OrderDetailActivity
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        sendTokenToServer(token)
    }
    
    private fun sendTokenToServer(token: String) {
        val sessionManager = SessionManager(applicationContext)
        val authToken = sessionManager.getAuthToken()
        
        if (authToken == null) {
            Log.d(TAG, "Not logged in, token will be sent after login")
            // Token will be sent when user logs in
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = RetrofitClient.apiService
                val response = apiService.updateDeviceToken(
                    DeviceTokenRequest(token, "android"),
                    "Bearer $authToken"
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Device token registered successfully")
                } else {
                    Log.e(TAG, "Failed to register device token: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering device token", e)
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data: ${remoteMessage.data}")
        
        // Handle notification
        remoteMessage.notification?.let {
            sendNotification(
                it.title ?: "New Order",
                it.body ?: "You have a new order",
                remoteMessage.data
            )
        }
        
        // Handle data-only messages
        if (remoteMessage.data.isNotEmpty() && remoteMessage.notification == null) {
            val title = remoteMessage.data["title"] ?: "New Order"
            val body = remoteMessage.data["body"] ?: "You have a new order"
            sendNotification(title, body, remoteMessage.data)
        }
    }
    
    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        val orderId = data["order_id"]?.toIntOrNull()
        
        val intent = if (orderId != null) {
            Intent(this, OrderDetailActivity::class.java).apply {
                putExtra("order_id", orderId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            Intent(this, com.order.resturantandroid.ui.dashboard.DashboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Order Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new orders"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
    
    companion object {
        private const val TAG = "OrderFCMService"
    }
}

