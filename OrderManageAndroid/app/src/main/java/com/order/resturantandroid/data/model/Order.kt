package com.order.resturantandroid.data.model

import com.google.gson.annotations.SerializedName

data class Order(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("order_number") val orderNumber: String = "",
    @SerializedName("customer_name") val customerName: String = "",
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("customer_address") val customerAddress: String? = null,
    @SerializedName("order_type") val orderType: String = "delivery", // "pickup" or "delivery"
    @SerializedName("status") val status: String = "pending",
    @SerializedName("total_amount") val totalAmount: String = "0.00",
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("items") val items: List<OrderItem>? = null,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("delivery_latitude") val deliveryLatitude: Double? = null,
    @SerializedName("delivery_longitude") val deliveryLongitude: Double? = null,
    @SerializedName("currency_code") val currencyCode: String? = "USD",
    @SerializedName("currency_symbol_position") val currencySymbolPosition: String? = "before",
    @SerializedName("tax") val tax: String? = null,
    @SerializedName("delivery_fees") val deliveryFees: String? = null
) {
    // Helper function to get items list safely
    fun getItemsList(): List<OrderItem> {
        return items ?: emptyList()
    }
    
    // Calculate subtotal from items
    fun getSubtotal(): Double {
        return items?.sumOf { item ->
            item.subtotal.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        } ?: 0.0
    }
}

data class OrderItem(
    @SerializedName("id") val id: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("product_price") val productPrice: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("subtotal") val subtotal: String
)

data class OrdersResponse(
    @SerializedName("orders") val orders: List<Order>
)

data class OrderStatusUpdate(
    @SerializedName("status") val status: String
)

