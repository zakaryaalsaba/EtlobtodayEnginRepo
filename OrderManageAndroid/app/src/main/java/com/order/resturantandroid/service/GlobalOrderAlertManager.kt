package com.order.resturantandroid.service

import android.content.Context
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.order.resturantandroid.BuildConfig

/**
 * App-wide new-order alarm monitor.
 * Listens to pending orders under orders/{websiteId} and plays an alarm when new ones appear.
 */
class GlobalOrderAlertManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var ordersListener: ValueEventListener? = null
    private var websiteRefPath: String? = null
    private var knownPendingIds: Set<Int> = emptySet()
    private var hasInitialSnapshot = false
    private var ringtone: Ringtone? = null

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("restaurant_prefs", Context.MODE_PRIVATE)

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "is_logged_in" || key == "website_id") {
            refreshListenerFromSession()
        }
    }

    fun start() {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        refreshListenerFromSession()
    }

    fun stop() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        detachFirebaseListener()
        stopRingtone()
    }

    private fun refreshListenerFromSession() {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val websiteId = prefs.getInt("website_id", -1)
        if (!isLoggedIn || websiteId == -1) {
            detachFirebaseListener()
            knownPendingIds = emptySet()
            hasInitialSnapshot = false
            return
        }

        val newPath = "orders/$websiteId"
        if (websiteRefPath == newPath && ordersListener != null) return
        attachFirebaseListener(websiteId)
    }

    private fun attachFirebaseListener(websiteId: Int) {
        detachFirebaseListener()
        websiteRefPath = "orders/$websiteId"
        val ref = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
            .getReference("orders")
            .child(websiteId.toString())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pendingIds = mutableSetOf<Int>()
                for (child in snapshot.children) {
                    val status = child.child("status").getValue(String::class.java)
                        ?.trim()
                        ?.lowercase()
                    if (status == "pending") {
                        val idAny = child.child("id").getValue(Any::class.java)
                        val id = (idAny as? Number)?.toInt()
                        if (id != null) pendingIds.add(id)
                    }
                }

                if (!hasInitialSnapshot) {
                    hasInitialSnapshot = true
                    knownPendingIds = pendingIds
                    return
                }

                val newIds = pendingIds - knownPendingIds
                if (newIds.isNotEmpty()) {
                    playAlarmBurst(newIds.size)
                }
                knownPendingIds = pendingIds
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Global order alarm listener cancelled: ${error.message}")
            }
        }
        ordersListener = listener
        ref.addValueEventListener(listener)
    }

    private fun detachFirebaseListener() {
        val path = websiteRefPath ?: return
        val listener = ordersListener ?: return
        val websiteId = path.substringAfter("orders/").toIntOrNull() ?: return
        FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
            .getReference("orders")
            .child(websiteId.toString())
            .removeEventListener(listener)
        ordersListener = null
        websiteRefPath = null
    }

    private fun playAlarmBurst(count: Int) {
        val times = count.coerceIn(1, 5)
        repeat(times) { index ->
            handler.postDelayed({ playSingleAlarm() }, index * 1400L)
        }
    }

    private fun playSingleAlarm() {
        try {
            if (System.currentTimeMillis() < muteUntilMs) return
            stopRingtone()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alarmUri != null) {
                ringtone = RingtoneManager.getRingtone(appContext, alarmUri)
                activeRingtone = ringtone
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play global new-order alarm", e)
        }
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
        activeRingtone = null
    }

    companion object {
        private const val TAG = "GlobalOrderAlertManager"
        @Volatile
        private var activeRingtone: Ringtone? = null
        @Volatile
        private var muteUntilMs: Long = 0L

        fun stopActiveAlarm() {
            try {
                activeRingtone?.stop()
            } catch (_: Exception) {
            }
            activeRingtone = null
            muteUntilMs = System.currentTimeMillis() + 3500L
        }
    }
}

