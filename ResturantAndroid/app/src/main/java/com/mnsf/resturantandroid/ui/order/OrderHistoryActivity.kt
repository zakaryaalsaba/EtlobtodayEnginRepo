package com.mnsf.resturantandroid.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityOrderHistoryBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.ui.order.RestaurantCurrencyInfo
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.viewmodel.OrderViewModel
import com.mnsf.resturantandroid.viewmodel.CartViewModel
import com.mnsf.resturantandroid.data.model.Order
import com.mnsf.resturantandroid.data.model.Product
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class OrderHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOrderHistoryBinding
    private lateinit var orderViewModel: OrderViewModel
    private lateinit var cartViewModel: CartViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var restaurantRepository: RestaurantRepository
    private val restaurantCurrencyMap = mutableMapOf<Int, RestaurantCurrencyInfo>()
    private var allOrders: List<Order> = emptyList()
    private var currentTab: Int = 0 // 0 = Active, 1 = Archive
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set toolbar as action bar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = getString(R.string.order_history)
        
        // Set navigation click listener
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        sessionManager = SessionManager(this)
        
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.please_login_to_view_orders), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val orderRepository = OrderRepository(RetrofitClient.apiService, sessionManager)
        orderViewModel = ViewModelProvider(
            this,
            OrderViewModelFactory(orderRepository, sessionManager)
        )[OrderViewModel::class.java]
        
        restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]
        
        setupTabs()
        setupRecyclerView()
        setupObservers()
        orderViewModel.loadOrders()
    }
    
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_active))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_archive))
        
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.let {
                    currentTab = it.position
                    filterAndDisplayOrders()
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun setupRecyclerView() {
        // This will be called after we know which tab is selected
        orderAdapter = OrderAdapter(
            onItemClick = { order ->
                // Navigate to order details
                val intent = Intent(this, OrderConfirmationActivity::class.java)
                intent.putExtra("order", order as java.io.Serializable)
                startActivity(intent)
            },
            onReorderClick = { order ->
                handleReorder(order)
            },
            onTrackClick = { order ->
                // Navigate to order tracking
                val intent = Intent(this, OrderTrackingActivity::class.java)
                intent.putExtra("order", order as java.io.Serializable)
                intent.putExtra("order_number", order.order_number)
                startActivity(intent)
            },
            restaurantCurrencyMap = emptyMap(),
            showReorderButton = currentTab == 1, // Show reorder button in Archive tab
            showTrackButton = true // Show track button for active orders
        )
        binding.recyclerViewOrders.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewOrders.adapter = orderAdapter
    }
    
    private fun filterAndDisplayOrders() {
        val filteredOrders = if (currentTab == 0) {
            // Active orders: pending, confirmed, preparing, ready
            allOrders.filter { 
                it.status.lowercase() in listOf("pending", "confirmed", "preparing", "ready")
            }
        } else {
            // Archive orders: completed, cancelled
            allOrders.filter { 
                it.status.lowercase() in listOf("completed", "cancelled")
            }
        }
        
        orderAdapter.setShowReorderButton(currentTab == 1)
        orderAdapter.setShowTrackButton(currentTab == 0) // Show track button in Active tab
        orderAdapter.submitList(filteredOrders)
        
        if (filteredOrders.isEmpty()) {
            binding.textEmpty.visibility = android.view.View.VISIBLE
            binding.recyclerViewOrders.visibility = android.view.View.GONE
        } else {
            binding.textEmpty.visibility = android.view.View.GONE
            binding.recyclerViewOrders.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun handleReorder(order: Order) {
        if (order.items.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.reorder_error), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading dialog
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.reorder_loading))
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                // Fetch products for this restaurant
                val productsResult = restaurantRepository.getProducts(order.website_id)
                
                productsResult.onSuccess { products ->
                    val productMap = products.associateBy { it.id }
                    var itemsAdded = 0
                    var itemsFailed = 0
                    
                    // Check if cart needs to be cleared
                    val firstProduct = order.items.firstOrNull()?.let { orderItem ->
                        productMap[orderItem.product_id]
                    }
                    
                    if (firstProduct != null) {
                        if (cartViewModel.isEmpty()) {
                            // Cart is empty, just add items
                            addOrderItemsToCart(order, productMap)
                            itemsAdded = order.items.size
                        } else if (cartViewModel.requiresCartClear(firstProduct)) {
                            // Cart has items from different restaurant
                            MaterialAlertDialogBuilder(this@OrderHistoryActivity)
                                .setTitle(getString(R.string.clear_cart))
                                .setMessage(getString(R.string.cart_contains_different_restaurant, "this restaurant"))
                                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                    loadingDialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.clear_cart_and_add)) { dialog, _ ->
                                    cartViewModel.clearCart()
                                    addOrderItemsToCart(order, productMap)
                                    itemsAdded = order.items.size
                                    loadingDialog.dismiss()
                                    Toast.makeText(this@OrderHistoryActivity, getString(R.string.reorder_success), Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                                .show()
                            return@launch
                        } else {
                            // Same restaurant, just add items
                            addOrderItemsToCart(order, productMap)
                            itemsAdded = order.items.size
                        }
                    } else {
                        itemsFailed = order.items.size
                    }
                    
                    loadingDialog.dismiss()
                    
                    if (itemsAdded > 0) {
                        Toast.makeText(this@OrderHistoryActivity, getString(R.string.reorder_success), Toast.LENGTH_SHORT).show()
                        // Navigate to cart
                        val intent = Intent(this@OrderHistoryActivity, com.mnsf.resturantandroid.ui.cart.CartActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@OrderHistoryActivity, getString(R.string.reorder_error), Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { exception ->
                    loadingDialog.dismiss()
                    Toast.makeText(this@OrderHistoryActivity, "${getString(R.string.reorder_error)}: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(this@OrderHistoryActivity, "${getString(R.string.reorder_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun addOrderItemsToCart(order: Order, productMap: Map<Int, Product>) {
        order.items?.forEach { orderItem ->
            val product = productMap[orderItem.product_id]
            if (product != null && product.is_available) {
                // Add product to cart with the same quantity
                repeat(orderItem.quantity) {
                    cartViewModel.addToCart(product)
                }
            }
        }
    }
    
    private fun setupObservers() {
        orderViewModel.orders.observe(this) { orders ->
            allOrders = orders
            filterAndDisplayOrders()
            
            // Load currency info for all unique restaurants
            loadRestaurantCurrencies(orders.map { it.website_id }.distinct())
        }
        
        orderViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
        
        orderViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    private fun loadRestaurantCurrencies(websiteIds: List<Int>) {
        lifecycleScope.launch {
            websiteIds.forEach { websiteId ->
                // Check if we already have currency info for this restaurant
                if (!restaurantCurrencyMap.containsKey(websiteId)) {
                    try {
                        val result = restaurantRepository.getRestaurant(websiteId)
                        result.onSuccess { restaurant ->
                            restaurantCurrencyMap[websiteId] = RestaurantCurrencyInfo(
                                currencyCode = restaurant.currency_code,
                                currencySymbolPosition = restaurant.currency_symbol_position
                            )
                            // Update adapter with new currency map
                            orderAdapter.updateCurrencyMap(restaurantCurrencyMap.toMap())
                        }.onFailure {
                            // If restaurant fetch fails, use default currency
                            restaurantCurrencyMap[websiteId] = RestaurantCurrencyInfo(
                                currencyCode = null,
                                currencySymbolPosition = null
                            )
                            orderAdapter.updateCurrencyMap(restaurantCurrencyMap.toMap())
                        }
                    } catch (e: Exception) {
                        // If restaurant fetch fails, use default currency
                        restaurantCurrencyMap[websiteId] = RestaurantCurrencyInfo(
                            currencyCode = null,
                            currencySymbolPosition = null
                        )
                        orderAdapter.updateCurrencyMap(restaurantCurrencyMap.toMap())
                    }
                }
            }
        }
    }
}

class OrderViewModelFactory(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderViewModel(orderRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

