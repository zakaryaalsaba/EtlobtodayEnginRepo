package com.mnsf.resturantandroid.network

import android.util.Log
import com.mnsf.resturantandroid.BuildConfig
import com.mnsf.resturantandroid.util.DeviceHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "ApiConnection"

    // API Base URL: Auto-detect emulator vs physical device
    // Emulator: 10.0.2.2 (maps to host machine's localhost)
    // Physical device: 192.168.1.107 (your computer's LAN IP - must be on same WiFi)
    // Falls back to BuildConfig.API_BASE_URL if DeviceHelper fails
    private val BASE_URL: String = run {
        val detectedUrl = try {
            DeviceHelper.getApiBaseUrl()
        } catch (e: Exception) {
            Log.w(TAG, "DeviceHelper failed, using BuildConfig.API_BASE_URL", e)
            Log.e("LoginFlow", "RetrofitClient: DeviceHelper failed, using BuildConfig: ${BuildConfig.API_BASE_URL}")
            BuildConfig.API_BASE_URL
        }
        
        var url = detectedUrl.trimStart('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        if (!url.endsWith("/")) url += "/"
        Log.d(TAG, "API base URL: $url (device: ${if (DeviceHelper.isEmulator()) "emulator" else "physical"}, check: same WiFi as server, server running on that IP:3000)")
        Log.e("LoginFlow", "RetrofitClient: Final API base URL: $url (device: ${if (DeviceHelper.isEmulator()) "emulator" else "physical device"})")
        url
    }

    private val connectionErrorInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()
        try {
            val response = chain.proceed(request)
            if (!response.isSuccessful) {
                Log.w(TAG, "Request failed: $url code=${response.code}")
            }
            response
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: $url", e)
            var cause: Throwable? = e.cause
            while (cause != null) {
                Log.e(TAG, "  cause: ${cause.javaClass.simpleName}: ${cause.message}")
                cause = cause.cause
            }
            throw e
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(connectionErrorInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Custom Gson instance that handles boolean values from MySQL (0/1)
    // Also handles payment_methods field which can be object or string
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())
        .registerTypeAdapter(Boolean::class.javaPrimitiveType, BooleanTypeAdapter())
        .registerTypeAdapter(com.mnsf.resturantandroid.data.model.Restaurant::class.java, RestaurantDeserializer())
        .registerTypeAdapter(com.mnsf.resturantandroid.data.model.Product::class.java, ProductDeserializer())
        .registerTypeAdapter(com.mnsf.resturantandroid.data.model.ProductAddon::class.java, ProductAddonDeserializer())
        .registerTypeAdapter(com.mnsf.resturantandroid.data.model.ProductAddonsResponse::class.java, ProductAddonsResponseDeserializer())
        .setLenient() // Allow lenient parsing
        .create()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

