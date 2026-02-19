package com.order.resturantandroid.util

import android.content.Context
import android.content.SharedPreferences
import java.util.*

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val SELECTED_LANGUAGE = "selected_language"
    
    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }
    
    fun getPersistedLocale(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SELECTED_LANGUAGE, "en") ?: "en"
    }
    
    private fun persist(context: Context, language: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SELECTED_LANGUAGE, language).apply()
    }
    
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun onAttach(context: Context): Context {
        val lang = getPersistedLocale(context)
        return setLocale(context, lang)
    }
}

