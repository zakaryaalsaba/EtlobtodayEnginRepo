package com.driver.resturantandroid.service

import com.driver.resturantandroid.util.FCMTokenManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.driver.resturantandroid.MainActivity
import com.driver.resturantandroid.R
import com.driver.resturantandroid.util.SoundHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DriverFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "========== NEW FCM TOKEN RECEIVED ==========")
        Log.d(TAG, "Refreshed token: $token")
        Log.d(TAG, "Token Length: ${token.length}")
        Log.d(TAG, "Token (first 50 chars): ${token.take(50)}...")
        
        // Send token to backend
        sendRegistrationToServer(token)
        Log.d(TAG, "========== END NEW FCM TOKEN ==========")
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "========== FCM MESSAGE RECEIVED ==========")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Message Type: ${remoteMessage.messageType}")
        Log.d(TAG, "Sent Time: ${remoteMessage.sentTime}")
        Log.d(TAG, "Notification payload: ${remoteMessage.notification != null}")
        Log.d(TAG, "Data payload size: ${remoteMessage.data.size}")
        Log.d(TAG, "Data payload: ${remoteMessage.data}")
        
        // When app is in foreground, FCM calls onMessageReceived
        // When app is in background, system handles notification automatically
        // We need to handle both cases
        
        var title: String
        var body: String
        var shouldSendNotification = false
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification payload found:")
            Log.d(TAG, "  - Title: ${notification.title}")
            Log.d(TAG, "  - Body: ${notification.body}")
            Log.d(TAG, "  - Icon: ${notification.icon}")
            Log.d(TAG, "  - Sound: ${notification.sound}")
            Log.d(TAG, "  - Tag: ${notification.tag}")
            Log.d(TAG, "  - Channel ID: ${notification.channelId}")
            
            title = notification.title ?: "New Order Available"
            body = notification.body ?: "A new delivery order is available"
            shouldSendNotification = true
            
            // Play sound so captain hears the order
            playNewOrderSound()
            // Send notification manually (app is in foreground)
            sendNotification(title, body, remoteMessage.data)
        }
        
        // Also handle data-only messages (if notification payload wasn't processed)
        if (!shouldSendNotification && remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "No notification payload, checking data payload...")
            Log.d(TAG, "Data payload found, extracting title and body")
            title = remoteMessage.data["title"] ?: "New Order Available"
            body = remoteMessage.data["body"] ?: "A new delivery order is available"
            
            Log.d(TAG, "Extracted - Title: $title, Body: $body")
            playNewOrderSound()
            sendNotification(title, body, remoteMessage.data)
        } else if (!shouldSendNotification) {
            Log.w(TAG, "No notification or data payload found in message")
        }
        
        Log.d(TAG, "========== END FCM MESSAGE ==========")
    }
    
    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        Log.d(TAG, "========== SENDING NOTIFICATION ==========")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Body: $messageBody")
        Log.d(TAG, "Data: $data")
        
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add data to intent
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            // Navigate to Available Orders tab
            putExtra("navigate_to", "available_orders")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = getString(R.string.default_notification_channel_id)
        Log.d(TAG, "Channel ID: $channelId")
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        Log.d(TAG, "Sound URI: $defaultSoundUri")
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Channel should already be created by NotificationHelper, but create it here as fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                Log.w(TAG, "Channel not found, creating fallback channel: $channelId")
                val channel = NotificationChannel(
                    channelId,
                    "Order Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new delivery orders"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created as fallback: $channelId")
            } else {
                Log.d(TAG, "Channel exists: $channelId")
                Log.d(TAG, "Channel importance: ${existingChannel.importance}")
                Log.d(TAG, "Channel enabled: ${existingChannel.importance != NotificationManager.IMPORTANCE_NONE}")
            }
        }
        
        // Check if notifications are enabled
        val areNotificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        Log.d(TAG, "Notifications enabled: $areNotificationsEnabled")
        
        if (!areNotificationsEnabled) {
            Log.e(TAG, "❌ Notifications are DISABLED for this app!")
        }
        
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "✅ Notification sent with ID: $notificationId")
        Log.d(TAG, "========== END SENDING NOTIFICATION ==========")
    }
    
    private fun sendRegistrationToServer(token: String) {
        // This will be called from MainActivity or LoginActivity after login
        // Store token locally for now
        val sharedPref = getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("fcm_token", token).apply()
        Log.d(TAG, "FCM token stored locally: $token")
    }
    
    /** Play sound when a new order push is received so the captain hears it. */
    private fun playNewOrderSound() {
        try {
            Log.i(SOUND_TAG, "New order push received - playing sound")
            SoundHelper.playOrderReceivedSound(this)
        } catch (e: Exception) {
            Log.e(SOUND_TAG, "Failed to play order sound: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "DriverFCMService"
        /** Use this in Logcat filter to see order sound logs: DriverOrderSound */
        private const val SOUND_TAG = "DriverOrderSound"
    }
}


