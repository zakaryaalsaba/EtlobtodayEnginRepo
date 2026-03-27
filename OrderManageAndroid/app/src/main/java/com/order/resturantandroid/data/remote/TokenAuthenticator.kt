package com.order.resturantandroid.data.remote

import android.content.Context
import android.util.Log
import com.order.resturantandroid.BuildConfig
import com.order.resturantandroid.util.SessionManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * On 401, exchanges [SessionManager] refresh token for new access + refresh tokens and retries once.
 */
class TokenAuthenticator(private val appContext: Context) : Authenticator {

    private val refreshHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val lock = ReentrantLock()

    override fun authenticate(route: Route?, response: Response): Request? {
        val url = response.request.url.toString()
        if (url.contains("/admin/login", ignoreCase = true) ||
            url.contains("/admin/refresh", ignoreCase = true)
        ) {
            return null
        }
        if (response.code != 401) return null

        // Only one automatic retry per call chain
        if (responseCount(response) > 1) {
            return null
        }

        lock.withLock {
            val session = SessionManager(appContext)
            val bearerInRequest = response.request.header("Authorization")
                ?.removePrefix("Bearer")
                ?.trim()
            val currentAccess = session.getAuthToken()

            // Another request already refreshed the access token
            if (!currentAccess.isNullOrBlank() && currentAccess != bearerInRequest) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .build()
            }

            val refreshToken = session.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                Log.w(TAG, "401 but no refresh token stored; user must log in again")
                session.logout()
                return null
            }

            val pair = refreshTokens(refreshToken)
            if (pair == null) {
                Log.w(TAG, "Token refresh failed; clearing session")
                session.logout()
                return null
            }

            session.saveAuthToken(pair.first)
            session.saveRefreshToken(pair.second)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${pair.first}")
                .build()
        }
    }

    private fun refreshTokens(refreshToken: String): Pair<String, String>? {
        val jsonBody = JSONObject().put("refreshToken", refreshToken).toString()
        val body = jsonBody.toRequestBody(JSON_MEDIA)
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        val req = Request.Builder()
            .url("$base/admin/refresh")
            .post(body)
            .build()
        return try {
            refreshHttp.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Log.w(TAG, "refresh HTTP ${resp.code}: $text")
                    return null
                }
                val json = JSONObject(text)
                val token = json.optString("token", "")
                val newRt = json.optString("refreshToken", "")
                if (token.isBlank() || newRt.isBlank()) null else Pair(token, newRt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "refresh request failed", e)
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var n = 1
        var p = response.priorResponse
        while (p != null) {
            n++
            p = p.priorResponse
        }
        return n
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
        private val JSON_MEDIA = "application/json; charset=UTF-8".toMediaType()
    }
}
