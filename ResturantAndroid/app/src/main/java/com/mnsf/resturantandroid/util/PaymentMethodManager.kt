package com.mnsf.resturantandroid.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mnsf.resturantandroid.data.model.SavedPaymentMethod

/**
 * Manages saved payment methods for faster checkout.
 * Stores payment methods in SharedPreferences using Gson serialization.
 */
class PaymentMethodManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val TAG = "PaymentMethodManager"
        private const val PREFS_NAME = "PaymentMethodPrefs"
        private const val KEY_SAVED_METHODS = "saved_payment_methods"
        private const val MAX_SAVED_METHODS = 5 // Maximum number of saved cards
    }
    
    /**
     * Saves a payment method. If it already exists (same token), updates it.
     * Removes oldest method if max limit reached.
     */
    fun savePaymentMethod(method: SavedPaymentMethod) {
        try {
            val methods = getSavedPaymentMethods().toMutableList()
            
            // Remove existing method with same token if exists
            methods.removeAll { it.token == method.token }
            
            // Add new method at the beginning (most recent first)
            methods.add(0, method)
            
            // Keep only the most recent MAX_SAVED_METHODS
            if (methods.size > MAX_SAVED_METHODS) {
                methods.removeAt(methods.size - 1)
            }
            
            val json = gson.toJson(methods)
            prefs.edit().putString(KEY_SAVED_METHODS, json).apply()
            Log.d(TAG, "Saved payment method: ${method.cardLast4Digits}, total methods: ${methods.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving payment method", e)
        }
    }
    
    /**
     * Gets all saved payment methods, sorted by most recent first.
     */
    fun getSavedPaymentMethods(): List<SavedPaymentMethod> {
        return try {
            val json = prefs.getString(KEY_SAVED_METHODS, null)
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<SavedPaymentMethod>>() {}.type
                gson.fromJson<List<SavedPaymentMethod>>(json, type) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved payment methods", e)
            emptyList()
        }
    }
    
    /**
     * Removes a saved payment method by token.
     */
    fun removePaymentMethod(token: String) {
        try {
            val methods = getSavedPaymentMethods().toMutableList()
            methods.removeAll { it.token == token }
            val json = gson.toJson(methods)
            prefs.edit().putString(KEY_SAVED_METHODS, json).apply()
            Log.d(TAG, "Removed payment method: $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing payment method", e)
        }
    }
    
    /**
     * Clears all saved payment methods.
     */
    fun clearAllPaymentMethods() {
        prefs.edit().remove(KEY_SAVED_METHODS).apply()
        Log.d(TAG, "Cleared all payment methods")
    }
    
    /**
     * Gets a payment method by token.
     */
    fun getPaymentMethodByToken(token: String): SavedPaymentMethod? {
        return getSavedPaymentMethods().firstOrNull { it.token == token }
    }
    
    /**
     * Debug method to check if payment methods are being saved.
     * Call this after a payment to verify.
     */
    fun debugPrintSavedMethods() {
        val methods = getSavedPaymentMethods()
        Log.d(TAG, "=== DEBUG: Saved Payment Methods ===")
        Log.d(TAG, "Total saved methods: ${methods.size}")
        methods.forEachIndexed { index, method ->
            Log.d(TAG, "  [$index] Card: ****${method.cardLast4Digits}, Brand: ${method.cardBrand ?: "Unknown"}, Token: ${method.token.take(20)}...")
        }
        Log.d(TAG, "===================================")
    }
}
