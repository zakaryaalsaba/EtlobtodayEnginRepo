package com.driver.resturantandroid.repository

import com.driver.resturantandroid.data.model.Driver
import com.driver.resturantandroid.data.model.DriverLoginRequest
import com.driver.resturantandroid.data.model.DriverLoginResponse
import com.driver.resturantandroid.data.model.DriverRegisterRequest
import com.driver.resturantandroid.data.model.DriverRegisterResponse
import android.util.Log
import com.driver.resturantandroid.network.ApiService
import com.driver.resturantandroid.network.RetrofitClient
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DriverRepository {
    private val apiService: ApiService = RetrofitClient.apiService
    
    suspend fun register(name: String, email: String, password: String, phone: String? = null): Result<DriverRegisterResponse> {
        return try {
            val response = apiService.driverRegister(DriverRegisterRequest(name, email, password, phone))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val errorJson = org.json.JSONObject(errorBody)
                        errorJson.optString("error", response.message() ?: "Registration failed")
                    } catch (e: Exception) {
                        response.message() ?: "Registration failed"
                    }
                } else {
                    response.message() ?: "Registration failed"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<DriverLoginResponse> {
        return try {
            val response = apiService.driverLogin(DriverLoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val errorJson = org.json.JSONObject(errorBody)
                        errorJson.optString("error", response.message() ?: "Login failed")
                    } catch (e: Exception) {
                        response.message() ?: "Login failed"
                    }
                } else {
                    response.message() ?: "Login failed"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDriverStatus(isOnline: Boolean, token: String): Result<Driver> {
        Log.d(RetrofitClient.TAG, "updateDriverStatus isOnline=$isOnline attempt=1")
        suspend fun attempt(): Result<Driver> {
            return try {
                val response = apiService.updateDriverStatus(
                    mapOf("isOnline" to isOnline),
                    "Bearer $token"
                )
                if (response.isSuccessful && response.body() != null) {
                    Log.d(RetrofitClient.TAG, "updateDriverStatus success isOnline=$isOnline")
                    Result.success(response.body()!!)
                } else {
                    Log.w(RetrofitClient.TAG, "updateDriverStatus failed code=${response.code()} message=${response.message()}")
                    Result.failure(Exception(response.message() ?: "Failed to update status"))
                }
            } catch (e: Exception) {
                val isRefused = isConnectionRefused(e)
                if (isRefused) {
                    Log.e(RetrofitClient.TAG, "updateDriverStatus: connection refused — backend not running or firewall blocking. Start server (npm start) and ensure phone is on same Wi‑Fi, port 3000 open.")
                } else {
                    Log.e(RetrofitClient.TAG, "updateDriverStatus error: ${e.message}", e)
                }
                Result.failure(e)
            }
        }
        var result = attempt()
        val err = result.exceptionOrNull()
        // Retry only for transient connection errors (timeout, DNS), not for connection refused (server down)
        if (result.isFailure && isConnectionError(err) && err != null && !isConnectionRefused(err)) {
            Log.w(RetrofitClient.TAG, "updateDriverStatus connection error, retrying in 800ms (attempt=2)")
            delay(800)
            result = attempt()
        }
        return result
    }

    private fun isConnectionError(e: Throwable?): Boolean {
        if (e == null) return false
        val msg = e.message?.lowercase() ?: ""
        return e is ConnectException || e is SocketTimeoutException || e is UnknownHostException ||
                msg.contains("failed to connect") || msg.contains("unable to resolve host") || msg.contains("connection refused")
    }

    private fun isConnectionRefused(e: Throwable): Boolean {
        var t: Throwable? = e
        while (t != null) {
            val msg = t.message?.lowercase() ?: ""
            if (msg.contains("econnrefused") || msg.contains("connection refused")) return true
            t = t.cause
        }
        return false
    }
    
    suspend fun updateLocation(latitude: Double, longitude: Double, token: String): Result<Unit> {
        return try {
            val response = apiService.updateDriverLocation(
                mapOf("latitude" to latitude, "longitude" to longitude),
                "Bearer $token"
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDriverProfile(token: String): Result<Driver> {
        return try {
            val response = apiService.getDriverProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to load profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDriverProfile(name: String, phone: String?, token: String): Result<Driver> {
        return try {
            val profileData = mutableMapOf<String, String?>(
                "name" to name
            )
            if (phone != null) {
                profileData["phone"] = phone
            }
            
            val response = apiService.updateDriverProfile(profileData, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val errorJson = org.json.JSONObject(errorBody)
                        errorJson.optString("error", response.message() ?: "Failed to update profile")
                    } catch (e: Exception) {
                        response.message() ?: "Failed to update profile"
                    }
                } else {
                    response.message() ?: "Failed to update profile"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadProfileImage(imageFile: File, token: String): Result<Driver> {
        return try {
            val requestFile = MultipartBody.Part.createFormData(
                "image",
                imageFile.name,
                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            )
            
            val response = apiService.uploadProfileImage(requestFile, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val errorJson = org.json.JSONObject(errorBody)
                        errorJson.optString("error", response.message() ?: "Failed to upload image")
                    } catch (e: Exception) {
                        response.message() ?: "Failed to upload image"
                    }
                } else {
                    response.message() ?: "Failed to upload image"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

