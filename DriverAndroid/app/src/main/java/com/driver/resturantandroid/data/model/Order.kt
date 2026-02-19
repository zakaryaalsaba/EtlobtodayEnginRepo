package com.driver.resturantandroid.data.model

data class OrderItem(
    val id: Int,
    val product_id: Int,
    val product_name: String,
    val product_price: String,
    val quantity: Int,
    val subtotal: String
)

data class Order(
    val id: Int,
    val website_id: Int,
    val customer_id: Int?,
    val order_number: String,
    val customer_name: String,
    val customer_email: String?,
    val customer_phone: String,
    val customer_address: String?,
    val order_type: String,
    val status: String,
    val total_amount: String,
    val payment_status: String,
    val payment_method: String?,
    val delivery_latitude: Double?,
    val delivery_longitude: Double?,
    val items: List<OrderItem>?,
    val created_at: String,
    val updated_at: String,
    val restaurant: RestaurantInfo? = null,
    val currency_code: String? = "USD",
    val currency_symbol_position: String? = "before",
    val delivery_fees: String? = null,
    val tip: String? = null
)

data class RestaurantInfo(
    val name: String?,
    val phone: String?,
    val logo_url: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class OrderStatusUpdate(
    val status: String
)

data class Restaurant(
    val id: Int,
    val restaurant_name: String,
    val address: String?,
    val phone: String?,
    val latitude: Double? = null,
    val longitude: Double? = null
)

