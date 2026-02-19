package com.mnsf.resturantandroid.data.model

data class Branch(
    val id: Int,
    val website_id: Int,
    val branch_number: Int,
    val name: String? = null,
    val name_ar: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val status: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val region_name: String? = null,
    val region_name_ar: String? = null,
    val city_name: String? = null,
    val city_name_ar: String? = null
)

data class BranchesResponse(
    val branches: List<Branch>
)
