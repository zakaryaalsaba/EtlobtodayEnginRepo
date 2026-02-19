package com.driver.resturantandroid.network

import com.driver.resturantandroid.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Driver Authentication
    @POST("drivers/register")
    suspend fun driverRegister(@Body request: DriverRegisterRequest): Response<DriverRegisterResponse>
    
    @POST("drivers/login")
    suspend fun driverLogin(@Body request: DriverLoginRequest): Response<DriverLoginResponse>
    
    // Driver Status
    @PUT("drivers/status")
    suspend fun updateDriverStatus(
        @Body status: Map<String, Boolean>,
        @Header("Authorization") token: String
    ): Response<Driver>
    
    @GET("drivers/me")
    suspend fun getDriverProfile(
        @Header("Authorization") token: String
    ): Response<Driver>
    
    @PUT("drivers/profile")
    suspend fun updateDriverProfile(
        @Body profile: Map<String, String?>,
        @Header("Authorization") token: String
    ): Response<Driver>
    
    @Multipart
    @POST("drivers/profile/image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Response<Driver>
    
    // Orders
    @GET("orders/delivery/available")
    suspend fun getAvailableOrders(
        @Header("Authorization") token: String
    ): Response<List<Order>>
    
    @GET("orders/delivery/assigned")
    suspend fun getAssignedOrders(
        @Header("Authorization") token: String
    ): Response<List<Order>>
    
    @GET("orders/delivery/history")
    suspend fun getOrderHistory(
        @Header("Authorization") token: String
    ): Response<List<Order>>
    
    @POST("orders/{orderId}/accept")
    suspend fun acceptOrder(
        @Path("orderId") orderId: Int,
        @Header("Authorization") token: String
    ): Response<Order>
    
    @POST("orders/{orderId}/reject")
    suspend fun rejectOrder(
        @Path("orderId") orderId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>
    
    @PUT("orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Int,
        @Body status: OrderStatusUpdate,
        @Header("Authorization") token: String
    ): Response<Order>
    
    @GET("orders/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: Int,
        @Header("Authorization") token: String
    ): Response<Order>
    
    // Location
    @PUT("drivers/location")
    suspend fun updateDriverLocation(
        @Body location: Map<String, Double>,
        @Header("Authorization") token: String
    ): Response<Unit>
    
    // Device Token
    @PUT("drivers/device-token")
    suspend fun updateDeviceToken(
        @Body request: Map<String, String>,
        @Header("Authorization") token: String
    ): Response<Map<String, String>>
    
    // Restaurant Info
    @GET("websites/{websiteId}")
    suspend fun getRestaurant(
        @Path("websiteId") websiteId: Int
    ): Response<Restaurant>
}

