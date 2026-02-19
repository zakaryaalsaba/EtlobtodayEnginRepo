package com.mnsf.resturantandroid.ui.restaurant

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityRestaurantDetailsBinding
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.I18nHelper
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.RestaurantRepository
import android.content.Context
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.viewmodel.CartViewModel
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import com.mnsf.resturantandroid.data.model.CartItem
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.data.model.Product
import com.bumptech.glide.Glide
import com.mnsf.resturantandroid.ui.cart.CartActivity
import com.mnsf.resturantandroid.ui.checkout.ConfirmLocationActivity
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.util.LocationHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mnsf.resturantandroid.network.Address
import com.mnsf.resturantandroid.data.model.Branch
import kotlin.math.sqrt
import kotlin.math.pow

class RestaurantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRestaurantDetailsBinding
    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var cartViewModel: CartViewModel
    private lateinit var menuAdapter: MenuPremiumAdapter
    private lateinit var sessionManager: SessionManager
    private var restaurantId: Int = -1
    private var currentRestaurantName: String = ""
    private var currencyCode: String? = null
    private var currencySymbolPosition: String? = null
    private var categoryNames: List<String> = emptyList()
    /** True while we are programmatically scrolling to a tab section; prevents scroll listener from resetting tab to Trending. */
    private var isScrollingToSection = false
    private var isFavorite: Boolean = false
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val FAVORITES_PREFS = "restaurant_favorites"
    
    // Branches
    private var branches: List<Branch> = emptyList()
    private var selectedBranch: Branch? = null
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestaurantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getIntExtra("restaurant_id", -1)
        if (restaurantId == -1) {
            Toast.makeText(this, "Invalid restaurant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d("RestaurantDetails", "restaurant_id (website_id)=$restaurantId — use in SQL: WHERE website_id = $restaurantId")

        sessionManager = SessionManager(this)

        // Set toolbar as action bar
        val toolbar = binding.root.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        restaurantViewModel = ViewModelProvider(
            this,
            RestaurantViewModelFactory(restaurantRepository)
        )[RestaurantViewModel::class.java]
        
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]

        binding.ivRestaurantLogo.visibility = android.view.View.VISIBLE
        binding.tabLayoutCategories.setBackgroundColor(ContextCompat.getColor(this, R.color.surface))

        setupRecyclerView()
        setupObservers()
        setupBottomBarCart()
        setupFloatingActions()
        loadFavoriteState()
        loadDefaultDeliveryAddressAndHeader()
        loadRestaurantBranches()
        setupAddressOrBranchClickListener()
        restaurantViewModel.loadRestaurant(restaurantId)
        restaurantViewModel.loadProducts(restaurantId)
        restaurantViewModel.loadRestaurantOffers(restaurantId)
    }

    override fun onResume() {
        super.onResume()
        updateBottomBarCart()
        refreshMenuCartState()
    }

    /** Load default address from API (addresses table) or request location; then update header. */
    private fun loadDefaultDeliveryAddressAndHeader() {
        lifecycleScope.launch {
            val customerId = sessionManager.getCustomerId()
            val token = sessionManager.getAuthToken()
            if (customerId != -1 && !token.isNullOrBlank()) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.getAddresses(customerId, "Bearer $token")
                    }
                    if (response.isSuccessful) {
                        val addresses = response.body()?.addresses ?: emptyList()
                        val defaultAddr = addresses.firstOrNull { it.is_default } ?: addresses.firstOrNull()
                        defaultAddr?.let { addr ->
                            val label = addr.address_type?.replaceFirstChar { it.uppercase() }
                                ?: addr.address_label?.ifBlank { null }
                                ?: getString(R.string.delivery_address)
                            val fullAddress = listOfNotNull(
                                addr.street,
                                addr.building_name,
                                addr.apartment_number,
                                addr.floor,
                                addr.area
                            ).joinToString(", ")
                            sessionManager.saveDeliveryLabel(label)
                            sessionManager.saveCustomerInfo(
                                customerId,
                                sessionManager.getCustomerName() ?: "",
                                sessionManager.getCustomerEmail(),
                                sessionManager.getCustomerPhone(),
                                fullAddress.ifBlank { null }
                            )
                            sessionManager.saveOrderType("delivery")
                            return@launch
                        }
                    }
                } catch (_: Exception) { }
            }
            if (sessionManager.getCustomerAddress().isNullOrEmpty() && sessionManager.getDeliveryLabel() == null) {
                if (LocationHelper.hasLocationPermission(this@RestaurantDetailsActivity)) {
                    fetchCurrentLocationAndSetDelivery()
                } else {
                    LocationHelper.requestLocationPermissions(
                        this@RestaurantDetailsActivity,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    /** Get current location, reverse geocode, save to session and set "Deliver to [address]". */
    private fun fetchCurrentLocationAndSetDelivery() {
        lifecycleScope.launch {
            LocationHelper.getCurrentLocation(this@RestaurantDetailsActivity)?.let { location ->
                val address = withContext(Dispatchers.IO) {
                    LocationHelper.getAddressFromLocation(
                        this@RestaurantDetailsActivity,
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
                }
            }
        }
    }

    /** Load branches for this restaurant and auto-select nearest branch if customer location is available. */
    private fun loadRestaurantBranches() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getRestaurantBranches(restaurantId)
                }
                if (response.isSuccessful) {
                    branches = response.body()?.branches ?: emptyList()
                    Log.d("RestaurantDetails", "Loaded ${branches.size} branches")
                    if (branches.isNotEmpty()) {
                        // Immediately select first branch as fallback, then try to find nearest
                        selectedBranch = branches.first()
                        runOnUiThread { updateRestaurantAddressOrBranch() }
                        // Then try to find nearest branch (async, will update again when done)
                        selectNearestBranch()
                    } else {
                        // No branches, ensure selectedBranch is null
                        selectedBranch = null
                        runOnUiThread { updateRestaurantAddressOrBranch() }
                    }
                } else {
                    Log.e("RestaurantDetails", "Failed to load branches: ${response.code()}")
                    selectedBranch = null
                    runOnUiThread { updateRestaurantAddressOrBranch() }
                }
            } catch (e: Exception) {
                Log.e("RestaurantDetails", "Failed to load branches", e)
                selectedBranch = null
                runOnUiThread { updateRestaurantAddressOrBranch() }
            }
        }
    }
    
    /** Update the address/branch display in the floating card. Shows branch if selected, otherwise restaurant address. */
    private fun updateRestaurantAddressOrBranch() {
        val layoutAddress = binding.layoutRestaurantAddress
        val tvAddress = binding.tvRestaurantAddress
        val imgArrow = binding.root.findViewById<ImageView>(R.id.imgBranchArrow)
        
        Log.d("RestaurantDetails", "updateRestaurantAddressOrBranch: branches=${branches.size}, selectedBranch=${selectedBranch?.id}")
        
        if (branches.isNotEmpty() && selectedBranch != null) {
            // Show selected branch (without city in floating card)
            val branchName = I18nHelper.getBranchNameDisplay(selectedBranch!!, this)
            val branchAddress = selectedBranch!!.address?.ifBlank { null }
            val branchRegion = selectedBranch!!.region_name_ar ?: selectedBranch!!.region_name
            // Exclude city from floating card display
            val addressParts = listOfNotNull(branchAddress, branchRegion)
            
            val displayText = if (addressParts.isNotEmpty()) {
                "$branchName - ${addressParts.joinToString(", ")}"
            } else {
                branchName
            }
            
            Log.d("RestaurantDetails", "Showing branch: $displayText")
            tvAddress.text = displayText
            imgArrow?.visibility = View.VISIBLE
            layoutAddress.visibility = View.VISIBLE
        } else {
            // Show restaurant address (if available)
            val restaurant = restaurantViewModel.selectedRestaurant.value
            val address = restaurant?.let { I18nHelper.getRestaurantAddressDisplay(it, this) }
            Log.d("RestaurantDetails", "Showing restaurant address: $address")
            if (!address.isNullOrBlank()) {
                tvAddress.text = address
                imgArrow?.visibility = View.GONE
                layoutAddress.visibility = View.VISIBLE
            } else {
                layoutAddress.visibility = View.GONE
            }
        }
    }
    
    /** Setup click listener for address/branch section to show branch selection if branches exist. */
    private fun setupAddressOrBranchClickListener() {
        binding.layoutRestaurantAddress.setOnClickListener {
            if (branches.isNotEmpty()) {
                showBranchSelectionBottomSheet()
            }
        }
    }
    
    /** Calculate distance between two lat/lng points using Haversine formula. */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).pow(2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2).pow(2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
    
    /** Select the nearest branch to customer's current location. */
    private fun selectNearestBranch() {
        if (branches.isEmpty()) return
        
        // Try to get customer location from LocationHelper
        lifecycleScope.launch {
            try {
                LocationHelper.getCurrentLocation(this@RestaurantDetailsActivity)?.let { location ->
                    val customerLat = location.latitude
                    val customerLon = location.longitude
                    
                    // Find branch with minimum distance
                    var nearestBranch: Branch? = null
                    var minDistance = Double.MAX_VALUE
                    
                    branches.forEach { branch ->
                        branch.latitude?.let { branchLat ->
                            branch.longitude?.let { branchLon ->
                                val distance = calculateDistance(customerLat, customerLon, branchLat, branchLon)
                                if (distance < minDistance) {
                                    minDistance = distance
                                    nearestBranch = branch
                                }
                            }
                        }
                    }
                    
                    // If no branch has coordinates, select first branch
                    selectedBranch = nearestBranch ?: branches.first()
                    runOnUiThread { updateRestaurantAddressOrBranch() }
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("RestaurantDetails", "Error getting location for branch selection", e)
            }
            
            // Fallback: select first branch if location unavailable
            selectedBranch = branches.first()
            runOnUiThread { updateRestaurantAddressOrBranch() }
        }
    }
    
    /** Show bottom sheet to select a branch. */
    private fun showBranchSelectionBottomSheet() {
        if (branches.isEmpty()) {
            showChooseLocationBottomSheet()
            return
        }
        
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_branch_selection, null)
        dialog.setContentView(sheetView)
        
        sheetView.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        
        val containerBranches = sheetView.findViewById<android.widget.LinearLayout>(R.id.containerBranches)
        containerBranches?.removeAllViews()
        
        branches.forEach { branch ->
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_branch_selection, containerBranches, false)
            
            val branchName = I18nHelper.getBranchNameDisplay(branch, this)
            item.findViewById<TextView>(R.id.tvBranchName)?.text = branchName
            
            val branchAddress = branch.address?.ifBlank { null }
            val branchRegion = branch.region_name_ar ?: branch.region_name
            val branchCity = branch.city_name_ar ?: branch.city_name
            val addressParts = listOfNotNull(branchAddress, branchRegion, branchCity)
            item.findViewById<TextView>(R.id.tvBranchAddress)?.text = 
                if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else null
            item.findViewById<TextView>(R.id.tvBranchAddress)?.visibility = 
                if (addressParts.isNotEmpty()) View.VISIBLE else View.GONE
            
            val isSelected = branch.id == selectedBranch?.id
            item.findViewById<TextView>(R.id.imgSelected)?.visibility =
                if (isSelected) View.VISIBLE else View.GONE
            
            item.setOnClickListener {
                selectedBranch = branch
                updateRestaurantAddressOrBranch()
                dialog.dismiss()
            }
            
            containerBranches?.addView(item)
        }
        
        dialog.show()
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
                                    val item = LayoutInflater.from(this@RestaurantDetailsActivity)
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
                                    val isSelected = addr.id == defaultAddr.id
                                    item.findViewById<ImageView>(R.id.imgSelected).visibility =
                                        if (isSelected) View.VISIBLE else View.GONE
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
            startActivity(ConfirmLocationActivity.newIntent(this, restaurantId))
            dialog.dismiss()
        }
        sheetView.findViewById<View>(R.id.cardCurrentLocation).setOnClickListener {
            sessionManager.saveOrderType("delivery")
            if (LocationHelper.hasLocationPermission(this)) {
                fetchCurrentLocationAndSetDelivery()
                dialog.dismiss()
            } else {
                LocationHelper.requestLocationPermissions(this, LOCATION_PERMISSION_REQUEST_CODE)
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
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuPremiumAdapter(
            onProductClick = { product ->
                if (product.addon_required == true) {
                    startActivity(MealDetailsActivity.newIntent(
                        this,
                        product,
                        currencyCode,
                        currencySymbolPosition,
                        currentRestaurantName
                    ))
                } else {
                    showMealSimpleBottomSheet(product)
                }
            },
            onAddToCartDirect = { product -> addProductToCartDirect(product) },
            onQuantityChange = { productId, addonIds, newQuantity ->
                if (newQuantity <= 0) {
                    cartViewModel.removeFromCart(productId, addonIds)
                } else {
                    cartViewModel.updateQuantity(productId, addonIds, newQuantity)
                }
                updateBottomBarCart()
            },
            currencyCode = currencyCode,
            currencySymbolPosition = currencySymbolPosition
        )
        binding.recyclerViewMenu.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMenu.adapter = menuAdapter
        refreshMenuCartState()
    }

    /** Builds map: product id -> (first cart line for that product, total quantity) for this restaurant. */
    private fun buildCartStateMap(): Map<Int, Pair<CartItem, Int>> {
        val items = cartViewModel.getCartItemsForRestaurant(restaurantId)
        return items.groupBy { it.product.id }.mapValues { (_, list) ->
            val first = list.first()
            val total = list.sumOf { it.quantity }
            Pair(first, total)
        }
    }

    private fun refreshMenuCartState() {
        menuAdapter.setCartState(buildCartStateMap())
    }

    /** Adds product (no add-ons) to cart directly. Uses discounted price when offer applies. */
    private fun addProductToCartDirect(product: Product) {
        if (!product.is_available) return
        val (displayPrice, _) = getDisplayPricesForProduct(product)
        Log.d("MoneyLog", "[RestaurantDetails] addProductToCartDirect product id=${product.id} name=${product.name} product.price=${product.price} unitPriceOverride=$displayPrice quantity=1")
        val currentRestaurantId = cartViewModel.getCurrentRestaurantId()
        if (currentRestaurantId == null || currentRestaurantId == product.website_id) {
            cartViewModel.addToCart(product, emptyList(), unitPriceOverride = displayPrice)
            Toast.makeText(this, getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show()
            updateBottomBarCart()
        } else {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_cart))
                .setMessage(getString(R.string.cart_contains_different_restaurant, currentRestaurantName))
                .setPositiveButton(getString(R.string.clear_cart_and_add)) { _, _ ->
                    Log.d("MoneyLog", "[RestaurantDetails] clearCartAndAdd product id=${product.id} unitPriceOverride=$displayPrice")
                    cartViewModel.clearCartAndAdd(product, emptyList(), unitPriceOverride = displayPrice)
                    Toast.makeText(this, getString(R.string.cart_cleared_and_item_added), Toast.LENGTH_SHORT).show()
                    updateBottomBarCart()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun getBestPercentOffForProduct(productId: Int): Double? {
        val offersList = restaurantViewModel.restaurantOffers.value ?: emptyList()
        val percentOffers = offersList.filter { offer ->
            (offer.offer_type?.trim()?.lowercase() == "percent_off") && (offer.value ?: 0.0) > 0
        }
        val applicable = percentOffers.filter { offer ->
            when (offer.offer_scope?.trim()?.lowercase()) {
                "selected_items" -> productId in offer.getSelectedProductIds()
                else -> true
            }
        }
        return applicable.mapNotNull { it.value }.maxOrNull()
    }

    private fun getDisplayPricesForProduct(product: Product): Pair<Double, Double?> {
        val percent = getBestPercentOffForProduct(product.id)
        if (percent == null || percent <= 0.0) {
            Log.d("MoneyLog", "[RestaurantDetails] getDisplayPrices product id=${product.id} name=${product.name} price=${product.price} (no discount)")
            return Pair(product.price, null)
        }
        val discounted = kotlin.math.round(product.price * (1.0 - percent / 100.0) * 100.0) / 100.0
        Log.d("MoneyLog", "[RestaurantDetails] getDisplayPrices product id=${product.id} name=${product.name} price=${product.price} percentOff=$percent displayPrice=$discounted")
        return Pair(discounted, product.price)
    }

    private fun showMealSimpleBottomSheet(product: Product) {
        if (!product.is_available) return
        val currentRestaurantId = cartViewModel.getCurrentRestaurantId()
        if (currentRestaurantId != null && currentRestaurantId != product.website_id) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_cart))
                .setMessage(getString(R.string.cart_contains_different_restaurant, currentRestaurantName))
                .setPositiveButton(getString(R.string.clear_cart_and_add)) { _, _ ->
                    val (displayPrice, _) = getDisplayPricesForProduct(product)
                    Log.d("MoneyLog", "[RestaurantDetails] clearCartAndAdd (sheet) product id=${product.id} unitPriceOverride=$displayPrice")
                    cartViewModel.clearCartAndAdd(product, emptyList(), unitPriceOverride = displayPrice)
                    Toast.makeText(this, getString(R.string.cart_cleared_and_item_added), Toast.LENGTH_SHORT).show()
                    updateBottomBarCart()
                    refreshMenuCartState()
                    showMealSimpleBottomSheet(product)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
            return
        }
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_meal_simple, null)
        dialog.setContentView(sheetView)
        sheetView.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        val ivMealImage = sheetView.findViewById<ImageView>(R.id.ivMealImage)
        val tvMealName = sheetView.findViewById<TextView>(R.id.tvMealName)
        val tvMealDescription = sheetView.findViewById<TextView>(R.id.tvMealDescription)
        val tvMealOriginalPrice = sheetView.findViewById<TextView>(R.id.tvMealOriginalPrice)
        val tvMealPrice = sheetView.findViewById<TextView>(R.id.tvMealPrice)
        val tvQuantity = sheetView.findViewById<TextView>(R.id.tvQuantity)
        val btnDecrease = sheetView.findViewById<View>(R.id.btnDecrease)
        val btnIncrease = sheetView.findViewById<View>(R.id.btnIncrease)
        val layoutAddItem = sheetView.findViewById<View>(R.id.layoutAddItem)
        val tvAddItemLabel = sheetView.findViewById<android.widget.TextView>(R.id.tvAddItemLabel)
        val tvAddItemPrice = sheetView.findViewById<android.widget.TextView>(R.id.tvAddItemPrice)
        tvMealName.text = I18nHelper.getProductNameDisplay(product, this)
        val desc = I18nHelper.getProductDescriptionDisplay(product, this)
        tvMealDescription.text = desc?.ifBlank { null } ?: getString(R.string.no_description_available)
        tvMealDescription.visibility = if (desc.isNullOrBlank()) View.GONE else View.VISIBLE
        val (displayPrice, originalForStrike) = getDisplayPricesForProduct(product)
        tvMealPrice.text = CurrencyFormatter.formatPrice(displayPrice, currencyCode, currencySymbolPosition)
        if (originalForStrike != null && originalForStrike > displayPrice) {
            tvMealOriginalPrice.visibility = View.VISIBLE
            tvMealOriginalPrice.text = CurrencyFormatter.formatPrice(originalForStrike, currencyCode, currencySymbolPosition)
            tvMealOriginalPrice.paintFlags = tvMealOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            tvMealOriginalPrice.visibility = View.GONE
        }
        product.image_url?.let { url ->
            val u = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(url).replace("localhost", "10.0.2.2")
            Glide.with(this).load(u).placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground).centerCrop().into(ivMealImage)
        } ?: ivMealImage.setImageResource(R.drawable.ic_launcher_foreground)
        val cartState = buildCartStateMap()[product.id]
        var quantity = cartState?.second ?: 1
        fun updateSheetTotal() {
            tvQuantity.text = quantity.toString()
            val total = kotlin.math.round(displayPrice * quantity * 100.0) / 100.0
            tvAddItemPrice.text = CurrencyFormatter.formatPrice(total, currencyCode, currencySymbolPosition)
        }
        tvAddItemLabel.text = getString(R.string.add_item)
        updateSheetTotal()
        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateSheetTotal()
            }
        }
        btnIncrease.setOnClickListener {
            quantity = (quantity + 1).coerceAtMost(99)
            updateSheetTotal()
        }
        layoutAddItem.setOnClickListener {
            if (cartState != null) {
                Log.d("MoneyLog", "[RestaurantDetails] sheet updateQuantity product id=${product.id} quantity=$quantity")
                cartViewModel.updateQuantity(product.id, cartState.first.addonIdsSorted(), quantity)
            } else {
                val roundedPrice = kotlin.math.round(displayPrice * 100.0) / 100.0
                Log.d("MoneyLog", "[RestaurantDetails] sheet addToCart product id=${product.id} product.price=${product.price} unitPriceOverride=$roundedPrice quantity=$quantity")
                cartViewModel.addToCart(product, emptyList(), unitPriceOverride = roundedPrice)
                if (quantity > 1) {
                    cartViewModel.updateQuantity(product.id, emptyList(), quantity)
                }
            }
            Toast.makeText(this, getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            updateBottomBarCart()
            refreshMenuCartState()
        }
        dialog.show()
    }

    /**
     * Builds premium menu list: Trending section (first 4 products in 2-column grid),
     * then per-category sections — category name as title (locale-aware), then all products under it (one per line).
     */
    private fun buildPremiumMenuList(products: List<Product>): List<MenuPremiumItem> {
        val list = mutableListOf<MenuPremiumItem>()
        val trending = products.take(4)
        val menuFallback = getString(R.string.menu)
        val allByCategory = products.groupBy { p ->
            I18nHelper.getProductCategoryDisplay(p, this).ifBlank { menuFallback }
        }
        val sortedCategories = allByCategory.keys.sorted()

        // Trending section
        list.add(MenuPremiumItem.TrendingHeader(getString(R.string.trending)))
        trending.chunked(2).forEach { pair ->
            list.add(MenuPremiumItem.TrendingPair(pair[0], pair.getOrNull(1)))
        }

        // After trending: each category as title (locale-aware), then all products in that category (one per line)
        sortedCategories.forEach { category ->
            list.add(MenuPremiumItem.CategoryHeader(category))
            allByCategory[category]?.forEach { list.add(MenuPremiumItem.MainProduct(it)) }
        }
        return list
    }

    private fun setupCategoryTabs(categories: List<String>) {
        categoryNames = categories
        binding.tabLayoutCategories.removeAllTabs()
        if (categories.isEmpty()) {
            binding.tabLayoutCategories.visibility = android.view.View.GONE
            return
        }
        binding.tabLayoutCategories.visibility = android.view.View.VISIBLE
        categories.forEach { name ->
            binding.tabLayoutCategories.addTab(
                binding.tabLayoutCategories.newTab().setText(name)
            )
        }
        binding.tabLayoutCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                isScrollingToSection = true
                val pos = if (tab.position == 0) 0 else menuAdapter.getCategoryStartPositions()[tab.text?.toString()]
                if (pos == null) {
                    isScrollingToSection = false
                    return
                }
                smoothScrollToSection(pos)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                isScrollingToSection = true
                val pos = if (tab.position == 0) 0 else menuAdapter.getCategoryStartPositions()[tab.text?.toString()]
                if (pos == null) {
                    isScrollingToSection = false
                    return
                }
                smoothScrollToSection(pos)
            }
        })

        // Sync tab selection when user manually scrolls; ignore while we are scrolling to a tapped tab
        binding.recyclerViewMenu.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isScrollingToSection = false
                }
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isScrollingToSection) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisible = lm.findFirstVisibleItemPosition()
                if (firstVisible < 0 || firstVisible >= menuAdapter.itemCount) return
                val item = menuAdapter.currentList.getOrNull(firstVisible)
                val idx = when (item) {
                    is MenuPremiumItem.TrendingHeader, is MenuPremiumItem.TrendingPair -> 0
                    is MenuPremiumItem.CategoryHeader -> categoryNames.indexOf(item.categoryName)
                    else -> -1
                }
                if (idx >= 0 && idx != binding.tabLayoutCategories.selectedTabPosition) {
                    binding.tabLayoutCategories.getTabAt(idx)?.select()
                }
            }
        })
    }

    /** Smoothly scrolls the menu list so the section at [position] moves to the top. */
    private fun smoothScrollToSection(position: Int) {
        val lm = binding.recyclerViewMenu.layoutManager as? LinearLayoutManager ?: return
        val smoothScroller = object : LinearSmoothScroller(this) {
            override fun getVerticalSnapPreference(): Int = LinearSmoothScroller.SNAP_TO_START
        }
        smoothScroller.targetPosition = position
        lm.startSmoothScroll(smoothScroller)
    }

    private fun updateMenuAdapterAndList(currencyCode: String?, currencyPosition: String?) {
        this.currencyCode = currencyCode
        this.currencySymbolPosition = currencyPosition
        val currentOffers = restaurantViewModel.restaurantOffers.value ?: emptyList()
        val percentOffCount = currentOffers.count { it.offer_type?.trim()?.lowercase() == "percent_off" }
        Log.d("RestaurantDetails", "updateMenuAdapterAndList: website_id=$restaurantId, offers=${currentOffers.size} (percent_off=$percentOffCount)")
        menuAdapter = MenuPremiumAdapter(
            onProductClick = { product ->
                if (product.addon_required == true) {
                    startActivity(MealDetailsActivity.newIntent(
                        this,
                        product,
                        this.currencyCode,
                        this.currencySymbolPosition,
                        currentRestaurantName
                    ))
                } else {
                    showMealSimpleBottomSheet(product)
                }
            },
            onAddToCartDirect = { product -> addProductToCartDirect(product) },
            onQuantityChange = { productId, addonIds, newQuantity ->
                if (newQuantity <= 0) {
                    cartViewModel.removeFromCart(productId, addonIds)
                } else {
                    cartViewModel.updateQuantity(productId, addonIds, newQuantity)
                }
                updateBottomBarCart()
            },
            currencyCode = currencyCode,
            currencySymbolPosition = currencyPosition,
            offers = currentOffers
        )
        binding.recyclerViewMenu.adapter = menuAdapter
        restaurantViewModel.products.value?.let { products ->
            val available = products.filter { it.is_available }
            val list = buildPremiumMenuList(available)
            Log.d("RestaurantDetails", "updateMenuAdapterAndList: submitting ${list.size} menu items (${available.size} products)")
            menuAdapter.submitList(list)
            // Tabs: first "Trending", then categories (from full product list so tabs always match)
            val menuFallback = getString(R.string.menu)
            val categories = available
                .map { p -> I18nHelper.getProductCategoryDisplay(p, this).ifBlank { menuFallback } }
                .distinct()
                .sorted()
            setupCategoryTabs(listOf(getString(R.string.trending)) + categories)
        }
        refreshMenuCartState()
    }

    private fun setupBottomBarCart() {
        binding.bottomBarCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        cartViewModel.cartItems.observe(this) {
            updateBottomBarCart()
            refreshMenuCartState()
        }
        updateBottomBarCart()
    }

    private fun setupFloatingActions() {
        binding.btnSearch.setOnClickListener {
            startActivity(SearchActivity.newIntent(
                this,
                restaurantId,
                currencyCode,
                currencySymbolPosition,
                currentRestaurantName
            ))
        }
        binding.btnShare.setOnClickListener {
            shareRestaurant()
        }
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }
    }

    private fun loadFavoriteState() {
        val prefs = getSharedPreferences(FAVORITES_PREFS, Context.MODE_PRIVATE)
        isFavorite = prefs.getBoolean("restaurant_$restaurantId", false)
        updateFavoriteIcon()
    }

    private fun updateFavoriteIcon() {
        binding.btnFavorite.setIconResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        val prefs = getSharedPreferences(FAVORITES_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("restaurant_$restaurantId", isFavorite).apply()
        updateFavoriteIcon()
        Toast.makeText(
            this,
            if (isFavorite) getString(R.string.added_to_favorites) else getString(R.string.removed_from_favorites),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareRestaurant() {
        val shareText = if (currentRestaurantName.isNotEmpty()) {
            "$currentRestaurantName - ${getString(R.string.check_out_this_restaurant)}"
        } else {
            getString(R.string.check_out_this_restaurant)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, currentRestaurantName)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_restaurant)))
    }


    private fun updateBottomBarCart() {
        val itemCount = cartViewModel.getItemCount()
        val total = cartViewModel.getTotalPrice()
        binding.tvCartCount.text = itemCount.toString()
        binding.tvViewCartLabel.text = getString(R.string.view_cart)
        binding.tvCartTotal.text = CurrencyFormatter.formatPrice(
            total,
            currencyCode,
            currencySymbolPosition
        )
        val hasItems = itemCount > 0
        binding.bottomBarCart.setCardBackgroundColor(
            if (hasItems) androidx.core.content.ContextCompat.getColor(this, R.color.primary_color)
            else 0xFFF5F5F5.toInt()
        )
        binding.tvCartCount.setBackgroundResource(if (hasItems) R.drawable.bg_cart_count_circle_white else R.drawable.bg_cart_count_circle)
        binding.tvCartCount.setTextColor(androidx.core.content.ContextCompat.getColor(this, if (hasItems) R.color.on_surface else R.color.on_surface))
        binding.ivCartIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, if (hasItems) R.color.white else R.color.on_surface))
        binding.tvViewCartLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, if (hasItems) R.color.white else R.color.on_surface))
        binding.tvCartTotal.setTextColor(androidx.core.content.ContextCompat.getColor(this, if (hasItems) R.color.white else R.color.on_surface))
    }

    private fun checkAndUpdateCustomerLocation() {
        lifecycleScope.launch {
            try {
                val customerId = sessionManager.getCustomerId()
                val authToken = sessionManager.getAuthToken()
                
                if (customerId == -1 || authToken == null) {
                    Log.d("RestaurantDetails", "checkAndUpdateCustomerLocation: Not logged in")
                    return@launch
                }
                
                // Check customer profile from backend
                val response = RetrofitClient.apiService.getCustomerProfile(
                    customerId = customerId,
                    token = "Bearer $authToken"
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val customer = response.body()!!.customer
                    
                    // Check if address, latitude, or longitude are missing
                    val needsLocation = customer.address.isNullOrEmpty()
                    
                    if (needsLocation) {
                        Log.d("RestaurantDetails", "checkAndUpdateCustomerLocation: Customer missing address, requesting location")
                        
                        // Check if we have permission
                        if (LocationHelper.hasLocationPermission(this@RestaurantDetailsActivity)) {
                            // Get location and update
                            getLocationAndUpdateProfile(customerId, authToken)
                        } else {
                            // Request permission
                            LocationHelper.requestLocationPermissions(
                                this@RestaurantDetailsActivity,
                                LOCATION_PERMISSION_REQUEST_CODE
                            )
                        }
                    } else {
                        // Update SessionManager with address if not already stored
                        val currentAddress = sessionManager.getCustomerAddress()
                        if (currentAddress.isNullOrEmpty() && !customer.address.isNullOrEmpty()) {
                            sessionManager.saveCustomerInfo(
                                customer.id,
                                customer.name,
                                customer.email,
                                customer.phone,
                                customer.address
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RestaurantDetails", "checkAndUpdateCustomerLocation: Error", e)
            }
        }
    }
    
    private fun getLocationAndUpdateProfile(customerId: Int, authToken: String) {
        lifecycleScope.launch {
            try {
                LocationHelper.getCurrentLocation(this@RestaurantDetailsActivity)?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val address = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        LocationHelper.getAddressFromLocation(
                            this@RestaurantDetailsActivity,
                            latitude,
                            longitude
                        )
                    }
                    
                    Log.d("RestaurantDetails", "getLocationAndUpdateProfile: Got location lat=$latitude, lng=$longitude, address=$address")
                    
                    // Update customer profile via API
                    val updateRequest = com.mnsf.resturantandroid.network.UpdateCustomerProfileRequest(
                        address = address
                    )
                    
                    val response = RetrofitClient.apiService.updateCustomerProfile(
                        customerId = customerId,
                        request = updateRequest,
                        token = "Bearer $authToken"
                    )
                    
                    if (response.isSuccessful && response.body() != null) {
                        val updatedCustomer = response.body()!!.customer
                        
                        // Update SessionManager
                        sessionManager.saveCustomerInfo(
                            updatedCustomer.id,
                            updatedCustomer.name,
                            updatedCustomer.email,
                            updatedCustomer.phone,
                            updatedCustomer.address
                        )
                        sessionManager.saveDeliveryLabel(
                            (updatedCustomer.address ?: "").take(30).let { if (it.length >= 30) "$it…" else it }.ifBlank { null }
                        )
                        sessionManager.saveOrderType("delivery")
                        
                        // Also update location separately
                        val locationRequest = com.mnsf.resturantandroid.network.LocationRequest(
                            latitude = latitude,
                            longitude = longitude,
                            address = address
                        )
                        
                        RetrofitClient.apiService.updateLocation(
                            customerId = customerId,
                            request = locationRequest,
                            token = "Bearer $authToken"
                        )
                        
                        Log.d("RestaurantDetails", "getLocationAndUpdateProfile: Profile updated successfully")
                    }
                } ?: run {
                    Log.w("RestaurantDetails", "getLocationAndUpdateProfile: Could not get location")
                }
            } catch (e: Exception) {
                Log.e("RestaurantDetails", "getLocationAndUpdateProfile: Error", e)
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val customerId = sessionManager.getCustomerId()
                val authToken = sessionManager.getAuthToken()
                if (customerId != -1 && authToken != null) {
                    getLocationAndUpdateProfile(customerId, authToken)
                } else {
                    fetchCurrentLocationAndSetDelivery()
                }
            } else {
                Log.d("RestaurantDetails", "onRequestPermissionsResult: Location permission denied")
            }
        }
    }
    
    private fun setupObservers() {
        restaurantViewModel.selectedRestaurant.observe(this) { restaurant ->
            restaurant?.let {
                currentRestaurantName = I18nHelper.getRestaurantNameDisplay(it, this)
                binding.tvRestaurantName.text = currentRestaurantName
                supportActionBar?.title = ""

                // Time under address: delivery time from DB (min–max mins) or placeholder; no delivery fee in card
                val timeLabel = when {
                    it.delivery_time_min != null && it.delivery_time_max != null ->
                        LocaleHelper.getStringWithEnglishNumbers(this, R.string.delivery_time_range, it.delivery_time_min!!, it.delivery_time_max!!)
                    else -> getString(R.string.delivery_time_placeholder)
                }
                binding.tvSubtitle.text = timeLabel

                // Show branch if selected, otherwise show restaurant address
                updateRestaurantAddressOrBranch()

                updateMenuAdapterAndList(it.currency_code, it.currency_symbol_position)
                updateBottomBarCart()

                val imageUrl = it.logo_url?.let { url ->
                    com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(url)
                        .replace("localhost", "10.0.2.2")
                }
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(binding.ivCoverImage)
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(binding.ivRestaurantLogo)
            }
        }

        restaurantViewModel.products.observe(this) { products ->
            val available = products.filter { it.is_available }
            if (available.isNotEmpty()) {
                val list = buildPremiumMenuList(available)
                Log.d("RestaurantDetails", "products observer: submitting ${list.size} menu items (${available.size} products)")
                menuAdapter.submitList(list)
                val menuFallback = getString(R.string.menu)
                val categories = available
                    .map { p -> I18nHelper.getProductCategoryDisplay(p, this).ifBlank { menuFallback } }
                    .distinct()
                    .sorted()
                setupCategoryTabs(listOf(getString(R.string.trending)) + categories)
                binding.recyclerViewMenu.visibility = android.view.View.VISIBLE
                binding.textEmptyMenu.visibility = android.view.View.GONE
            } else {
                binding.tabLayoutCategories.visibility = android.view.View.GONE
                binding.recyclerViewMenu.visibility = android.view.View.GONE
                binding.textEmptyMenu.visibility = android.view.View.VISIBLE
            }
        }

        restaurantViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }

        restaurantViewModel.error.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        restaurantViewModel.restaurantOffers.observe(this) { offers ->
            val list = offers ?: emptyList()
            val percentOff = list.filter { it.offer_type?.trim()?.lowercase() == "percent_off" }
            Log.d("RestaurantDetails", "restaurantOffers observer: website_id=$restaurantId, ${list.size} offer(s), ${percentOff.size} percent_off (values=${percentOff.map { it.value }.joinToString()})")
            menuAdapter.setOffers(list)
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

