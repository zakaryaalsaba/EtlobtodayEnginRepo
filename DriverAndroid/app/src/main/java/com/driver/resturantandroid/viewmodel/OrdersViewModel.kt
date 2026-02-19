package com.driver.resturantandroid.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driver.resturantandroid.data.model.Order
import com.driver.resturantandroid.repository.OrderRepository
import com.driver.resturantandroid.service.FirebaseOrderService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OrdersViewModel : ViewModel() {
    private val orderRepository = OrderRepository()
    private val firebaseOrderService = FirebaseOrderService()
    
    /** Firebase listener is only active when driver is online; cancelled when offline. */
    private var firebaseListenerJob: Job? = null
    
    companion object {
        private const val TAG = "OrdersViewModel"
    }
    
    private val _availableOrders = MutableLiveData<List<Order>>()
    val availableOrders: LiveData<List<Order>> = _availableOrders
    
    // Track previous orders to detect new ones
    private var previousOrderNumbers: Set<String> = emptySet()
    
    private val _assignedOrders = MutableLiveData<List<Order>>()
    val assignedOrders: LiveData<List<Order>> = _assignedOrders
    
    private val _orderHistory = MutableLiveData<List<Order>>()
    val orderHistory: LiveData<List<Order>> = _orderHistory
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Event to trigger sound when new order is received
    private val _newOrderReceived = MutableLiveData<Boolean>()
    val newOrderReceived: LiveData<Boolean> = _newOrderReceived
    
    /**
     * Start listening to Firebase for real-time available orders (read-only).
     * Only call when driver is online; otherwise use stopListeningToFirebaseOrders().
     */
    fun startListeningToFirebaseOrders(token: String) {
        Log.d(TAG, "🚀 startListeningToFirebaseOrders called (driver online)")
        firebaseListenerJob?.cancel()
        firebaseListenerJob = null
        _isLoading.value = true
        _error.value = null

        firebaseListenerJob = firebaseOrderService.listenToOrders()
            .onEach { orders ->
                Log.d(TAG, "📨 Flow emitted ${orders.size} orders")
                Log.d(TAG, "📊 Previous order numbers count: ${previousOrderNumbers.size}")
                Log.d(TAG, "📊 Previous order numbers: ${previousOrderNumbers.joinToString()}")
                
                // Detect new orders by comparing order numbers
                val currentOrderNumbers = orders.map { it.order_number }.toSet()
                val newOrderNumbers = currentOrderNumbers - previousOrderNumbers
                
                Log.d(TAG, "📊 Current order numbers count: ${currentOrderNumbers.size}")
                Log.d(TAG, "📊 Current order numbers: ${currentOrderNumbers.joinToString()}")
                Log.d(TAG, "📊 New order numbers count: ${newOrderNumbers.size}")
                Log.d(TAG, "📊 New order numbers: ${newOrderNumbers.joinToString()}")
                Log.d(TAG, "📊 Previous orders empty? ${previousOrderNumbers.isEmpty()}")
                
                if (newOrderNumbers.isNotEmpty() && previousOrderNumbers.isNotEmpty()) {
                    // Only play sound if we had previous orders (not initial load) and there are new ones
                    Log.d(TAG, "🔔 ✅ NEW ORDER DETECTED! Triggering sound for: ${newOrderNumbers.joinToString()}")
                    _newOrderReceived.value = true // Trigger sound in Fragment
                    Log.d(TAG, "🔔 ✅ _newOrderReceived set to true")
                } else {
                    if (newOrderNumbers.isEmpty()) {
                        Log.d(TAG, "🔕 No new orders (newOrderNumbers is empty)")
                    }
                    if (previousOrderNumbers.isEmpty()) {
                        Log.d(TAG, "🔕 Skipping sound - initial load (previousOrderNumbers is empty)")
                    }
                }
                
                previousOrderNumbers = currentOrderNumbers
                Log.d(TAG, "📊 Updated previousOrderNumbers to: ${previousOrderNumbers.joinToString()}")
                _availableOrders.value = orders
                _isLoading.value = false
                _error.value = null
                if (orders.isNotEmpty()) {
                    orders.forEach { order ->
                        Log.d(TAG, "  → Order in LiveData: ${order.order_number}, Status: ${order.status}")
                    }
                }
            }
            .catch { exception ->
                Log.e(TAG, "❌ Firebase Flow error caught: ${exception.message}", exception)
                _error.value = exception.message
                _isLoading.value = false
                Log.d(TAG, "🔄 Falling back to REST API")
                loadAvailableOrdersFromAPI(token)
            }
            .launchIn(viewModelScope)

        Log.d(TAG, "✅ Firebase Flow launched (listener active while driver is online)")
        loadAvailableOrdersFromAPI(token)
    }

    /**
     * Stop Firebase listener and clear available orders. Call when driver goes offline.
     * Orders will not be received until the driver is online again.
     */
    fun stopListeningToFirebaseOrders() {
        Log.d(TAG, "🛑 stopListeningToFirebaseOrders called (driver offline)")
        firebaseListenerJob?.cancel()
        firebaseListenerJob = null
        _availableOrders.value = emptyList()
        Log.d(TAG, "🛑 Resetting previousOrderNumbers (was: ${previousOrderNumbers.size} orders)")
        previousOrderNumbers = emptySet() // Reset when stopping
        _isLoading.value = false
        _error.value = null
    }
    
    /**
     * Call this after sound has been played to reset the event.
     */
    fun onNewOrderSoundPlayed() {
        Log.d(TAG, "🔔 onNewOrderSoundPlayed called - resetting _newOrderReceived to false")
        _newOrderReceived.value = false
    }
    
    /**
     * Load available orders from REST API (fallback or initial load).
     */
    fun loadAvailableOrdersFromAPI(token: String) {
        Log.d(TAG, "🌐 loadAvailableOrdersFromAPI called")
        viewModelScope.launch {
            Log.d(TAG, "🌐 Calling REST API getAvailableOrders")
            orderRepository.getAvailableOrders(token)
                .onSuccess { orders ->
                    Log.d(TAG, "✅ REST API success: Loaded ${orders.size} available orders")
                    // Merge with Firebase data or use as fallback
                    val currentFirebaseOrders = _availableOrders.value ?: emptyList()
                    Log.d(TAG, "📊 Current Firebase orders count: ${currentFirebaseOrders.size}")
                    if (currentFirebaseOrders.isEmpty()) {
                        Log.d(TAG, "📝 Firebase orders empty, using REST API data")
                        _availableOrders.value = orders
                    } else {
                        Log.d(TAG, "📝 Firebase has orders, keeping Firebase data")
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ REST API error: ${exception.message}", exception)
                    // Don't override Firebase data if REST API fails
                }
        }
    }
    
    /**
     * Legacy method - now redirects to Firebase listener.
     */
    fun loadAvailableOrders(token: String) {
        startListeningToFirebaseOrders(token)
    }
    
    fun loadAssignedOrders(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            orderRepository.getAssignedOrders(token)
                .onSuccess { orders ->
                    android.util.Log.d("OrdersViewModel", "Loaded ${orders.size} assigned orders from server")
                    // Filter out completed and cancelled orders (backend should already do this, but extra safety)
                    val activeOrders = orders.filter { 
                        val isActive = it.status != "completed" && it.status != "cancelled" && it.status != "delivered"
                        if (!isActive) {
                            android.util.Log.d("OrdersViewModel", "Filtering out order ${it.id} (${it.order_number}) with status: ${it.status}")
                        }
                        isActive
                    }
                    android.util.Log.d("OrdersViewModel", "After filtering: ${activeOrders.size} active orders")
                    _assignedOrders.value = activeOrders
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
        }
    }
    
    fun loadOrderHistory(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            orderRepository.getOrderHistory(token)
                .onSuccess { orders ->
                    _orderHistory.value = orders
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
        }
    }
    
    fun acceptOrder(orderId: Int, token: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            orderRepository.acceptOrder(orderId, token)
                .onSuccess {
                    onSuccess()
                    // Firebase will automatically update when order is removed from available list
                    // Also refresh assigned orders
                    loadAssignedOrders(token)
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _isLoading.value = false
                    onError(exception.message ?: "Failed to accept order")
                }
        }
    }
    
    fun rejectOrder(orderId: Int, token: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            orderRepository.rejectOrder(orderId, token)
                .onSuccess {
                    // Remove order from available list so it disappears from driver activity
                    removeOrderFromAvailable(orderId)
                    onSuccess()
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _isLoading.value = false
                    onError(exception.message ?: "Failed to reject order")
                }
        }
    }

    /** Removes an order from the available list (e.g. after driver rejects). */
    fun removeOrderFromAvailable(orderId: Int) {
        val current = _availableOrders.value ?: return
        val updated = current.filter { it.id != orderId }
        if (updated.size != current.size) {
            _availableOrders.value = updated
            Log.d(TAG, "Removed order $orderId from available list. Now ${updated.size} orders.")
        }
    }
    
    fun updateOrderStatus(orderId: Int, status: String, token: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            orderRepository.updateOrderStatus(orderId, status, token)
                .onSuccess { updatedOrder ->
                    android.util.Log.d("OrdersViewModel", "Status update returned order: ${updatedOrder.order_number}, status: ${updatedOrder.status}")
                    
                    // If order was marked as delivered, it becomes "completed" and should be removed from active orders
                    // Also check if the status update was for "delivered" regardless of what the backend returned
                    val isCompleted = updatedOrder.status == "completed" || 
                                     updatedOrder.status == "delivered" || 
                                     status == "delivered"
                    
                    if (isCompleted) {
                        // Immediately remove from assigned orders list
                        val currentOrders = _assignedOrders.value ?: emptyList()
                        val filteredOrders = currentOrders.filter { it.id != orderId }
                        _assignedOrders.value = filteredOrders
                        android.util.Log.d("OrdersViewModel", "Order ${orderId} completed (status=${updatedOrder.status}, requested=${status}), removed from active orders. Remaining: ${filteredOrders.size}")
                    }
                    
                    // Always refresh from server to ensure we have complete order data
                    // The backend might return incomplete data, so it's safer to refetch
                    loadAssignedOrders(token)
                    onSuccess()
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _isLoading.value = false
                    android.util.Log.e("OrdersViewModel", "Status update failed: ${exception.message}")
                    onError(exception.message ?: "Failed to update order status")
                }
        }
    }
}

