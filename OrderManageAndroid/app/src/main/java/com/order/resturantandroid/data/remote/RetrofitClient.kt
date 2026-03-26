package com.order.resturantandroid.data.remote

import com.order.resturantandroid.BuildConfig
import android.util.Log
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"

    private class LoggingEventListener : EventListener() {
        override fun callStart(call: Call) {
            Log.d(TAG, "HTTP call start: ${call.request().method} ${call.request().url}")
        }

        override fun callEnd(call: Call) {
            Log.d(TAG, "HTTP call end: ${call.request().url}")
        }

        override fun dnsStart(call: Call, domainName: String) {
            Log.d(TAG, "DNS start: $domainName")
        }

        override fun connectFailed(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?,
            ioe: IOException
        ) {
            Log.e(
                TAG,
                "Connect failed: ${inetSocketAddress.hostString}:${inetSocketAddress.port} protocol=${protocol?.toString()} msg=${ioe.message}",
                ioe
            )
        }

        override fun responseHeadersEnd(call: Call, response: okhttp3.Response) {
            Log.d(TAG, "HTTP response headers: ${response.code} ${call.request().url}")
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .eventListenerFactory { LoggingEventListener() }
        .addInterceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (e: Exception) {
                Log.e(TAG, "Request failed: ${request.method} ${request.url} msg=${e.message}", e)
                throw e
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)

    init {
        Log.d(TAG, "API_BASE_URL = ${BuildConfig.API_BASE_URL}")
    }
}

