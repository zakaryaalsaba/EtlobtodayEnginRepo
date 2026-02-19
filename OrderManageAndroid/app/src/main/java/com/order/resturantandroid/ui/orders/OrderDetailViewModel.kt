package com.order.resturantandroid.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.data.repository.OrderRepository
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch

class OrderDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository()
    private val sessionManager = SessionManager(application)
    
    private val _order = MutableLiveData<Order?>()
    val order: LiveData<Order?> = _order
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _statusUpdateSuccess = MutableLiveData<Boolean>()
    val statusUpdateSuccess: LiveData<Boolean> = _statusUpdateSuccess
    
    private var currentOrderNumber: String? = null
    
    fun loadOrder(orderNumber: String) {
        currentOrderNumber = orderNumber
        val token = sessionManager.getAuthToken() ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getOrder(orderNumber, token).fold(
                onSuccess = { order ->
                    _order.value = order
                    currentOrderNumber = order.orderNumber // Update in case it changed
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load order"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun updateOrderStatus(orderId: Int, status: String) {
        val token = sessionManager.getAuthToken() ?: return
        val orderNumber = currentOrderNumber ?: _order.value?.orderNumber
        
        if (orderNumber.isNullOrBlank()) {
            _error.value = "Order number not available"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.updateOrderStatus(orderId, status, token).fold(
                onSuccess = {
                    _statusUpdateSuccess.value = true
                    _isLoading.value = false
                    // Reload order using order number
                    loadOrder(orderNumber)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update status"
                    _isLoading.value = false
                }
            )
        }
    }
}

