package com.order.resturantandroid.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.order.resturantandroid.BuildConfig
import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.data.model.OrderItem
import com.order.resturantandroid.data.repository.OrderRepository
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

data class DashboardStatistics(
    val totalOrders: Int,
    val totalRevenue: Double,
    val ordersByStatus: Map<String, Int>
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val orderRepository = OrderRepository()
    
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

    private val _newCount = MutableLiveData<Int>()
    val newCount: LiveData<Int> = _newCount

    private val _acceptedCount = MutableLiveData<Int>()
    val acceptedCount: LiveData<Int> = _acceptedCount

    private val _upcomingCount = MutableLiveData<Int>()
    val upcomingCount: LiveData<Int> = _upcomingCount
    
    private val _newSpotlightOrder = MutableLiveData<Order?>()
    val newSpotlightOrder: LiveData<Order?> = _newSpotlightOrder
    
    private val _newSpotlightOrders = MutableLiveData<List<Order>>(emptyList())
    val newSpotlightOrders: LiveData<List<Order>> = _newSpotlightOrders
    
    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    private var isPolling = false
    private var ordersRefListener: ValueEventListener? = null
    private val isoFallbackDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    
    private val DEFAULT_CURRENCY_CODE = "USD"
    private val DEFAULT_CURRENCY_SYMBOL_POSITION = "before"

    /** Matches restaurant website settings (API); used when Firebase has no currency fields yet. */
    private var websiteCurrencyCode: String = DEFAULT_CURRENCY_CODE
    private var websiteSymbolPosition: String = DEFAULT_CURRENCY_SYMBOL_POSITION
    
    fun hasDateFilter(): Boolean {
        return dateFrom != null || dateTo != null
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private suspend fun refreshWebsiteCurrency() {
        val token = sessionManager.getAuthToken() ?: return
        orderRepository.getRestaurantWebsite(token).fold(
            onSuccess = { website ->
                website.currencyCode?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    websiteCurrencyCode = it.uppercase(Locale.US)
                }
                website.currencySymbolPosition?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    websiteSymbolPosition = it.lowercase(Locale.US)
                }
            },
            onFailure = { }
        )
    }

    fun loadOrders() {
        val websiteId = sessionManager.getWebsiteId()
        if (websiteId == -1) {
            _error.value = "Session expired. Please login again."
            return
        }
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            refreshWebsiteCurrency()
            val db = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
            val ref = db.getReference("orders").child(websiteId.toString())
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    updateOrdersFromFirebaseSnapshot(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    _error.value = error.message ?: "Failed to load orders from Firebase"
                    _isLoading.value = false
                }
            })
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
                    val orderDate = parseCreatedAtToMillis(order.createdAt) ?: return@filter false
                    val afterFrom = dateFrom == null || orderDate >= dateFrom!!
                    val beforeTo = dateTo == null || orderDate <= dateTo!!
                    afterFrom && beforeTo
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            // Default: show active non-pending orders in bottom list.
            // Pending is shown in the top "New" spotlight card.
            filteredOrders = filteredOrders.filter { order ->
                val statusNorm = (order.status ?: "")
                    .trim()
                    .lowercase(Locale.getDefault())
                statusNorm != "completed" && statusNorm != "cancelled" && statusNorm != "pending"
            }
        }
        
        _orders.value = filteredOrders
        calculateStatistics(filteredOrders)
        updateSectionCounts(ordersList)
    }

    private fun updateSectionCounts(allOrders: List<Order>) {
        // "Talabat-like" sections (simple mapping for our statuses)
        // New: pending
        // Accepted: confirmed
        // Upcoming: preparing/ready/picked_up
        val normStatuses = allOrders.map { (it.status ?: "").trim().lowercase(Locale.getDefault()) }
        _newCount.value = normStatuses.count { it == "pending" }
        _acceptedCount.value = normStatuses.count { it == "confirmed" }
        _upcomingCount.value = normStatuses.count { it == "preparing" || it == "ready" || it == "picked_up" }

        // Spotlight latest pending order under "New"
        val latestPending = allOrders
            .filter { (it.status ?: "").trim().lowercase(Locale.getDefault()) == "pending" }
            .maxByOrNull { parseCreatedAtToMillis(it.createdAt) ?: Long.MIN_VALUE }
        _newSpotlightOrder.value = latestPending

        val pendingOrders = allOrders
            .filter { (it.status ?: "").trim().lowercase(Locale.getDefault()) == "pending" }
            .sortedBy { parseCreatedAtToMillis(it.createdAt) ?: Long.MAX_VALUE }
        _newSpotlightOrders.value = pendingOrders
    }
    
    private fun updateOrdersFromFirebaseSnapshot(snapshot: DataSnapshot) {
        val ordersList = snapshot.children.mapNotNull { child ->
            firebaseSnapshotToOrder(child)
        }
        
        // First-come, first-serve list ordering.
        val sorted = ordersList.sortedBy { parseCreatedAtToMillis(it.createdAt) ?: Long.MAX_VALUE }
        
        _currencyCode.value = websiteCurrencyCode
        _currencySymbolPosition.value = websiteSymbolPosition
        _allOrders.value = sorted
        applyFilters(sorted)
        _isLoading.value = false
    }
    
    private fun parseCreatedAtToMillis(createdAt: String?): Long? {
        if (createdAt.isNullOrBlank()) return null
        return try {
            // REST format
            dateFormat.parse(createdAt)?.time
                ?: run {
                    // Firebase ISO format
                    try {
                        Instant.parse(createdAt).toEpochMilli()
                    } catch (_: Exception) {
                        isoFallbackDateFormat.parse(createdAt)?.time
                    }
                }
        } catch (_: Exception) {
            null
        }
    }
    
    private fun firebaseSnapshotToOrder(snapshot: DataSnapshot): Order? {
        val orderIdAny = snapshot.child("id").getValue(Any::class.java)
        val orderId = when (orderIdAny) {
            is Number -> orderIdAny.toInt()
            else -> null
        } ?: return null
        
        val orderNumber = getString(snapshot, "order_number") ?: return null
        val customerName = getString(snapshot, "customer_name")
        val customerPhone = getStringNullable(snapshot, "customer_phone")
        val customerAddress = getStringNullable(snapshot, "customer_address")
        
        val orderType = getString(snapshot, "order_type") ?: "pickup"
        val status = getString(snapshot, "status") ?: "pending"
        val totalAmount = getString(snapshot, "total_amount") ?: "0.00"
        val resolvedCurrencyCode = getStringNullable(snapshot, "currency_code") ?: websiteCurrencyCode
        val resolvedSymbolPosition = getStringNullable(snapshot, "currency_symbol_position") ?: websiteSymbolPosition
        val paymentMethod = getStringNullable(snapshot, "payment_method")
        val paymentStatus = getStringNullable(snapshot, "payment_status")
        val notes = getStringNullable(snapshot, "notes")
        val createdAt = formatCreatedAtFromSnapshot(snapshot)
        
        val deliveryLatitude = getDoubleNullable(snapshot, "delivery_latitude")
        val deliveryLongitude = getDoubleNullable(snapshot, "delivery_longitude")
        
        val items = snapshot.child("items").children.mapNotNull { itemSnap ->
            val productIdAny = itemSnap.child("product_id").getValue(Any::class.java)
            val productId = (productIdAny as? Number)?.toInt() ?: return@mapNotNull null
            
            val item = OrderItem(
                id = (itemSnap.child("id").getValue(Any::class.java) as? Number)?.toInt() ?: 0,
                productId = productId,
                productName = getString(itemSnap, "product_name") ?: "",
                productPrice = getString(itemSnap, "product_price") ?: "0.00",
                quantity = (itemSnap.child("quantity").getValue(Any::class.java) as? Number)?.toInt()
                    ?: 0,
                subtotal = getString(itemSnap, "subtotal") ?: "0.00"
            )
            item
        }.takeIf { it.isNotEmpty() } ?: emptyList()
        
        return Order(
            id = orderId,
            orderNumber = orderNumber,
            customerName = customerName ?: "",
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            orderType = orderType,
            status = status,
            totalAmount = totalAmount,
            currencyCode = resolvedCurrencyCode,
            currencySymbolPosition = resolvedSymbolPosition,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            notes = notes,
            createdAt = createdAt,
            items = if (items.isNotEmpty()) items else emptyList(),
            driverId = getIntNullable(snapshot, "driver_id"),
            deliveryLatitude = deliveryLatitude,
            deliveryLongitude = deliveryLongitude
        )
    }
    
    /**
     * Firebase may store [created_at] as ISO string or as a number (legacy JS Date → ms).
     * Always expose a single ISO-8601 string to the rest of the app.
     */
    private fun formatCreatedAtFromSnapshot(snapshot: DataSnapshot): String {
        val v = snapshot.child("created_at").getValue(Any::class.java) ?: return ""
        return when (v) {
            is Number -> {
                val n = v.toDouble().toLong()
                val ms = when {
                    n >= 1_000_000_000_000L -> n
                    n in 1_000_000_000L until 1_000_000_000_000L -> n * 1000L
                    else -> n
                }
                Instant.ofEpochMilli(ms).toString()
            }
            else -> v.toString().trim()
        }
    }

    private fun getString(snapshot: DataSnapshot, field: String): String? {
        val v = snapshot.child(field).getValue(Any::class.java) ?: return null
        return v.toString()
    }
    
    private fun getStringNullable(snapshot: DataSnapshot, field: String): String? {
        val v = snapshot.child(field).getValue(Any::class.java) ?: return null
        val s = v.toString()
        return if (s.isBlank() || s == "null") null else s
    }
    
    private fun getIntNullable(snapshot: DataSnapshot, field: String): Int? {
        val v = snapshot.child(field).getValue(Any::class.java) ?: return null
        return (v as? Number)?.toInt()
    }
    
    private fun getDoubleNullable(snapshot: DataSnapshot, field: String): Double? {
        val v = snapshot.child(field).getValue(Any::class.java) ?: return null
        return when (v) {
            is Number -> v.toDouble()
            else -> v.toString().toDoubleOrNull()
        }
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

        val websiteId = sessionManager.getWebsiteId()
        if (websiteId == -1) {
            _error.value = "Session expired. Please login again."
            isPolling = false
            return
        }

        viewModelScope.launch {
            refreshWebsiteCurrency()
            val db = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
            val ref = db.getReference("orders").child(websiteId.toString())
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    updateOrdersFromFirebaseSnapshot(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    _error.value = error.message ?: "Firebase listener cancelled"
                    _isLoading.value = false
                }
            }

            ordersRefListener = listener
            ref.addValueEventListener(listener)
        }
    }
    
    fun stopPolling() {
        isPolling = false
        // Remove listener by creating the same reference again.
        // Firebase doesn't require exact ref instance, but the removal requires the same ValueEventListener.
        if (ordersRefListener != null) {
            val websiteId = sessionManager.getWebsiteId()
            if (websiteId != -1) {
                val db = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
                val ref = db.getReference("orders").child(websiteId.toString())
                ref.removeEventListener(ordersRefListener!!)
            }
        }
        ordersRefListener = null
    }

    fun confirmOrderQuickly(orderId: Int) {
        val token = sessionManager.getAuthToken()
        if (token.isNullOrBlank()) {
            _error.value = "Session expired. Please login again."
            return
        }
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, "confirmed", token).fold(
                onSuccess = {
                    // Firebase listener will refresh UI state.
                },
                onFailure = { ex ->
                    _error.value = ex.message ?: "Failed to confirm order"
                }
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
