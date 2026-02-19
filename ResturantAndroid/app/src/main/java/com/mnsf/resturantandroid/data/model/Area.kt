package com.mnsf.resturantandroid.data.model

data class Area(
    val id: Int,
    val region_id: Int,
    val name: String,
    val name_ar: String? = null,
    val region_name: String? = null,
    val region_name_ar: String? = null,
    val city_id: Int? = null,
    val city_name: String? = null,
    val city_name_ar: String? = null
)

data class AreasResponse(
    val areas: List<Area>
)
