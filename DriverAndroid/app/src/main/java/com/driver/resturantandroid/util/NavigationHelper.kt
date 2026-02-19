package com.driver.resturantandroid.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.location.Location

object NavigationHelper {
    /**
     * Opens Google Maps navigation to the specified address
     * This is the MANDATORY requirement - opens Google Maps app directly
     */
    fun navigateToAddress(context: Context, address: String) {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(address)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to web maps if Google Maps app is not installed
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(webIntent)
        }
    }
    
    /**
     * Opens Google Maps navigation to coordinates
     */
    fun navigateToCoordinates(context: Context, latitude: Double, longitude: Double) {
        val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to web maps
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(webIntent)
        }
    }
    
    /**
     * Calculate distance between two locations in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000f // Convert to kilometers
    }
    
    /**
     * Format distance for display
     */
    fun formatDistance(km: Float): String {
        return if (km < 1) {
            String.format("%.0f m", km * 1000)
        } else {
            String.format("%.1f km", km)
        }
    }
    
    /**
     * Estimate delivery time based on distance
     */
    fun estimateDeliveryTime(km: Float): String {
        // Assume average speed of 30 km/h in city
        val hours = km / 30f
        val minutes = (hours * 60).toInt()
        return if (minutes < 60) {
            "$minutes min"
        } else {
            "${minutes / 60}h ${minutes % 60}m"
        }
    }
}

