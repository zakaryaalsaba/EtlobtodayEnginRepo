package com.order.resturantandroid.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.data.repository.OrderRepository
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class DashboardStatistics(
    val totalOrders: Int,
    val totalRevenue: Double,
    val ordersByStatus: Map<String, Int>
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository()
    private val sessionManager = SessionManager(application)
    
    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders
    
    private val _allOrders = MutableLiveData<List<Order>>()
    val allOrders: LiveData<List<Order>> = _allOrders
    
    private val _statistics = MutableLiveData<DashboardStatistics>()
    val statistics: LiveData<DashboardStatistics> = _statistics
    
    private val _currencyCode = MutableLiveData<String>()
    val currencyCode: LiveData<String> = _currencyCode
    
    private val _currencySymbolPosition = MutableLiveData<String>()
    val currencySymbolPosition: LiveData<String> = _currencySymbolPosition
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    private var isPolling = false
    
    fun hasDateFilter(): Boolean {
        return dateFrom != null || dateTo != null
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    fun loadOrders() {
        val websiteId = sessionManager.getWebsiteId()
        val token = sessionManager.getAuthToken()
        
        if (websiteId == -1 || token == null) {
            _error.value = "Session expired. Please login again."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getOrders(websiteId, null, token).fold(
                onSuccess = { ordersList ->
                    // Extract currency info from first order if available
                    ordersList.firstOrNull()?.let { firstOrder ->
                        _currencyCode.value = firstOrder.currencyCode ?: "USD"
                        _currencySymbolPosition.value = firstOrder.currencySymbolPosition ?: "before"
                    } ?: run {
                        _currencyCode.value = "USD"
                        _currencySymbolPosition.value = "before"
                    }
                    
                    _allOrders.value = ordersList
                    applyFilters(ordersList)
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load orders"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun setDateFilter(fromDate: Long?, toDate: Long?) {
        dateFrom = fromDate
        dateTo = toDate
        _allOrders.value?.let { applyFilters(it) }
    }
    
    fun clearDateFilter() {
        dateFrom = null
        dateTo = null
        _allOrders.value?.let { applyFilters(it) }
    }
    
    private fun applyFilters(ordersList: List<Order>) {
        var filteredOrders = ordersList
        
        // Apply date filter
        if (dateFrom != null || dateTo != null) {
            filteredOrders = filteredOrders.filter { order ->
                try {
                    val orderDate = dateFormat.parse(order.createdAt)?.time ?: return@filter false
                    val afterFrom = dateFrom == null || orderDate >= dateFrom!!
                    val beforeTo = dateTo == null || orderDate <= dateTo!!
                    afterFrom && beforeTo
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            // Default: show only active orders when no date filter
            filteredOrders = filteredOrders.filter { 
                it.status != "completed" && it.status != "cancelled" 
            }
        }
        
        _orders.value = filteredOrders
        calculateStatistics(filteredOrders)
    }
    
    private fun calculateStatistics(orders: List<Order>) {
        val totalOrders = orders.size
        var totalRevenue = 0.0
        val ordersByStatus = mutableMapOf<String, Int>()
        
        orders.forEach { order ->
            // Calculate revenue
            try {
                val amount = order.totalAmount.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                totalRevenue += amount
            } catch (e: Exception) {
                // Ignore invalid amounts
            }
            
            // Count by status
            val status = order.status.lowercase()
            ordersByStatus[status] = (ordersByStatus[status] ?: 0) + 1
        }
        
        _statistics.value = DashboardStatistics(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            ordersByStatus = ordersByStatus
        )
    }
    
    fun startPolling() {
        if (isPolling) return
        isPolling = true
        
        viewModelScope.launch {
            while (isPolling) {
                loadOrders()
                delay(5000) // Poll every 5 seconds
            }
        }
    }
    
    fun stopPolling() {
        isPolling = false
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
