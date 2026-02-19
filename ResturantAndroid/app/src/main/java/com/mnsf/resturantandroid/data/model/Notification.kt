package com.mnsf.resturantandroid.data.model

import com.google.gson.annotations.SerializedName

data class Notification(
    val id: Int,
    @SerializedName("customer_id")
    val customerId: Int?,
    @SerializedName("order_id")
    val orderId: Int?,
    @SerializedName("website_id")
    val websiteId: Int?,
    val title: String,
    val message: String,
    val type: String = "order_update",
    val status: String? = null,
    @SerializedName("is_read")
    val isRead: Boolean = false,
    @SerializedName("restaurant_name")
    val restaurantName: String? = null,
    @SerializedName("order_number")
    val orderNumber: String? = null,
    @SerializedName("created_at")
    val createdAt: String
)

data class NotificationsResponse(
    val notifications: List<Notification>
)

