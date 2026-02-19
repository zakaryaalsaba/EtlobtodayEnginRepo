package com.mnsf.resturantandroid.repository

import com.mnsf.resturantandroid.data.model.Notification
import com.mnsf.resturantandroid.network.ApiService
import com.mnsf.resturantandroid.util.SessionManager

class NotificationRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun getNotifications(customerId: Int): Result<List<Notification>> {
        return try {
            val token = sessionManager.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.getCustomerNotifications(customerId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.notifications)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch notifications"))
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "getNotifications: Exception", e)
            Result.failure(e)
        }
    }
    
    suspend fun markAsRead(customerId: Int, notificationId: Int): Result<Boolean> {
        return try {
            val token = sessionManager.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.markNotificationAsRead(customerId, notificationId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.success)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to mark notification as read"))
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "markAsRead: Exception", e)
            Result.failure(e)
        }
    }
    
    suspend fun markAllAsRead(customerId: Int): Result<Boolean> {
        return try {
            val token = sessionManager.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.markAllNotificationsAsRead(customerId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.success)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to mark all notifications as read"))
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "markAllAsRead: Exception", e)
            Result.failure(e)
        }
    }
}

