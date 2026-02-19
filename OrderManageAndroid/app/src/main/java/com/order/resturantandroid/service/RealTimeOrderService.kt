package com.order.resturantandroid.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class RealTimeOrderService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPolling()
        return START_STICKY
    }
    
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Poll for new orders
                    // This will be handled by the ViewModel
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling", e)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        serviceScope.cancel()
    }
    
    companion object {
        private const val TAG = "RealTimeOrderService"
    }
}

