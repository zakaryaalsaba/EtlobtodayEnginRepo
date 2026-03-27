package com.order.resturantandroid.data.repository

import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.data.model.OrderStatusUpdate
import com.order.resturantandroid.data.remote.ApiService
import com.order.resturantandroid.data.remote.RestaurantWebsite
import com.order.resturantandroid.data.remote.RetrofitClient
import android.util.Log

class OrderRepository {
    private val tag = "OrderRepository"
    private val apiService: ApiService = RetrofitClient.apiService
    
    suspend fun getOrders(websiteId: Int, status: String? = null, token: String): Result<List<Order>> {
        return try {
            val response = apiService.getOrders(websiteId, status, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.orders)
            } else {
                val errorBody = try {
                    response.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                android.util.Log.e(
                    tag,
                    "getOrders() failed: http=${response.code()} msg=${response.message()} websiteId=$websiteId status=$status errorBody=$errorBody"
                )
                Result.failure(
                    Exception(
                        response.message()
                            ?: "Failed to fetch orders (http ${response.code()})"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "getOrders() failed: ${e.javaClass.simpleName} msg=${e.message}")
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
            Log.e(tag, "getOrder() failed: ${e.javaClass.simpleName} msg=${e.message}", e)
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
                val errorBody = try {
                    response.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                android.util.Log.e(
                    tag,
                    "updateOrderStatus() failed: http=${response.code()} msg=${response.message()} orderId=$orderId status=$status errorBody=$errorBody"
                )
                Result.failure(
                    Exception(
                        response.message()
                            ?: "Failed to update order status (http ${response.code()})"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "updateOrderStatus() failed: ${e.javaClass.simpleName} msg=${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getRestaurantWebsite(token: String): Result<RestaurantWebsite> {
        return try {
            val response = apiService.getRestaurantInfo("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.website)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch website"))
            }
        } catch (e: Exception) {
            Log.e(tag, "getRestaurantWebsite() failed: ${e.message}")
            Result.failure(e)
        }
    }
}

