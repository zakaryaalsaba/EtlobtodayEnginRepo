package com.mnsf.resturantandroid.data.model

data class Region(
    val id: Int,
    val city_id: Int,
    val name: String,
    val name_ar: String? = null,
    val city_name: String? = null,
    val city_name_ar: String? = null
)

data class RegionsResponse(
    val regions: List<Region>
)
