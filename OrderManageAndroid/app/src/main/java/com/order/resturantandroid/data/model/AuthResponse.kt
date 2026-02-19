package com.order.resturantandroid.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("admin") val admin: Admin
)

data class Admin(
    @SerializedName("id") val id: Int,
    @SerializedName("website_id") val websiteId: Int,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String?,
    @SerializedName("restaurant_name") val restaurantName: String
)

