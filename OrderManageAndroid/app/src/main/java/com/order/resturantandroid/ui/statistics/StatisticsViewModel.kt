package com.order.resturantandroid.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.data.repository.OrderRepository
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class StatisticsData(
    val totalOrders: Int,
    val totalRevenue: Double,
    val ordersByStatus: Map<String, Int>,
    val currencyCode: String,
    val currencySymbolPosition: String
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository()
    private val sessionManager = SessionManager(application)
    
    private val _statistics = MutableLiveData<StatisticsData>()
    val statistics: LiveData<StatisticsData> = _statistics
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    fun loadStatistics() {
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
                    val filteredOrders = applyDateFilter(ordersList)
                    calculateStatistics(filteredOrders)
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load statistics"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun setDateFilter(fromDate: Long?, toDate: Long?) {
        dateFrom = fromDate
        dateTo = toDate
        loadStatistics()
    }
    
    fun clearDateFilter() {
        dateFrom = null
        dateTo = null
        loadStatistics()
    }
    
    private fun applyDateFilter(orders: List<Order>): List<Order> {
        if (dateFrom == null && dateTo == null) {
            return orders
        }
        
        return orders.filter { order ->
            try {
                val orderDate = dateFormat.parse(order.createdAt)?.time ?: return@filter false
                val afterFrom = dateFrom == null || orderDate >= dateFrom!!
                val beforeTo = dateTo == null || orderDate <= dateTo!!
                afterFrom && beforeTo
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun calculateStatistics(orders: List<Order>) {
        val totalOrders = orders.size
        var totalRevenue = 0.0
        val ordersByStatus = mutableMapOf<String, Int>()
        
        var currencyCode = "USD"
        var currencySymbolPosition = "before"
        
        orders.forEach { order ->
            // Get currency from first order
            if (orders.isNotEmpty() && orders[0] == order) {
                currencyCode = order.currencyCode ?: "USD"
                currencySymbolPosition = order.currencySymbolPosition ?: "before"
            }
            
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
        
        _statistics.value = StatisticsData(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            ordersByStatus = ordersByStatus,
            currencyCode = currencyCode,
            currencySymbolPosition = currencySymbolPosition
        )
    }
}
