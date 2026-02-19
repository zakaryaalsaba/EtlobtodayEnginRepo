package com.mnsf.resturantandroid.data.model

data class DeliveryZone(
    val id: Int,
    val delivery_company_id: Int,
    val area_id: Int,
    val zone_name_id: Int,
    val price: Double? = null,
    val status: String,
    val zone_name_ar: String? = null,
    val zone_name_en: String? = null,
    val area_name: String? = null,
    val area_name_ar: String? = null,
    val region_id: Int? = null,
    val region_name: String? = null,
    val region_name_ar: String? = null,
    val city_id: Int? = null,
    val city_name: String? = null,
    val city_name_ar: String? = null
)

data class DeliveryZonesResponse(
    val zones: List<DeliveryZone>
)
