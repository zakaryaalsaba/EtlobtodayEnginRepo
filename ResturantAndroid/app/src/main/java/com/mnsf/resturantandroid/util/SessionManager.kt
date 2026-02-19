package com.mnsf.resturantandroid.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "RestaurantAppPrefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_CUSTOMER_ID = "customer_id"
        private const val KEY_CUSTOMER_NAME = "customer_name"
        private const val KEY_CUSTOMER_EMAIL = "customer_email"
        private const val KEY_CUSTOMER_PHONE = "customer_phone"
        private const val KEY_CUSTOMER_ADDRESS = "customer_address"
        private const val KEY_DELIVERY_LABEL = "delivery_label"
        private const val KEY_ORDER_TYPE = "order_type"
    }
    
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun saveCustomerInfo(id: Int, name: String, email: String?, phone: String? = null, address: String? = null) {
        prefs.edit()
            .putInt(KEY_CUSTOMER_ID, id)
            .putString(KEY_CUSTOMER_NAME, name)
            .putString(KEY_CUSTOMER_EMAIL, email ?: "")
            .putString(KEY_CUSTOMER_PHONE, phone)
            .putString(KEY_CUSTOMER_ADDRESS, address)
            .apply()
    }
    
    fun getCustomerId(): Int {
        return prefs.getInt(KEY_CUSTOMER_ID, -1)
    }
    
    fun getCustomerName(): String? {
        return prefs.getString(KEY_CUSTOMER_NAME, null)
    }
    
    fun getCustomerEmail(): String? {
        return prefs.getString(KEY_CUSTOMER_EMAIL, null)
    }
    
    fun getCustomerPhone(): String? {
        return prefs.getString(KEY_CUSTOMER_PHONE, null)
    }
    
    fun getCustomerAddress(): String? {
        return prefs.getString(KEY_CUSTOMER_ADDRESS, null)
    }

    fun saveDeliveryLabel(label: String?) {
        prefs.edit().putString(KEY_DELIVERY_LABEL, label ?: "").apply()
    }

    /** Saves only address and label (e.g. for guest when using current location). */
    fun saveDeliveryAddressOnly(address: String?, label: String?) {
        prefs.edit()
            .putString(KEY_CUSTOMER_ADDRESS, address ?: "")
            .putString(KEY_DELIVERY_LABEL, label ?: "")
            .apply()
    }

    fun getDeliveryLabel(): String? {
        val v = prefs.getString(KEY_DELIVERY_LABEL, null)
        return if (v.isNullOrEmpty()) null else v
    }

    fun saveOrderType(type: String) {
        prefs.edit().putString(KEY_ORDER_TYPE, type).apply()
    }

    fun getOrderType(): String {
        return prefs.getString(KEY_ORDER_TYPE, "delivery") ?: "delivery"
    }
    
    fun isLoggedIn(): Boolean {
        return getAuthToken() != null && getCustomerId() != -1
    }
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}

