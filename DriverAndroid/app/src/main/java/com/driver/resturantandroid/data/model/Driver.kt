package com.driver.resturantandroid.data.model

data class Driver(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val isOnline: Boolean = false,
    val status: String? = null, // pending, approved, rejected
    val image_url: String? = null // Profile image URL
)

data class DriverLoginRequest(
    val email: String,
    val password: String
)

data class DriverLoginResponse(
    val driver: Driver,
    val token: String? = null,
    val message: String? = null
)

data class DriverRegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

data class DriverRegisterResponse(
    val driver: Driver,
    val message: String
)

