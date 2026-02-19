package com.order.storecontroller

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.order.storecontroller.util.LocaleHelper

class StoreControllerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        LocaleHelper.setLocale(this, LocaleHelper.getLocale(this))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
}
