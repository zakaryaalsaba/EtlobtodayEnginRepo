package com.mnsf.resturantandroid.data.model

data class Restaurant(
    val id: Int,
    val restaurant_name: String,
    val restaurant_name_ar: String? = null,
    val logo_url: String? = null,
    val description: String? = null,
    val description_ar: String? = null,
    val address: String? = null,
    val address_ar: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val primary_color: String? = null,
    val secondary_color: String? = null,
    val is_published: Boolean = false,
    val payment_methods: String? = null, // JSON string
    val currency_code: String? = null,
    val currency_symbol_position: String? = null, // "before" or "after"
    val tax_enabled: Boolean? = false,
    val tax_rate: Double? = 0.0,
    val delivery_fee: Double? = 0.0,
    val delivery_time_min: Int? = null,
    val delivery_time_max: Int? = null,
    val order_type_dine_in_enabled: Boolean? = true,
    val order_type_pickup_enabled: Boolean? = true,
    val order_type_delivery_enabled: Boolean? = true,
    val delivery_company_id: Int? = null,
    val created_at: String? = null,
    /** Restaurant location for driver navigation (from API when available). */
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class CliQServices(
    val enabled: Boolean = false,
    val phone: String? = null,
    val name: String? = null
)

data class PaymentMethods(
    val cashOnPickup: Boolean = true,
    val cashOnDelivery: Boolean = true,
    val creditCard: Boolean = false,
    val onlinePayment: Boolean = false,
    val mobilePayment: Boolean = false,
    val cliQServices: CliQServices? = null
)

data class RestaurantsResponse(
    val websites: List<Restaurant>
)

data class WebsiteResponse(
    val website: Restaurant
)

/** Offer from a restaurant (for home "Offers" section and detail discounts). */
data class Offer(
    val id: Int,
    val website_id: Int,
    val offer_type: String,
    val title: String,
    val description: String? = null,
    val value: Double? = null,
    val min_order_value: Double? = null,
    val valid_from: String? = null,
    val valid_until: String? = null,
    val display_order: Int? = 0,
    val restaurant_name: String? = null,
    val logo_url: String? = null,
    /** First product image of the restaurant (for offer card). */
    val first_product_image_url: String? = null,
    /** For percent_off: "all_items" or "selected_items". */
    val offer_scope: String? = null,
    /** For selected_items: JSON array of product ids, or null. */
    val selected_product_ids: Any? = null,
    /** For selected_items: JSON array of addon ids, or null. */
    val selected_addon_ids: Any? = null
) {
    /** Parsed product IDs for selected_items scope; empty if all_items or invalid. */
    fun getSelectedProductIds(): List<Int> = when (selected_product_ids) {
        is List<*> -> (selected_product_ids as List<*>).mapNotNull { (it as? Number)?.toInt() }
        is String -> try {
            org.json.JSONArray(selected_product_ids).let { arr -> (0 until arr.length()).mapNotNull { arr.opt(it)?.let { v -> (v as? Number)?.toInt() } } }
        } catch (_: Exception) { emptyList() }
        else -> emptyList()
    }

    /** Parsed addon IDs for selected_items scope; empty if all_items or invalid. */
    fun getSelectedAddonIds(): List<Int> = when (selected_addon_ids) {
        is List<*> -> (selected_addon_ids as List<*>).mapNotNull { (it as? Number)?.toInt() }
        is String -> try {
            org.json.JSONArray(selected_addon_ids).let { arr -> (0 until arr.length()).mapNotNull { arr.opt(it)?.let { v -> (v as? Number)?.toInt() } } }
        } catch (_: Exception) { emptyList() }
        else -> emptyList()
    }
}data class OffersResponse(
    val offers: List<Offer>
)
