package com.mnsf.resturantandroid.repository

import com.mnsf.resturantandroid.data.model.*
import com.mnsf.resturantandroid.network.ApiService
import com.mnsf.resturantandroid.util.SessionManager

class OrderRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun createOrder(request: CreateOrderRequest): Result<Order> {
        return try {
            android.util.Log.d("OrderRepository", "createOrder: Creating order for website_id=${request.website_id}")
            val response = apiService.createOrder(request)
            android.util.Log.d("OrderRepository", "createOrder: Response received - isSuccessful=${response.isSuccessful}, code=${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!
                val order = orderResponse.order
                android.util.Log.d("OrderRepository", "createOrder: Order created - number=${order.order_number}, items count=${order.items?.size ?: 0}")
                Result.success(order)
            } else {
                val errorMessage = response.message() ?: "Failed to create order"
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("OrderRepository", "createOrder: Failed - message=$errorMessage, body=$errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "createOrder: Exception", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getOrderByNumber(orderNumber: String): Result<Order> {
        return try {
            android.util.Log.d("OrderRepository", "getOrderByNumber: Fetching order $orderNumber")
            val response = apiService.getOrderByNumber(orderNumber)
            android.util.Log.d("OrderRepository", "getOrderByNumber: Response received - isSuccessful=${response.isSuccessful}, code=${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!
                val order = orderResponse.order
                android.util.Log.d("OrderRepository", "getOrderByNumber: Order fetched - number=${order.order_number}, items count=${order.items?.size ?: 0}")
                Result.success(order)
            } else {
                val errorMessage = response.message() ?: "Failed to fetch order"
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("OrderRepository", "getOrderByNumber: Failed - message=$errorMessage, body=$errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "getOrderByNumber: Exception", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getCustomerOrders(customerId: Int): Result<List<Order>> {
        return try {
            val token = sessionManager.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.getCustomerOrders(customerId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.orders)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch orders"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

