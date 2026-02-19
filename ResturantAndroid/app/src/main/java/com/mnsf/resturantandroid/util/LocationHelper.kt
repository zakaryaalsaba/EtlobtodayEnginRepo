package com.mnsf.resturantandroid.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.Locale

object LocationHelper {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    /**
     * Initialize the location client
     */
    fun initialize(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request location permissions
     */
    fun requestLocationPermissions(activity: android.app.Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            requestCode
        )
    }
    
    /**
     * Get current location
     * @return Location object or null if permission not granted or location unavailable
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) {
            return null
        }
        
        try {
            initialize(context)
            
            // First try to get last known location (fast, cached)
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return lastLocation
            }
            
            // If last location is not available, try to get a fresh location with timeout
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .build()
            
            val locationSettingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build()
            
            val settingsClient = LocationServices.getSettingsClient(context)
            val settingsResponse = settingsClient.checkLocationSettings(locationSettingsRequest).await()
            
            if (settingsResponse.locationSettingsStates?.isLocationPresent == true) {
                // Try to get current location with a timeout
                return try {
                    withTimeout(3000) {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    }
                } catch (e: Exception) {
                    // Timeout or error - return null
                    null
                }
            }
        } catch (e: Exception) {
            // Silently handle errors - location is optional for order creation
            android.util.Log.d("LocationHelper", "Could not get location: ${e.message}")
        }
        
        return null
    }
    
    /**
     * Get address from coordinates (reverse geocoding)
     * Note: For API 33+, this uses a suspend function with CompletableDeferred
     */
    suspend fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For API 33+, use the callback API with CompletableDeferred
                val deferred = CompletableDeferred<String?>()
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    val address = addresses.firstOrNull()?.getAddressLine(0)
                    deferred.complete(address)
                }
                deferred.await()
            } else {
                // For older APIs, use synchronous method
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.getAddressLine(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get coordinates from address (geocoding)
     */
    fun getLocationFromAddress(context: Context, address: String): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(address, 1) { addresses ->
                    addresses.firstOrNull()?.let {
                        Pair(it.latitude, it.longitude)
                    }
                }
                null
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(address, 1)
            }
            
            addresses?.firstOrNull()?.let {
                Pair(it.latitude, it.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

