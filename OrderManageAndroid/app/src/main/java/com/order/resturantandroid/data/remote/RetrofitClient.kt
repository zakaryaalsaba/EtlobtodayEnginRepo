package com.order.resturantandroid.data.remote

import android.content.Context
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

    @Volatile
    private var apiServiceInstance: ApiService? = null

    /**
     * Must be called from [com.order.resturantandroid.OrderManagerApplication.onCreate] before any API usage.
     */
    fun init(context: Context) {
        if (apiServiceInstance != null) return
        synchronized(this) {
            if (apiServiceInstance != null) return
            val appContext = context.applicationContext
            val okHttpClient = buildOkHttpClient(appContext)
            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiServiceInstance = retrofit.create(ApiService::class.java)
            Log.d(TAG, "API_BASE_URL = ${BuildConfig.API_BASE_URL}")
        }
    }

    val apiService: ApiService
        get() = apiServiceInstance
            ?: error("RetrofitClient.init(context) was not called from Application.onCreate")

    private fun buildOkHttpClient(appContext: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .eventListenerFactory { LoggingEventListener() }
            .authenticator(TokenAuthenticator(appContext))
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
    }

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
}
