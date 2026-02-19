package com.driver.resturantandroid.util

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

object SoundHelper {
    /** Use "DriverOrderSound" in Logcat filter to see all order sound logs */
    private const val TAG = "DriverOrderSound"
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Plays a notification sound when a new order is received.
     * Uses the default notification sound or system notification sound.
     */
    fun playOrderReceivedSound(context: Context) {
        Log.d(TAG, "🔊 playOrderReceivedSound called")
        try {
            // Stop any currently playing sound
            Log.d(TAG, "🔊 Stopping any existing sound")
            stopSound()
            
            // Use default notification sound
            val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            Log.d(TAG, "🔊 Default sound URI: $defaultSoundUri")
            
            if (defaultSoundUri == null) {
                Log.e(TAG, "❌ Default sound URI is null!")
                return
            }
            
            mediaPlayer = MediaPlayer.create(context, defaultSoundUri)
            
            if (mediaPlayer == null) {
                Log.e(TAG, "❌ MediaPlayer.create returned null!")
                return
            }
            
            Log.d(TAG, "🔊 MediaPlayer created successfully")
            
            mediaPlayer?.apply {
                setOnCompletionListener {
                    Log.d(TAG, "🔊 Sound playback completed")
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "❌ MediaPlayer error: what=$what, extra=$extra")
                    release()
                    mediaPlayer = null
                    true
                }
                isLooping = false
                Log.d(TAG, "🔊 Starting MediaPlayer...")
                start()
                Log.d(TAG, "🔊 ✅ MediaPlayer started successfully, isPlaying: $isPlaying")
            }
            
            Log.d(TAG, "🔊 ✅ Sound playback initiated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception playing sound: ${e.message}", e)
            e.printStackTrace()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    /**
     * Stops any currently playing sound.
     */
    fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sound: ${e.message}", e)
        }
    }
    
    /**
     * Releases resources when done.
     */
    fun release() {
        stopSound()
    }
}
