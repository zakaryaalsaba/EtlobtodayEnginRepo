package com.mnsf.resturantandroid.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityOrderConfirmationBinding
import com.mnsf.resturantandroid.data.model.Order
import com.mnsf.resturantandroid.ui.MainActivity
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.launch

class OrderConfirmationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOrderConfirmationBinding
    private var order: Order? = null
    /** Total to show (from checkout). When non-null, use instead of order.total_amount so confirmation matches checkout. */
    private var displayTotalAmount: Double? = null
    private var currencyCode: String? = null
    private var currencySymbolPosition: String? = null
    private lateinit var orderRepository: OrderRepository
    private lateinit var restaurantViewModel: RestaurantViewModel
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set toolbar as action bar (without back button)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
        supportActionBar?.title = getString(R.string.order_confirmed)
        
        // Initialize repositories
        orderRepository = OrderRepository(RetrofitClient.apiService, com.mnsf.resturantandroid.util.SessionManager(this))
        val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        restaurantViewModel = ViewModelProvider(
            this,
            com.mnsf.resturantandroid.ui.restaurant.RestaurantViewModelFactory(restaurantRepository)
        )[RestaurantViewModel::class.java]
        
        // Get order from intent (passed as Serializable or JSON)
        val orderFromIntent = intent.getSerializableExtra("order") as? Order
        order = orderFromIntent
        // Total from checkout (so confirmation shows same price as checkout); fallback to order.total_amount from API
        displayTotalAmount = if (intent.hasExtra("display_total_amount")) intent.getDoubleExtra("display_total_amount", 0.0) else null
        Log.d("OrderConfirmation", "onCreate: Order from intent - ${if (orderFromIntent != null) "Found: ${orderFromIntent.order_number}, items: ${orderFromIntent.items?.size ?: 0}, displayTotal=$displayTotalAmount" else "null"}")
        
        if (orderFromIntent == null) {
            // Try to get order number and fetch from API if needed
            val orderNumber = intent.getStringExtra("order_number")
            if (orderNumber != null) {
                fetchOrderFromAPI(orderNumber)
                return
            } else {
                Toast.makeText(this, getString(R.string.order_not_found), Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }
        
        // Load restaurant currency info
        orderFromIntent?.website_id?.let { websiteId ->
            loadRestaurantCurrency(websiteId)
        }
        
        displayOrderConfirmation()
        setupClickListeners()
    }
    
    private fun fetchOrderFromAPI(orderNumber: String) {
        lifecycleScope.launch {
            try {
                val result = orderRepository.getOrderByNumber(orderNumber)
                result.onSuccess { fetchedOrder ->
                    order = fetchedOrder
                    Log.d("OrderConfirmation", "fetchOrderFromAPI: Order fetched - number=${fetchedOrder.order_number}, items: ${fetchedOrder.items?.size ?: 0}")
                    // Load restaurant currency info
                    order?.website_id?.let { websiteId ->
                        loadRestaurantCurrency(websiteId)
                    }
                    displayOrderConfirmation()
                    setupClickListeners()
                }.onFailure { exception ->
                    Log.e("OrderConfirmation", "Failed to fetch order: ${exception.message}")
                    Toast.makeText(this@OrderConfirmationActivity, 
                        "Failed to load order: ${exception.message}", 
                        Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("OrderConfirmation", "Error fetching order", e)
                Toast.makeText(this@OrderConfirmationActivity, 
                    "Error loading order: ${e.message}", 
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun loadRestaurantCurrency(websiteId: Int) {
        restaurantViewModel.loadRestaurant(websiteId)
        restaurantViewModel.selectedRestaurant.observe(this) { restaurant ->
            restaurant?.let {
                currencyCode = it.currency_code
                currencySymbolPosition = it.currency_symbol_position
                // Update display with currency
                displayOrderConfirmation()
            }
        }
    }
    
    private fun displayOrderConfirmation() {
        order?.let {
            // Use checkout total when passed so confirmation shows same price as checkout
            val totalToShow = displayTotalAmount ?: it.total_amount
            Log.d("OrderConfirmation", "displayOrderConfirmation: Order number=${it.order_number}, items=${it.items?.size ?: 0}, total=$totalToShow (from ${if (displayTotalAmount != null) "checkout" else "API"})")
            binding.tvOrderNumber.text = it.order_number ?: "N/A"
            binding.tvOrderStatus.text = formatStatus(it.status)
            
            // Format total amount with currency (same as checkout when display_total_amount was passed)
            val formattedTotal = CurrencyFormatter.formatPrice(
                totalToShow,
                currencyCode,
                currencySymbolPosition
            )
            binding.tvTotalAmount.text = formattedTotal
            
            // Display order items if available
            it.items?.let { items ->
                Log.d("OrderConfirmation", "displayOrderConfirmation: Displaying ${items.size} items")
                if (items.isEmpty()) {
                    binding.tvOrderItems.text = getString(R.string.no_items_available)
                } else {
                    val itemsText = items.joinToString("\n") { item ->
                        val formattedSubtotal = CurrencyFormatter.formatPrice(
                            item.subtotal,
                            currencyCode,
                            currencySymbolPosition
                        )
                        "${item.product_name} x${item.quantity} - $formattedSubtotal"
                    }
                    binding.tvOrderItems.text = itemsText
                }
            } ?: run {
                Log.w("OrderConfirmation", "displayOrderConfirmation: Order items is null")
                binding.tvOrderItems.text = getString(R.string.no_items_available)
            }
        } ?: run {
            // Order is null, show error message
            binding.tvOrderNumber.text = "Error"
            binding.tvOrderStatus.text = getString(R.string.order_not_found)
            binding.tvTotalAmount.text = "0.00"
        }
    }
    
    private fun formatStatus(status: String?): String {
        if (status == null || status.isBlank()) {
            return getString(R.string.status_pending)
        }
        return when (status.lowercase()) {
            "pending" -> getString(R.string.status_pending)
            "confirmed" -> getString(R.string.status_confirmed)
            "preparing" -> getString(R.string.status_preparing)
            "ready" -> getString(R.string.status_ready)
            "completed" -> getString(R.string.status_completed)
            "cancelled" -> getString(R.string.status_cancelled)
            else -> status
        }
    }
    
    private fun setupClickListeners() {
        binding.btnTrackOrder.setOnClickListener {
            // Navigate to order tracking
            order?.let { currentOrder ->
                val intent = Intent(this, OrderTrackingActivity::class.java)
                intent.putExtra("order", currentOrder)
                intent.putExtra("order_number", currentOrder.order_number)
                startActivity(intent)
            }
        }
        
        binding.btnViewHistory.setOnClickListener {
            // Navigate to order history
            val intent = Intent(this, OrderHistoryActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnBackToRestaurants.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
    
    // Disable back button - users should use the buttons on the screen
    override fun onBackPressed() {
        // Do nothing - prevent back button from working
        // Users should use "Back to Restaurants" or "View Order History" buttons
    }
}

