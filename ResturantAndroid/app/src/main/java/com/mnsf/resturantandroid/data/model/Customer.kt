package com.mnsf.resturantandroid.data.model

data class Customer(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,
    val profile_picture_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class AuthResponse(
    val customer: Customer,
    val token: String,
    val message: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

