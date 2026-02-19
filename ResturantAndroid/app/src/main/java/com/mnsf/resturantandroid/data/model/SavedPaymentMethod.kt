package com.mnsf.resturantandroid.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a saved payment method from PayTabs.
 * Stores tokenized card information for faster checkout.
 */
@Parcelize
data class SavedPaymentMethod(
    val token: String,
    val cardLast4Digits: String,
    val cardBrand: String? = null, // e.g., "Visa", "Mastercard"
    val cardExpiry: String? = null, // e.g., "12/25"
    val savedAt: Long = System.currentTimeMillis() // Timestamp when saved
) : Parcelable
