package com.order.resturantandroid.data.remote

import com.google.gson.annotations.SerializedName
import com.order.resturantandroid.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication
    @POST("admin/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @GET("admin/me")
    suspend fun getProfile(@Header("Authorization") token: String): Response<Admin>
    
    // Orders
    @GET("orders/website/{websiteId}")
    suspend fun getOrders(
        @Path("websiteId") websiteId: Int,
        @Query("status") status: String?,
        @Header("Authorization") token: String
    ): Response<OrdersResponse>
    
    @GET("orders/{orderNumber}")
    suspend fun getOrder(
        @Path("orderNumber") orderNumber: String,
        @Header("Authorization") token: String
    ): Response<OrderResponse>
    
    data class OrderResponse(
        @SerializedName("order") val order: Order
    )
    
    @PUT("orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Int,
        @Body statusUpdate: OrderStatusUpdate,
        @Header("Authorization") token: String
    ): Response<Order>
    
    // Restaurant info
    @GET("restaurant/website")
    suspend fun getRestaurantInfo(
        @Header("Authorization") token: String
    ): Response<RestaurantWebsiteResponse>
    
    @PUT("restaurant/website")
    suspend fun updateRestaurantInfo(
        @Body update: RestaurantWebsiteUpdate,
        @Header("Authorization") token: String
    ): Response<RestaurantWebsiteResponse>
    
    // Device token
    @PUT("admin/device-token")
    suspend fun updateDeviceToken(
        @Body request: DeviceTokenRequest,
        @Header("Authorization") token: String
    ): Response<DeviceTokenResponse>
    
    // Delivery zones
    @GET("restaurant/zones")
    suspend fun getRestaurantZones(
        @Header("Authorization") token: String
    ): Response<RestaurantZonesResponse>

    // Request driver for a zone (inserts into orders_delivery)
    @POST("restaurant/request-driver")
    suspend fun requestDriver(
        @Body body: RequestDriverBody,
        @Header("Authorization") token: String
    ): Response<RequestDriverResponse>
}

data class RequestDriverBody(
    @SerializedName("zone_id") val zoneId: Int
)

data class RequestDriverResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("record") val record: OrdersDeliveryRecord?
)

data class OrdersDeliveryRecord(
    @SerializedName("id") val id: Int,
    @SerializedName("website_id") val websiteId: Int,
    @SerializedName("zone_id") val zoneId: Int,
    @SerializedName("zone_name") val zoneName: String,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class DeviceTokenRequest(
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("device_type") val deviceType: String = "android"
)

data class DeviceTokenResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)

data class RestaurantWebsiteResponse(
    @SerializedName("website") val website: RestaurantWebsite
)

data class RestaurantWebsiteUpdate(
    @SerializedName("is_published") val isPublished: Boolean? = null,
    @SerializedName("restaurant_name") val restaurantName: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RestaurantWebsite(
    @SerializedName("id") val id: Int,
    @SerializedName("restaurant_name") val restaurantName: String,
    @SerializedName("address") val address: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("is_published") val isPublished: Any? = null, // Can be 0/1 or true/false
    @SerializedName("description") val description: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
) {
    fun getIsPublished(): Boolean {
        return when (isPublished) {
            is Boolean -> isPublished
            is Number -> isPublished.toInt() == 1
            is String -> isPublished == "1" || isPublished.lowercase() == "true"
            else -> false
        }
    }
    
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }
}

data class DeliveryCompany(
    @SerializedName("id") val id: Int,
    @SerializedName("company_name") val companyName: String
)

data class DeliveryZone(
    @SerializedName("id") val id: Int,
    @SerializedName("delivery_company_id") val deliveryCompanyId: Int,
    @SerializedName("website_id") val websiteId: Int?,
    @SerializedName("zone_name_ar") val zoneNameAr: String?,
    @SerializedName("zone_name_en") val zoneNameEn: String?,
    @SerializedName("price") val price: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("image_path") val imagePath: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class RestaurantInfo(
    @SerializedName("restaurant_name") val restaurantName: String?,
    @SerializedName("restaurant_name_ar") val restaurantNameAr: String?
)

data class RestaurantZonesResponse(
    @SerializedName("deliveryCompany") val deliveryCompany: DeliveryCompany?,
    @SerializedName("restaurant") val restaurant: RestaurantInfo?,
    @SerializedName("zones") val zones: List<DeliveryZone>
)
