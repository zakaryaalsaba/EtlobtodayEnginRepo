package com.mnsf.resturantandroid.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "RestaurantAppPrefs"
    private const val PREFS_LANGUAGE = "language"
    
    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }
    
    fun getLocale(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_LANGUAGE, "ar") ?: "ar" // Default to Arabic
    }
    
    private fun persist(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_LANGUAGE, language).apply()
    }
    
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            configuration.setLayoutDirection(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        
        return context
    }
    
    fun getLocalizedContext(context: Context): Context {
        val language = getLocale(context)
        return setLocale(context, language)
    }

    /**
     * Get a string resource with format arguments, but format numbers in English (0-9)
     * so numerals are not translated to Arabic-Indic or other script.
     */
    fun getStringWithEnglishNumbers(context: Context, resId: Int, vararg formatArgs: Any): String {
        return String.format(Locale.US, context.getString(resId), *formatArgs)
    }
}

