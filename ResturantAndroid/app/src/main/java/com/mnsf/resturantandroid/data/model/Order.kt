package com.mnsf.resturantandroid.data.model

import java.io.Serializable

data class OrderItem(
    val id: Int? = null,
    val product_id: Int,
    val product_name: String,
    val product_price: Double,
    val quantity: Int,
    val subtotal: Double
) : Serializable

data class Order(
    val id: Int? = null,
    val website_id: Int,
    val customer_id: Int? = null,
    val order_number: String,
    val customer_name: String,
    val customer_email: String? = null,
    val customer_phone: String,
    val customer_address: String? = null,
    val order_type: String? = "pickup",
    val status: String,
    val total_amount: Double,
    val payment_method: String? = null,
    val payment_status: String? = "pending",
    val notes: String? = null,
    val tip: Double? = null,
    val delivery_instructions: String? = null,
    val service_fee: Double? = null,
    val created_at: String? = null,
    val items: List<OrderItem>? = null
) : Serializable

/** Restaurant info sent with order so Driver app can show pickup location. */
data class RestaurantInfoRequest(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class CreateOrderRequest(
    val website_id: Int,
    val customer_id: Int? = null,
    val customer_name: String,
    val customer_email: String? = null,
    val customer_phone: String,
    val customer_address: String? = null,
    val order_type: String = "pickup",
    val payment_method: String = "cash",
    val payment_intent_id: String? = null,
    val delivery_latitude: Double? = null,
    val delivery_longitude: Double? = null,
    val items: List<OrderItemRequest>,
    val notes: String? = null,
    val tip: Double? = null,
    val delivery_instructions: String? = null,
    /** Client-calculated total (matches checkout display). When set, backend uses it for order total. */
    @com.google.gson.annotations.SerializedName("total_amount") val total_amount: Double? = null,
    /** Restaurant info so backend can save it to Firebase for the Driver app. */
    val restaurant: RestaurantInfoRequest? = null
)

data class OrderItemRequest(
    val product_id: Int,
    val quantity: Int
)

data class OrdersResponse(
    val orders: List<Order>
)

data class OrderResponse(
    val order: Order
)

