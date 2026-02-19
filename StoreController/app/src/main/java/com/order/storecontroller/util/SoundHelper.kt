package com.order.storecontroller.util

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

object SoundHelper {
    private const val TAG = "StoreControllerOrderSound"
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Plays the default notification sound when a new order is received/confirmed.
     */
    fun playOrderReceivedSound(context: Context) {
        Log.d(TAG, "playOrderReceivedSound called")
        try {
            stopSound()
            val defaultSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (defaultSoundUri == null) {
                Log.e(TAG, "Default sound URI is null")
                return
            }
            mediaPlayer = MediaPlayer.create(context, defaultSoundUri)
            if (mediaPlayer == null) {
                Log.e(TAG, "MediaPlayer.create returned null")
                return
            }
            mediaPlayer?.apply {
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, _, _ ->
                    release()
                    mediaPlayer = null
                    true
                }
                isLooping = false
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception playing sound: ${e.message}", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sound: ${e.message}", e)
        }
    }
}
