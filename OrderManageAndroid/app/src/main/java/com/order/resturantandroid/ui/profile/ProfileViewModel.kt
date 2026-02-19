package com.order.resturantandroid.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.order.resturantandroid.data.remote.ApiService
import com.order.resturantandroid.data.remote.RestaurantWebsite
import com.order.resturantandroid.data.remote.RestaurantWebsiteResponse
import com.order.resturantandroid.data.remote.RestaurantWebsiteUpdate
import com.order.resturantandroid.data.remote.RetrofitClient
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService: ApiService = RetrofitClient.apiService
    private val sessionManager = SessionManager(application)
    
    private val _restaurant = MutableLiveData<RestaurantWebsite?>()
    val restaurant: LiveData<RestaurantWebsite?> = _restaurant
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess
    
    fun loadRestaurantProfile() {
        val token = sessionManager.getAuthToken()
        
        if (token == null) {
            _error.value = "Session expired. Please login again."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val response = apiService.getRestaurantInfo("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _restaurant.value = response.body()!!.website
                    _isLoading.value = false
                } else {
                    _error.value = response.message() ?: "Failed to load restaurant profile"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load restaurant profile"
                _isLoading.value = false
            }
        }
    }
    
    fun updateRestaurantProfile(
        restaurantName: String? = null,
        address: String? = null,
        phone: String? = null,
        email: String? = null,
        isPublished: Boolean? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val token = sessionManager.getAuthToken()
        
        if (token == null) {
            _error.value = "Session expired. Please login again."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false
            
            try {
                val update = RestaurantWebsiteUpdate(
                    restaurantName = restaurantName,
                    address = address,
                    phone = phone,
                    email = email,
                    isPublished = isPublished,
                    latitude = latitude,
                    longitude = longitude
                )
                
                val response = apiService.updateRestaurantInfo(update, "Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _restaurant.value = response.body()!!.website
                    _updateSuccess.value = true
                    _isLoading.value = false
                } else {
                    _error.value = response.message() ?: "Failed to update restaurant profile"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update restaurant profile"
                _isLoading.value = false
            }
        }
    }
}
