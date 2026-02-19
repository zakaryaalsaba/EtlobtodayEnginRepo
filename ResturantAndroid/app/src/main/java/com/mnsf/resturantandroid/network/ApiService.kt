package com.mnsf.resturantandroid.network

import com.mnsf.resturantandroid.data.model.*
import com.mnsf.resturantandroid.data.model.Branch
import com.mnsf.resturantandroid.data.model.BranchesResponse
import com.mnsf.resturantandroid.data.model.Region
import com.mnsf.resturantandroid.data.model.RegionsResponse
import com.mnsf.resturantandroid.data.model.Area
import com.mnsf.resturantandroid.data.model.AreasResponse
import com.mnsf.resturantandroid.data.model.DeliveryZone
import com.mnsf.resturantandroid.data.model.DeliveryZonesResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/login/phone")
    suspend fun loginWithPhone(@Body request: PhoneLoginRequest): Response<AuthResponse>
    
    @GET("auth/me")
    suspend fun getCurrentCustomer(@Header("Authorization") token: String): Response<CustomerResponse>
    
    // Restaurants (openNow=true: only restaurants operating now per business hours)
    @GET("websites")
    suspend fun getRestaurants(@Query("open_now") openNow: Boolean? = null): Response<RestaurantsResponse>
    
    @GET("websites/{id}")
    suspend fun getRestaurant(@Path("id") id: Int): Response<WebsiteResponse>

    /** All active offers from all restaurants (valid dates, is_active). For home Offers section. */
    @GET("websites/offers/list")
    suspend fun getOffersList(): Response<OffersResponse>

    /** Active offers for a single restaurant (for detail screen discounts). */
    @GET("websites/{id}/offers")
    suspend fun getOffersByWebsiteId(@Path("id") websiteId: Int): Response<OffersResponse>
    
    /** Get all active branches for a restaurant (public endpoint for customers). */
    @GET("websites/{id}/branches")
    suspend fun getRestaurantBranches(@Path("id") websiteId: Int): Response<BranchesResponse>
    
    /** Get regions for a restaurant's delivery company (public endpoint for customers). */
    @GET("websites/{id}/regions")
    suspend fun getRestaurantRegions(@Path("id") websiteId: Int): Response<RegionsResponse>
    
    /** Get areas for a restaurant's delivery company, filtered by region_id (public endpoint for customers). */
    @GET("websites/{id}/areas")
    suspend fun getRestaurantAreas(
        @Path("id") websiteId: Int,
        @Query("region_id") regionId: Int
    ): Response<AreasResponse>
    
    /** Get delivery zones for a restaurant's delivery company, filtered by area_id (public endpoint for customers). */
    @GET("websites/{id}/zones")
    suspend fun getRestaurantZones(
        @Path("id") websiteId: Int,
        @Query("area_id") areaId: Int
    ): Response<DeliveryZonesResponse>
    
    // Products
    @GET("products/website/{websiteId}")
    suspend fun getProducts(@Path("websiteId") websiteId: Int): Response<ProductsResponse>
    
    @GET("products/{productId}/addons")
    suspend fun getProductAddons(@Path("productId") productId: Int): Response<ProductAddonsResponse>
    
    // Orders
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderResponse>
    
    @POST("orders/process-payment-with-token")
    suspend fun processPaymentWithToken(@Body request: ProcessPaymentWithTokenRequest): Response<ProcessPaymentWithTokenResponse>
    
    @GET("orders/{orderNumber}")
    suspend fun getOrderByNumber(@Path("orderNumber") orderNumber: String): Response<OrderResponse>
    
    @GET("customers/{customerId}/orders")
    suspend fun getCustomerOrders(
        @Path("customerId") customerId: Int,
        @Header("Authorization") token: String
    ): Response<OrdersResponse>

    // Settings (active service fee for checkout)
    @GET("settings")
    suspend fun getSettings(): Response<SettingsResponse>
    
    // Notifications
    @GET("customers/{customerId}/notifications")
    suspend fun getCustomerNotifications(
        @Path("customerId") customerId: Int,
        @Header("Authorization") token: String
    ): Response<NotificationsResponse>
    
    @PUT("customers/{customerId}/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("customerId") customerId: Int,
        @Path("notificationId") notificationId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse>
    
    @PUT("customers/{customerId}/notifications/read-all")
    suspend fun markAllNotificationsAsRead(
        @Path("customerId") customerId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse>
    
    // Device token and location
    @PUT("customers/{customerId}/device-token")
    suspend fun updateDeviceToken(
        @Path("customerId") customerId: Int,
        @Body request: DeviceTokenRequest,
        @Header("Authorization") token: String
    ): Response<ApiResponse>
    
    @PUT("customers/{customerId}/location")
    suspend fun updateLocation(
        @Path("customerId") customerId: Int,
        @Body request: LocationRequest,
        @Header("Authorization") token: String
    ): Response<LocationResponse>
    
    @GET("customers/{customerId}/location")
    suspend fun getCustomerLocation(
        @Path("customerId") customerId: Int,
        @Header("Authorization") token: String
    ): Response<LocationResponse>
    
    // Customer Profile
    @GET("customers/{customerId}")
    suspend fun getCustomerProfile(
        @Path("customerId") customerId: Int,
        @Header("Authorization") token: String
    ): Response<CustomerProfileResponse>
    
    @PUT("customers/{customerId}")
    suspend fun updateCustomerProfile(
        @Path("customerId") customerId: Int,
        @Body request: UpdateCustomerProfileRequest,
        @Header("Authorization") token: String
    ): Response<CustomerProfileResponse>
    
    @Multipart
    @POST("customers/{customerId}/profile-picture")
    suspend fun uploadProfilePicture(
        @Path("customerId") customerId: Int,
        @Part profile_picture: okhttp3.MultipartBody.Part,
        @Header("Authorization") token: String
    ): Response<CustomerProfileResponse>

    // Addresses (multiple per customer)
    @GET("customers/{customerId}/addresses")
    suspend fun getAddresses(
        @Path("customerId") customerId: Int,
        @Header("Authorization") token: String
    ): Response<AddressesResponse>

    @GET("customers/{customerId}/addresses/{addressId}")
    suspend fun getAddress(
        @Path("customerId") customerId: Int,
        @Path("addressId") addressId: Int,
        @Header("Authorization") token: String
    ): Response<AddressResponse>

    @POST("customers/{customerId}/addresses")
    suspend fun createAddress(
        @Path("customerId") customerId: Int,
        @Body request: CreateAddressRequest,
        @Header("Authorization") token: String
    ): Response<AddressResponse>

    @PUT("customers/{customerId}/addresses/{addressId}")
    suspend fun updateAddress(
        @Path("customerId") customerId: Int,
        @Path("addressId") addressId: Int,
        @Body request: UpdateAddressRequest,
        @Header("Authorization") token: String
    ): Response<AddressResponse>

    @DELETE("customers/{customerId}/addresses/{addressId}")
    suspend fun deleteAddress(
        @Path("customerId") customerId: Int,
        @Path("addressId") addressId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse>
    
    // Payment Methods (saved credit cards)
    @POST("customers/{customerId}/payment-methods")
    suspend fun savePaymentMethod(
        @Path("customerId") customerId: Int,
        @Body request: SavePaymentMethodRequest,
        @Header("Authorization") token: String
    ): Response<PaymentMethodResponse>
    
    @GET("customers/{customerId}/payment-methods")
    suspend fun getPaymentMethods(
        @Path("customerId") customerId: Int,
        @Header("Authorization") token: String
    ): Response<PaymentMethodsResponse>
    
    @DELETE("customers/{customerId}/payment-methods/{paymentMethodId}")
    suspend fun deletePaymentMethod(
        @Path("customerId") customerId: Int,
        @Path("paymentMethodId") paymentMethodId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse>
}

data class ApiResponse(
    val success: Boolean,
    val message: String
)

data class CustomerResponse(
    val customer: Customer
)

data class DeviceTokenRequest(
    val device_token: String,
    val device_type: String = "android"
)

data class LocationRequest(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

data class LocationResponse(
    val success: Boolean,
    val message: String,
    val location: LocationData? = null
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

data class CustomerProfileResponse(
    val success: Boolean? = null,
    val customer: Customer,
    val message: String? = null
)

data class UpdateCustomerProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val profile_picture_url: String? = null
)

data class PhoneLoginRequest(
    val phone: String,
    val firebase_token: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null
)

// Address (from addresses table)
data class Address(
    val id: Int,
    val customer_id: Int,
    val area: String? = null,
    val region_id: Int? = null,
    val region_name: String? = null,
    val area_id: Int? = null,
    val area_name: String? = null,
    val zone_id: Int? = null,
    val zone_name: String? = null,
    val zone_price: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address_type: String? = null,
    val building_name: String? = null,
    val apartment_number: String? = null,
    val floor: String? = null,
    val street: String? = null,
    val phone_country_code: String? = null,
    val phone_number: String? = null,
    val additional_directions: String? = null,
    val address_label: String? = null,
    val is_default: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class AddressesResponse(val addresses: List<Address>)
data class AddressResponse(val address: Address)

data class SettingsResponse(
    val service_fee: Double = 0.0,
    val id: Int? = null,
    val status: String? = null
)

data class CreateAddressRequest(
    val area: String? = null,
    val region_id: Int? = null,
    val region_name: String? = null,
    val area_id: Int? = null,
    val area_name: String? = null,
    val zone_id: Int? = null,
    val zone_name: String? = null,
    val zone_price: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address_type: String? = null,
    val building_name: String? = null,
    val apartment_number: String? = null,
    val floor: String? = null,
    val street: String? = null,
    val phone_country_code: String? = null,
    val phone_number: String? = null,
    val additional_directions: String? = null,
    val address_label: String? = null,
    val is_default: Boolean = false
)

data class UpdateAddressRequest(
    val area: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address_type: String? = null,
    val building_name: String? = null,
    val apartment_number: String? = null,
    val floor: String? = null,
    val street: String? = null,
    val phone_country_code: String? = null,
    val phone_number: String? = null,
    val additional_directions: String? = null,
    val address_label: String? = null,
    val is_default: Boolean? = null
)

// Payment Methods
data class SavePaymentMethodRequest(
    val token: String,
    val card_last4: String,
    val card_brand: String? = null,
    val expiry_month: Int? = null,
    val expiry_year: Int? = null,
    val paytabs_response_json: String? = null,
    val is_default: Boolean = false
)data class PaymentMethod(
    val id: Int,
    val customer_id: Int,
    val token: String,
    val card_last4: String,
    val card_brand: String? = null,
    val expiry_month: Int? = null,
    val expiry_year: Int? = null,
    val is_default: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class PaymentMethodResponse(
    val payment_method: PaymentMethod
)

data class PaymentMethodsResponse(
    val payment_methods: List<PaymentMethod>
)

// Process payment with saved token
data class ProcessPaymentWithTokenRequest(
    val profile_id: String,
    val server_key: String? = null, // Optional: will use env var on backend if not provided
    val token: String,
    val amount: Double,
    val currency: String,
    val cart_id: String,
    val cart_description: String? = null,
    val customer_details: Map<String, Any>? = null
)

data class ProcessPaymentWithTokenResponse(
    val success: Boolean,
    val transaction_reference: String? = null,
    val payment_result: Map<String, Any>? = null,
    val payment_info: Map<String, Any>? = null,
    val error: String? = null,
    val message: String? = null
)
