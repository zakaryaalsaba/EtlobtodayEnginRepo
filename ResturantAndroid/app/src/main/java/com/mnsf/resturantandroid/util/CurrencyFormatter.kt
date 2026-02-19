package com.mnsf.resturantandroid.util

import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    
    /**
     * Format a price with currency symbol based on currency code and position
     * @param amount The price amount
     * @param currencyCode Currency code (e.g., "USD", "JOD", "EUR")
     * @param symbolPosition "before" or "after" (defaults to "before" if null)
     * @return Formatted price string with currency symbol
     */
    fun formatPrice(amount: Double, currencyCode: String?, symbolPosition: String?): String {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        
        // Default to USD if currency code is null or empty
        val code = currencyCode?.takeIf { it.isNotBlank() } ?: "USD"
        val position = symbolPosition?.lowercase() ?: "before"
        
        // Get currency symbol
        val symbol = try {
            val currency = Currency.getInstance(code)
            currency.symbol
        } catch (e: Exception) {
            // Fallback to common symbols
            when (code.uppercase()) {
                "USD" -> "$"
                "JOD" -> "د.ا"
                "EUR" -> "€"
                "GBP" -> "£"
                else -> code
            }
        }
        
        // Format based on position
        return when (position) {
            "after" -> "$formattedAmount $symbol"
            else -> "$symbol$formattedAmount"
        }
    }
    
    /**
     * Get currency symbol for a given currency code
     */
    fun getCurrencySymbol(currencyCode: String?): String {
        val code = currencyCode?.takeIf { it.isNotBlank() } ?: "USD"
        return try {
            val currency = Currency.getInstance(code)
            currency.symbol
        } catch (e: Exception) {
            when (code.uppercase()) {
                "USD" -> "$"
                "JOD" -> "د.ا"
                "EUR" -> "€"
                "GBP" -> "£"
                else -> code
            }
        }
    }
}

