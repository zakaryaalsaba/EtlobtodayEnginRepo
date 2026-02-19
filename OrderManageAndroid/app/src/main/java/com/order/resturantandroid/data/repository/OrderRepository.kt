package com.order.resturantandroid.data.repository

import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.data.model.OrderStatusUpdate
import com.order.resturantandroid.data.remote.ApiService
import com.order.resturantandroid.data.remote.RetrofitClient

class OrderRepository {
    private val apiService: ApiService = RetrofitClient.apiService
    
    suspend fun getOrders(websiteId: Int, status: String? = null, token: String): Result<List<Order>> {
        return try {
            val response = apiService.getOrders(websiteId, status, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.orders)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch orders"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOrder(orderNumber: String, token: String): Result<Order> {
        return try {
            val response = apiService.getOrder(orderNumber, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!
                // Handle both wrapped and direct order responses
                val order = orderResponse.order
                Result.success(order)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to fetch order: ${response.message()}. Error: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error fetching order: ${e.message}", e))
        }
    }
    
    suspend fun updateOrderStatus(orderId: Int, status: String, token: String): Result<Order> {
        return try {
            val response = apiService.updateOrderStatus(
                orderId,
                OrderStatusUpdate(status),
                "Bearer $token"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update order status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

