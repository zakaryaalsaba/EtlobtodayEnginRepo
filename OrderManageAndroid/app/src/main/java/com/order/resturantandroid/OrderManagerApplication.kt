package com.order.resturantandroid

import android.app.Application
import android.content.Context
import com.order.resturantandroid.data.remote.RetrofitClient
import com.order.resturantandroid.service.GlobalOrderAlertManager
import com.order.resturantandroid.util.LocaleHelper

class OrderManagerApplication : Application() {
    private var globalOrderAlertManager: GlobalOrderAlertManager? = null
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        globalOrderAlertManager = GlobalOrderAlertManager(this).also { it.start() }
    }

    override fun onTerminate() {
        super.onTerminate()
        globalOrderAlertManager?.stop()
    }
}

