package com.order.resturantandroid.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.TimeZone

/**
 * Parses order [created_at] from API / Firebase to epoch millis.
 * Supports: ISO-8601, MySQL datetime, and Firebase RTDB numeric timestamps (ms or seconds).
 */
fun parseOrderCreatedAtMillis(createdAt: String?): Long? {
    if (createdAt.isNullOrBlank()) return null
    val trimmed = createdAt.trim()

    // Firebase RTDB often stores legacy JS Date as a number → Android sees "1737654321000" or "1.73E12"
    trimmed.toDoubleOrNull()?.let { d ->
        if (!d.isFinite() || d <= 0) return@let
        val n = d.toLong()
        val ms = when {
            // 13-digit style epoch ms (e.g. 1737654321000)
            n >= 1_000_000_000_000L -> n
            // 10-digit epoch seconds
            n in 1_000_000_000L until 1_000_000_000_000L -> n * 1000L
            else -> n
        }
        if (ms > 0L) return ms
    }

    try {
        return Instant.parse(trimmed).toEpochMilli()
    } catch (_: DateTimeParseException) {
        // continue
    }

    val patternsUtc = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to true,
        "yyyy-MM-dd'T'HH:mm:ss.SSSX" to true,
        "yyyy-MM-dd'T'HH:mm:ss'Z'" to true
    )
    val patternsLocal = listOf(
        "yyyy-MM-dd HH:mm:ss" to false,
        "yyyy-MM-dd HH:mm:ss.SSS" to false,
        "yyyy-MM-dd'T'HH:mm:ss" to false
    )
    for ((pattern, utc) in patternsUtc + patternsLocal) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            if (utc) sdf.timeZone = TimeZone.getTimeZone("UTC")
            val d = sdf.parse(trimmed) ?: continue
            return d.time
        } catch (_: Exception) {
            // try next
        }
    }
    return null
}

fun formatOrderPlacedAt(createdAt: String?, millis: Long?): String {
    val m = millis ?: parseOrderCreatedAtMillis(createdAt) ?: return ""
    val out = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    return out.format(m)
}

fun formatElapsedMmSs(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
