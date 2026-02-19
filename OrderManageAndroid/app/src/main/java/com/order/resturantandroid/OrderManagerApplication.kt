package com.order.resturantandroid

import android.app.Application
import android.content.Context
import com.order.resturantandroid.util.LocaleHelper

class OrderManagerApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase if needed
    }
}

