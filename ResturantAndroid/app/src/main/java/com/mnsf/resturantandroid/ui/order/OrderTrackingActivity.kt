package com.mnsf.resturantandroid.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Order
import com.mnsf.resturantandroid.databinding.ActivityOrderTrackingBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.ui.MainActivity
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrderTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderTrackingBinding
    private var order: Order? = null
    private var orderNumber: String? = null
    private var currencyCode: String? = null
    private var currencySymbolPosition: String? = null
    private lateinit var orderRepository: OrderRepository
    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var sessionManager: SessionManager
    private var pollingJob: Job? = null
    private var isPolling = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = getString(R.string.track_order)

        sessionManager = SessionManager(this)
        orderRepository = OrderRepository(RetrofitClient.apiService, sessionManager)
        val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        restaurantViewModel = ViewModelProvider(
            this,
            com.mnsf.resturantandroid.ui.restaurant.RestaurantViewModelFactory(restaurantRepository)
        )[RestaurantViewModel::class.java]

        // Get order from intent
        order = intent.getSerializableExtra("order") as? Order
        orderNumber = intent.getStringExtra("order_number") ?: order?.order_number

        if (orderNumber == null) {
            finish()
            return
        }

        // Load restaurant currency info
        order?.website_id?.let { websiteId ->
            loadRestaurantCurrency(websiteId)
        }

        // Display initial order info
        if (order != null) {
            displayOrderStatus(order!!)
        } else {
            // Fetch order if not provided
            fetchOrder()
        }

        setupClickListeners()
        startPolling()
    }

    private fun fetchOrder() {
        orderNumber?.let { number ->
            lifecycleScope.launch {
                binding.progressBar.visibility = View.VISIBLE
                try {
                    val result = orderRepository.getOrderByNumber(number)
                    result.onSuccess { fetchedOrder ->
                        order = fetchedOrder
                        fetchedOrder.website_id?.let { websiteId ->
                            loadRestaurantCurrency(websiteId)
                        }
                        displayOrderStatus(fetchedOrder)
                        binding.progressBar.visibility = View.GONE
                    }.onFailure { exception ->
                        Log.e("OrderTracking", "Failed to fetch order: ${exception.message}")
                        binding.progressBar.visibility = View.GONE
                        // Show error
                    }
                } catch (e: Exception) {
                    Log.e("OrderTracking", "Error fetching order", e)
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun loadRestaurantCurrency(websiteId: Int) {
        restaurantViewModel.loadRestaurant(websiteId)
        restaurantViewModel.selectedRestaurant.observe(this) { restaurant ->
            restaurant?.let {
                currencyCode = it.currency_code
                currencySymbolPosition = it.currency_symbol_position
                // Update restaurant name
                binding.tvRestaurantName.text = it.restaurant_name
                order?.let { currentOrder ->
                    displayOrderStatus(currentOrder)
                }
            }
        }
    }

    private fun startPolling() {
        if (isPolling || orderNumber == null) return
        
        isPolling = true
        pollingJob = lifecycleScope.launch {
            while (isPolling) {
                delay(5000) // Poll every 5 seconds
                
                if (!isPolling) break
                
                orderNumber?.let { number ->
                    try {
                        val result = orderRepository.getOrderByNumber(number)
                        result.onSuccess { updatedOrder ->
                            val oldStatus = order?.status
                            order = updatedOrder
                            
                            // Update display if status changed
                            if (oldStatus != updatedOrder.status) {
                                displayOrderStatus(updatedOrder)
                                // Animate status change
                                animateStatusChange(updatedOrder.status)
                            } else {
                                displayOrderStatus(updatedOrder)
                            }
                            
                            // Stop polling if order is completed or cancelled
                            if (updatedOrder.status.lowercase() in listOf("completed", "cancelled")) {
                                stopPolling()
                            }
                        }.onFailure { exception ->
                            Log.e("OrderTracking", "Polling error: ${exception.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("OrderTracking", "Polling exception", e)
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        isPolling = false
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun displayOrderStatus(order: Order) {
        binding.apply {
            tvOrderNumber.text = order.order_number
            
            // Load restaurant name if not already loaded
            order.website_id?.let { websiteId ->
                restaurantViewModel.selectedRestaurant.value?.let { restaurant ->
                    tvRestaurantName.text = restaurant.restaurant_name
                } ?: run {
                    // Load restaurant if not loaded
                    if (restaurantViewModel.selectedRestaurant.value == null) {
                        loadRestaurantCurrency(websiteId)
                    }
                }
            }
            
            // Update status timeline
            updateStatusTimeline(order.status)
            
            // Display order items
            order.items?.let { items ->
                if (items.isEmpty()) {
                    tvOrderItems.text = getString(R.string.no_items_available)
                } else {
                    val itemsText = items.joinToString("\n") { item ->
                        val formattedSubtotal = CurrencyFormatter.formatPrice(
                            item.subtotal,
                            currencyCode,
                            currencySymbolPosition
                        )
                        "${item.product_name} x${item.quantity} - $formattedSubtotal"
                    }
                    tvOrderItems.text = itemsText
                }
            } ?: run {
                tvOrderItems.text = getString(R.string.no_items_available)
            }
            
            // Display total
            val formattedTotal = CurrencyFormatter.formatPrice(
                order.total_amount,
                currencyCode,
                currencySymbolPosition
            )
            tvTotalAmount.text = formattedTotal
        }
    }

    private fun updateStatusTimeline(status: String) {
        val statusLower = status.lowercase()
        
        // Reset all status indicators
        binding.apply {
            // Pending
            ivStatusPending.setImageResource(
                if (statusLower == "pending") R.drawable.ic_status_active
                else if (getStatusIndex(statusLower) > 0) R.drawable.ic_status_completed
                else R.drawable.ic_status_pending
            )
            tvStatusPending.setTextColor(
                if (statusLower == "pending") getColor(R.color.primary_color)
                else if (getStatusIndex(statusLower) > 0) getColor(R.color.success)
                else getColor(R.color.on_surface_variant)
            )
            
            // Confirmed
            ivStatusConfirmed.setImageResource(
                if (statusLower == "confirmed") R.drawable.ic_status_active
                else if (getStatusIndex(statusLower) > 1) R.drawable.ic_status_completed
                else R.drawable.ic_status_pending
            )
            tvStatusConfirmed.setTextColor(
                if (statusLower == "confirmed") getColor(R.color.primary_color)
                else if (getStatusIndex(statusLower) > 1) getColor(R.color.success)
                else getColor(R.color.on_surface_variant)
            )
            
            // Preparing
            ivStatusPreparing.setImageResource(
                if (statusLower == "preparing") R.drawable.ic_status_active
                else if (getStatusIndex(statusLower) > 2) R.drawable.ic_status_completed
                else R.drawable.ic_status_pending
            )
            tvStatusPreparing.setTextColor(
                if (statusLower == "preparing") getColor(R.color.primary_color)
                else if (getStatusIndex(statusLower) > 2) getColor(R.color.success)
                else getColor(R.color.on_surface_variant)
            )
            
            // Ready
            ivStatusReady.setImageResource(
                if (statusLower == "ready") R.drawable.ic_status_active
                else if (getStatusIndex(statusLower) > 3) R.drawable.ic_status_completed
                else R.drawable.ic_status_pending
            )
            tvStatusReady.setTextColor(
                if (statusLower == "ready") getColor(R.color.primary_color)
                else if (getStatusIndex(statusLower) > 3) getColor(R.color.success)
                else getColor(R.color.on_surface_variant)
            )
            
            // Completed
            ivStatusCompleted.setImageResource(
                if (statusLower == "completed") R.drawable.ic_status_completed
                else R.drawable.ic_status_pending
            )
            tvStatusCompleted.setTextColor(
                if (statusLower == "completed") getColor(R.color.success)
                else getColor(R.color.on_surface_variant)
            )
            
            // Update main status icon and message
            updateMainStatusDisplay(statusLower)
        }
    }

    private fun getStatusIndex(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> 0
            "confirmed" -> 1
            "preparing" -> 2
            "ready" -> 3
            "completed" -> 4
            "cancelled" -> -1
            else -> 0
        }
    }

    private fun updateMainStatusDisplay(status: String) {
        binding.apply {
            when (status) {
                "pending" -> {
                    ivMainStatus.setImageResource(R.drawable.ic_status_pending_large)
                    tvStatusMessage.text = getString(R.string.tracking_status_pending)
                }
                "confirmed" -> {
                    ivMainStatus.setImageResource(R.drawable.ic_status_confirmed_large)
                    tvStatusMessage.text = getString(R.string.tracking_status_confirmed)
                }
                "preparing" -> {
                    ivMainStatus.setImageResource(R.drawable.ic_status_preparing_large)
                    tvStatusMessage.text = getString(R.string.tracking_status_preparing)
                }
                "ready" -> {
                    ivMainStatus.setImageResource(R.drawable.ic_status_ready_large)
                    tvStatusMessage.text = getString(R.string.tracking_status_ready)
                }
                "completed" -> {
                    ivMainStatus.setImageResource(R.drawable.ic_status_completed_large)
                    tvStatusMessage.text = getString(R.string.tracking_status_completed)
                }
                "cancelled" -> {
                    ivMainStatus.setImageResource(R.drawable.ic_status_cancelled_large)
                    tvStatusMessage.text = getString(R.string.tracking_status_cancelled)
                }
                else -> {
                    ivMainStatus.setImageResource(R.drawable.ic_status_pending_large)
                    tvStatusMessage.text = getString(R.string.tracking_status_pending)
                }
            }
        }
    }

    private fun animateStatusChange(newStatus: String) {
        // Animate the status change
        binding.ivMainStatus.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                binding.ivMainStatus.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun setupClickListeners() {
        binding.btnBackToHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    // Disable back button
    override fun onBackPressed() {
        // Do nothing - users should use the button
    }
}

