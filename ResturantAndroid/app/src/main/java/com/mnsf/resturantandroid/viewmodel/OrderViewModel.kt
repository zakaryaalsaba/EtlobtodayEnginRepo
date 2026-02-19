package com.mnsf.resturantandroid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnsf.resturantandroid.data.model.*
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch

class OrderViewModel(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _orderState = MutableLiveData<OrderState>()
    val orderState: LiveData<OrderState> = _orderState
    
    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    sealed class OrderState {
        data class Success(val order: Order) : OrderState()
        data class Error(val message: String) : OrderState()
    }
    
    fun createOrder(
        restaurantId: Int,
        customerName: String,
        customerPhone: String,
        customerEmail: String? = null,
        customerAddress: String? = null,
        orderType: String = "pickup",
        paymentMethod: String = "cash",
        paymentIntentId: String? = null,
        cartItems: List<CartItem>,
        notes: String? = null,
        deliveryLatitude: Double? = null,
        deliveryLongitude: Double? = null,
        tip: Double? = null,
        deliveryInstructions: String? = null,
        totalAmount: Double? = null,
        /** Restaurant info for Driver app (pickup name, phone, address, location). */
        restaurantInfo: RestaurantInfoRequest? = null
    ) {
        if (cartItems.isEmpty()) {
            _orderState.value = OrderState.Error("Cart is empty")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val customerId = if (sessionManager.isLoggedIn()) sessionManager.getCustomerId() else null
            
            val orderItems = cartItems.map { cartItem ->
                OrderItemRequest(cartItem.product.id, cartItem.quantity)
            }
            
            val request = CreateOrderRequest(
                website_id = restaurantId,
                customer_id = customerId,
                customer_name = customerName,
                customer_email = customerEmail,
                customer_phone = customerPhone,
                customer_address = customerAddress,
                order_type = orderType,
                payment_method = paymentMethod,
                payment_intent_id = paymentIntentId,
                delivery_latitude = deliveryLatitude,
                delivery_longitude = deliveryLongitude,
                items = orderItems,
                notes = notes,
                tip = tip,
                delivery_instructions = deliveryInstructions,
                total_amount = totalAmount,
                restaurant = restaurantInfo
            )
            
            val result = orderRepository.createOrder(request)
            _isLoading.value = false
            
            result.onSuccess { order ->
                _orderState.value = OrderState.Success(order)
            }.onFailure { exception ->
                val errorMessage = exception.message ?: "Failed to create order"
                _orderState.value = OrderState.Error(errorMessage)
                _error.value = errorMessage
            }
        }
    }
    
    fun loadOrders() {
        if (!sessionManager.isLoggedIn()) {
            _error.value = "Please login to view orders"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val customerId = sessionManager.getCustomerId()
            val result = orderRepository.getCustomerOrders(customerId)
            _isLoading.value = false
            
            result.onSuccess { orderList ->
                _orders.value = orderList
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to load orders"
            }
        }
    }
}

