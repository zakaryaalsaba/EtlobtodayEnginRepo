package com.order.resturantandroid.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.order.resturantandroid.R
import com.order.resturantandroid.databinding.ActivityDashboardBinding
import com.order.resturantandroid.ui.auth.LoginActivity
import com.order.resturantandroid.ui.orders.OrderDetailActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.order.resturantandroid.data.remote.DeviceTokenRequest
import com.order.resturantandroid.data.remote.RetrofitClient
import com.order.resturantandroid.ui.profile.ProfileActivity
import com.order.resturantandroid.ui.statistics.StatisticsActivity
import com.order.resturantandroid.ui.delivery.RequestDeliveryActivity
import com.order.resturantandroid.util.LocaleHelper
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels {
        AndroidViewModelFactory.getInstance(application)
    }
    private lateinit var sessionManager: SessionManager
    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        // Check if logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupObservers()
        setupRefresh()
        
        // Register device token for push notifications
        registerDeviceToken()
        
        // Load orders
        viewModel.loadOrders()
        
        // Start polling for real-time updates
        viewModel.startPolling()
    }
    
    private fun registerDeviceToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")

            // Send token to backend
            val authToken = sessionManager.getAuthToken()
            if (authToken != null && token != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val apiService = RetrofitClient.apiService
                        val response = apiService.updateDeviceToken(
                            DeviceTokenRequest(token, "android"),
                            "Bearer $authToken"
                        )
                        
                        if (response.isSuccessful) {
                            Log.d(TAG, "Device token registered successfully")
                        } else {
                            Log.e(TAG, "Failed to register device token: ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error registering device token", e)
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh orders when returning to screen
        viewModel.loadOrders()
    }
    
    override fun onPause() {
        super.onPause()
        // Optionally stop polling when app is in background
        // viewModel.stopPolling()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Set custom centered title
        val restaurantName = sessionManager.getRestaurantName() ?: getString(R.string.dashboard)
        binding.toolbarTitle.text = restaurantName
    }
    
    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ).apply {
            isDrawerIndicatorEnabled = true
        }
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        
        binding.navView.setNavigationItemSelectedListener(this)
        
        // Setup drawer header
        val headerView = binding.navView.getHeaderView(0)
        val tvRestaurantName = headerView.findViewById<android.widget.TextView>(R.id.navHeaderRestaurantName)
        val tvAdminEmail = headerView.findViewById<android.widget.TextView>(R.id.navHeaderAdminEmail)
        
        tvRestaurantName?.text = sessionManager.getRestaurantName() ?: getString(R.string.dashboard)
        tvAdminEmail?.text = sessionManager.getAdminEmail() ?: ""
    }
    
    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter { order ->
            try {
                android.util.Log.d("DashboardActivity", "Opening order details for order ID: ${order.id}, Number: ${order.orderNumber}")
                val intent = Intent(this, OrderDetailActivity::class.java)
                intent.putExtra("order_id", order.id)
                intent.putExtra("order_number", order.orderNumber)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error opening order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = ordersAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.orders.observe(this) { orders ->
            try {
                val hasOrders = !orders.isNullOrEmpty()
                ordersAdapter.submitList(orders ?: emptyList())
                
                // Show/hide RecyclerView and empty state
                binding.recyclerViewOrders.visibility = if (hasOrders) android.view.View.VISIBLE else android.view.View.GONE
                binding.emptyState.visibility = if (hasOrders) android.view.View.GONE else android.view.View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error displaying orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            try {
                binding.swipeRefresh.isRefreshing = isLoading ?: false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadOrders()
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_orders -> {
                // Already on orders list, just close drawer
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_statistics -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                startActivity(Intent(this, StatisticsActivity::class.java))
            }
            R.id.nav_request_delivery -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                startActivity(Intent(this, RequestDeliveryActivity::class.java))
            }
            R.id.nav_profile -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            R.id.nav_language -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                showLanguageDialog()
            }
            R.id.nav_signout -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                showLogoutDialog()
            }
        }
        return true
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.english),
            getString(R.string.arabic)
        )
        
        val currentLang = LocaleHelper.getPersistedLocale(this)
        val selectedIndex = if (currentLang == "ar") 1 else 0
        
        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                val newLang = if (which == 1) "ar" else "en"
                if (newLang != currentLang) {
                    LocaleHelper.setLocale(this, newLang)
                    recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setPositiveButton(R.string.logout) { _, _ ->
                sessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    companion object {
        private const val TAG = "DashboardActivity"
    }
}

