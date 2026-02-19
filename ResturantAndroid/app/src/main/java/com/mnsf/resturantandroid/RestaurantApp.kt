package com.mnsf.resturantandroid

import android.app.Application
import android.content.Context
import android.util.Log
import com.mnsf.resturantandroid.util.LocaleHelper
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class RestaurantApp : Application() {

    companion object {
        private const val CRASH_FILE = "last_crash_paytabs.txt"
        private const val TAG = "CrashCapture"
    }

    override fun attachBaseContext(base: Context) {
        // Set default locale to Arabic
        super.attachBaseContext(LocaleHelper.setLocale(base, LocaleHelper.getLocale(base)))
    }

    override fun onCreate() {
        super.onCreate()

        // On restart after a crash: log the previous crash so it can be captured and sent
        logAndClearLastCrashIfAny()

        // Install handler to save crash to file and log immediately (binary XML / inflation errors show here)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrashToLogcat(throwable)
            saveCrashToFile(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
                ?: throw throwable
        }

        // Ensure Arabic is set as default if no preference exists
        val language = LocaleHelper.getLocale(this)
        LocaleHelper.setLocale(this, language)
    }

    private fun logCrashToLogcat(throwable: Throwable) {
        val msg = "CRASH: ${throwable.javaClass.simpleName}: ${throwable.message}"
        Log.e(TAG, msg)
        // Also log with AndroidRuntime so it shows when filtering for system crashes
        Log.e("AndroidRuntime", "RestaurantApp: $msg")
        var cause: Throwable? = throwable.cause
        var depth = 0
        while (cause != null && depth < 5) {
            val causeMsg = "  cause[$depth]: ${cause.javaClass.simpleName}: ${cause.message}"
            Log.e(TAG, causeMsg)
            Log.e("AndroidRuntime", "RestaurantApp: $causeMsg")
            cause = cause.cause
            depth++
        }
        Log.e(TAG, "Stack trace:", throwable)
    }

    private fun saveCrashToFile(throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            File(filesDir, CRASH_FILE).writeText(trace)
            Log.e(TAG, "Crash saved to file for next launch: $CRASH_FILE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash", e)
        }
    }

    private fun logAndClearLastCrashIfAny() {
        try {
            val file = File(filesDir, CRASH_FILE)
            if (file.exists()) {
                val content = file.readText()
                file.delete()
                Log.e(TAG, "========== PREVIOUS RUN CRASH (e.g. PayTabs) ==========")
                Log.e(TAG, content)
                Log.e(TAG, "========== END PREVIOUS CRASH ==========")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read last crash", e)
        }
    }
}

