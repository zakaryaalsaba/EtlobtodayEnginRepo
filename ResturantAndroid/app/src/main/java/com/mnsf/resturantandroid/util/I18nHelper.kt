package com.mnsf.resturantandroid.util

import android.content.Context
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.data.model.ProductAddon
import com.mnsf.resturantandroid.data.model.Restaurant
import com.mnsf.resturantandroid.data.model.Branch
import com.mnsf.resturantandroid.data.model.Region
import com.mnsf.resturantandroid.data.model.Area
import com.mnsf.resturantandroid.data.model.DeliveryZone

/**
 * Returns the display string for the current locale: Arabic (_ar) when locale is "ar" and _ar is non-empty, else English/default.
 */
object I18nHelper {

    fun isArabic(context: Context): Boolean =
        LocaleHelper.getLocale(context).startsWith("ar", ignoreCase = true)

    fun getRestaurantNameDisplay(restaurant: Restaurant, context: Context): String =
        if (isArabic(context) && !restaurant.restaurant_name_ar.isNullOrBlank())
            restaurant.restaurant_name_ar
        else
            restaurant.restaurant_name

    fun getRestaurantDescriptionDisplay(restaurant: Restaurant, context: Context): String? =
        if (isArabic(context) && !restaurant.description_ar.isNullOrBlank())
            restaurant.description_ar
        else
            restaurant.description

    fun getRestaurantAddressDisplay(restaurant: Restaurant, context: Context): String? =
        if (isArabic(context) && !restaurant.address_ar.isNullOrBlank())
            restaurant.address_ar
        else
            restaurant.address

    fun getProductNameDisplay(product: Product, context: Context): String =
        if (isArabic(context) && !product.name_ar.isNullOrBlank())
            product.name_ar
        else
            product.name

    fun getProductDescriptionDisplay(product: Product, context: Context): String? =
        if (isArabic(context) && !product.description_ar.isNullOrBlank())
            product.description_ar
        else
            product.description

    fun getProductCategoryDisplay(product: Product, context: Context): String {
        val fallback = product.category?.takeIf { it.isNotBlank() } ?: return "" // caller should substitute menu string
        return if (isArabic(context) && !product.category_ar.isNullOrBlank())
            product.category_ar
        else
            fallback
    }

    fun getAddonNameDisplay(addon: ProductAddon, context: Context): String =
        if (isArabic(context) && !addon.name_ar.isNullOrBlank())
            addon.name_ar
        else
            addon.name

    fun getAddonDescriptionDisplay(addon: ProductAddon, context: Context): String? =
        if (isArabic(context) && !addon.description_ar.isNullOrBlank())
            addon.description_ar
        else
            addon.description

    fun getBranchNameDisplay(branch: Branch, context: Context): String {
        val name = if (isArabic(context) && !branch.name_ar.isNullOrBlank())
            branch.name_ar
        else
            branch.name
        
        return name?.ifBlank { null } ?: "Branch ${branch.branch_number}"
    }

    fun getRegionNameDisplay(region: Region, context: Context): String {
        return if (isArabic(context) && !region.name_ar.isNullOrBlank())
            region.name_ar
        else
            region.name
    }

    fun getAreaNameDisplay(area: Area, context: Context): String {
        return if (isArabic(context) && !area.name_ar.isNullOrBlank())
            area.name_ar
        else
            area.name
    }

    fun getZoneNameDisplay(zone: DeliveryZone, context: Context): String {
        return if (isArabic(context) && !zone.zone_name_ar.isNullOrBlank())
            zone.zone_name_ar
        else
            zone.zone_name_en ?: ""
    }
}
