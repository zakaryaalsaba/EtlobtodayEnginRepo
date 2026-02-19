package com.driver.resturantandroid.network

import android.util.Log
import com.driver.resturantandroid.util.DeviceHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    /** Use this tag in Logcat to filter: adb logcat -s DriverApi */
    const val TAG = "DriverApi"

    // Emulator: 10.0.2.2. Physical device: use local.properties API_BASE_URL or DeviceHelper default LAN IP
    private val BASE_URL: String = run {
        val raw = try {
            DeviceHelper.getApiBaseUrl()
        } catch (e: Exception) {
            Log.w(TAG, "DeviceHelper failed, using BuildConfig.API_BASE_URL", e)
            com.driver.resturantandroid.BuildConfig.API_BASE_URL
        }
        // Normalize: trim, strip ALL leading slashes (avoids "Failed to connect to /192.168.1.11:3000"), ensure scheme and trailing slash
        var url = raw.trim().trimStart('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        if (!url.endsWith("/")) url = "$url/"
        Log.i(TAG, "baseUrl=$url")
        url
    }

    private val loggingInterceptor = HttpLoggingInterceptor(
        object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d(TAG, message)
            }
        }
    ).apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

