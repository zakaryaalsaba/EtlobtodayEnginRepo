package com.driver.resturantandroid

import android.app.Application
import android.content.Context
import com.driver.resturantandroid.util.LocaleHelper

class DriverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Set default locale on app start
        LocaleHelper.setLocale(this, LocaleHelper.getLocale(this))
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
}

