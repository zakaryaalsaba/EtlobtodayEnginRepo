package com.mnsf.resturantandroid.ui.checkout

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityCheckoutBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.ui.order.OrderConfirmationActivity
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.LocationHelper
import com.mnsf.resturantandroid.util.PaymentMethodManager
import com.mnsf.resturantandroid.data.model.SavedPaymentMethod
import com.mnsf.resturantandroid.network.Address
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.mnsf.resturantandroid.viewmodel.CartViewModel
import com.mnsf.resturantandroid.viewmodel.OrderViewModel
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.data.model.PaymentMethods
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mnsf.resturantandroid.BuildConfig
import com.payment.paymentsdk.PaymentSdkActivity
import com.payment.paymentsdk.PaymentSdkConfigBuilder
import com.payment.paymentsdk.integrationmodels.PaymentSdkBillingDetails
import com.payment.paymentsdk.integrationmodels.PaymentSdkShippingDetails
import com.payment.paymentsdk.integrationmodels.PaymentSdkLanguageCode
import com.payment.paymentsdk.integrationmodels.PaymentSdkTransactionType
import com.payment.paymentsdk.integrationmodels.PaymentSdkError
import com.payment.paymentsdk.integrationmodels.PaymentSdkTransactionDetails
import com.payment.paymentsdk.sharedclasses.interfaces.CallbackPaymentInterface

class CheckoutActivity : AppCompatActivity(), CallbackPaymentInterface {
    
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var cartViewModel: CartViewModel
    private lateinit var orderViewModel: OrderViewModel
    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var paymentMethodManager: PaymentMethodManager
    private var restaurantId: Int = -1
    private var paymentMethods: PaymentMethods? = null
    private var selectedSavedPaymentMethod: SavedPaymentMethod? = null // Selected saved card, null = new card
    private var currencyCode: String? = null
    private var currencySymbolPosition: String? = null
    private var taxEnabled: Boolean = false
    private var taxRate: Double = 0.0
    private var deliveryFee: Double = 0.0
    private var dineInEnabled: Boolean = true
    private var pickupEnabled: Boolean = true
    private var deliveryEnabled: Boolean = true
    private val gson = Gson()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002
    
    private var placeOrderCountdown: CountDownTimer? = null
    private var isPlaceOrderProcessing = false
    private var serviceFeeFromDb: Double = 0.0
    private var offers: List<Offer> = emptyList()
    private var selectedAddress: Address? = null // Current selected/default address for delivery fee calculation

    /** Pending order data when PayTabs card payment is in progress (used in onPaymentFinish). */
    private var pendingOrderCustomerName: String? = null
    private var pendingOrderCustomerEmail: String? = null
    private var pendingOrderCustomerPhone: String? = null
    private var pendingOrderCustomerAddress: String? = null
    private var pendingOrderType: String? = null
    private var pendingOrderNotes: String? = null
    private var pendingOrderDeliveryLat: Double? = null
    private var pendingOrderDeliveryLng: Double? = null
    private var pendingOrderCartItems: List<com.mnsf.resturantandroid.data.model.CartItem>? = null
    private var pendingOrderTip: Double = 0.0
    private var pendingOrderDeliveryInstructions: String? = null

    /** Total amount sent with the last placed order (matches checkout display). Used for confirmation screen. */
    private var lastPlacedOrderTotal: Double? = null

    /** Current restaurant (from loadRestaurantInfo). Sent with order so Driver app can show pickup info. */
    private var currentRestaurant: com.mnsf.resturantandroid.data.model.Restaurant? = null
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onResume() {
        super.onResume()
        // Reload addresses if restaurant has delivery company (in case address was updated)
        currentRestaurant?.let { restaurant ->
            if (restaurant.delivery_company_id != null && restaurant.delivery_company_id > 0) {
                loadCustomerAddressesForDeliveryFee()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d("CheckoutActivity", "onCreate: Starting")
            binding = ActivityCheckoutBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("CheckoutActivity", "onCreate: Binding inflated")
            
            restaurantId = intent.getIntExtra("restaurant_id", -1)
            Log.d("CheckoutActivity", "onCreate: restaurantId = $restaurantId")
            if (restaurantId == -1) {
                Log.e("CheckoutActivity", "onCreate: Invalid restaurant ID")
                Toast.makeText(this, getString(R.string.error_no_restaurant), Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        
        // Header: back button and title (restaurant name set when loaded)
        binding.btnBack.setOnClickListener { finish() }
        binding.tvTitleCheckout.text = getString(R.string.checkout)

        sessionManager = SessionManager(this)
        paymentMethodManager = PaymentMethodManager(this)
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]
        
        val orderRepository = OrderRepository(RetrofitClient.apiService, sessionManager)
        orderViewModel = ViewModelProvider(this, OrderViewModelFactory(orderRepository, sessionManager))[OrderViewModel::class.java]
        
        val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        restaurantViewModel = ViewModelProvider(this, RestaurantViewModelFactory(restaurantRepository))[RestaurantViewModel::class.java]
        
            setupObservers()
            Log.d("CheckoutActivity", "onCreate: Observers setup")
            setupTipListeners()
            setupClickListeners()
            Log.d("CheckoutActivity", "onCreate: Click listeners setup")
            checkAndUpdateCustomerLocation()
            setupCustomerAddress()
            updateDeliverySectionsVisibility()
            Log.d("CheckoutActivity", "onCreate: Customer address setup")
            loadRestaurantInfo()
            loadSettings()
            Log.d("CheckoutActivity", "onCreate: Restaurant info loading")
            updateOrderSummary()
            Log.d("CheckoutActivity", "onCreate: Order summary updated")
            
            // Order type is set from beginning (RestaurantDetails / choose location sheet), not selected here

            // Change address: go to Confirm Location to pick/edit address
            binding.btnChangeAddress.setOnClickListener {
                startActivity(ConfirmLocationActivity.newIntent(this, restaurantId))
                finish()
            }

            // Delivery time text (for delivery orders)
            binding.tvDeliveryTime.text = getString(R.string.arriving_in_approx_mins, 15, 25)
            Log.d("CheckoutActivity", "onCreate: Completed successfully")
        } catch (e: Exception) {
            Log.e("CheckoutActivity", "onCreate: Fatal error", e)
            e.printStackTrace()
            Toast.makeText(this, "Error initializing checkout: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    /** Fetches global settings (e.g. service_fee) from API and updates serviceFeeFromDb, then refreshes order summary. */
    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.apiService.getSettings()
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    serviceFeeFromDb = body?.service_fee ?: 0.0
                    Log.d("CheckoutActivity", "loadSettings: service_fee=$serviceFeeFromDb")
                    runOnUiThread { updateOrderSummary() }
                } else {
                    Log.w("CheckoutActivity", "loadSettings: failed ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "loadSettings: Error", e)
            }
        }
    }

    private fun loadRestaurantInfo() {
        try {
            Log.d("CheckoutActivity", "loadRestaurantInfo: Loading restaurant $restaurantId")
            restaurantViewModel.loadRestaurant(restaurantId)
            restaurantViewModel.selectedRestaurant.observe(this) { restaurant ->
                try {
                    Log.d("CheckoutActivity", "loadRestaurantInfo: Restaurant received: ${restaurant?.restaurant_name}")
                    currentRestaurant = restaurant
                    restaurant?.let {
                        // Parse payment methods from restaurant
                        Log.d("CheckoutActivity", "loadRestaurantInfo: Payment methods JSON: ${it.payment_methods}")
                        parsePaymentMethods(it.payment_methods)
                        updatePaymentMethodUI()
                        
                        // Store currency info
                        currencyCode = it.currency_code
                        currencySymbolPosition = it.currency_symbol_position
                        taxEnabled = it.tax_enabled == true
                        taxRate = it.tax_rate ?: 0.0
                        deliveryFee = it.delivery_fee ?: 0.0
                        
                        // Store order type settings
                        dineInEnabled = it.order_type_dine_in_enabled ?: true
                        pickupEnabled = it.order_type_pickup_enabled ?: true
                        deliveryEnabled = it.order_type_delivery_enabled ?: true
                        
                        Log.d("CheckoutActivity", "loadRestaurantInfo: Currency - code=${currencyCode}, position=${currencySymbolPosition}, taxEnabled=$taxEnabled, taxRate=$taxRate, deliveryFee=$deliveryFee")
                        Log.d("CheckoutActivity", "loadRestaurantInfo: Order types - dineIn=$dineInEnabled, pickup=$pickupEnabled, delivery=$deliveryEnabled")
                        Log.d("CheckoutActivity", "loadRestaurantInfo: Delivery company ID=${it.delivery_company_id}")
                        
                        // If restaurant has delivery company, load customer addresses to get zone_price
                        if (it.delivery_company_id != null && it.delivery_company_id > 0) {
                            loadCustomerAddressesForDeliveryFee()
                        }
                        
                        // Update order type UI based on enabled types
                        updateOrderTypeUI()
                        // Set restaurant name in header and in pick-up card when order type is pickup
                        binding.tvRestaurantName.text = it.restaurant_name
                        binding.tvPickupRestaurantName.text = it.restaurant_name
                        binding.tvPickupReadyTime.text = getString(R.string.ready_for_pickup_approx)
                        // Show/hide delivery vs pick-up sections
                        updateDeliverySectionsVisibility()
                        // Update order summary with currency, tax, and delivery fee
                        updateOrderSummary()
                    } ?: run {
                        Log.w("CheckoutActivity", "loadRestaurantInfo: Restaurant is null")
                    }
                } catch (e: Exception) {
                    Log.e("CheckoutActivity", "loadRestaurantInfo: Error in observer", e)
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("CheckoutActivity", "loadRestaurantInfo: Fatal error", e)
            e.printStackTrace()
        }
    }
    
    private fun parsePaymentMethods(paymentMethodsJson: String?) {
        try {
            Log.d("CheckoutActivity", "parsePaymentMethods: Input: $paymentMethodsJson")
            if (paymentMethodsJson.isNullOrEmpty()) {
                Log.d("CheckoutActivity", "parsePaymentMethods: Using default payment methods")
                // Default payment methods
                paymentMethods = PaymentMethods(
                    cashOnPickup = true,
                    cashOnDelivery = true,
                    creditCard = false,
                    onlinePayment = false,
                    mobilePayment = false,
                    cliQServices = null
                )
                return
            }
            
            val parsed = gson.fromJson(paymentMethodsJson, PaymentMethods::class.java)
            Log.d("CheckoutActivity", "parsePaymentMethods: Parsed successfully - cashOnPickup=${parsed.cashOnPickup}, creditCard=${parsed.creditCard}, cliQServices=${parsed.cliQServices?.enabled}")
            paymentMethods = PaymentMethods(
                cashOnPickup = parsed.cashOnPickup,
                cashOnDelivery = parsed.cashOnDelivery,
                creditCard = parsed.creditCard,
                onlinePayment = parsed.onlinePayment,
                mobilePayment = parsed.mobilePayment,
                cliQServices = parsed.cliQServices
            )
        } catch (e: JsonSyntaxException) {
            Log.e("CheckoutActivity", "parsePaymentMethods: JSON parse error", e)
            e.printStackTrace()
            // Default payment methods on parse error
            paymentMethods = PaymentMethods(
                cashOnPickup = true,
                cashOnDelivery = true,
                creditCard = false,
                onlinePayment = false,
                mobilePayment = false,
                cliQServices = null
            )
        } catch (e: Exception) {
            Log.e("CheckoutActivity", "parsePaymentMethods: Unexpected error", e)
            e.printStackTrace()
            // Default payment methods on error
            paymentMethods = PaymentMethods(
                cashOnPickup = true,
                cashOnDelivery = true,
                creditCard = false,
                onlinePayment = false,
                mobilePayment = false,
                cliQServices = null
            )
        }
    }
    
    /** Show/hide delivery vs pick-up cards based on order type from session. */
    private fun updateDeliverySectionsVisibility() {
        val orderType = sessionManager.getOrderType()
        val isDelivery = orderType == "delivery"
        val isPickup = orderType == "pickup"
        val deliveryVisibility = if (isDelivery) android.view.View.VISIBLE else android.view.View.GONE
        binding.cardDeliveryAddress.visibility = deliveryVisibility
        binding.cardDeliveryTime.visibility = deliveryVisibility
        binding.cardTip.visibility = deliveryVisibility
        binding.cardDeliveryInstructions.visibility = deliveryVisibility
        binding.cardPickupInfo.visibility = if (isPickup) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutCustomTip.visibility = if (isDelivery) binding.layoutCustomTip.visibility else android.view.View.GONE
        if (isDelivery) setupCustomerAddress()
    }

    private fun updateOrderTypeUI() {
        try {
            Log.d("CheckoutActivity", "updateOrderTypeUI: Starting - dineIn=$dineInEnabled, pickup=$pickupEnabled, delivery=$deliveryEnabled")
            
            // Show/hide order type radio buttons based on enabled types
            binding.rbDineIn.visibility = if (dineInEnabled) android.view.View.VISIBLE else android.view.View.GONE
            binding.rbPickup.visibility = if (pickupEnabled) android.view.View.VISIBLE else android.view.View.GONE
            binding.rbDelivery.visibility = if (deliveryEnabled) android.view.View.VISIBLE else android.view.View.GONE
            
            // If current selection is disabled, select the first available option
            val currentSelection = when {
                binding.rbDineIn.isChecked -> "dine_in"
                binding.rbPickup.isChecked -> "pickup"
                binding.rbDelivery.isChecked -> "delivery"
                else -> null
            }
            
            val isCurrentSelectionDisabled = when (currentSelection) {
                "dine_in" -> !dineInEnabled
                "pickup" -> !pickupEnabled
                "delivery" -> !deliveryEnabled
                else -> false
            }
            
            if (isCurrentSelectionDisabled || currentSelection == null) {
                // Select first available option
                when {
                    pickupEnabled -> {
                        binding.rbPickup.isChecked = true
                        Log.d("CheckoutActivity", "updateOrderTypeUI: Selected pickup as default")
                    }
                    dineInEnabled -> {
                        binding.rbDineIn.isChecked = true
                        Log.d("CheckoutActivity", "updateOrderTypeUI: Selected dine-in as default")
                    }
                    deliveryEnabled -> {
                        binding.rbDelivery.isChecked = true
                        Log.d("CheckoutActivity", "updateOrderTypeUI: Selected delivery as default")
                    }
                    else -> {
                        Log.w("CheckoutActivity", "updateOrderTypeUI: No order types enabled!")
                    }
                }
                updateDeliverySectionsVisibility()
            }
            
            // Ensure at least one order type is available (should always be true)
            val hasAnyOrderType = dineInEnabled || pickupEnabled || deliveryEnabled
            if (!hasAnyOrderType) {
                Log.w("CheckoutActivity", "updateOrderTypeUI: No order types enabled - this should not happen!")
            }
            
            Log.d("CheckoutActivity", "updateOrderTypeUI: Completed")
        } catch (e: Exception) {
            Log.e("CheckoutActivity", "updateOrderTypeUI: Error", e)
            e.printStackTrace()
        }
    }
    
    private fun updatePaymentMethodUI() {
        try {
            Log.d("CheckoutActivity", "updatePaymentMethodUI: Starting")
            val methods = paymentMethods
            if (methods == null) {
                Log.w("CheckoutActivity", "updatePaymentMethodUI: Payment methods is null, skipping")
                return
            }
            
        val orderType = sessionManager.getOrderType()
        Log.d("CheckoutActivity", "updatePaymentMethodUI: Order type = $orderType")
        
        // Show payment method card if at least one method is available
        val cliQEnabled = methods.cliQServices?.enabled == true
        val hasPaymentMethods = when (orderType) {
            "pickup", "dine_in" -> methods.cashOnPickup || methods.creditCard || methods.onlinePayment || methods.mobilePayment || cliQEnabled
            "delivery" -> methods.cashOnDelivery || methods.creditCard || methods.onlinePayment || methods.mobilePayment || cliQEnabled
            else -> false
        }
        
        if (hasPaymentMethods) {
            binding.cardPaymentMethod.visibility = android.view.View.VISIBLE
            
            // Show/hide cash option based on order type
            val cashAvailable = when (orderType) {
                "pickup", "dine_in" -> methods.cashOnPickup
                "delivery" -> methods.cashOnDelivery
                else -> false
            }
            
            // Hide Credit/Debit Card option - we'll use saved cards and + Add new card instead
            binding.rbCard.visibility = android.view.View.GONE
            binding.rbOnline.visibility = if (methods.onlinePayment) android.view.View.VISIBLE else android.view.View.GONE
            binding.rbMobile.visibility = if (methods.mobilePayment) android.view.View.VISIBLE else android.view.View.GONE
            binding.rbCliQ.visibility = if (cliQEnabled) android.view.View.VISIBLE else android.view.View.GONE
            
            // Update CliQ Services radio button text to show name/phone if available
            if (cliQEnabled) {
                val cliQName = methods.cliQServices?.name
                val cliQPhone = methods.cliQServices?.phone
                val cliQText = buildString {
                    append(getString(R.string.payment_cliq))
                    if (!cliQName.isNullOrEmpty() || !cliQPhone.isNullOrEmpty()) {
                        append(" (")
                        if (!cliQName.isNullOrEmpty()) {
                            append(cliQName)
                            if (!cliQPhone.isNullOrEmpty()) {
                                append(" - ")
                            }
                        }
                        if (!cliQPhone.isNullOrEmpty()) {
                            append(cliQPhone)
                        }
                        append(")")
                    }
                }
                binding.rbCliQ.text = cliQText
            }
            
            // Load and show saved payment methods if credit card is available
            if (methods.creditCard) {
                loadSavedPaymentMethods()
            }
            
            // Reorder payment methods: Saved cards first, then + Add new card, then Cash (last)
            // Cash should be moved to the end
            if (cashAvailable && binding.rbCash.parent != null) {
                val parent = binding.rbCash.parent as? android.view.ViewGroup
                parent?.removeView(binding.rbCash)
                binding.rgPaymentMethod.addView(binding.rbCash) // Add at the end
            }
            
            // Ensure at least one payment method is selected
            val hasSelection = binding.rbCash.isChecked || binding.rbOnline.isChecked || 
                              binding.rbMobile.isChecked || binding.rbCliQ.isChecked || selectedSavedPaymentMethod != null
            if (!hasSelection) {
                // Default to Cash if available, otherwise first available option
                if (cashAvailable) {
                    binding.rbCash.isChecked = true
                } else if (methods.onlinePayment) {
                    binding.rbOnline.isChecked = true
                } else if (methods.mobilePayment) {
                    binding.rbMobile.isChecked = true
                } else if (cliQEnabled) {
                    binding.rbCliQ.isChecked = true
                }
            }
            
            // Update button text based on selected payment method
            updatePlaceOrderButtonText()
        } else {
            binding.cardPaymentMethod.visibility = android.view.View.GONE
            Log.d("CheckoutActivity", "updatePaymentMethodUI: No payment methods available, hiding card")
        }
            Log.d("CheckoutActivity", "updatePaymentMethodUI: Completed")
        } catch (e: Exception) {
            Log.e("CheckoutActivity", "updatePaymentMethodUI: Error", e)
            e.printStackTrace()
            // Hide payment method card on error to prevent crash
            try {
                binding.cardPaymentMethod.visibility = android.view.View.GONE
            } catch (e2: Exception) {
                Log.e("CheckoutActivity", "updatePaymentMethodUI: Error hiding card", e2)
            }
        }
    }
    
    /**
     * Loads and displays saved payment methods from backend.
     * Order: Saved cards first, then "+ Add new card", then Cash (last).
     */
    private fun loadSavedPaymentMethods() {
        val customerId = sessionManager.getCustomerId()
        val authToken = sessionManager.getAuthToken()
        
        if (customerId == -1 || authToken == null) {
            Log.d("CheckoutActivity", "loadSavedPaymentMethods: Customer not logged in, skipping")
            // Show "Add new card" option if credit card payment is available
            if (paymentMethods?.creditCard == true) {
                binding.rbAddNewCard.visibility = android.view.View.VISIBLE
                setupAddNewCardClickListener()
            }
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPaymentMethods(
                    customerId = customerId,
                    token = "Bearer $authToken"
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val paymentMethodsList = response.body()!!.payment_methods
                    Log.d("CheckoutActivity", "loadSavedPaymentMethods: Loaded ${paymentMethodsList.size} payment methods from backend")
                    
                    // Remove all existing saved card radio buttons (they have dynamic IDs)
                    val savedCardViews = mutableListOf<android.view.View>()
                    for (i in 0 until binding.rgPaymentMethod.childCount) {
                        val child = binding.rgPaymentMethod.getChildAt(i)
                        if (child.tag != null && child.tag is com.mnsf.resturantandroid.network.PaymentMethod) {
                            savedCardViews.add(child)
                        }
                    }
                    savedCardViews.forEach { binding.rgPaymentMethod.removeView(it) }
                    
                    runOnUiThread {
                        // Reorder: Saved cards first, then + Add new card, then Cash
                        // Step 1: Remove Cash temporarily to reorder
                        val cashParent = binding.rbCash.parent as? android.view.ViewGroup
                        cashParent?.removeView(binding.rbCash)
                        
                        // Step 2: Add saved cards first
                        var insertIndex = 0
                        if (paymentMethodsList.isNotEmpty()) {
                            paymentMethodsList.forEach { paymentMethod ->
                                val radioButton = com.google.android.material.radiobutton.MaterialRadioButton(this@CheckoutActivity).apply {
                                    id = android.view.View.generateViewId()
                                    text = "${paymentMethod.card_brand ?: "Card"} •••• ${paymentMethod.card_last4}"
                                    textSize = 15f
                                    setTextColor(ContextCompat.getColor(this@CheckoutActivity, R.color.black))
                                    setPadding(0, 32, 0, 32) // 8dp vertical padding
                                    setCompoundDrawablesWithIntrinsicBounds(
                                        R.drawable.ic_baseline_credit_card_24, 0, 0, 0
                                    )
                                    compoundDrawablePadding = 48 // 12dp padding
                                    setButtonTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.primary_color)))
                                    
                                    // Store payment method data in tag
                                    tag = paymentMethod
                                    
                                    // Set click listener to select this saved card
                                    setOnClickListener {
                                        selectedSavedPaymentMethod = SavedPaymentMethod(
                                            token = paymentMethod.token,
                                            cardLast4Digits = paymentMethod.card_last4,
                                            cardBrand = paymentMethod.card_brand
                                        )
                                        // Uncheck other radio buttons
                                        binding.rbCash.isChecked = false
                                        binding.rbAddNewCard.isChecked = false
                                        updatePlaceOrderButtonText()
                                    }
                                }
                                
                                binding.rgPaymentMethod.addView(radioButton, insertIndex++)
                                
                                // If this is the newly added card, select it
                                if (selectedSavedPaymentMethod?.token == paymentMethod.token) {
                                    radioButton.isChecked = true
                                }
                            }
                        }
                        
                        // Step 3: Show "+ Add new card" option (after saved cards)
                        if (paymentMethods?.creditCard == true) {
                            binding.rbAddNewCard.visibility = android.view.View.VISIBLE
                            // Remove and re-add to ensure correct position
                            val addNewCardParent = binding.rbAddNewCard.parent as? android.view.ViewGroup
                            addNewCardParent?.removeView(binding.rbAddNewCard)
                            binding.rgPaymentMethod.addView(binding.rbAddNewCard, insertIndex++)
                            setupAddNewCardClickListener()
                        }
                        
                        // Step 4: Add Cash at the end (last)
                        binding.rgPaymentMethod.addView(binding.rbCash)
                        
                        // If no saved card is selected and we have saved cards, select the first one
                        if (paymentMethodsList.isNotEmpty() && selectedSavedPaymentMethod == null) {
                            val firstCardButton = binding.rgPaymentMethod.getChildAt(0) as? com.google.android.material.radiobutton.MaterialRadioButton
                            firstCardButton?.let {
                                val firstPaymentMethod = it.tag as? com.mnsf.resturantandroid.network.PaymentMethod
                                if (firstPaymentMethod != null) {
                                    selectedSavedPaymentMethod = SavedPaymentMethod(
                                        token = firstPaymentMethod.token,
                                        cardLast4Digits = firstPaymentMethod.card_last4,
                                        cardBrand = firstPaymentMethod.card_brand
                                    )
                                    it.isChecked = true
                                    updatePlaceOrderButtonText()
                                }
                            }
                        }
                    }
                } else {
                    Log.e("CheckoutActivity", "loadSavedPaymentMethods: Failed to load: ${response.code()} ${response.message()}")
                    runOnUiThread {
                        if (paymentMethods?.creditCard == true) {
                            binding.rbAddNewCard.visibility = android.view.View.VISIBLE
                            setupAddNewCardClickListener()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "loadSavedPaymentMethods: Error", e)
                e.printStackTrace()
                runOnUiThread {
                    if (paymentMethods?.creditCard == true) {
                        binding.rbAddNewCard.visibility = android.view.View.VISIBLE
                        setupAddNewCardClickListener()
                    }
                }
            }
        }
    }
    
    /**
     * Sets up the click listener for "+ Add new card" button.
     * This just selects the option - PayTabs will be launched when "Place Order" is clicked.
     */
    private fun setupAddNewCardClickListener() {
        binding.rbAddNewCard.setOnClickListener {
            selectedSavedPaymentMethod = null // Clear selection
            binding.rbCash.isChecked = false
            binding.rbAddNewCard.isChecked = true
            updatePlaceOrderButtonText()
            
            // Don't launch PayTabs here - wait for "Place Order" button
            // When "Place Order" is clicked, it will launch PayTabs to add card AND process payment
            Log.d("CheckoutPay", "+ Add new card selected. Will launch PayTabs when Place Order is clicked.")
        }
    }
    
    /**
     * Launches PayTabs payment flow just to add a card (not process payment).
     * Uses minimal amount (0.01) for card tokenization.
     */
    private fun launchPayTabsCardPaymentForAddingCard() {
        if (isFinishing || isDestroyed) {
            return
        }
        
        val customerName = sessionManager.getCustomerName() ?: "Guest"
        val customerEmail = sessionManager.getCustomerEmail()?.ifBlank { "customer@email.com" } ?: "customer@email.com"
        val customerPhone = sessionManager.getCustomerPhone() ?: "N/A"
        val customerAddress = sessionManager.getCustomerAddress() ?: "N/A"

        // Use minimal amount (0.01) just to add the card
        val amount = 0.01
        val currency = currencyCode ?: "JOD"
        val cartId = "add_card_${System.currentTimeMillis()}"

        val billingData = PaymentSdkBillingDetails(
            "N/A",
            "JO",
            customerEmail,
            customerName,
            customerPhone,
            "N/A",
            customerAddress,
            "N/A"
        )
        val shippingData = PaymentSdkShippingDetails(
            "N/A",
            "JO",
            customerEmail,
            customerName,
            customerPhone,
            "N/A",
            customerAddress,
            "N/A"
        )

        var configBuilder = PaymentSdkConfigBuilder(
            BuildConfig.PAYTABS_PROFILE_ID,
            BuildConfig.PAYTABS_SERVER_KEY,
            BuildConfig.PAYTABS_CLIENT_KEY,
            amount,
            currency
        )
            .setCartId(cartId)
            .setCartDescription("Add payment card")
            .setMerchantCountryCode("JO")
            .setBillingData(billingData)
            .setShippingData(shippingData)
            .setLanguageCode(PaymentSdkLanguageCode.EN)
            .setTransactionType(PaymentSdkTransactionType.SALE)
            .showBillingInfo(false)
            .showShippingInfo(false)
        
        // Enable tokenization for new card
        var tokenizationEnabled = false
        try {
            val builderClass = configBuilder.javaClass
            val tokeniseMethods = builderClass.declaredMethods.filter { 
                it.name == "setTokenise" || it.name.startsWith("setTokenise")
            }
            
            for (method in tokeniseMethods) {
                try {
                    when (method.parameterTypes.size) {
                        0 -> {
                            method.invoke(configBuilder)
                            tokenizationEnabled = true
                            Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}()")
                            break
                        }
                        1 -> {
                            val paramType = method.parameterTypes[0]
                            when {
                                paramType == Boolean::class.java || paramType == Boolean::class.javaPrimitiveType -> {
                                    method.invoke(configBuilder, true)
                                    tokenizationEnabled = true
                                    Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}(true)")
                                    break
                                }
                                paramType == Int::class.java || paramType == Int::class.javaPrimitiveType -> {
                                    method.invoke(configBuilder, 2)
                                    tokenizationEnabled = true
                                    Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}(2)")
                                    break
                                }
                            }
                        }
                        2 -> {
                            // Handle setTokenise(PaymentSdkTokenise, PaymentSdkTokenFormat)
                            val param1Type = method.parameterTypes[0]
                            val param2Type = method.parameterTypes[1]
                            
                            try {
                                var tokeniseEnumClass: Class<*>? = null
                                var tokeniseValue: Any? = null
                                
                                val possibleTokenisePaths = listOf(
                                    "com.payment.paymentsdk.integrationmodels.PaymentSdkTokenise",
                                    "com.payment.paymentsdk.PaymentSdkTokenise",
                                    param1Type.name
                                )
                                
                                for (className in possibleTokenisePaths) {
                                    try {
                                        val clazz = Class.forName(className)
                                        if (clazz.isEnum) {
                                            tokeniseEnumClass = clazz
                                            val enumValues = clazz.enumConstants
                                            for (enumName in listOf("MERCHANT_MANDATORY", "USER_OPTIONAL", "USER_MANDATORY")) {
                                                tokeniseValue = enumValues?.firstOrNull { 
                                                    (it as? Enum<*>)?.name == enumName 
                                                }
                                                if (tokeniseValue != null) break
                                            }
                                            if (tokeniseValue == null && enumValues != null && enumValues.isNotEmpty()) {
                                                tokeniseValue = enumValues[0]
                                            }
                                            break
                                        }
                                    } catch (e: ClassNotFoundException) {
                                        continue
                                    }
                                }
                                
                                if (tokeniseValue == null && param1Type.isEnum) {
                                    tokeniseEnumClass = param1Type
                                    val enumValues = param1Type.enumConstants
                                    for (enumName in listOf("MERCHANT_MANDATORY", "USER_OPTIONAL", "USER_MANDATORY")) {
                                        tokeniseValue = enumValues?.firstOrNull { 
                                            (it as? Enum<*>)?.name == enumName 
                                        }
                                        if (tokeniseValue != null) break
                                    }
                                    if (tokeniseValue == null && enumValues != null && enumValues.isNotEmpty()) {
                                        tokeniseValue = enumValues[0]
                                    }
                                }
                                
                                var tokenFormatValue: Any? = null
                                val possibleFormatPaths = listOf(
                                    "com.payment.paymentsdk.integrationmodels.PaymentSdkTokenFormat\$Hex32Format",
                                    "com.payment.paymentsdk.integrationmodels.PaymentSdkTokenFormat",
                                    param2Type.name
                                )
                                
                                for (className in possibleFormatPaths) {
                                    try {
                                        val formatClass = Class.forName(className)
                                        if (formatClass.simpleName == "PaymentSdkTokenFormat" || formatClass.name.contains("PaymentSdkTokenFormat")) {
                                            val nestedClasses = formatClass.declaredClasses
                                            val hex32Class = nestedClasses?.firstOrNull { 
                                                it.simpleName.contains("Hex32", ignoreCase = true) || 
                                                it.simpleName == "Hex32Format"
                                            }
                                            if (hex32Class != null) {
                                                try {
                                                    tokenFormatValue = hex32Class.getDeclaredConstructor().newInstance()
                                                    break
                                                } catch (e: Exception) {
                                                    // Ignore
                                                }
                                            }
                                        } else if (formatClass.simpleName.contains("Hex32", ignoreCase = true)) {
                                            try {
                                                tokenFormatValue = formatClass.getDeclaredConstructor().newInstance()
                                                break
                                            } catch (e: Exception) {
                                                // Ignore
                                            }
                                        }
                                    } catch (e: ClassNotFoundException) {
                                        continue
                                    }
                                }
                                
                                if (tokenFormatValue == null) {
                                    try {
                                        tokenFormatValue = param2Type.getDeclaredConstructor().newInstance()
                                    } catch (e: Exception) {
                                        try {
                                            val nestedClasses = param2Type.declaredClasses
                                            val hex32Class = nestedClasses?.firstOrNull { 
                                                it.simpleName.contains("Hex32", ignoreCase = true) 
                                            }
                                            if (hex32Class != null) {
                                                tokenFormatValue = hex32Class.getDeclaredConstructor().newInstance()
                                            }
                                        } catch (e2: Exception) {
                                            // Ignore
                                        }
                                    }
                                }
                                
                                if (tokeniseValue != null && tokenFormatValue != null) {
                                    method.invoke(configBuilder, tokeniseValue, tokenFormatValue)
                                    tokenizationEnabled = true
                                    Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}(PaymentSdkTokenise.${(tokeniseValue as? Enum<*>)?.name}, PaymentSdkTokenFormat.Hex32Format)")
                                    break
                                }
                            } catch (e: Exception) {
                                Log.e("CheckoutPay", "Error handling 2-parameter setTokenise: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("CheckoutPay", "Failed to call ${method.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("CheckoutPay", "❌ Error enabling tokenization: ${e.message}")
        }
        
        if (!tokenizationEnabled) {
            Log.e("CheckoutPay", "⚠️ WARNING: Tokenization not enabled! Card will not be saved.")
        }
        
        val configData = configBuilder.build()

        try {
            PaymentSdkActivity.startCardPayment(this, configData, this)
        } catch (e: Exception) {
            Log.e("CheckoutPay", "startCardPayment threw", e)
            Toast.makeText(this, getString(R.string.payment_failed) + ": " + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        cartViewModel.cartItems.observe(this) {
            updateOrderSummary()
        }
        
        orderViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnPlaceOrder.isEnabled = !isLoading
        }
        
        orderViewModel.orderState.observe(this) { state ->
            try {
                Log.d("CheckoutActivity", "orderState observer: State received - ${state::class.simpleName}")
                when (state) {
                    is OrderViewModel.OrderState.Success -> {
                        try {
                            Log.d("CheckoutActivity", "orderState: Order successful - number=${state.order.order_number}, items=${state.order.items?.size ?: 0}")
                            
                            // Clear cart
                            cartViewModel.clearCart()
                            Log.d("CheckoutActivity", "orderState: Cart cleared")
                            
                            // Navigate to order confirmation activity (pass checkout total so confirmation shows same price)
                            val intent = Intent(this, OrderConfirmationActivity::class.java)
                            intent.putExtra("order", state.order)
                            lastPlacedOrderTotal?.let { total ->
                                intent.putExtra("display_total_amount", total)
                            }
                            Log.d("CheckoutActivity", "orderState: Starting OrderConfirmationActivity with order ${state.order.order_number}, display_total=${lastPlacedOrderTotal}")
                            startActivity(intent)
                            finish()
                            Log.d("CheckoutActivity", "orderState: Navigation completed")
                        } catch (e: Exception) {
                            Log.e("CheckoutActivity", "orderState: Error navigating to confirmation", e)
                            e.printStackTrace()
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    is OrderViewModel.OrderState.Error -> {
                        Log.e("CheckoutActivity", "orderState: Order failed - ${state.message}")
                        resetPlaceOrderButton()
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "orderState: Fatal error in observer", e)
                e.printStackTrace()
            }
        }
        
        orderViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /** Returns current tip amount (JOD 1, JOD 2, or custom decimal). */
    private fun getTipAmount(): Double {
        return when (binding.chipGroupTip.checkedChipId) {
            R.id.chipTip1 -> 1.0
            R.id.chipTip2 -> 2.0
            R.id.chipTipCustom -> {
                val s = binding.etTipCustom.text?.toString()?.trim() ?: ""
                s.toDoubleOrNull()?.coerceIn(0.0, Double.MAX_VALUE) ?: 0.0
            }
            else -> 0.0
        }
    }

    /** Returns selected delivery instruction(s) as comma-separated string (address at order time). */
    private fun getDeliveryInstructionsString(): String? {
        val id = binding.chipGroupDeliveryInstructions.checkedChipId
        if (id == android.view.View.NO_ID) return null
        val chip = binding.chipGroupDeliveryInstructions.findViewById<com.google.android.material.chip.Chip>(id)
        val text = chip?.text?.toString()?.trim()
        return if (text.isNullOrEmpty()) null else text
    }

    private fun setupTipListeners() {
        // Decimal-only filter for custom tip (e.g. 1.5, 0.25)
        binding.etTipCustom.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val existing = dest?.toString() ?: ""
            val proposed = existing.substring(0, dstart) + source?.substring(start, end) + existing.substring(dend, existing.length)
            if (proposed.isEmpty()) return@InputFilter null
            if (!Regex("^\\d*\\.?\\d{0,2}$").matches(proposed)) return@InputFilter ""
            null
        })
        binding.etTipCustom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateOrderSummary() }
        })
        binding.chipGroupTip.setOnCheckedStateChangeListener { _, checkedIds ->
            val isCustom = binding.chipGroupTip.checkedChipId == R.id.chipTipCustom
            binding.layoutCustomTip.visibility = if (isCustom) android.view.View.VISIBLE else android.view.View.GONE
            if (checkedIds.isNotEmpty() && (checkedIds[0] == R.id.chipTip1 || checkedIds[0] == R.id.chipTip2)) {
                binding.chipTipCustom.text = getString(R.string.tip_custom)
                binding.chipTipCustom.isChipIconVisible = false
                binding.etTipCustom.setText("")
            }
            updateOrderSummary()
        }
        // Arrow up: increase tip by 0.5
        binding.btnTipUp.setOnClickListener {
            val current = binding.etTipCustom.text?.toString()?.toDoubleOrNull() ?: 0.0
            binding.etTipCustom.setText(String.format("%.2f", (current + 0.5).coerceAtLeast(0.0)))
            updateOrderSummary()
        }
        // Arrow down: decrease tip by 0.5
        binding.btnTipDown.setOnClickListener {
            val current = binding.etTipCustom.text?.toString()?.toDoubleOrNull() ?: 0.0
            binding.etTipCustom.setText(String.format("%.2f", (current - 0.5).coerceAtLeast(0.0)))
            updateOrderSummary()
        }
        // Apply: update Custom chip label to "JOD X.XX" with pen icon (editable)
        binding.btnTipApply.setOnClickListener {
            val amount = getTipAmount()
            val formatted = CurrencyFormatter.formatPrice(amount, currencyCode, "before")
            binding.chipTipCustom.text = formatted
            binding.chipTipCustom.setChipIconResource(R.drawable.ic_edit_24)
            binding.chipTipCustom.isChipIconVisible = true
            updateOrderSummary()
            binding.layoutCustomTip.visibility = android.view.View.GONE
        }
        // Rider tip info icon: show "This tip goes all the way to the driver"
        binding.iconRiderTipInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.tip_goes_to_driver))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        
        // Service fee info icon
        binding.iconServiceFeeInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.service_fee_info))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun updateOrderSummary() {
        val cartItems = cartViewModel.getCartItemsForRestaurant(restaurantId)
        val subtotal = cartViewModel.getTotalPrice()
        cartItems.forEachIndexed { i, item ->
            val lineSub = item.getSubtotal()
            android.util.Log.d("MoneyLog", "[Checkout] item[$i] product id=${item.product.id} name=${item.product.name} product.price=${item.product.price} unitPriceOverride=${item.unitPriceOverride} addonPriceOverrides=${item.addonPriceOverrides} quantity=${item.quantity} lineSubtotal=$lineSub")
        }
        val orderType = sessionManager.getOrderType()
        val isDelivery = orderType == "delivery"
        val isPickup = orderType == "pickup"
        
        // Tip amount (JOD 1, 2, or custom) - only for delivery; rider tip gone for pickup
        val tipAmount = if (isDelivery) getTipAmount() else 0.0
        
        // Delivery fee: Use zone_price from selected address if available (for delivery company),
        // otherwise fallback to restaurant's delivery_fee
        val deliveryFeeAmount = if (isDelivery) {
            val zonePrice = selectedAddress?.zone_price
            when {
                zonePrice != null && zonePrice > 0 -> zonePrice
                deliveryFee > 0 -> deliveryFee
                else -> 0.0
            }
        } else {
            0.0
        }
        val pickupDiscountAmount = if (isPickup && deliveryFee > 0) deliveryFee else 0.0
        
        // Sales tax: on subtotal only (same as CartActivity for consistent display)
        val taxAmount = if (taxEnabled && taxRate > 0) {
            kotlin.math.round(subtotal * (taxRate / 100.0) * 100.0) / 100.0
        } else {
            0.0
        }
        
        val total = if (isPickup) {
            subtotal - pickupDiscountAmount + taxAmount + serviceFeeFromDb
        } else {
            subtotal + deliveryFeeAmount + taxAmount + tipAmount + serviceFeeFromDb
        }
        android.util.Log.d("MoneyLog", "[Checkout] updateOrderSummary subtotal=$subtotal orderType=$orderType deliveryFee=$deliveryFeeAmount pickupDiscount=$pickupDiscountAmount taxRate=$taxRate taxAmount=$taxAmount tipAmount=$tipAmount serviceFeeFromDb=$serviceFeeFromDb total=$total")

        // Service fee row (from global settings)
        if (serviceFeeFromDb > 0) {
            binding.layoutServiceFee.visibility = android.view.View.VISIBLE
            binding.tvServiceFee.text = CurrencyFormatter.formatPrice(
                serviceFeeFromDb,
                currencyCode,
                currencySymbolPosition
            )
        } else {
            binding.layoutServiceFee.visibility = android.view.View.GONE
        }
        
        // Format and display subtotal
        val formattedSubtotal = CurrencyFormatter.formatPrice(
            subtotal,
            currencyCode,
            currencySymbolPosition
        )
        binding.tvSubtotal.text = formattedSubtotal
        
        // Delivery fee row (delivery only); for pickup show Pickup discount (negative) instead
        if (isDelivery) {
            binding.layoutDeliveryFee.visibility = android.view.View.VISIBLE
            binding.layoutPickupDiscount.visibility = android.view.View.GONE
            val formattedDeliveryFee = CurrencyFormatter.formatPrice(
                deliveryFeeAmount,
                currencyCode,
                currencySymbolPosition
            )
            binding.tvDeliveryFee.text = formattedDeliveryFee
            binding.tvFreeDeliveryLabel.visibility = if (deliveryFeeAmount <= 0) android.view.View.VISIBLE else android.view.View.GONE
        } else if (isPickup && pickupDiscountAmount > 0) {
            binding.layoutDeliveryFee.visibility = android.view.View.GONE
            binding.layoutPickupDiscount.visibility = android.view.View.VISIBLE
            binding.tvPickupDiscount.text = "- " + CurrencyFormatter.formatPrice(
                pickupDiscountAmount,
                currencyCode,
                currencySymbolPosition
            )
        } else {
            binding.layoutDeliveryFee.visibility = android.view.View.GONE
            binding.layoutPickupDiscount.visibility = android.view.View.GONE
        }

        // Rider tip row: delivery only; must be gone for pickup
        if (isDelivery) {
            binding.layoutTip.visibility = android.view.View.VISIBLE
            binding.tvTip.text = CurrencyFormatter.formatPrice(tipAmount, currencyCode, currencySymbolPosition)
        } else {
            binding.layoutTip.visibility = android.view.View.GONE
        }

        // Discount row and savings banner (show when discount > 0; backend can provide later)
        val discountAmount = 0.0
        binding.layoutDiscount.visibility = if (discountAmount > 0) android.view.View.VISIBLE else android.view.View.GONE
        if (discountAmount > 0) {
            binding.tvDiscount.text = "- " + CurrencyFormatter.formatPrice(discountAmount, currencyCode, currencySymbolPosition)
        }
        binding.layoutSavingsBanner.visibility = if (discountAmount > 0) android.view.View.VISIBLE else android.view.View.GONE
        if (discountAmount > 0) {
            binding.tvSavings.text = getString(R.string.awesome_saving, CurrencyFormatter.formatPrice(discountAmount, currencyCode, currencySymbolPosition))
        }
        
        // Show/hide tax / service fee line item
        if (taxEnabled && taxRate > 0) {
            binding.layoutTax.visibility = android.view.View.VISIBLE
            val formattedTax = CurrencyFormatter.formatPrice(
                taxAmount,
                currencyCode,
                currencySymbolPosition
            )
            binding.tvTax.text = formattedTax
        } else {
            binding.layoutTax.visibility = android.view.View.GONE
        }
        
        // Format and display total
        val formattedTotal = CurrencyFormatter.formatPrice(
            total,
            currencyCode,
            currencySymbolPosition
        )
        binding.tvTotal.text = formattedTotal
        binding.tvItemCount.text = "${cartViewModel.getItemCount()} ${getString(R.string.items)}"
    }

    /** Returns the order total (same logic as updateOrderSummary) for PayTabs amount. */
    private fun getOrderTotalAmount(): Double {
        val subtotal = cartViewModel.getTotalPrice()
        val orderType = sessionManager.getOrderType()
        val isDelivery = orderType == "delivery"
        val isPickup = orderType == "pickup"
        val tipAmount = if (isDelivery) getTipAmount() else 0.0
        val zonePrice = selectedAddress?.zone_price
        val deliveryFeeAmount = if (isDelivery) {
            when {
                zonePrice != null && zonePrice > 0 -> zonePrice
                deliveryFee > 0 -> deliveryFee
                else -> 0.0
            }
        } else {
            0.0
        }
        val pickupDiscountAmount = if (isPickup && deliveryFee > 0) deliveryFee else 0.0
        // Sales tax on subtotal only (same as CartActivity)
        val taxAmount = if (taxEnabled && taxRate > 0) kotlin.math.round(subtotal * (taxRate / 100.0) * 100.0) / 100.0 else 0.0
        val total = if (isPickup) {
            subtotal - pickupDiscountAmount + taxAmount + serviceFeeFromDb
        } else {
            subtotal + deliveryFeeAmount + taxAmount + tipAmount + serviceFeeFromDb
        }
        val totalRounded = kotlin.math.round(total * 100.0) / 100.0
        Log.d("MoneyLog", "[Checkout] getOrderTotalAmount subtotal=$subtotal deliveryFee=$deliveryFeeAmount taxAmount=$taxAmount tipAmount=$tipAmount serviceFeeFromDb=$serviceFeeFromDb total=$totalRounded")
        return totalRounded
    }
    
    /** Gets the button text based on selected payment method. */
    private fun getPlaceOrderButtonText(): String {
        return when {
            binding.rbAddNewCard.isChecked -> {
                // Show "Add Card & Continue" when "+ Add new card" is selected
                getString(R.string.add_card_and_continue)
            }
            selectedSavedPaymentMethod != null -> {
                // Show "Make Payment" for saved cards
                getString(R.string.make_payment)
            }
            else -> {
                // Show "Place Order" for cash/other payment methods
                getString(R.string.place_order)
            }
        }
    }
    
    /** Updates the place order button text based on selected payment method. */
    private fun updatePlaceOrderButtonText() {
        // Only update if not in countdown or processing state
        if (placeOrderCountdown == null && !isPlaceOrderProcessing) {
            binding.btnPlaceOrder.text = getPlaceOrderButtonText()
        }
    }
    
    private fun setupClickListeners() {
        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            // When payment method changes, reset saved card selection if switching to cash/other
            if (checkedId == binding.rbCash.id || checkedId == binding.rbOnline.id || checkedId == binding.rbMobile.id || checkedId == binding.rbCliQ.id) {
                selectedSavedPaymentMethod = null
            }
            updatePlaceOrderButtonText()
        }
        binding.btnPlaceOrder.setOnClickListener {
            if (isPlaceOrderProcessing) return@setOnClickListener
            
            // If countdown is running: cancel it and reset button text based on payment method
            if (placeOrderCountdown != null) {
                placeOrderCountdown?.cancel()
                placeOrderCountdown = null
                binding.btnPlaceOrder.text = getPlaceOrderButtonText()
                return@setOnClickListener
            }
            
            try {
                val cartItems = cartViewModel.getCartItemsForRestaurant(restaurantId)
                if (cartItems.isEmpty()) {
                    Toast.makeText(this, getString(R.string.cart_empty), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // If "+ Add new card" is selected, skip countdown and process immediately
                if (binding.rbAddNewCard.isChecked) {
                    Log.d("CheckoutPay", "+ Add new card selected - skipping countdown, processing immediately")
                    isPlaceOrderProcessing = true
                    binding.btnPlaceOrder.isEnabled = false
                    binding.btnPlaceOrder.text = getString(R.string.processing)
                    binding.btnPlaceOrder.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))
                    )
                    submitOrderNow()
                    return@setOnClickListener
                }
                
                // For other payment methods, start 5-second countdown: button shows "Cancel (5)", "Cancel (4)", ...
                var secondsLeft = 5
                binding.btnPlaceOrder.text = getString(R.string.cancel_countdown, secondsLeft)
                placeOrderCountdown = object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        secondsLeft = (millisUntilFinished / 1000).toInt()
                        if (secondsLeft > 0) {
                            binding.btnPlaceOrder.text = getString(R.string.cancel_countdown, secondsLeft)
                        }
                    }
                    override fun onFinish() {
                        Log.d("CheckoutPay", "[0] countdown onFinish - calling submitOrderNow")
                        placeOrderCountdown = null
                        isPlaceOrderProcessing = true
                        binding.btnPlaceOrder.isEnabled = false
                        binding.btnPlaceOrder.text = getString(R.string.processing)
                        binding.btnPlaceOrder.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))
                        )
                        submitOrderNow()
                    }
                }.start()
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "btnPlaceOrder: Error", e)
                Toast.makeText(this, getString(R.string.error_placing_order), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /** Called after 5-second countdown ends: grayed-out "Processing..." then submit. */
    private fun submitOrderNow() {
        Log.d("CheckoutPay", "[1] submitOrderNow called, thread=${Thread.currentThread().name}")
        try {
            val cartItems = cartViewModel.getCartItemsForRestaurant(restaurantId)
            Log.d("CheckoutPay", "[2] cartItems.size=${cartItems.size}")
            if (cartItems.isEmpty()) {
                Log.w("CheckoutPay", "[2b] cart empty, resetting and returning")
                resetPlaceOrderButton()
                Toast.makeText(this, getString(R.string.cart_empty), Toast.LENGTH_SHORT).show()
                return
            }
            val customerName = sessionManager.getCustomerName() ?: "Guest"
            val customerEmail = sessionManager.getCustomerEmail()
            val customerPhone = sessionManager.getCustomerPhone() ?: "N/A"
            val orderType = sessionManager.getOrderType()
            val customerAddress: String? = sessionManager.getCustomerAddress()
            val notes = binding.etNotes.text.toString().trim()
            Log.d("CheckoutPay", "[3] orderType=$orderType")
            val paymentMethod = when {
                selectedSavedPaymentMethod != null -> "card" // Saved card selected
                binding.rbAddNewCard.isChecked -> "card" // "+ Add new card" selected - will launch PayTabs
                binding.rbCash.isChecked -> "cash"
                binding.rbOnline.isChecked -> "online"
                binding.rbMobile.isChecked -> "mobile"
                binding.rbCliQ.isChecked -> "cliq"
                else -> "cash"
            }
            Log.d("CheckoutPay", "[4] paymentMethod=$paymentMethod (rbCash=${binding.rbCash.isChecked} rbAddNewCard=${binding.rbAddNewCard.isChecked} selectedSavedCard=${selectedSavedPaymentMethod != null} rbOnline=${binding.rbOnline.isChecked} rbMobile=${binding.rbMobile.isChecked})")
            val tipAmount = if (orderType == "delivery") getTipAmount() else 0.0
            val deliveryInstructions = if (orderType == "delivery") getDeliveryInstructionsString() else null

            if (paymentMethod == "card") {
                Log.d("CheckoutPay", "[5] ENTERED CARD BRANCH")
                
                // Check if saved card is selected
                if (selectedSavedPaymentMethod != null) {
                    Log.d("CheckoutPay", "[5a] Saved card selected: ****${selectedSavedPaymentMethod!!.cardLast4Digits}, token: ${selectedSavedPaymentMethod!!.token.take(20)}...")
                    // Process payment with saved token server-side (no PayTabs UI)
                    processPaymentWithSavedToken(
                        customerName, customerEmail, customerPhone, customerAddress,
                        orderType, notes, cartItems, tipAmount, deliveryInstructions
                    )
                    return
                }
                
                // "+ Add new card" selected - launch PayTabs to add card AND process payment
                // This will tokenize the card AND charge the customer in one transaction
                Log.d("CheckoutPay", "[5b] + Add new card selected - will launch PayTabs to add card and process payment")
                val profileOk = !BuildConfig.PAYTABS_PROFILE_ID.isNullOrEmpty()
                val serverOk = !BuildConfig.PAYTABS_SERVER_KEY.isNullOrEmpty()
                val clientOk = !BuildConfig.PAYTABS_CLIENT_KEY.isNullOrEmpty()
                Log.d("CheckoutPay", "[5c] PayTabs keys: profileId=${profileOk} serverKey=${serverOk} clientKey=${clientOk}")
                if (!profileOk || !serverOk || !clientOk) {
                    Log.e("CheckoutPay", "[5d] PayTabs NOT configured - missing keys, returning")
                    resetPlaceOrderButton()
                    Toast.makeText(this, getString(R.string.card_payment_not_configured), Toast.LENGTH_LONG).show()
                    return
                }
                pendingOrderCustomerName = customerName
                pendingOrderCustomerEmail = customerEmail
                pendingOrderCustomerPhone = customerPhone
                pendingOrderCustomerAddress = customerAddress
                pendingOrderType = orderType
                pendingOrderNotes = notes
                pendingOrderCartItems = cartItems
                pendingOrderTip = tipAmount
                pendingOrderDeliveryInstructions = deliveryInstructions
                if (orderType == "delivery") {
                    Log.d("CheckoutPay", "[6a] orderType=delivery, checking location")
                    if (LocationHelper.hasLocationPermission(this)) {
                        Log.d("CheckoutPay", "[6b] has location permission, launching coroutine for getCurrentLocation")
                        lifecycleScope.launch {
                            try {
                                Log.d("CheckoutPay", "[6c] inside coroutine, calling getCurrentLocation")
                                val location = LocationHelper.getCurrentLocation(this@CheckoutActivity)
                                if (location != null) {
                                    pendingOrderDeliveryLat = location.latitude
                                    pendingOrderDeliveryLng = location.longitude
                                } else {
                                    pendingOrderDeliveryLat = null
                                    pendingOrderDeliveryLng = null
                                }
                                runOnUiThread {
                                    Log.d("CheckoutPay", "[6d] delivery runOnUiThread: isFinishing=$isFinishing isDestroyed=$isDestroyed")
                                    if (!isFinishing && !isDestroyed) {
                                        Log.d("CheckoutPay", "[6e] launching PayTabs (delivery)")
                                        launchPayTabsCardPayment(pendingOrderDeliveryLat, pendingOrderDeliveryLng)
                                    } else {
                                        Log.w("CheckoutPay", "[6f] activity finishing/destroyed, NOT launching PayTabs")
                                        resetPlaceOrderButton()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CheckoutActivity", "submitOrderNow: delivery location error", e)
                                pendingOrderDeliveryLat = null
                                pendingOrderDeliveryLng = null
                                runOnUiThread {
                                    Log.d("CheckoutPay", "[6g] delivery catch runOnUiThread: isFinishing=$isFinishing isDestroyed=$isDestroyed")
                                    if (!isFinishing && !isDestroyed) {
                                        Log.d("CheckoutPay", "[6h] launching PayTabs (delivery catch)")
                                        launchPayTabsCardPayment(null, null)
                                    } else {
                                        resetPlaceOrderButton()
                                    }
                                }
                            }
                        }
                    } else {
                        resetPlaceOrderButton()
                        Toast.makeText(this, getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
                        LocationHelper.requestLocationPermissions(this, 1001)
                    }
                } else {
                    pendingOrderDeliveryLat = null
                    pendingOrderDeliveryLng = null
                    Log.d("CheckoutPay", "[6] pickup/dine_in - launching PayTabs directly")
                    launchPayTabsCardPayment(null, null)
                }
                Log.d("CheckoutPay", "[7] card branch done, returning (PayTabs should have opened)")
                return
            }

            Log.d("CheckoutPay", "[8] NOT card - taking cash/other path, orderType=$orderType paymentMethod=$paymentMethod")
            if (orderType == "delivery") {
                if (LocationHelper.hasLocationPermission(this)) {
                    lifecycleScope.launch {
                        try {
                            // Try to get location, but don't block order creation if unavailable
                            val location = LocationHelper.getCurrentLocation(this@CheckoutActivity)
                            if (location != null) {
                                Log.d("CheckoutPay", "Location obtained: lat=${location.latitude}, lng=${location.longitude}")
                                createOrderWithLocation(
                                    customerName, customerEmail, customerPhone, customerAddress,
                                    orderType, paymentMethod, notes, location.latitude, location.longitude, cartItems,
                                    tipAmount, deliveryInstructions, null
                                )
                            } else {
                                // Location unavailable - proceed with order creation without location
                                // Don't show error message as order will still be created successfully
                                Log.w("CheckoutPay", "Location unavailable, creating order without location coordinates")
                                createOrderWithLocation(
                                    customerName, customerEmail, customerPhone, customerAddress,
                                    orderType, paymentMethod, notes, null, null, cartItems,
                                    tipAmount, deliveryInstructions, null
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("CheckoutPay", "Error getting location for cash delivery order", e)
                            // Proceed with order creation even if location fails
                            createOrderWithLocation(
                                customerName, customerEmail, customerPhone, customerAddress,
                                orderType, paymentMethod, notes, null, null, cartItems,
                                tipAmount, deliveryInstructions, null
                            )
                        }
                    }
                } else {
                    resetPlaceOrderButton()
                    Toast.makeText(this, getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
                    LocationHelper.requestLocationPermissions(this, 1001)
                }
            } else {
                createOrderWithLocation(
                    customerName, customerEmail, customerPhone, customerAddress,
                    orderType, paymentMethod, notes, null, null, cartItems,
                    tipAmount, deliveryInstructions, null
                )
            }
        } catch (e: Exception) {
            Log.e("CheckoutPay", "[submitOrderNow] exception", e)
            Log.e("CheckoutActivity", "submitOrderNow: Error", e)
            resetPlaceOrderButton()
            Toast.makeText(this, getString(R.string.error_placing_order), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resetPlaceOrderButton() {
        isPlaceOrderProcessing = false
        binding.btnPlaceOrder.isEnabled = true
        binding.btnPlaceOrder.text = getPlaceOrderButtonText()
        binding.btnPlaceOrder.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.primary_color)))
    }
    
    /**
     * Processes payment with saved token server-side (no PayTabs UI).
     * Then creates the order with the transaction reference.
     */
    private fun processPaymentWithSavedToken(
        customerName: String,
        customerEmail: String?,
        customerPhone: String,
        customerAddress: String?,
        orderType: String,
        notes: String?,
        cartItems: List<com.mnsf.resturantandroid.data.model.CartItem>,
        tipAmount: Double,
        deliveryInstructions: String?
    ) {
        lifecycleScope.launch {
            try {
                val savedCard = selectedSavedPaymentMethod
                if (savedCard == null) {
                    Log.e("CheckoutPay", "processPaymentWithSavedToken: No saved card selected")
                    resetPlaceOrderButton()
                    Toast.makeText(this@CheckoutActivity, "No card selected", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val amountRaw = getOrderTotalAmount()
                if (amountRaw < 0.01) {
                    Log.e("CheckoutPay", "processPaymentWithSavedToken: Invalid amount=$amountRaw, aborting")
                    resetPlaceOrderButton()
                    Toast.makeText(this@CheckoutActivity, getString(R.string.error_placing_order), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val amount = amountRaw
                val currency = currencyCode ?: "JOD"
                val cartId = "order_${restaurantId}_${System.currentTimeMillis()}"
                val cartDescription = getString(R.string.order_summary)

                // Prepare customer details
                val customerDetails = mutableMapOf<String, Any>(
                    "name" to customerName,
                    "phone" to customerPhone
                )
                customerEmail?.let { customerDetails["email"] = it }
                customerAddress?.let { customerDetails["street1"] = it }

                // Call backend to process payment with token
                // Note: We don't send server_key from Android because BuildConfig.PAYTABS_SERVER_KEY
                // is a Mobile SDK key, which cannot be used for server-to-server API calls.
                // The backend MUST use PAYTABS_SERVER_KEY environment variable (Web API key).
                val profileId = BuildConfig.PAYTABS_PROFILE_ID ?: ""
                
                if (profileId.isEmpty()) {
                    Log.e("CheckoutPay", "❌ PAYTABS_PROFILE_ID is empty in BuildConfig!")
                    resetPlaceOrderButton()
                    Toast.makeText(this@CheckoutActivity, "PayTabs profile ID not configured", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                Log.d("CheckoutPay", "Payment request details:")
                Log.d("CheckoutPay", "  profile_id: $profileId (length: ${profileId.length})")
                Log.d("CheckoutPay", "  server_key: NOT sent (backend uses PAYTABS_SERVER_KEY env var - Web API key)")
                Log.d("CheckoutPay", "  token: ${savedCard.token.take(20)}... (length: ${savedCard.token.length})")
                Log.d("CheckoutPay", "  amount: $amount")
                Log.d("CheckoutPay", "  currency: $currency")
                
                val paymentRequest = com.mnsf.resturantandroid.network.ProcessPaymentWithTokenRequest(
                    profile_id = profileId,
                    server_key = null, // Backend uses PAYTABS_SERVER_KEY env var (Web API key)
                    token = savedCard.token,
                    amount = amount,
                    currency = currency,
                    cart_id = cartId,
                    cart_description = cartDescription,
                    customer_details = customerDetails
                )

                Log.d("CheckoutPay", "Processing payment with saved token: ****${savedCard.cardLast4Digits}")
                
                // Log the exact JSON that will be sent
                try {
                    val jsonPayload = Gson().toJson(paymentRequest)
                    Log.d("CheckoutPay", "📤 Request JSON payload: $jsonPayload")
                    Log.d("CheckoutPay", "📤 Request object fields: profile_id=${paymentRequest.profile_id}, server_key=${if (paymentRequest.server_key != null) "present (${paymentRequest.server_key!!.length} chars)" else "null"}, token=${paymentRequest.token.take(20)}...")
                } catch (e: Exception) {
                    Log.e("CheckoutPay", "Failed to serialize request for logging", e)
                }
                
                val paymentResponse = RetrofitClient.apiService.processPaymentWithToken(paymentRequest)

                if (paymentResponse.isSuccessful && paymentResponse.body() != null) {
                    val paymentResult = paymentResponse.body()!!
                    if (paymentResult.success && !paymentResult.transaction_reference.isNullOrEmpty()) {
                        Log.e("CheckoutPay", "✅ Payment successful with saved token: ${paymentResult.transaction_reference}")
                        
                        // Get delivery location if needed
                        var deliveryLat: Double? = null
                        var deliveryLng: Double? = null
                        
                        if (orderType == "delivery" && LocationHelper.hasLocationPermission(this@CheckoutActivity)) {
                            try {
                                val location = LocationHelper.getCurrentLocation(this@CheckoutActivity)
                                deliveryLat = location?.latitude
                                deliveryLng = location?.longitude
                            } catch (e: Exception) {
                                Log.e("CheckoutPay", "Error getting location for saved card payment", e)
                            }
                        }

                        // Create order with transaction reference
                        createOrderWithLocation(
                            customerName = customerName,
                            customerEmail = customerEmail,
                            customerPhone = customerPhone,
                            customerAddress = customerAddress,
                            orderType = orderType,
                            paymentMethod = "card",
                            notes = notes,
                            deliveryLatitude = deliveryLat,
                            deliveryLongitude = deliveryLng,
                            cartItems = cartItems,
                            tip = tipAmount,
                            deliveryInstructions = deliveryInstructions,
                            paymentIntentId = paymentResult.transaction_reference
                        )
                    } else {
                        Log.e("CheckoutPay", "❌ Payment failed with saved token: ${paymentResult.message}")
                        resetPlaceOrderButton()
                        Toast.makeText(
                            this@CheckoutActivity,
                            paymentResult.message ?: "Payment failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val errorBody = paymentResponse.errorBody()?.string()
                    Log.e("CheckoutPay", "❌ Payment API error: ${paymentResponse.code()} - $errorBody")
                    
                    // Parse error message for better user feedback
                    var errorMessage = "Payment failed"
                    try {
                        if (!errorBody.isNullOrEmpty()) {
                            val errorJson = Gson().fromJson(errorBody, Map::class.java)
                            val message = errorJson["message"] as? String
                            val hint = errorJson["hint"] as? String
                            
                            if (message != null) {
                                errorMessage = message
                                if (hint != null) {
                                    errorMessage += "\n\n$hint"
                                }
                            }
                            
                            // Check for PCI DSS error specifically
                            if (message?.contains("PCI DSS", ignoreCase = true) == true) {
                                errorMessage = "Saved card payments require PayTabs account configuration.\n\n" +
                                        "Please contact PayTabs support to enable recurring transactions.\n\n" +
                                        "For now, please use 'Add new card' option."
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CheckoutPay", "Error parsing error response", e)
                        errorMessage = paymentResponse.message() ?: "Payment failed"
                    }
                    
                    resetPlaceOrderButton()
                    Toast.makeText(
                        this@CheckoutActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("CheckoutPay", "❌ Error processing payment with saved token", e)
                e.printStackTrace()
                resetPlaceOrderButton()
                Toast.makeText(
                    this@CheckoutActivity,
                    "Error processing payment: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /** Builds restaurant info payload so Driver app can show pickup name, phone, address and navigate. */
    private fun toRestaurantInfoRequest(restaurant: com.mnsf.resturantandroid.data.model.Restaurant): com.mnsf.resturantandroid.data.model.RestaurantInfoRequest =
        com.mnsf.resturantandroid.data.model.RestaurantInfoRequest(
            name = restaurant.restaurant_name,
            phone = restaurant.phone,
            address = restaurant.address,
            latitude = restaurant.latitude,
            longitude = restaurant.longitude
        )

    private fun createOrderWithLocation(
        customerName: String,
        customerEmail: String?,
        customerPhone: String,
        customerAddress: String?,
        orderType: String,
        paymentMethod: String,
        notes: String?,
        deliveryLatitude: Double?,
        deliveryLongitude: Double?,
        cartItems: List<com.mnsf.resturantandroid.data.model.CartItem>,
        tip: Double = 0.0,
        deliveryInstructions: String? = null,
        paymentIntentId: String? = null
    ) {
        try {
            Log.d("CheckoutPay", "[createOrder] called - paymentMethod=$paymentMethod orderType=$orderType (this is CASH/other path; if you selected card, this should NOT appear)")
            Log.d("CheckoutActivity", "createOrderWithLocation: Creating order - paymentMethod=$paymentMethod, orderType=$orderType, lat=$deliveryLatitude, lng=$deliveryLongitude, tip=$tip, paymentIntentId=$paymentIntentId")
            val orderTotal = getOrderTotalAmount()
            lastPlacedOrderTotal = orderTotal
            Log.d("MoneyLog", "[Checkout] createOrderWithLocation sending total_amount=$orderTotal paymentMethod=$paymentMethod")
            orderViewModel.createOrder(
                restaurantId = restaurantId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                customerAddress = customerAddress,
                orderType = orderType,
                paymentMethod = paymentMethod,
                paymentIntentId = paymentIntentId,
                cartItems = cartItems,
                notes = if (notes != null && notes.isNotEmpty()) notes else null,
                deliveryLatitude = deliveryLatitude,
                deliveryLongitude = deliveryLongitude,
                tip = if (tip > 0) tip else null,
                deliveryInstructions = deliveryInstructions,
                totalAmount = orderTotal,
                restaurantInfo = currentRestaurant?.let { toRestaurantInfoRequest(it) }
            )
            Log.d("CheckoutActivity", "createOrderWithLocation: Order creation initiated")
        } catch (e: Exception) {
            Log.e("CheckoutActivity", "createOrderWithLocation: Error", e)
            Toast.makeText(this, getString(R.string.error_placing_order), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches PayTabs card payment immediately on main thread (no delay).
     * Aligned with ChatGPT fix: remove Handler.postDelayed, call startCardPayment directly,
     * minimize lifecycle race conditions. Theme override in manifest (PayTabsFixTheme) helps
     * prevent silent crashes during PayTabs activity inflation.
     */
    private fun launchPayTabsCardPayment(deliveryLat: Double?, deliveryLng: Double?) {
        if (isFinishing || isDestroyed) {
            resetPlaceOrderButton()
            return
        }
        val cartItems = pendingOrderCartItems
        if (cartItems.isNullOrEmpty()) {
            resetPlaceOrderButton()
            Toast.makeText(this, getString(R.string.cart_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val customerName = pendingOrderCustomerName ?: "Guest"
        val customerEmail = pendingOrderCustomerEmail?.ifBlank { "customer@email.com" } ?: "customer@email.com"
        val customerPhone = pendingOrderCustomerPhone ?: "N/A"
        val customerAddress = pendingOrderCustomerAddress ?: "N/A"

        val amountRaw = getOrderTotalAmount()
        if (amountRaw < 0.01) {
            Log.e("CheckoutPay", "launchPayTabsCardPayment: Invalid amount=$amountRaw, aborting")
            resetPlaceOrderButton()
            Toast.makeText(this, getString(R.string.error_placing_order), Toast.LENGTH_SHORT).show()
            return
        }
        val amount = ((amountRaw * 100).toInt() / 100.0).coerceAtLeast(0.01)
        Log.d("CheckoutPay", "PayTabs amount: $amount (raw: $amountRaw)")
        val currency = currencyCode ?: "JOD"
        val cartId = "order_${restaurantId}_${System.currentTimeMillis()}"

        val billingData = PaymentSdkBillingDetails(
            "N/A",
            "JO",
            customerEmail,
            customerName,
            customerPhone,
            "N/A",
            customerAddress,
            "N/A"
        )
        val shippingData = PaymentSdkShippingDetails(
            "N/A",
            "JO",
            customerEmail,
            customerName,
            customerPhone,
            "N/A",
            customerAddress,
            "N/A"
        )

        var configBuilder = PaymentSdkConfigBuilder(
            BuildConfig.PAYTABS_PROFILE_ID,
            BuildConfig.PAYTABS_SERVER_KEY,
            BuildConfig.PAYTABS_CLIENT_KEY,
            amount,
            currency
        )
            .setCartId(cartId)
            .setCartDescription(getString(R.string.order_summary))
            .setMerchantCountryCode("JO")
            .setBillingData(billingData)
            .setShippingData(shippingData)
            .setLanguageCode(PaymentSdkLanguageCode.EN)
            .setTransactionType(PaymentSdkTransactionType.SALE)
            .showBillingInfo(false)
            .showShippingInfo(false)
        
        // Enable tokenization to save card for future payments
        if (selectedSavedPaymentMethod == null) {
            // For new card, enable tokenization
            // Try calling setTokenise directly (it exists in available methods)
            var tokenizationEnabled = false
            
            try {
                // Direct call approach - try as extension function or method
                // Since setTokenise exists, try calling it directly
                @Suppress("UNCHECKED_CAST")
                val builderClass = configBuilder.javaClass
                
                // List all setTokenise variants
                val tokeniseMethods = builderClass.declaredMethods.filter { 
                    it.name == "setTokenise" || it.name.startsWith("setTokenise")
                }
                Log.d("CheckoutPay", "Found setTokenise methods: ${tokeniseMethods.map { "${it.name}(${it.parameterTypes.joinToString { it.simpleName }})" }.joinToString(", ")}")
                
                // Try each variant
                for (method in tokeniseMethods) {
                    try {
                        when (method.parameterTypes.size) {
                            0 -> {
                                // No parameters
                                method.invoke(configBuilder)
                                tokenizationEnabled = true
                                Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}()")
                                break
                            }
                            1 -> {
                                val paramType = method.parameterTypes[0]
                                when {
                                    paramType == Boolean::class.java || paramType == Boolean::class.javaPrimitiveType -> {
                                        method.invoke(configBuilder, true)
                                        tokenizationEnabled = true
                                        Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}(true)")
                                        break
                                    }
                                    paramType == Int::class.java || paramType == Int::class.javaPrimitiveType -> {
                                        method.invoke(configBuilder, 2) // 2 = Hex32 format
                                        tokenizationEnabled = true
                                        Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}(2)")
                                        break
                                    }
                                    else -> {
                                        Log.d("CheckoutPay", "Skipping ${method.name} with parameter type: ${paramType.simpleName}")
                                    }
                                }
                            }
                            2 -> {
                                // Handle setTokenise(PaymentSdkTokenise, PaymentSdkTokenFormat)
                                val param1Type = method.parameterTypes[0]
                                val param2Type = method.parameterTypes[1]
                                Log.d("CheckoutPay", "Attempting 2-parameter setTokenise: param1=${param1Type.name}, param2=${param2Type.name}")
                                
                                try {
                                    // Try to load PaymentSdkTokenise enum directly from PayTabs SDK package
                                    var tokeniseEnumClass: Class<*>? = null
                                    var tokeniseValue: Any? = null
                                    
                                    // Try common PayTabs SDK package paths
                                    val possibleTokenisePaths = listOf(
                                        "com.payment.paymentsdk.integrationmodels.PaymentSdkTokenise",
                                        "com.payment.paymentsdk.PaymentSdkTokenise",
                                        param1Type.name
                                    )
                                    
                                    for (className in possibleTokenisePaths) {
                                        try {
                                            val clazz = Class.forName(className)
                                            if (clazz.isEnum) {
                                                tokeniseEnumClass = clazz
                                                Log.d("CheckoutPay", "Found PaymentSdkTokenise class: $className")
                                                
                                                // Get enum values
                                                val enumValues = clazz.enumConstants
                                                
                                                // Prefer MERCHANT_MANDATORY, fallback to USER_OPTIONAL, then USER_MANDATORY
                                                for (enumName in listOf("MERCHANT_MANDATORY", "USER_OPTIONAL", "USER_MANDATORY")) {
                                                    tokeniseValue = enumValues?.firstOrNull { 
                                                        (it as? Enum<*>)?.name == enumName 
                                                    }
                                                    if (tokeniseValue != null) {
                                                        Log.d("CheckoutPay", "Found PaymentSdkTokenise.$enumName")
                                                        break
                                                    }
                                                }
                                                
                                                // Fallback to first available enum value
                                                if (tokeniseValue == null && enumValues != null && enumValues.isNotEmpty()) {
                                                    tokeniseValue = enumValues[0]
                                                    Log.d("CheckoutPay", "Using first available PaymentSdkTokenise value: ${(tokeniseValue as? Enum<*>)?.name}")
                                                }
                                                break
                                            }
                                        } catch (e: ClassNotFoundException) {
                                            // Try next path
                                            continue
                                        }
                                    }
                                    
                                    // If direct loading failed, use the parameter type itself
                                    if (tokeniseValue == null && param1Type.isEnum) {
                                        tokeniseEnumClass = param1Type
                                        val enumValues = param1Type.enumConstants
                                        for (enumName in listOf("MERCHANT_MANDATORY", "USER_OPTIONAL", "USER_MANDATORY")) {
                                            tokeniseValue = enumValues?.firstOrNull { 
                                                (it as? Enum<*>)?.name == enumName 
                                            }
                                            if (tokeniseValue != null) break
                                        }
                                        if (tokeniseValue == null && enumValues != null && enumValues.isNotEmpty()) {
                                            tokeniseValue = enumValues[0]
                                        }
                                    }
                                    
                                    // Try to load PaymentSdkTokenFormat class
                                    var tokenFormatValue: Any? = null
                                    val possibleFormatPaths = listOf(
                                        "com.payment.paymentsdk.integrationmodels.PaymentSdkTokenFormat\$Hex32Format",
                                        "com.payment.paymentsdk.integrationmodels.PaymentSdkTokenFormat",
                                        param2Type.name
                                    )
                                    
                                    // First, try to find Hex32Format nested class
                                    for (className in possibleFormatPaths) {
                                        try {
                                            val formatClass = Class.forName(className)
                                            
                                            // If it's the base class, look for nested Hex32Format
                                            if (formatClass.simpleName == "PaymentSdkTokenFormat" || formatClass.name.contains("PaymentSdkTokenFormat")) {
                                                // Try to find Hex32Format as nested class
                                                val nestedClasses = formatClass.declaredClasses
                                                val hex32Class = nestedClasses?.firstOrNull { 
                                                    it.simpleName.contains("Hex32", ignoreCase = true) || 
                                                    it.simpleName == "Hex32Format"
                                                }
                                                if (hex32Class != null) {
                                                    try {
                                                        tokenFormatValue = hex32Class.getDeclaredConstructor().newInstance()
                                                        Log.d("CheckoutPay", "Created PaymentSdkTokenFormat.Hex32Format() from nested class")
                                                        break
                                                    } catch (e: Exception) {
                                                        Log.d("CheckoutPay", "Could not instantiate Hex32Format: ${e.message}")
                                                    }
                                                }
                                            } else if (formatClass.simpleName.contains("Hex32", ignoreCase = true)) {
                                                // Direct Hex32Format class
                                                try {
                                                    tokenFormatValue = formatClass.getDeclaredConstructor().newInstance()
                                                    Log.d("CheckoutPay", "Created Hex32Format directly")
                                                    break
                                                } catch (e: Exception) {
                                                    Log.d("CheckoutPay", "Could not instantiate Hex32Format directly: ${e.message}")
                                                }
                                            }
                                        } catch (e: ClassNotFoundException) {
                                            continue
                                        }
                                    }
                                    
                                    // Fallback: try to instantiate param2Type directly (might be a sealed class)
                                    if (tokenFormatValue == null) {
                                        try {
                                            // Try no-arg constructor
                                            tokenFormatValue = param2Type.getDeclaredConstructor().newInstance()
                                            Log.d("CheckoutPay", "Created PaymentSdkTokenFormat using no-arg constructor")
                                        } catch (e: Exception) {
                                            // Try finding nested classes in param2Type
                                            try {
                                                val nestedClasses = param2Type.declaredClasses
                                                val hex32Class = nestedClasses?.firstOrNull { 
                                                    it.simpleName.contains("Hex32", ignoreCase = true) 
                                                }
                                                if (hex32Class != null) {
                                                    tokenFormatValue = hex32Class.getDeclaredConstructor().newInstance()
                                                    Log.d("CheckoutPay", "Created Hex32Format from param2Type nested classes")
                                                }
                                            } catch (e2: Exception) {
                                                Log.d("CheckoutPay", "Could not create token format: ${e2.message}")
                                            }
                                        }
                                    }
                                    
                                    if (tokeniseValue != null && tokenFormatValue != null) {
                                        method.invoke(configBuilder, tokeniseValue, tokenFormatValue)
                                        tokenizationEnabled = true
                                        Log.e("CheckoutPay", "✅ Tokenization enabled via ${method.name}(PaymentSdkTokenise.${(tokeniseValue as? Enum<*>)?.name}, PaymentSdkTokenFormat.Hex32Format)")
                                        break
                                    } else {
                                        Log.w("CheckoutPay", "Could not create required enum/class instances for 2-parameter setTokenise")
                                        Log.w("CheckoutPay", "  tokeniseValue: ${tokeniseValue != null} (class: ${tokeniseEnumClass?.name})")
                                        Log.w("CheckoutPay", "  tokenFormatValue: ${tokenFormatValue != null} (class: ${param2Type.name})")
                                    }
                                } catch (e: Exception) {
                                    Log.e("CheckoutPay", "Error handling 2-parameter setTokenise: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                            else -> {
                                Log.d("CheckoutPay", "Skipping ${method.name} with ${method.parameterTypes.size} parameters")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("CheckoutPay", "Failed to call ${method.name}: ${e.message}")
                    }
                }
                
                if (!tokenizationEnabled && tokeniseMethods.isEmpty()) {
                    Log.e("CheckoutPay", "❌ setTokenise method not found in available methods")
                }
            } catch (e: Exception) {
                Log.e("CheckoutPay", "❌ Error enabling tokenization: ${e.message}")
                e.printStackTrace()
            }
            
            if (!tokenizationEnabled) {
                Log.e("CheckoutPay", "⚠️ WARNING: Tokenization not enabled! Card will not be saved.")
                Log.e("CheckoutPay", "Note: Tokenization may need to be enabled in PayTabs merchant dashboard")
                Log.e("CheckoutPay", "Even without tokenization, we can still extract card info from transaction response")
            }
        } else {
            // For saved card, use the token
            try {
                configBuilder.javaClass.getMethod("setToken", String::class.java).invoke(configBuilder, selectedSavedPaymentMethod!!.token)
                Log.d("CheckoutPay", "Using saved card token: ${selectedSavedPaymentMethod!!.cardLast4Digits}")
            } catch (e: Exception) {
                Log.w("CheckoutPay", "Could not set token (may not be supported)", e)
            }
        }
        
        val configData = configBuilder.build()

        logConnectivityState()
        val serverKeyPrefix = (BuildConfig.PAYTABS_SERVER_KEY ?: "").take(8)
        Log.d("PayTabsResult", "launchPayTabs: amount=$amount currency=$currency cartId=$cartId profileId=${(BuildConfig.PAYTABS_PROFILE_ID ?: "").take(4)}*** serverKeyPrefix=$serverKeyPrefix*** (use sk_test_ for TEST, sk_live_ for LIVE; never mix)")

        try {
            PaymentSdkActivity.startCardPayment(this, configData, this)
        } catch (e: Exception) {
            Log.e("CheckoutPay", "startCardPayment threw", e)
            resetPlaceOrderButton()
            Toast.makeText(this, getString(R.string.payment_failed) + ": " + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    override fun onError(error: PaymentSdkError) {
        Log.e("PayTabsResult", "onError: code=${error.code} msg=${error.msg} trace=${error.trace}")
        Log.e("PayTabsResult", "onError: full error=$error")
        if (error.code == -1 || (error.msg ?: "").contains("Network", ignoreCase = true)) {
            logConnectivityState()
            Log.e("PayTabsResult", "onError NETWORK: PayTabs SDK could not reach servers. Check: (1) Device has internet (WiFi/mobile). (2) Try mobile data if on WiFi. (3) Disable VPN/proxy. (4) PayTabs profile/keys are for correct env (test vs live). (5) Try on real device if on emulator.")
        }
        resetPlaceOrderButton()
        Toast.makeText(this, getString(R.string.payment_failed) + ": " + error.msg, Toast.LENGTH_LONG).show()
    }

    /** Log current connectivity so we can correlate PayTabs network errors with device network state. */
    private fun logConnectivityState() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork
                val caps = network?.let { cm.getNetworkCapabilities(it) }
                val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                val hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                Log.d("PayTabsResult", "connectivity: hasInternet=$hasInternet hasWifi=$hasWifi hasCellular=$hasCellular activeNetwork=$network")
            } else {
                @Suppress("DEPRECATION")
                val ni = cm.activeNetworkInfo
                Log.d("PayTabsResult", "connectivity: activeNetworkInfo=${ni?.typeName} connected=${ni?.isConnected}")
            }
        } catch (e: Exception) {
            Log.e("PayTabsResult", "connectivity: failed to get state", e)
        }
    }

    override fun onPaymentCancel() {
        Log.d("PayTabsResult", "onPaymentCancel: user cancelled PayTabs screen")
        resetPlaceOrderButton()
        Toast.makeText(this, getString(R.string.payment_cancelled), Toast.LENGTH_SHORT).show()
    }

    override fun onPaymentFinish(details: PaymentSdkTransactionDetails) {
        // ========== COMPREHENSIVE PAYTABS RESPONSE LOGGING ==========
        Log.e("PayTabsResult", "═══════════════════════════════════════════════════════════")
        Log.e("PayTabsResult", "PAYTABS PAYMENT RESPONSE RECEIVED")
        Log.e("PayTabsResult", "═══════════════════════════════════════════════════════════")
        
        val isSuccess = details.isSuccess == true
        val transactionRef = details.transactionReference ?: ""
        val responseCode = details.paymentResult?.responseCode
        val responseMessage = details.paymentResult?.responseMessage
        
        // Log basic transaction info
        Log.e("PayTabsResult", "┌─ TRANSACTION SUMMARY ─────────────────────────────────────")
        Log.e("PayTabsResult", "│ isSuccess: $isSuccess")
        Log.e("PayTabsResult", "│ transactionReference (tran_ref): $transactionRef")
        Log.e("PayTabsResult", "│ responseCode: $responseCode")
        Log.e("PayTabsResult", "│ responseMessage: $responseMessage")
        Log.e("PayTabsResult", "└────────────────────────────────────────────────────────────")
        
        // Extract and log all available fields using reflection
        try {
            val detailsClass = details.javaClass
            Log.e("PayTabsResult", "┌─ PAYMENT SDK TRANSACTION DETAILS ────────────────────────")
            
            // Try to extract common fields
            val fieldNames = listOf(
                "cartId", "cart_id", "cartDescription", "cart_description",
                "cartCurrency", "cart_currency", "cartAmount", "cart_amount",
                "token", "tokenizationToken", "tokenization_token",
                "cardLast4Digits", "card_last_4_digits", "cardLast4", "card_last4",
                "cardBrand", "card_brand", "cardScheme", "card_scheme",
                "cardType", "card_type", "paymentDescription", "payment_description",
                "customerEmail", "customer_email", "customerName", "customer_name",
                "customerPhone", "customer_phone", "customerDetails", "customer_details",
                "paymentInfo", "payment_info", "paymentResult", "payment_result",
                "redirectUrl", "redirect_url", "callback", "return"
            )
            
            for (fieldName in fieldNames) {
                try {
                    val field = detailsClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val value = field.get(details)
                    if (value != null) {
                        Log.e("PayTabsResult", "│ $fieldName: $value")
                    }
                } catch (e: NoSuchFieldException) {
                    // Try getter method
                    try {
                        val capitalized = fieldName.replaceFirstChar { it.uppercaseChar() }
                        val methodName = "get$capitalized"
                        val method = detailsClass.getMethod(methodName)
                        val value = method.invoke(details)
                        if (value != null) {
                            Log.e("PayTabsResult", "│ $fieldName: $value")
                        }
                    } catch (e2: Exception) {
                        // Field/method not found, skip
                    }
                } catch (e: Exception) {
                    // Skip this field
                }
            }
            
            // Log paymentResult object details
            try {
                val paymentResult = details.paymentResult
                if (paymentResult != null) {
                    Log.e("PayTabsResult", "│")
                    Log.e("PayTabsResult", "│ ┌─ PAYMENT RESULT ────────────────────────────────────────")
                    val paymentResultClass = paymentResult.javaClass
                    val paymentResultFields = listOf(
                        "responseStatus", "response_status", "status",
                        "responseCode", "response_code", "code",
                        "responseMessage", "response_message", "message",
                        "acquirerMessage", "acquirer_message",
                        "acquirerRRN", "acquirer_rrn", "rrn",
                        "transactionTime", "transaction_time", "time"
                    )
                    
                    for (fieldName in paymentResultFields) {
                        try {
                            val field = paymentResultClass.getDeclaredField(fieldName)
                            field.isAccessible = true
                            val value = field.get(paymentResult)
                            if (value != null) {
                                Log.e("PayTabsResult", "│ │ $fieldName: $value")
                            }
                        } catch (e: NoSuchFieldException) {
                            try {
                                val capitalized = fieldName.replaceFirstChar { it.uppercaseChar() }
                                val methodName = "get$capitalized"
                                val method = paymentResultClass.getMethod(methodName)
                                val value = method.invoke(paymentResult)
                                if (value != null) {
                                    Log.e("PayTabsResult", "│ │ $fieldName: $value")
                                }
                            } catch (e2: Exception) {
                                // Skip
                            }
                        } catch (e: Exception) {
                            // Skip
                        }
                    }
                    Log.e("PayTabsResult", "│ └─────────────────────────────────────────────────────────")
                }
            } catch (e: Exception) {
                Log.e("PayTabsResult", "│ paymentResult extraction error: ${e.message}")
            }
            
            // Log paymentInfo object details
            try {
                val paymentInfoMethod = detailsClass.getMethod("getPaymentInfo")
                val paymentInfo = paymentInfoMethod.invoke(details)
                if (paymentInfo != null) {
                    Log.e("PayTabsResult", "│")
                    Log.e("PayTabsResult", "│ ┌─ PAYMENT INFO ───────────────────────────────────────────")
                    val paymentInfoClass = paymentInfo.javaClass
                    val paymentInfoFields = listOf(
                        "cardType", "card_type", "type",
                        "cardScheme", "card_scheme", "scheme", "brand",
                        "paymentDescription", "payment_description", "description",
                        "cardLast4Digits", "card_last_4_digits", "last4", "last_4"
                    )
                    
                    for (fieldName in paymentInfoFields) {
                        try {
                            val field = paymentInfoClass.getDeclaredField(fieldName)
                            field.isAccessible = true
                            val value = field.get(paymentInfo)
                            if (value != null) {
                                Log.e("PayTabsResult", "│ │ $fieldName: $value")
                            }
                        } catch (e: NoSuchFieldException) {
                            try {
                                val capitalized = fieldName.replaceFirstChar { it.uppercaseChar() }
                                val methodName = "get$capitalized"
                                val method = paymentInfoClass.getMethod(methodName)
                                val value = method.invoke(paymentInfo)
                                if (value != null) {
                                    Log.e("PayTabsResult", "│ │ $fieldName: $value")
                                }
                            } catch (e2: Exception) {
                                // Skip
                            }
                        } catch (e: Exception) {
                            // Skip
                        }
                    }
                    Log.e("PayTabsResult", "│ └─────────────────────────────────────────────────────────")
                }
            } catch (e: Exception) {
                Log.e("PayTabsResult", "│ paymentInfo extraction error: ${e.message}")
            }
            
            // Log customerDetails if available
            try {
                val customerDetailsMethod = detailsClass.getMethod("getCustomerDetails")
                val customerDetails = customerDetailsMethod.invoke(details)
                if (customerDetails != null) {
                    Log.e("PayTabsResult", "│")
                    Log.e("PayTabsResult", "│ ┌─ CUSTOMER DETAILS ────────────────────────────────────────")
                    val customerDetailsClass = customerDetails.javaClass
                    val customerFields = listOf(
                        "name", "email", "phone",
                        "street1", "street_1", "address",
                        "city", "state", "country", "ip"
                    )
                    
                    for (fieldName in customerFields) {
                        try {
                            val field = customerDetailsClass.getDeclaredField(fieldName)
                            field.isAccessible = true
                            val value = field.get(customerDetails)
                            if (value != null) {
                                Log.e("PayTabsResult", "│ │ $fieldName: $value")
                            }
                        } catch (e: NoSuchFieldException) {
                            try {
                                val capitalized = fieldName.replaceFirstChar { it.uppercaseChar() }
                                val methodName = "get$capitalized"
                                val method = customerDetailsClass.getMethod(methodName)
                                val value = method.invoke(customerDetails)
                                if (value != null) {
                                    Log.e("PayTabsResult", "│ │ $fieldName: $value")
                                }
                            } catch (e2: Exception) {
                                // Skip
                            }
                        } catch (e: Exception) {
                            // Skip
                        }
                    }
                    Log.e("PayTabsResult", "│ └─────────────────────────────────────────────────────────")
                }
            } catch (e: Exception) {
                // customerDetails might not be available
            }
            
            // Log token if available
            try {
                val tokenMethod = detailsClass.getMethod("getToken")
                val token = tokenMethod.invoke(details) as? String
                if (!token.isNullOrEmpty()) {
                    Log.e("PayTabsResult", "│")
                    Log.e("PayTabsResult", "│ TOKEN (tokenization): ${token.take(30)}... (length: ${token.length})")
                }
            } catch (e: Exception) {
                // Token might not be available
            }
            
            Log.e("PayTabsResult", "└────────────────────────────────────────────────────────────")
            
            // Log full object toString for debugging
            Log.d("PayTabsResult", "Full details object: $details")
            Log.d("PayTabsResult", "Details class: ${detailsClass.name}")
            
        } catch (e: Exception) {
            Log.e("PayTabsResult", "Error extracting response details: ${e.message}")
            e.printStackTrace()
        }
        
        Log.e("PayTabsResult", "═══════════════════════════════════════════════════════════")
        // ========== END COMPREHENSIVE LOGGING ==========
        
        // Extract and save payment method after successful payment
        // Try to save even if tokenization wasn't explicitly enabled (PayTabs may return token by default)
        if (isSuccess && selectedSavedPaymentMethod == null) {
            try {
                Log.d("PayTabsResult", "Attempting to extract payment method details...")
                Log.d("PayTabsResult", "Details class: ${details.javaClass.name}")
                Log.d("PayTabsResult", "Details toString: $details")
                
                // List all available methods and fields for debugging
                val methods = details.javaClass.declaredMethods.map { it.name }
                val fields = details.javaClass.declaredFields.map { it.name }
                Log.d("PayTabsResult", "Available methods: ${methods.joinToString(", ")}")
                Log.d("PayTabsResult", "Available fields: ${fields.joinToString(", ")}")
                
                // Extract token - try multiple approaches
                var token: String? = null
                try {
                    // Method 1: Try getToken() method (confirmed exists in available methods)
                    val getTokenMethod = details.javaClass.getMethod("getToken")
                    token = getTokenMethod.invoke(details) as? String
                    Log.e("PayTabsResult", "Token from getToken(): ${token?.take(20)}... (length: ${token?.length})")
                } catch (e: Exception) {
                    Log.e("PayTabsResult", "getToken() method failed: ${e.message}")
                    e.printStackTrace()
                }
                
                if (token.isNullOrEmpty()) {
                    try {
                        // Method 2: Try token field
                        val tokenField = details.javaClass.getDeclaredField("token")
                        tokenField.isAccessible = true
                        token = tokenField.get(details) as? String
                        Log.d("PayTabsResult", "Token from token field: $token")
                    } catch (e: Exception) {
                        Log.d("PayTabsResult", "token field not found: ${e.message}")
                    }
                }
                
                // Extract card last 4 digits - might be in PaymentInfo object
                var cardLast4: String? = null
                var paymentDescription: String? = null
                try {
                    // Try getPaymentInfo() first (might contain card details)
                    val paymentInfo = details.javaClass.getMethod("getPaymentInfo")?.invoke(details)
                    if (paymentInfo != null) {
                        Log.d("PayTabsResult", "PaymentInfo object: $paymentInfo")
                        val paymentInfoClass = paymentInfo.javaClass
                        val paymentInfoMethods = paymentInfoClass.declaredMethods.map { it.name }
                        val paymentInfoFields = paymentInfoClass.declaredFields.map { it.name }
                        Log.d("PayTabsResult", "PaymentInfo methods: ${paymentInfoMethods.joinToString(", ")}")
                        Log.d("PayTabsResult", "PaymentInfo fields: ${paymentInfoFields.joinToString(", ")}")
                        
                        // Try to get cardLast4Digits from PaymentInfo
                        try {
                            val cardMethod = paymentInfoClass.getMethod("getCardLast4Digits")
                            cardLast4 = cardMethod.invoke(paymentInfo) as? String
                            Log.e("PayTabsResult", "CardLast4 from PaymentInfo.getCardLast4Digits(): $cardLast4")
                        } catch (e: Exception) {
                            Log.d("PayTabsResult", "PaymentInfo.getCardLast4Digits() not found: ${e.message}")
                        }
                        
                        // Extract last 4 digits from paymentDescription (format: "4000 00## #### 0002")
                        try {
                            val paymentDescMethod = paymentInfoClass.getMethod("getPaymentDescription")
                            paymentDescription = paymentDescMethod.invoke(paymentInfo) as? String
                            Log.d("PayTabsResult", "PaymentDescription from PaymentInfo: $paymentDescription")
                            
                            if (!paymentDescription.isNullOrEmpty()) {
                                // Extract last 4 digits from paymentDescription
                                // Format examples: "4000 00## #### 0002", "5200 00## #### 0007"
                                // The last 4 digits are after the last space or at the end
                                val digitsOnly = paymentDescription.replace(Regex("[^0-9]"), "")
                                if (digitsOnly.length >= 4) {
                                    cardLast4 = digitsOnly.takeLast(4)
                                    Log.e("PayTabsResult", "✅ Extracted cardLast4 from paymentDescription: $cardLast4 (from: $paymentDescription)")
                                } else {
                                    // Try to find last 4 digits pattern: "#### 0002" or "0002"
                                    val last4Pattern = Regex("(\\d{4})(?:\\s*$|\\s*##)")
                                    val match = last4Pattern.find(paymentDescription)
                                    if (match != null) {
                                        cardLast4 = match.groupValues[1]
                                        Log.e("PayTabsResult", "✅ Extracted cardLast4 using pattern: $cardLast4")
                                    } else {
                                        // Fallback: extract last 4 digits from end of string
                                        val trimmed = paymentDescription.trim()
                                        if (trimmed.length >= 4) {
                                            val lastPart = trimmed.takeLast(10) // Take last 10 chars to find digits
                                            val digits = lastPart.filter { it.isDigit() }
                                            if (digits.length >= 4) {
                                                cardLast4 = digits.takeLast(4)
                                                Log.e("PayTabsResult", "✅ Extracted cardLast4 from end: $cardLast4")
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("PayTabsResult", "PaymentInfo.getPaymentDescription() not found: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("PayTabsResult", "getPaymentInfo() failed: ${e.message}")
                }
                
                // Fallback: Try direct methods on details
                if (cardLast4.isNullOrEmpty()) {
                    try {
                        cardLast4 = details.javaClass.getMethod("getCardLast4Digits")?.invoke(details) as? String
                        Log.d("PayTabsResult", "CardLast4 from getCardLast4Digits(): $cardLast4")
                    } catch (e: Exception) {
                        Log.d("PayTabsResult", "getCardLast4Digits() method not found: ${e.message}")
                    }
                }
                
                if (cardLast4.isNullOrEmpty()) {
                    try {
                        val cardField = details.javaClass.getDeclaredField("cardLast4Digits")
                        cardField.isAccessible = true
                        cardLast4 = cardField.get(details) as? String
                        Log.d("PayTabsResult", "CardLast4 from cardLast4Digits field: $cardLast4")
                    } catch (e: Exception) {
                        Log.d("PayTabsResult", "cardLast4Digits field not found: ${e.message}")
                    }
                }
                
                // Extract card brand from PaymentInfo
                var cardBrand: String? = null
                try {
                    val paymentInfo = details.javaClass.getMethod("getPaymentInfo")?.invoke(details)
                    if (paymentInfo != null) {
                        val paymentInfoClass = paymentInfo.javaClass
                        // Try getCardScheme() first (this is what PayTabs uses: "Visa", "MasterCard")
                        try {
                            val cardSchemeMethod = paymentInfoClass.getMethod("getCardScheme")
                            cardBrand = cardSchemeMethod.invoke(paymentInfo) as? String
                            Log.d("PayTabsResult", "CardBrand from PaymentInfo.getCardScheme(): $cardBrand")
                        } catch (e: Exception) {
                            Log.d("PayTabsResult", "PaymentInfo.getCardScheme() not found: ${e.message}")
                        }
                        
                        // Fallback: try getPayment_method()
                        if (cardBrand.isNullOrEmpty()) {
                            try {
                                val paymentMethodMethod = paymentInfoClass.getMethod("getPayment_method")
                                cardBrand = paymentMethodMethod.invoke(paymentInfo) as? String
                                Log.d("PayTabsResult", "CardBrand from PaymentInfo.getPayment_method(): $cardBrand")
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("PayTabsResult", "Error extracting cardBrand from PaymentInfo: ${e.message}")
                }
                
                // Fallback: Try direct methods on details
                if (cardBrand.isNullOrEmpty()) {
                    try {
                        cardBrand = details.javaClass.getMethod("getCardBrand")?.invoke(details) as? String
                        Log.d("PayTabsResult", "CardBrand from getCardBrand(): $cardBrand")
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                
                if (cardBrand.isNullOrEmpty()) {
                    try {
                        val brandField = details.javaClass.getDeclaredField("cardBrand")
                        brandField.isAccessible = true
                        cardBrand = brandField.get(details) as? String
                        Log.d("PayTabsResult", "CardBrand from cardBrand field: $cardBrand")
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                
                // Save payment method if token is available
                if (!token.isNullOrEmpty() && !cardLast4.isNullOrEmpty()) {
                    // Save locally first
                    val savedMethod = SavedPaymentMethod(
                        token = token,
                        cardLast4Digits = cardLast4,
                        cardBrand = cardBrand
                    )
                    paymentMethodManager.savePaymentMethod(savedMethod)
                    Log.e("PayTabsResult", "✅ SUCCESS: Saved payment method locally: ****${cardLast4}, token length: ${token.length}")
                    
                    // Save to backend database if customer is logged in
                    val customerId = sessionManager.getCustomerId()
                    val authToken = sessionManager.getAuthToken()
                    if (customerId != -1 && authToken != null) {
                        lifecycleScope.launch {
                            try {
                                // Extract expiry from PaymentInfo if available
                                var expiryMonth: Int? = null
                                var expiryYear: Int? = null
                                try {
                                    val paymentInfo = details.javaClass.getMethod("getPaymentInfo")?.invoke(details)
                                    if (paymentInfo != null) {
                                        val paymentInfoClass = paymentInfo.javaClass
                                        try {
                                            val monthMethod = paymentInfoClass.getMethod("getExpiryMonth")
                                            expiryMonth = monthMethod.invoke(paymentInfo) as? Int
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                        try {
                                            val yearMethod = paymentInfoClass.getMethod("getExpiryYear")
                                            expiryYear = yearMethod.invoke(paymentInfo) as? Int
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.d("PayTabsResult", "Could not extract expiry: ${e.message}")
                                }
                                
                                // Convert PayTabs response to JSON string
                                val paytabsResponseJson = gson.toJson(details.toString())
                                
                                val request = com.mnsf.resturantandroid.network.SavePaymentMethodRequest(
                                    token = token,
                                    card_last4 = cardLast4,
                                    card_brand = cardBrand,
                                    expiry_month = expiryMonth,
                                    expiry_year = expiryYear,
                                    paytabs_response_json = paytabsResponseJson,
                                    is_default = false // First card is not default, user can set later
                                )
                                
                                val response = RetrofitClient.apiService.savePaymentMethod(
                                    customerId = customerId,
                                    request = request,
                                    token = "Bearer $authToken"
                                )
                                
                                if (response.isSuccessful) {
                                    Log.e("PayTabsResult", "✅ SUCCESS: Saved payment method to database: ****${cardLast4}")
                                    // Reload payment methods to update UI and select the newly added card
                                    runOnUiThread {
                                        // Store the token to select it after reload
                                        val newCardToken = token
                                        loadSavedPaymentMethods()
                                        // Select the newly added card after a short delay to ensure UI is updated
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            selectedSavedPaymentMethod = savedMethod
                                            // Find and check the radio button for this card
                                            for (i in 0 until binding.rgPaymentMethod.childCount) {
                                                val child = binding.rgPaymentMethod.getChildAt(i)
                                                if (child is com.google.android.material.radiobutton.MaterialRadioButton) {
                                                    val pm = child.tag as? com.mnsf.resturantandroid.network.PaymentMethod
                                                    if (pm?.token == newCardToken) {
                                                        child.isChecked = true
                                                        break
                                                    }
                                                }
                                            }
                                            updatePlaceOrderButtonText()
                                        }, 300)
                                    }
                                } else {
                                    Log.e("PayTabsResult", "❌ Failed to save payment method to database: ${response.code()} ${response.message()}")
                                }
                            } catch (e: Exception) {
                                Log.e("PayTabsResult", "❌ Error saving payment method to database: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    } else {
                        Log.d("PayTabsResult", "Customer not logged in, skipping database save")
                    }
                    
                    // Verify it was saved locally
                    paymentMethodManager.debugPrintSavedMethods()
                } else {
                    // If token is missing, try to use transactionReference as fallback token
                    if (!transactionRef.isNullOrEmpty() && !cardLast4.isNullOrEmpty()) {
                        Log.w("PayTabsResult", "⚠️ Token not available, but transactionReference found. Using as fallback.")
                        val savedMethod = SavedPaymentMethod(
                            token = transactionRef, // Use transaction reference as token
                            cardLast4Digits = cardLast4,
                            cardBrand = cardBrand
                        )
                        paymentMethodManager.savePaymentMethod(savedMethod)
                        paymentMethodManager.debugPrintSavedMethods()
                        Log.e("PayTabsResult", "✅ Saved payment method using transactionReference: ****${cardLast4}")
                    } else {
                        Log.e("PayTabsResult", "❌ FAILED: Token/transactionRef and cardLast4 not available")
                        Log.e("PayTabsResult", "  - Token: ${token?.take(10)}... (length: ${token?.length})")
                        Log.e("PayTabsResult", "  - TransactionRef: ${transactionRef.take(10)}... (length: ${transactionRef.length})")
                        Log.e("PayTabsResult", "  - CardLast4: $cardLast4")
                        Log.e("PayTabsResult", "Possible causes:")
                        Log.e("PayTabsResult", "  1. Tokenization not enabled in PayTabs merchant account")
                        Log.e("PayTabsResult", "  2. Card details not returned in this SDK version")
                        Log.e("PayTabsResult", "  3. Need to check PaymentInfo object for card details")
                    }
                }
            } catch (e: Exception) {
                Log.e("PayTabsResult", "❌ ERROR extracting/saving payment method", e)
                e.printStackTrace()
            }
        } else {
            if (!isSuccess) {
                Log.d("PayTabsResult", "Payment not successful, skipping token save")
            }
            if (selectedSavedPaymentMethod != null) {
                Log.d("PayTabsResult", "Using saved payment method, skipping token save")
            }
        }
        
        if (isSuccess) {
            // Process the order payment
            // Note: When "+ Add new card" is selected and "Place Order" is clicked,
            // PayTabs is launched with the full order amount, so the card is added AND payment is processed
            val customerName = pendingOrderCustomerName ?: "Guest"
            val customerEmail = pendingOrderCustomerEmail
            val customerPhone = pendingOrderCustomerPhone ?: "N/A"
            val customerAddress = pendingOrderCustomerAddress
            val orderType = pendingOrderType ?: "pickup"
            val notes = pendingOrderNotes
            val tip = pendingOrderTip
            val deliveryInstructions = pendingOrderDeliveryInstructions
            val cartItems = pendingOrderCartItems ?: return
            val lat = pendingOrderDeliveryLat
            val lng = pendingOrderDeliveryLng
            pendingOrderCustomerName = null
            pendingOrderCustomerEmail = null
            pendingOrderCustomerPhone = null
            pendingOrderCustomerAddress = null
            pendingOrderType = null
            pendingOrderNotes = null
            pendingOrderCartItems = null
            pendingOrderDeliveryLat = null
            pendingOrderDeliveryLng = null
            createOrderWithLocation(
                customerName, customerEmail, customerPhone, customerAddress,
                orderType, "card", notes, lat, lng, cartItems, tip, deliveryInstructions,
                paymentIntentId = transactionRef.ifEmpty { null }
            )
        } else {
            val msg = responseMessage ?: getString(R.string.payment_failed)
            Log.e("PayTabsResult", "onPaymentFinish FAILED: responseCode=$responseCode responseMessage=$msg (check PayTabs portal for transaction or decline reason)")
            resetPlaceOrderButton()
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        placeOrderCountdown?.cancel()
        placeOrderCountdown = null
        super.onDestroy()
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
    
    private fun setupCustomerAddress() {
        val address = sessionManager.getCustomerAddress()
        val phone = sessionManager.getCustomerPhone()
        if (!address.isNullOrEmpty() && sessionManager.getOrderType() == "delivery") {
            binding.cardDeliveryAddress.visibility = android.view.View.VISIBLE
            binding.tvAddressLabel.text = getString(R.string.delivery_address)
            binding.tvAddressStreet.text = address
            binding.tvAddressPhone.text = if (!phone.isNullOrEmpty()) "${getString(R.string.mobile_number)}: $phone" else ""
        } else {
            binding.cardDeliveryAddress.visibility = android.view.View.GONE
        }
    }
    
    /**
     * Load customer addresses to get zone_price for delivery fee calculation.
     * This is called when restaurant has an approved delivery company.
     */
    private fun loadCustomerAddressesForDeliveryFee() {
        lifecycleScope.launch {
            try {
                val customerId = sessionManager.getCustomerId()
                val authToken = sessionManager.getAuthToken()
                
                if (customerId == -1 || authToken.isNullOrBlank()) {
                    Log.d("CheckoutActivity", "loadCustomerAddressesForDeliveryFee: Not logged in")
                    return@launch
                }
                
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getAddresses(customerId, "Bearer $authToken")
                }
                
                if (response.isSuccessful) {
                    val addresses = response.body()?.addresses ?: emptyList()
                    // Get default address or first address
                    selectedAddress = addresses.firstOrNull { it.is_default } ?: addresses.firstOrNull()
                    
                    selectedAddress?.let { addr ->
                        Log.d("CheckoutActivity", "loadCustomerAddressesForDeliveryFee: Selected address zone_price=${addr.zone_price}")
                        // Update order summary with new delivery fee
                        runOnUiThread {
                            updateOrderSummary()
                        }
                    } ?: run {
                        Log.d("CheckoutActivity", "loadCustomerAddressesForDeliveryFee: No addresses found")
                    }
                } else {
                    Log.w("CheckoutActivity", "loadCustomerAddressesForDeliveryFee: Failed ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "loadCustomerAddressesForDeliveryFee: Error", e)
            }
        }
    }
    
    private fun checkAndUpdateCustomerLocation() {
        lifecycleScope.launch {
            try {
                val customerId = sessionManager.getCustomerId()
                val authToken = sessionManager.getAuthToken()
                
                if (customerId == -1 || authToken == null) {
                    Log.d("CheckoutActivity", "checkAndUpdateCustomerLocation: Not logged in")
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
                        Log.d("CheckoutActivity", "checkAndUpdateCustomerLocation: Customer missing address, requesting location")
                        
                        // Check if we have permission
                        if (LocationHelper.hasLocationPermission(this@CheckoutActivity)) {
                            // Get location and update
                            getLocationAndUpdateProfile(customerId, authToken)
                        } else {
                            // Request permission
                            LocationHelper.requestLocationPermissions(
                                this@CheckoutActivity,
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
                            setupCustomerAddress() // Refresh UI
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "checkAndUpdateCustomerLocation: Error", e)
            }
        }
    }
    
    private fun getLocationAndUpdateProfile(customerId: Int, authToken: String) {
        lifecycleScope.launch {
            try {
                LocationHelper.getCurrentLocation(this@CheckoutActivity)?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val address = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        LocationHelper.getAddressFromLocation(
                            this@CheckoutActivity,
                            latitude,
                            longitude
                        )
                    }
                    
                    Log.d("CheckoutActivity", "getLocationAndUpdateProfile: Got location lat=$latitude, lng=$longitude, address=$address")
                    
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
                        
                        // Refresh UI
                        runOnUiThread {
                            setupCustomerAddress()
                        }
                        
                        Log.d("CheckoutActivity", "getLocationAndUpdateProfile: Profile updated successfully")
                    }
                } ?: run {
                    Log.w("CheckoutActivity", "getLocationAndUpdateProfile: Could not get location")
                }
            } catch (e: Exception) {
                Log.e("CheckoutActivity", "getLocationAndUpdateProfile: Error", e)
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
                // Permission granted, get location and update profile
                val customerId = sessionManager.getCustomerId()
                val authToken = sessionManager.getAuthToken()
                
                if (customerId != -1 && authToken != null) {
                    getLocationAndUpdateProfile(customerId, authToken)
                }
            } else {
                Log.d("CheckoutActivity", "onRequestPermissionsResult: Location permission denied")
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

