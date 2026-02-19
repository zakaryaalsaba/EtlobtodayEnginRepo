package com.driver.resturantandroid.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driver.resturantandroid.data.model.Order
import com.driver.resturantandroid.service.FirebaseOrderService
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeViewModel : ViewModel() {
    private val firebaseOrderService = FirebaseOrderService()
    
    private val _availableOrders = MutableLiveData<List<Order>>()
    val availableOrders: LiveData<List<Order>> = _availableOrders
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        startListeningToOrders()
    }
    
    /**
     * Start listening to Firebase for real-time order updates (read-only).
     * All write operations should go through MySQL/REST API.
     */
    private fun startListeningToOrders() {
        _isLoading.value = true
        firebaseOrderService.listenToOrders()
            .onEach { orders ->
                _availableOrders.value = orders
                _isLoading.value = false
                _error.value = null
                Log.d("HomeViewModel", "Received ${orders.size} orders from Firebase")
            }
            .catch { exception ->
                _error.value = exception.message
                _isLoading.value = false
                Log.e("HomeViewModel", "Error listening to Firebase orders", exception)
            }
            .launchIn(viewModelScope)
    }
    
    override fun onCleared() {
        super.onCleared()
        // Firebase listeners are automatically cleaned up when the flow is cancelled
    }
}