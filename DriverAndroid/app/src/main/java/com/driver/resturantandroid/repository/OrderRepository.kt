package com.driver.resturantandroid.repository

import com.driver.resturantandroid.data.model.Order
import com.driver.resturantandroid.data.model.Restaurant
import com.driver.resturantandroid.network.ApiService
import com.driver.resturantandroid.network.RetrofitClient

class OrderRepository {
    private val apiService: ApiService = RetrofitClient.apiService
    
    suspend fun getRestaurant(websiteId: Int): Result<Restaurant> {
        return try {
            val response = apiService.getRestaurant(websiteId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch restaurant"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAvailableOrders(token: String): Result<List<Order>> {
        return try {
            val response = apiService.getAvailableOrders("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch orders"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAssignedOrders(token: String): Result<List<Order>> {
        return try {
            val response = apiService.getAssignedOrders("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch assigned orders"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOrderHistory(token: String): Result<List<Order>> {
        return try {
            val response = apiService.getOrderHistory("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch order history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun acceptOrder(orderId: Int, token: String): Result<Order> {
        return try {
            val response = apiService.acceptOrder(orderId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string()?.let { body ->
                    try {
                        org.json.JSONObject(body).optString("error", response.message() ?: "Failed to accept order")
                    } catch (_: Exception) {
                        response.message() ?: "Failed to accept order"
                    }
                } ?: (response.message() ?: "Failed to accept order")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rejectOrder(orderId: Int, token: String): Result<Unit> {
        return try {
            val response = apiService.rejectOrder(orderId, "Bearer $token")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to reject order"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateOrderStatus(orderId: Int, status: String, token: String): Result<Order> {
        return try {
            val response = apiService.updateOrderStatus(
                orderId,
                com.driver.resturantandroid.data.model.OrderStatusUpdate(status),
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

