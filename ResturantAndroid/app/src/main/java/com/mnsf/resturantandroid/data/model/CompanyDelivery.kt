package com.mnsf.resturantandroid.data.model

/**
 * Company Delivery Model
 * 
 * Represents a delivery company that restaurants can partner with for delivery services.
 * Delivery companies manage zones, areas, and regions for their delivery operations.
 * 
 * Approval Process:
 * 1. Restaurant admin requests partnership with a delivery company via Order Type Settings > Delivery Company
 * 2. Delivery company admin reviews and approves/rejects the request
 * 3. Once approved, restaurant's delivery_company_id is set in restaurant_websites table
 * 4. Restaurant can then use the delivery company's zones for customer address selection
 * 5. Delivery fees are calculated based on the selected zone's price
 * 
 * Future Enhancements:
 * - Multiple delivery companies per restaurant (restaurant can choose which company to use per order)
 * - Dynamic pricing based on distance/time
 * - Delivery company-specific features (tracking, driver assignment, etc.)
 */
data class CompanyDelivery(
    val id: Int,
    val company_name: String,
    val company_name_ar: String? = null,
    val status: String? = null, // active, inactive
    val created_at: String? = null,
    val updated_at: String? = null
)

data class CompanyDeliveryRequest(
    val id: Int,
    val website_id: Int,
    val delivery_company_id: Int,
    val status: String, // pending, approved, rejected
    val company_name: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
