package com.mnsf.resturantandroid.repository

import android.util.Log
import com.mnsf.resturantandroid.BuildConfig
import com.mnsf.resturantandroid.data.model.*
import com.mnsf.resturantandroid.network.ApiService
import com.mnsf.resturantandroid.network.PhoneLoginRequest
import com.mnsf.resturantandroid.util.SessionManager

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                sessionManager.saveAuthToken(authResponse.token)
                sessionManager.saveCustomerInfo(
                    authResponse.customer.id,
                    authResponse.customer.name,
                    authResponse.customer.email,
                    authResponse.customer.phone,
                    authResponse.customer.address
                )
                Result.success(authResponse)
            } else {
                Result.failure(Exception(response.message() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            logConnectionError("register", e)
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            Log.e("LoginFlow", "sending login to server: ${BuildConfig.API_BASE_URL}")
            val response = apiService.login(request)
            Log.e("LoginFlow", "server responded: code=${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                sessionManager.saveAuthToken(authResponse.token)
                sessionManager.saveCustomerInfo(
                    authResponse.customer.id,
                    authResponse.customer.name,
                    authResponse.customer.email,
                    authResponse.customer.phone,
                    authResponse.customer.address
                )
                Result.success(authResponse)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            logConnectionError("login", e)
            Result.failure(e)
        }
    }

    suspend fun loginWithPhone(phone: String, firebaseToken: String?, latitude: Double? = null, longitude: Double? = null, address: String? = null): Result<AuthResponse> {
        return try {
            val response = apiService.loginWithPhone(PhoneLoginRequest(phone, firebaseToken, latitude, longitude, address))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                sessionManager.saveAuthToken(authResponse.token)
                sessionManager.saveCustomerInfo(
                    authResponse.customer.id,
                    authResponse.customer.name,
                    authResponse.customer.email,
                    authResponse.customer.phone,
                    authResponse.customer.address
                )
                Result.success(authResponse)
            } else {
                Result.failure(Exception(response.message() ?: "Phone login failed"))
            }
        } catch (e: Exception) {
            logConnectionError("loginWithPhone", e)
            Result.failure(e)
        }
    }

    private fun logConnectionError(operation: String, e: Exception) {
        Log.e("AuthRepository", "$operation: ${e.message}", e)
        Log.e("AuthRepository", "API_BASE_URL in use: ${BuildConfig.API_BASE_URL}")
        var cause: Throwable? = e.cause
        while (cause != null) {
            Log.e("AuthRepository", "  cause: ${cause.javaClass.simpleName}: ${cause.message}")
            cause = cause.cause
        }
    }

    fun logout() {
        sessionManager.logout()
    }
    
    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }
    
    fun getAuthToken(): String? {
        return sessionManager.getAuthToken()
    }
}

