package com.order.resturantandroid.data.repository

import com.order.resturantandroid.data.model.AuthResponse
import com.order.resturantandroid.data.remote.ApiService
import com.order.resturantandroid.data.remote.LoginRequest
import com.order.resturantandroid.data.remote.RetrofitClient

class AuthRepository {
    private val apiService: ApiService = RetrofitClient.apiService
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProfile(token: String): Result<com.order.resturantandroid.data.model.Admin> {
        return try {
            val response = apiService.getProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

