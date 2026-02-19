package com.mnsf.resturantandroid.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mnsf.resturantandroid.network.Address
import com.mnsf.resturantandroid.ui.checkout.ConfirmLocationActivity
import com.mnsf.resturantandroid.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityMainBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.AuthRepository
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.ui.auth.LoginActivity
import android.content.Context
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.viewmodel.CartViewModel
import com.mnsf.resturantandroid.viewmodel.OrderViewModel
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    /** Set true to skip activity_main and all setup; show only "Hello World" after login (find crash). Set false and rebuild to run real app. */
    private val MAIN_ACTIVITY_TEST_HELLO = false

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private var cartBadge: BadgeDrawable? = null
    private var deliverToCustomView: View? = null
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var cartViewModel: CartViewModel
    private lateinit var orderViewModel: OrderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("LoginFlow", "MainActivity onCreate ENTERED")
        super.onCreate(savedInstanceState)

        Log.e("LoginFlow", "MainActivity: super.onCreate() done, creating SessionManager")
        sessionManager = SessionManager(this)

        // TEST: skip activity_main and all setup; show only Hello World. No inflation, no toolbar, no nav.
        if (MAIN_ACTIVITY_TEST_HELLO) {
            Log.e("LoginFlow", "MainActivity: in TEST_HELLO block")
            if (!sessionManager.isLoggedIn()) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
            Log.e("LoginFlow", "MainActivity: creating Hello World TextView")
            val tv = TextView(this).apply {
                text = "Hello World"
                textSize = 28f
                setPadding(48, 48, 48, 48)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply { gravity = Gravity.CENTER }
            }
            Log.e("LoginFlow", "MainActivity: calling setContentView(Hello World)")
            setContentView(tv)
            Log.e("LoginFlow", "MainActivity: setContentView done - you should see Hello World")
            return
        }
        
        try {
            Log.e("LoginFlow", "MainActivity: about to inflate activity_main.xml")
            Log.d("MainActivity", "onCreate: Starting MainActivity")
            
            try {
                Log.d("MainActivity", "onCreate: Inflating activity_main.xml...")
                binding = ActivityMainBinding.inflate(layoutInflater)
                Log.e("LoginFlow", "MainActivity: activity_main.xml inflated OK")
                Log.d("MainActivity", "onCreate: ActivityMainBinding inflated OK")
            } catch (e: Exception) {
                Log.e("LoginFlow", "MainActivity INFLATE ERROR: ${e.javaClass.simpleName}: ${e.message}")
                Log.e("LoginFlow", "  cause: ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}")
                Log.e("MainActivity", "onCreate: BINARY XML / INFLATE ERROR in activity_main", e)
                throw e
            }
            try {
                Log.e("LoginFlow", "MainActivity: about to setContentView(binding.root)")
                setContentView(binding.root)
                Log.e("LoginFlow", "MainActivity: setContentView done")
            } catch (e: Exception) {
                Log.e("LoginFlow", "MainActivity setContentView ERROR: ${e.message}")
                Log.e("MainActivity", "onCreate: setContentView failed", e)
                throw e
            }

            // Check if user is logged in, if not redirect to login
            if (!sessionManager.isLoggedIn()) {
                Log.d("MainActivity", "onCreate: User not logged in, redirecting to LoginActivity")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
            
            Log.e("LoginFlow", "MainActivity: user logged in, initializing ViewModels")
            try {
                val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
                val authRepository = AuthRepository(RetrofitClient.apiService, sessionManager)
                val orderRepository = OrderRepository(RetrofitClient.apiService, sessionManager)
                restaurantViewModel = ViewModelProvider(this, RestaurantViewModelFactory(restaurantRepository))[RestaurantViewModel::class.java]
                cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]
                orderViewModel = ViewModelProvider(this, OrderViewModelFactory(orderRepository, sessionManager))[OrderViewModel::class.java]
                Log.e("LoginFlow", "MainActivity: ViewModels created")
            } catch (e: Exception) {
                Log.e("LoginFlow", "MainActivity ViewModels ERROR: ${e.message}")
                throw e
            }

            try {
                Log.e("LoginFlow", "MainActivity: about to setSupportActionBar")
                setSupportActionBar(binding.toolbar)
                Log.e("LoginFlow", "MainActivity: setSupportActionBar done, getting navController")
                val navView: BottomNavigationView = binding.navView
                navView.background = null
                binding.navDrawer.setBackgroundColor(ContextCompat.getColor(this, R.color.surface))
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                Log.e("LoginFlow", "MainActivity: navController got, setupWithNavController")
                navView.setupWithNavController(navController)
                Log.e("LoginFlow", "MainActivity: setupDrawerNavigation")
                setupDrawerNavigation(navController)
                Log.e("LoginFlow", "MainActivity: setupDrawerHeader")
                setupDrawerHeader()
                Log.e("LoginFlow", "MainActivity: onCreate completed successfully")
            } catch (e: Exception) {
                Log.e("LoginFlow", "MainActivity NAV ERROR: ${e.javaClass.simpleName}: ${e.message}")
                Log.e("MainActivity", "onCreate: Error setting up navigation", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "onCreate: Fatal error in onCreate", e)
            e.printStackTrace()
            // Show error to user
            android.widget.Toast.makeText(this, "Error initializing app: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.e("LoginFlow", "MainActivity onCreateOptionsMenu ENTERED")
        if (MAIN_ACTIVITY_TEST_HELLO) return false
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Get cart menu item
        val cartItem = menu.findItem(R.id.action_cart)
        
        // Create and attach badge to cart icon
        cartBadge = BadgeDrawable.create(this)
        cartBadge?.let { badge ->
            // Set badge properties
            badge.backgroundColor = getColor(R.color.error) // Red color for better visibility
            badge.badgeTextColor = getColor(R.color.white)
            badge.maxCharacterCount = 2 // Show "99+" for counts > 99
            badge.number = 0
            badge.isVisible = false
            
            // Attach badge to the toolbar with the menu item ID
            // We need to do this after the menu is inflated and toolbar is laid out
            binding.toolbar.post {
                try {
                    Log.e("LoginFlow", "MainActivity onCreateOptionsMenu: toolbar.post runnable STARTED")
                    BadgeUtils.attachBadgeDrawable(badge, binding.toolbar, R.id.action_cart)
                } catch (e: Exception) {
                    Log.e("LoginFlow", "MainActivity toolbar.post CRASH: ${e.javaClass.simpleName}: ${e.message}", e)
                    Log.e("MainActivity", "Error attaching badge: ${e.message}", e)
                    // Fallback: try to find the icon view manually
                    val iconView = binding.toolbar.findViewById<View>(R.id.action_cart)
                    if (iconView != null) {
                        BadgeUtils.attachBadgeDrawable(badge, iconView)
                    }
                }
            }
        }
        
        Log.e("LoginFlow", "MainActivity onCreateOptionsMenu: about to observe cartItems")
        // Observe cart changes and update badge
        cartViewModel.cartItems.observe(this) { items ->
            val count = cartViewModel.getItemCount()
            updateCartBadge(count)
            
            // Also update title (optional, can remove if badge is enough)
            if (count > 0) {
                cartItem?.title = "${getString(R.string.cart)} ($count)"
            } else {
                cartItem?.title = getString(R.string.cart)
            }
        }
        
        Log.e("LoginFlow", "MainActivity onCreateOptionsMenu: completed, return true")
        return true
    }
    
    private fun setupDrawerNavigation(navController: androidx.navigation.NavController) {
        // Configure which destinations should show the drawer icon
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications
            ),
            binding.drawerLayout
        )
        
                // Setup ActionBar with NavController and DrawerLayout
                setupActionBarWithNavController(navController, appBarConfiguration)
                
                // When on Home tab: show "Deliver to [label]" + arrow instead of "Restaurants"
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    if (destination.id == R.id.navigation_home) {
                        supportActionBar?.setDisplayShowTitleEnabled(false)
                    if (deliverToCustomView == null) {
                        deliverToCustomView = LayoutInflater.from(this).inflate(R.layout.view_toolbar_deliver_to, binding.toolbar, false)
                        deliverToCustomView?.setOnClickListener { showChooseLocationBottomSheet() }
                    }
                        deliverToCustomView?.let { v ->
                            if (v.parent != null) (v.parent as? android.view.ViewGroup)?.removeView(v)
                            binding.toolbar.addView(v)
                            updateMainDeliverToHeader()
                        }
                    } else {
                        supportActionBar?.setDisplayShowTitleEnabled(true)
                        deliverToCustomView?.let { v ->
                            (v.parent as? android.view.ViewGroup)?.removeView(v)
                        }
                    }
                }
                // Apply "Deliver to" header if start destination is Home (listener may not run for initial destination)
                if (navController.currentDestination?.id == R.id.navigation_home) {
                    supportActionBar?.setDisplayShowTitleEnabled(false)
                    if (deliverToCustomView == null) {
                        deliverToCustomView = LayoutInflater.from(this).inflate(R.layout.view_toolbar_deliver_to, binding.toolbar, false)
                        deliverToCustomView?.setOnClickListener { showChooseLocationBottomSheet() }
                    }
                    deliverToCustomView?.let { v ->
                        if (v.parent != null) (v.parent as? android.view.ViewGroup)?.removeView(v)
                        binding.toolbar.addView(v)
                        updateMainDeliverToHeader()
                    }
                }
                
                // Handle drawer menu item clicks manually (don't use setupWithNavController to avoid conflicts)
        binding.navDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_account -> {
                    startActivity(Intent(this, com.mnsf.resturantandroid.ui.account.AccountActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_language -> {
                    switchLanguage()
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    handleLogout()
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }
    
    private fun setupDrawerHeader() {
        val headerView = binding.navDrawer.getHeaderView(0)
        val tvUserName = headerView.findViewById<android.widget.TextView>(R.id.tv_user_name)
        val tvUserEmail = headerView.findViewById<android.widget.TextView>(R.id.tv_user_email)
        
        // Set user info from session
        val userName = sessionManager.getCustomerName() ?: getString(R.string.app_name)
        val userEmail = sessionManager.getCustomerEmail() ?: ""
        
        tvUserName.text = userName
        tvUserEmail.text = userEmail
        
        // Make header clickable to open AccountActivity
        headerView.setOnClickListener {
            startActivity(Intent(this, com.mnsf.resturantandroid.ui.account.AccountActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
    
    private fun switchLanguage() {
        val currentLanguage = LocaleHelper.getLocale(this)
        val newLanguage = if (currentLanguage == "ar") "en" else "ar"
        
        // Save new language preference
        LocaleHelper.setLocale(this, newLanguage)
        
        // Restart the activity to apply language change
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun handleLogout() {
        sessionManager.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        if (MAIN_ACTIVITY_TEST_HELLO) return super.onSupportNavigateUp()
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(binding.drawerLayout) || super.onSupportNavigateUp()
    }
    
    private fun updateCartBadge(count: Int) {
        cartBadge?.let { badge ->
            if (count > 0) {
                badge.number = count
                badge.isVisible = true
            } else {
                badge.isVisible = false
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (MAIN_ACTIVITY_TEST_HELLO) return
        try {
            Log.e("LoginFlow", "MainActivity onResume: about to setupDrawerHeader")
            setupDrawerHeader()
            Log.e("LoginFlow", "MainActivity onResume: setupDrawerHeader done, refreshCart")
            cartViewModel.refreshCart()
            Log.e("LoginFlow", "MainActivity onResume: refreshCart done, checking destination")
            if (findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id == R.id.navigation_home) {
                Log.e("LoginFlow", "MainActivity onResume: on Home, updateMainDeliverToHeader")
                updateMainDeliverToHeader()
            }
            Log.e("LoginFlow", "MainActivity onResume: completed")
        } catch (e: Exception) {
            Log.e("LoginFlow", "MainActivity onResume CRASH: ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }

    private fun updateMainDeliverToHeader() {
        // Prefer short label (address_type e.g. Apartment/House/Office) so the arrow is visible
        val raw = sessionManager.getDeliveryLabel()
            ?: (if (sessionManager.getCustomerAddress() != null) getString(R.string.label_current_location) else null)
            ?: getString(R.string.deliver_to_placeholder)
        val label = if (raw.length > 20) "${raw.take(20)}…" else raw
        binding.toolbar.findViewById<TextView>(R.id.tvDeliverTo)?.text = getString(R.string.deliver_to, label)
    }

    private fun showChooseLocationBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_choose_location, null)
        dialog.setContentView(sheetView)
        sheetView.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        val containerSaved = sheetView.findViewById<android.widget.LinearLayout>(R.id.containerSavedAddresses)
        val tvSavedLabel = sheetView.findViewById<TextView>(R.id.tvSavedAddressesLabel)
        val customerId = sessionManager.getCustomerId()
        val token = sessionManager.getAuthToken()
        if (customerId != -1 && !token.isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.getAddresses(customerId, "Bearer $token")
                    }
                    if (response.isSuccessful) {
                        val addresses = response.body()?.addresses ?: emptyList()
                        runOnUiThread {
                            if (addresses.isNotEmpty()) {
                                tvSavedLabel.visibility = View.VISIBLE
                                containerSaved.visibility = View.VISIBLE
                                containerSaved.removeAllViews()
                                val defaultAddr = addresses.firstOrNull { it.is_default } ?: addresses.first()
                                addresses.forEach { addr ->
                                    val item = LayoutInflater.from(this@MainActivity)
                                        .inflate(R.layout.item_saved_address, containerSaved, false)
                                    val label = addr.address_type?.replaceFirstChar { it.uppercase() }
                                        ?: addr.address_label?.ifBlank { null }
                                        ?: getString(R.string.delivery_address)
                                    val full = listOfNotNull(
                                        addr.street,
                                        addr.building_name,
                                        addr.apartment_number,
                                        addr.floor,
                                        addr.area
                                    ).joinToString(", ")
                                    item.findViewById<TextView>(R.id.tvAddressLabel).text = label
                                    item.findViewById<TextView>(R.id.tvAddressFull).text = full
                                    item.findViewById<ImageView>(R.id.imgSelected).visibility =
                                        if (addr.id == defaultAddr.id) View.VISIBLE else View.GONE
                                    item.setOnClickListener {
                                        selectSavedAddress(addr)
                                        dialog.dismiss()
                                    }
                                    containerSaved.addView(item)
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
        val currentAddr = sessionManager.getCustomerAddress()
        sheetView.findViewById<TextView>(R.id.tvCurrentLocationAddress).text =
            currentAddr?.take(60)?.plus(if ((currentAddr.length) > 60) "…" else "") ?: ""
        sheetView.findViewById<View>(R.id.cardDifferentLocation).setOnClickListener {
            sessionManager.saveOrderType("delivery")
            startActivity(ConfirmLocationActivity.newIntent(this, -1))
            dialog.dismiss()
        }
        sheetView.findViewById<View>(R.id.cardCurrentLocation).setOnClickListener {
            sessionManager.saveOrderType("delivery")
            if (LocationHelper.hasLocationPermission(this)) {
                fetchCurrentLocationAndSetDelivery()
                dialog.dismiss()
            } else {
                LocationHelper.requestLocationPermissions(this, 1001)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun selectSavedAddress(addr: Address) {
        val label = addr.address_type?.replaceFirstChar { it.uppercase() }
            ?: addr.address_label?.ifBlank { null }
            ?: getString(R.string.delivery_address)
        val full = listOfNotNull(
            addr.street,
            addr.building_name,
            addr.apartment_number,
            addr.floor,
            addr.area
        ).joinToString(", ")
        sessionManager.saveDeliveryLabel(label)
        sessionManager.saveCustomerInfo(
            sessionManager.getCustomerId(),
            sessionManager.getCustomerName() ?: "",
            sessionManager.getCustomerEmail(),
            sessionManager.getCustomerPhone(),
            full.ifBlank { null }
        )
        sessionManager.saveOrderType("delivery")
        updateMainDeliverToHeader()
    }

    private fun fetchCurrentLocationAndSetDelivery() {
        lifecycleScope.launch {
            LocationHelper.getCurrentLocation(this@MainActivity)?.let { location ->
                val address = withContext(Dispatchers.IO) {
                    LocationHelper.getAddressFromLocation(
                        this@MainActivity,
                        location.latitude,
                        location.longitude
                    )
                }
                address?.let {
                    val label = getString(R.string.label_current_location)
                    sessionManager.saveOrderType("delivery")
                    if (sessionManager.getCustomerId() != -1) {
                        sessionManager.saveDeliveryLabel(label)
                        sessionManager.saveCustomerInfo(
                            sessionManager.getCustomerId(),
                            sessionManager.getCustomerName() ?: "",
                            sessionManager.getCustomerEmail(),
                            sessionManager.getCustomerPhone(),
                            it
                        )
                    } else {
                        sessionManager.saveDeliveryAddressOnly(it, label)
                    }
                    runOnUiThread { updateMainDeliverToHeader() }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                val intent = Intent(this, com.mnsf.resturantandroid.ui.cart.CartActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        if (MAIN_ACTIVITY_TEST_HELLO) {
            super.onBackPressed()
            return
        }
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

class RestaurantViewModelFactory(
    private val restaurantRepository: RestaurantRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RestaurantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RestaurantViewModel(restaurantRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
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

