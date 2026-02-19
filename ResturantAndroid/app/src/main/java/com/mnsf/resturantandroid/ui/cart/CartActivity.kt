package com.mnsf.resturantandroid.ui.cart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.CartItem
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.databinding.ActivityCartBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.ui.checkout.CheckoutActivity
import com.mnsf.resturantandroid.ui.checkout.ConfirmLocationActivity
import com.mnsf.resturantandroid.ui.restaurant.MealDetailsActivity
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.utils.UrlHelper
import com.mnsf.resturantandroid.viewmodel.CartViewModel
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCartBinding
    private lateinit var cartViewModel: CartViewModel
    private lateinit var cartAdapter: CartAdapter
    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var sessionManager: SessionManager
    private var currencyCode: String? = null
    private var currencySymbolPosition: String? = null
    private var taxEnabled: Boolean = false
    private var taxRate: Double = 0.0
    private var currentRestaurantName: String = ""
    private var offers: List<Offer> = emptyList()
    private var lastLoggedSubtotal: Double? = null
    private var lastLoggedTotal: Double? = null

    /** Calculate subtotal for display. Uses stored overrides when present (from offer at add-to-cart). Returns (subtotal, originalForStrikethrough?). */
    private fun getSubtotalWithOffers(cartItem: CartItem): Pair<Double, Double?> {
        val quantity = cartItem.quantity
        if (cartItem.unitPriceOverride != null || cartItem.addonPriceOverrides != null) {
            val discountedSubtotal = cartItem.getSubtotal()
            val originalSubtotal = (cartItem.product.price * quantity) + cartItem.selectedAddons.sumOf { it.price } * quantity
            return if (originalSubtotal > discountedSubtotal) Pair(discountedSubtotal, originalSubtotal) else Pair(discountedSubtotal, null)
        }
        val product = cartItem.product
        val productPercent = offers.filter { offer ->
            (offer.offer_type?.trim()?.lowercase() == "percent_off") && (offer.value ?: 0.0) > 0
        }.filter { offer ->
            when (offer.offer_scope?.trim()?.lowercase()) {
                "selected_items" -> product.id in offer.getSelectedProductIds()
                else -> true
            }
        }.mapNotNull { it.value }.maxOrNull()
        val productPrice = if (productPercent != null && productPercent > 0.0) product.price * (1.0 - productPercent / 100.0) else product.price
        val addonsTotal = cartItem.selectedAddons.sumOf { addon ->
            val addonPercent = offers.filter { offer ->
                (offer.offer_type?.trim()?.lowercase() == "percent_off") && (offer.value ?: 0.0) > 0
            }.filter { offer ->
                when (offer.offer_scope?.trim()?.lowercase()) {
                    "selected_items" -> addon.id in offer.getSelectedAddonIds()
                    else -> true
                }
            }.mapNotNull { it.value }.maxOrNull()
            if (addonPercent != null && addonPercent > 0.0) addon.price * (1.0 - addonPercent / 100.0) else addon.price
        }
        val discountedSubtotal = (productPrice + addonsTotal) * quantity
        val originalSubtotal = (product.price + cartItem.selectedAddons.sumOf { it.price }) * quantity
        return if (discountedSubtotal < originalSubtotal) Pair(discountedSubtotal, originalSubtotal) else Pair(originalSubtotal, null)
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnBack.setOnClickListener { finish() }
        sessionManager = SessionManager(this)
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]
        cartViewModel.refreshCart() // Refresh to get latest cart items
        
        // Initialize restaurant view model to get currency info
        val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        restaurantViewModel = ViewModelProvider(
            this,
            com.mnsf.resturantandroid.ui.restaurant.RestaurantViewModelFactory(restaurantRepository)
        )[RestaurantViewModel::class.java]
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadRestaurantCurrency()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh cart when returning to this activity
        cartViewModel.refreshCart()
        // Reload currency info in case restaurant changed
        loadRestaurantCurrency()
        loadOffers()
    }
    
    private fun loadOffers() {
        val restaurantId = cartViewModel.cartItems.value?.firstOrNull()?.product?.website_id
        if (restaurantId != null) {
            restaurantViewModel.loadRestaurantOffers(restaurantId)
        }
    }
    
    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChange = { productId, addonIds, quantity ->
                cartViewModel.updateQuantity(productId, addonIds, quantity)
            },
            onRemove = { productId, addonIds ->
                cartViewModel.removeFromCart(productId, addonIds)
            },
            onEditClick = { product ->
                startActivity(
                    MealDetailsActivity.newIntent(
                        this,
                        product,
                        currencyCode,
                        currencySymbolPosition,
                        currentRestaurantName
                    )
                )
            },
            currencyCode = currencyCode,
            currencySymbolPosition = currencySymbolPosition,
            offers = offers
        )
        binding.recyclerViewCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCart.adapter = cartAdapter
    }
    
    private fun loadRestaurantCurrency() {
        // Get restaurant ID from first cart item
        val restaurantId = cartViewModel.cartItems.value?.firstOrNull()?.product?.website_id
        if (restaurantId != null) {
            restaurantViewModel.loadRestaurant(restaurantId)
            restaurantViewModel.selectedRestaurant.observe(this) { restaurant ->
                restaurant?.let {
                    currencyCode = it.currency_code
                    currencySymbolPosition = it.currency_symbol_position
                    taxEnabled = it.tax_enabled == true
                    taxRate = it.tax_rate ?: 0.0
                    // Update adapter with currency info
                    updateCartAdapterCurrency()
                    // Update totals
                    updateTotals()
                    // Update restaurant header
                    updateRestaurantHeader(it)
                }
            }
            // Load offers for this restaurant
            restaurantViewModel.restaurantOffers.observe(this) { offersList ->
                offers = offersList ?: emptyList()
                cartAdapter.setOffers(offers)
                updateTotals() // Recalculate totals with offers
            }
        } else {
            binding.tvRestaurantName.visibility = android.view.View.GONE
            binding.restaurantHeader.visibility = android.view.View.GONE
        }
    }
    
    private fun updateRestaurantHeader(restaurant: com.mnsf.resturantandroid.data.model.Restaurant) {
        currentRestaurantName = restaurant.restaurant_name
        binding.tvRestaurantName.visibility = android.view.View.VISIBLE
        binding.tvRestaurantName.text = restaurant.restaurant_name
        binding.restaurantHeader.visibility = android.view.View.GONE
    }
    
    private fun updateCartAdapterCurrency() {
        cartAdapter = CartAdapter(
            onQuantityChange = { productId, addonIds, quantity ->
                cartViewModel.updateQuantity(productId, addonIds, quantity)
            },
            onRemove = { productId, addonIds ->
                cartViewModel.removeFromCart(productId, addonIds)
            },
            onEditClick = { product ->
                startActivity(
                    MealDetailsActivity.newIntent(
                        this,
                        product,
                        currencyCode,
                        currencySymbolPosition,
                        currentRestaurantName
                    )
                )
            },
            currencyCode = currencyCode,
            currencySymbolPosition = currencySymbolPosition,
            offers = offers
        )
        binding.recyclerViewCart.adapter = cartAdapter
        cartViewModel.cartItems.value?.let { items ->
            cartAdapter.submitList(items.toList())
        }
    }
    
    private fun setupObservers() {
        cartViewModel.cartItems.observe(this) { items ->
            cartAdapter.submitList(items.toList())
            
            if (items.isEmpty()) {
                binding.recyclerViewCart.visibility = android.view.View.GONE
                binding.textEmptyCart.visibility = android.view.View.VISIBLE
                binding.cartSummary.visibility = android.view.View.GONE
                binding.btnCheckout.visibility = android.view.View.GONE
                binding.btnCheckout.isEnabled = false
                binding.stampBanner.visibility = android.view.View.GONE
            } else {
                binding.recyclerViewCart.visibility = android.view.View.VISIBLE
                binding.textEmptyCart.visibility = android.view.View.GONE
                binding.cartSummary.visibility = android.view.View.VISIBLE
                binding.btnCheckout.visibility = android.view.View.VISIBLE
                binding.btnCheckout.isEnabled = true
                binding.stampBanner.visibility = android.view.View.VISIBLE
                loadRestaurantCurrency()
            }
            updateTotals()
        }
    }
    
    private fun updateTotals() {
        val subtotal = cartViewModel.getTotalPrice()
        val taxAmount = if (taxEnabled && taxRate > 0) {
            subtotal * (taxRate / 100.0)
        } else {
            0.0
        }
        val total = subtotal + taxAmount
        // Compare rounded to 2 decimals so floating-point noise doesn't trigger repeated logs
        val subRounded = kotlin.math.round(subtotal * 100.0) / 100.0
        val totalRounded = kotlin.math.round(total * 100.0) / 100.0
        if (lastLoggedSubtotal != subRounded || lastLoggedTotal != totalRounded) {
            lastLoggedSubtotal = subRounded
            lastLoggedTotal = totalRounded
            val restaurantId = cartViewModel.getCurrentRestaurantId()
            if (restaurantId != null) {
                val items = cartViewModel.getCartItemsForRestaurant(restaurantId)
                items.forEachIndexed { i, item ->
                    val lineSub = item.getSubtotal()
                    Log.d("MoneyLog", "[Cart] item[$i] product id=${item.product.id} name=${item.product.name} product.price=${item.product.price} unitPriceOverride=${item.unitPriceOverride} addonPriceOverrides=${item.addonPriceOverrides} quantity=${item.quantity} lineSubtotal=$lineSub")
                }
            }
            Log.d("MoneyLog", "[Cart] updateTotals subtotal=$subRounded taxEnabled=$taxEnabled taxRate=$taxRate taxAmount=$taxAmount total=$totalRounded")
        }

        // Format and display subtotal
        val formattedSubtotal = CurrencyFormatter.formatPrice(
            subtotal,
            currencyCode,
            currencySymbolPosition
        )
        binding.tvSubtotal.text = formattedSubtotal
        
        // Show/hide tax line item
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
    
    private fun setupClickListeners() {
        binding.btnAddItems.setOnClickListener { finish() }
        binding.btnCheckout.setOnClickListener {
            if (cartViewModel.getItemCount() <= 0) return@setOnClickListener
            val restaurantId = cartViewModel.getCurrentRestaurantId()
            if (restaurantId == null) {
                Toast.makeText(this, getString(R.string.error_no_restaurant), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // If customer has saved addresses → go to CheckoutActivity (Place Order). Otherwise → ConfirmLocationActivity.
            navigateCheckoutByAddress(restaurantId)
        }
        binding.btnStampInfo.setOnClickListener {
            Toast.makeText(this, getString(R.string.place_order_earn_stamp), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * If the customer has at least one address in the Address table → go to CheckoutActivity (Place Order).
     * Otherwise (no addresses or not logged in) → go to ConfirmLocationActivity to set/confirm address.
     */
    private fun navigateCheckoutByAddress(restaurantId: Int) {
        val customerId = sessionManager.getCustomerId()
        val token = sessionManager.getAuthToken()
        if (customerId == -1 || token.isNullOrBlank()) {
            startActivity(ConfirmLocationActivity.newIntent(this, restaurantId))
            return
        }
        binding.btnCheckout.isEnabled = false
        lifecycleScope.launch {
            val hasAddresses = withContext(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.apiService.getAddresses(customerId, "Bearer $token")
                    response.isSuccessful && (response.body()?.addresses?.isNotEmpty() == true)
                } catch (_: Exception) {
                    false
                }
            }
            binding.btnCheckout.isEnabled = true
            if (hasAddresses) {
                startActivity(Intent(this@CartActivity, CheckoutActivity::class.java).apply {
                    putExtra("restaurant_id", restaurantId)
                })
            } else {
                startActivity(ConfirmLocationActivity.newIntent(this@CartActivity, restaurantId))
            }
        }
    }
}

