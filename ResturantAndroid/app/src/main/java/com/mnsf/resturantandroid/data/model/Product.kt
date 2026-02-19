package com.mnsf.resturantandroid.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: Int,
    val website_id: Int,
    val name: String,
    val name_ar: String? = null,
    val description: String? = null,
    val description_ar: String? = null,
    val price: Double,
    val category: String? = null,
    val category_ar: String? = null,
    val image_url: String? = null,
    val is_available: Boolean = true,
    val addon_required: Boolean? = false,
    val addon_required_min: Int? = null,
    val created_at: String? = null
) : Parcelable

@Parcelize
data class ProductAddon(
    val id: Int,
    val product_id: Int,
    val name: String,
    val name_ar: String? = null,
    val description: String? = null,
    val description_ar: String? = null,
    val image_url: String? = null,
    val price: Double,
    val is_required: Boolean = false,
    val display_order: Int = 0
) : Parcelable

data class ProductsResponse(
    val products: List<Product>
)

data class ProductAddonsResponse(
    val addons: List<ProductAddon>,
    val addon_required: Boolean,
    val addon_required_min: Int?
)