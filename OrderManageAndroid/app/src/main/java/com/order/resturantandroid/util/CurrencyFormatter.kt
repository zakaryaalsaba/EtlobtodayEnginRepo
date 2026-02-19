package com.order.resturantandroid.util

import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    
    /**
     * Get currency symbol for a currency code
     * Uses custom mapping for better control over currency symbols
     */
    private fun getCurrencySymbol(currencyCode: String): String {
        // Custom currency symbol mapping for better control
        val symbolMap = mapOf(
            "JOD" to "JOD",
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "SAR" to "ر.س",
            "AED" to "د.إ",
            "EGP" to "E£",
            "KWD" to "د.ك",
            "BHD" to "د.ب",
            "OMR" to "ر.ع",
            "QAR" to "ر.ق",
            "TRY" to "₺",
            "JPY" to "¥",
            "CNY" to "¥",
            "INR" to "₹"
        )
        
        val code = currencyCode.trim().uppercase()
        return symbolMap[code] ?: run {
            // Fallback to Java Currency API
            try {
                Currency.getInstance(code).symbol
            } catch (e: Exception) {
                android.util.Log.w("CurrencyFormatter", "Unknown currency code: $code, using code as symbol")
                code // Use currency code itself as fallback
            }
        }
    }
    
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
            
            // Get currency symbol - use provided currency code
            val actualCurrencyCode = currencyCode?.trim()?.uppercase() ?: "USD"
            android.util.Log.d("CurrencyFormatter", "Formatting amount: $amountValue with currency: $actualCurrencyCode, position: $symbolPosition")
            
            val symbol = getCurrencySymbol(actualCurrencyCode)
            android.util.Log.d("CurrencyFormatter", "Currency symbol: $symbol")
            
            // Format based on position
            val formatted = when (symbolPosition?.lowercase()?.trim()) {
                "after" -> String.format("%.2f %s", amountValue, symbol)
                else -> String.format("%s %.2f", symbol, amountValue)
            }
            android.util.Log.d("CurrencyFormatter", "Formatted result: $formatted")
            return formatted
        } catch (e: Exception) {
            android.util.Log.e("CurrencyFormatter", "Error formatting amount: ${e.message}", e)
            // Fallback to original amount if formatting fails
            return amount
        }
    }
}
