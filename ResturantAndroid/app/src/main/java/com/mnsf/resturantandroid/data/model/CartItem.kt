package com.mnsf.resturantandroid.data.model

/**
 * @param unitPriceOverride When non-null, used instead of product.price for subtotal (e.g. offer discount).
 * @param addonPriceOverrides When non-null, map of addon id -> price used for subtotal (e.g. offer discount).
 */
data class CartItem(
    val product: Product,
    var quantity: Int,
    val selectedAddons: List<ProductAddon> = emptyList(),
    val unitPriceOverride: Double? = null,
    val addonPriceOverrides: Map<Int, Double>? = null
) {
    fun getSubtotal(): Double {
        val productUnitPrice = unitPriceOverride ?: product.price
        val productTotal = productUnitPrice * quantity
        val addonsTotal = selectedAddons.sumOf { addon ->
            addonPriceOverrides?.get(addon.id) ?: addon.price
        } * quantity
        return kotlin.math.round((productTotal + addonsTotal) * 100.0) / 100.0
    }

    /** Sorted addon ids for matching cart lines (same product + same addons = same line). */
    fun addonIdsSorted(): List<Int> = selectedAddons.map { it.id }.sorted()
}

