package com.order.resturantandroid.util

import java.util.Locale

/** Use for formatted numbers so Arabic locale does not show Eastern Arabic numerals. */
val ENGLISH_NUMBER_LOCALE: Locale = Locale.US

private val NON_LATIN_DIGIT_TO_LATIN = mapOf(
    '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
    '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
    '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
    '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
)

/** Replace Arabic-Indic / Persian digits with Latin 0-9 (e.g. order numbers from APIs). */
fun String.withEnglishDigits(): String {
    if (isEmpty()) return this
    val sb = StringBuilder(length)
    for (c in this) {
        sb.append(NON_LATIN_DIGIT_TO_LATIN[c] ?: c)
    }
    return sb.toString()
}
