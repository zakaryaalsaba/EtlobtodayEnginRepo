package com.driver.resturantandroid.util

import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    
    /**
     * Format amount with currency symbol based on currency code and position
     */
    fun formatAmount(
        amount: String,
        currencyCode: String? = "USD",
        symbolPosition: String? = "before"
    ): String {
        try {
            // Remove any existing currency symbols and whitespace
            val cleanAmount = amount.replace(Regex("[^0-9.]"), "").trim()
            val amountValue = cleanAmount.toDoubleOrNull() ?: return amount
            
            // Get currency symbol
            val currency = try {
                Currency.getInstance(currencyCode ?: "USD")
            } catch (e: Exception) {
                Currency.getInstance("USD")
            }
            
            val symbol = currency.symbol
            
            // Format based on position
            return when (symbolPosition?.lowercase()) {
                "after" -> String.format("%.2f %s", amountValue, symbol)
                else -> String.format("%s %.2f", symbol, amountValue)
            }
        } catch (e: Exception) {
            // Fallback to original amount if formatting fails
            return amount
        }
    }
    
    /**
     * Get currency symbol for a currency code
     */
    fun getCurrencySymbol(currencyCode: String?): String {
        return try {
            Currency.getInstance(currencyCode ?: "USD").symbol
        } catch (e: Exception) {
            "$"
        }
    }
}
