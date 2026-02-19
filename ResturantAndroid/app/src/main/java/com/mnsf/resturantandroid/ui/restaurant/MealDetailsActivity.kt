package com.mnsf.resturantandroid.ui.restaurant

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.data.model.ProductAddon
import com.mnsf.resturantandroid.databinding.ActivityMealDetailsBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.I18nHelper
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.viewmodel.CartViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MealDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealDetailsBinding
    private lateinit var cartViewModel: CartViewModel
    private var product: Product? = null
    private var currencyCode: String? = null
    private var currencySymbolPosition: String? = null
    private var restaurantName: String = ""
    private var quantity: Int = 1
    private var requiredAddons: List<ProductAddon> = emptyList()
    private var optionalAddons: List<ProductAddon> = emptyList()
    private var addonRequired: Boolean = false
    private var addonRequiredMin: Int? = null
    private var mealNote: String = ""
    private lateinit var requiredAddonAdapter: AddonAdapter
    private lateinit var optionalAddonAdapter: OptionalAddonAdapter
    private var offers: List<Offer> = emptyList()
    /** Product base price to use in total (discounted when an offer applies). */
    private var productDisplayPrice: Double = 0.0

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.white))

        product = intent.getParcelableExtra(EXTRA_PRODUCT)
        currencyCode = intent.getStringExtra(EXTRA_CURRENCY_CODE)
        currencySymbolPosition = intent.getStringExtra(EXTRA_CURRENCY_SYMBOL_POSITION)
        restaurantName = intent.getStringExtra(EXTRA_RESTAURANT_NAME) ?: ""

        if (product == null) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]

        setupToolbar()
        bindProduct(product!!)
        productDisplayPrice = product!!.price
        setupQuantityStepper()
        setupAddonAdapters()
        setupNotesAndCompleteHint()
        loadOffers()
        loadAddons()
        setupAddToCart()
    }

    private fun loadOffers() {
        val p = product ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                RestaurantRepository(RetrofitClient.apiService).getOffersByWebsiteId(p.website_id)
            }
            result.onSuccess { list ->
                offers = list
                applyDiscountAndUpdatePrice()
            }
        }
    }

    private fun getBestPercentOff(productId: Int): Double? {
        val percentOffers = offers.filter { offer ->
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

    private fun getDisplayPrices(product: Product): Pair<Double, Double?> {
        val percent = getBestPercentOff(product.id)
        if (percent == null || percent <= 0.0) return Pair(product.price, null)
        val discounted = kotlin.math.round(product.price * (1.0 - percent / 100.0) * 100.0) / 100.0
        return Pair(discounted, product.price)
    }

    /** Best percent discount for this addon (0.0–100.0) or null if none. */
    private fun getBestPercentOffForAddon(addonId: Int): Double? {
        val percentOffers = offers.filter { offer ->
            (offer.offer_type?.trim()?.lowercase() == "percent_off") && (offer.value ?: 0.0) > 0
        }
        val applicable = percentOffers.filter { offer ->
            when (offer.offer_scope?.trim()?.lowercase()) {
                "selected_items" -> addonId in offer.getSelectedAddonIds()
                else -> true
            }
        }
        return applicable.mapNotNull { it.value }.maxOrNull()
    }

    /** (displayPrice, originalPriceForStrikethrough?) for an addon. */
    private fun getAddonDisplayPrice(addon: ProductAddon): Pair<Double, Double?> {
        val percent = getBestPercentOffForAddon(addon.id)
        if (percent == null || percent <= 0.0) {
            Log.d("MoneyLog", "[MealDetails] addon id=${addon.id} name=${addon.name} price=${addon.price} (no discount)")
            return Pair(addon.price, null)
        }
        val discounted = kotlin.math.round(addon.price * (1.0 - percent / 100.0) * 100.0) / 100.0
        Log.d("MoneyLog", "[MealDetails] addon id=${addon.id} name=${addon.name} price=${addon.price} percentOff=$percent displayPrice=$discounted")
        return Pair(discounted, addon.price)
    }

    private fun applyDiscountAndUpdatePrice() {
        val p = product ?: return
        val (displayPrice, originalForStrike) = getDisplayPrices(p)
        productDisplayPrice = kotlin.math.round(displayPrice * 100.0) / 100.0
        Log.d("MoneyLog", "[MealDetails] applyDiscount product id=${p.id} name=${p.name} product.price=${p.price} displayPrice=$productDisplayPrice originalForStrike=$originalForStrike")
        binding.tvMealPrice.text = CurrencyFormatter.formatPrice(displayPrice, currencyCode, currencySymbolPosition)
        if (originalForStrike != null && originalForStrike > displayPrice) {
            binding.tvMealOriginalPrice.visibility = View.VISIBLE
            binding.tvMealOriginalPrice.text = CurrencyFormatter.formatPrice(originalForStrike, currencyCode, currencySymbolPosition)
            binding.tvMealOriginalPrice.paintFlags = binding.tvMealOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.tvMealOriginalPrice.visibility = View.GONE
        }
        requiredAddonAdapter.setAddonPriceResolver { addon -> getAddonDisplayPrice(addon) }
        updateTotal()
    }

    private fun setupNotesAndCompleteHint() {
        binding.tvAddNote.setOnClickListener { showAddNoteDialog() }
    }

    private fun showAddNoteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val etNote = dialogView.findViewById<TextInputEditText>(R.id.etNote)
        etNote.setText(mealNote)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_note))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                mealNote = etNote.text?.toString()?.trim() ?: ""
                if (mealNote.isNotEmpty()) {
                    Toast.makeText(this, getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateCompleteSelectionsVisibility() {
        val canAdd = if (addonRequired) {
            val selected = getSelectedAddons().filter { it.is_required }
            val minRequired = addonRequiredMin ?: 1
            if (minRequired == -1) selected.size == requiredAddons.size
            else selected.size >= minRequired
        } else true
        binding.tvCompleteSelections.visibility = if (canAdd) View.GONE else View.VISIBLE
        binding.tvSelectRequiredHint.visibility = if (addonRequired && !canAdd) View.VISIBLE else View.GONE
        val enabled = (product?.is_available == true) && canAdd
        binding.layoutAddItem.isClickable = enabled
        binding.layoutAddItem.isFocusable = enabled
        updateAddToCartButtonAppearance(enabled)
    }

    private fun updateAddToCartButtonAppearance(enabled: Boolean) {
        val white = ContextCompat.getColor(this, R.color.white)
        val onLightPurple = ContextCompat.getColor(this, R.color.on_primary_container)
        binding.layoutAddItem.setBackgroundResource(
            if (enabled) R.drawable.bg_add_item_purple else R.drawable.bg_add_item_light_purple
        )
        binding.tvAddItemLabel.setTextColor(if (enabled) white else onLightPurple)
        binding.tvAddItemPrice.setTextColor(if (enabled) white else onLightPurple)
    }

    private fun setupToolbar() {
        binding.layoutCloseButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun bindProduct(p: Product) {
        binding.tvMealName.text = I18nHelper.getProductNameDisplay(p, this)
        val desc = I18nHelper.getProductDescriptionDisplay(p, this)
        binding.tvMealDescription.text = desc?.ifBlank { null } ?: getString(R.string.no_description_available)
        binding.tvMealDescription.visibility = if (desc.isNullOrBlank()) View.GONE else View.VISIBLE
        // Initial price (no discount until offers load); applyDiscountAndUpdatePrice() will update when offers arrive
        binding.tvMealPrice.text = CurrencyFormatter.formatPrice(p.price, currencyCode, currencySymbolPosition)
        binding.tvMealOriginalPrice.visibility = View.GONE

        p.image_url?.let { url ->
            val emulatorUrl = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(url)
                .replace("localhost", "10.0.2.2")
            Glide.with(this)
                .load(emulatorUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(binding.ivMealImage)
        } ?: binding.ivMealImage.setImageResource(R.drawable.ic_launcher_foreground)

        binding.layoutAddItem.isClickable = p.is_available
        binding.layoutAddItem.isFocusable = p.is_available
        binding.tvAddItemLabel.text = if (p.is_available) getString(R.string.add_item) else getString(R.string.unavailable)
        binding.tvAddItemPrice.text = ""
        updateAddToCartButtonAppearance(p.is_available)
        // Price and label are updated in updateTotal()
    }

    private fun setupQuantityStepper() {
        binding.tvQuantity.text = quantity.toString()
        binding.btnIncrease.setOnClickListener {
            quantity = (quantity + 1).coerceAtMost(99)
            binding.tvQuantity.text = quantity.toString()
            updateTotal()
        }
        binding.btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
                updateTotal()
            }
        }
    }

    private fun setupAddonAdapters() {
        requiredAddonAdapter = AddonAdapter(
            currencyCode,
            currencySymbolPosition,
            onSelectionChanged = {
                updateTotal()
                updateCompleteSelectionsVisibility()
            }
        )
        optionalAddonAdapter = OptionalAddonAdapter(
            priceResolver = { addon -> getAddonDisplayPrice(addon).first },
            onSelectionChanged = {
                updateTotal()
                updateCompleteSelectionsVisibility()
            }
        )
        binding.recyclerRequiredAddons.layoutManager = LinearLayoutManager(this)
        binding.recyclerRequiredAddons.adapter = requiredAddonAdapter
        binding.recyclerOptionalAddons.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerOptionalAddons.adapter = optionalAddonAdapter
    }

    private fun loadAddons() {
        val p = product ?: return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getProductAddons(p.id)
                }
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    addonRequired = data.addon_required
                    addonRequiredMin = data.addon_required_min
                    requiredAddons = data.addons.filter { it.is_required }.sortedBy { it.display_order }
                    optionalAddons = data.addons.filter { !it.is_required }.sortedBy { it.display_order }
                    if (requiredAddons.isNotEmpty()) {
                        binding.sectionRequiredAddons.visibility = View.VISIBLE
                        val minN = addonRequiredMin ?: 1
                        binding.tvChooseN.text = LocaleHelper.getStringWithEnglishNumbers(this@MealDetailsActivity, R.string.choose_n, if (minN == -1) requiredAddons.size else minN)
                        requiredAddonAdapter.setRequiredInfo(true, addonRequiredMin)
                        requiredAddonAdapter.submitList(requiredAddons)
                    } else {
                        binding.sectionRequiredAddons.visibility = View.GONE
                    }
                    if (optionalAddons.isNotEmpty()) {
                        binding.sectionOptionalAddons.visibility = View.VISIBLE
                        optionalAddonAdapter.submitList(optionalAddons)
                    } else {
                        binding.sectionOptionalAddons.visibility = View.GONE
                    }
                } else {
                    binding.sectionRequiredAddons.visibility = View.GONE
                    binding.sectionOptionalAddons.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.sectionRequiredAddons.visibility = View.GONE
                binding.sectionOptionalAddons.visibility = View.GONE
            }
            updateTotal()
            updateCompleteSelectionsVisibility()
        }
    }

    private fun getSelectedAddons(): List<ProductAddon> {
        return requiredAddonAdapter.getSelectedAddons() + optionalAddonAdapter.getSelectedAddons()
    }

    private fun updateTotal() {
        val p = product ?: return
        val base = productDisplayPrice * quantity
        val selectedAddons = getSelectedAddons()
        val addonPricesPerUnit = selectedAddons.map { addon -> getAddonDisplayPrice(addon).first }
        val addonsTotal = addonPricesPerUnit.sum() * quantity
        val total = base + addonsTotal
        Log.d("MoneyLog", "[MealDetails] updateTotal productDisplayPrice=$productDisplayPrice quantity=$quantity base=$base addonPricesPerUnit=$addonPricesPerUnit addonsTotal=$addonsTotal total=$total")
        val formattedPrice = CurrencyFormatter.formatPrice(total, currencyCode, currencySymbolPosition)
        val canAdd = if (addonRequired) {
            val requiredSelected = getSelectedAddons().filter { it.is_required }
            val minRequired = addonRequiredMin ?: 1
            if (minRequired == -1) requiredSelected.size == requiredAddons.size else requiredSelected.size >= minRequired
        } else true
        binding.tvAddItemLabel.text = when {
            !p.is_available -> getString(R.string.unavailable)
            else -> getString(R.string.add_item)
        }
        binding.tvAddItemPrice.text = when {
            !p.is_available -> ""
            canAdd -> formattedPrice
            else -> ""
        }
        updateCompleteSelectionsVisibility()
    }

    private fun setupAddToCart() {
        binding.layoutAddItem.setOnClickListener {
            val p = product ?: return@setOnClickListener
            if (!p.is_available) return@setOnClickListener
            val selectedAddons = getSelectedAddons()
            if (addonRequired) {
                val requiredSelected = selectedAddons.filter { it.is_required }
                val minRequired = addonRequiredMin ?: 1
                if (minRequired == -1) {
                    if (requiredSelected.size != requiredAddons.size) {
                        Toast.makeText(this, getString(R.string.please_select_all_required_addons), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } else {
                    if (selectedAddons.size < minRequired) {
                        Toast.makeText(this, LocaleHelper.getStringWithEnglishNumbers(this, R.string.please_select_at_least_addons, minRequired), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            }
            val unitRounded = kotlin.math.round(productDisplayPrice * 100.0) / 100.0
            val addonOverrides = selectedAddons.associate { it.id to kotlin.math.round(getAddonDisplayPrice(it).first * 100.0) / 100.0 }
            val lineSubtotal = kotlin.math.round((unitRounded + addonOverrides.values.sum()) * quantity * 100.0) / 100.0
            Log.d("MoneyLog", "[MealDetails] ADD_TO_CART product id=${p.id} name=${p.name} product.price=${p.price} unitPriceOverride=$unitRounded addonOverrides=$addonOverrides quantity=$quantity lineSubtotal=$lineSubtotal")
            if (cartViewModel.getCurrentRestaurantId() == null || cartViewModel.getCurrentRestaurantId() == p.website_id) {
                cartViewModel.addToCart(p, selectedAddons, unitPriceOverride = unitRounded, addonPriceOverrides = addonOverrides)
                if (quantity > 1) {
                    cartViewModel.updateQuantity(p.id, selectedAddons.map { it.id }, quantity)
                }
                Toast.makeText(this, getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                showRestaurantMismatchDialog(p, selectedAddons, addonOverrides)
            }
        }
    }

    private fun showRestaurantMismatchDialog(p: Product, selectedAddons: List<ProductAddon>, addonPriceOverrides: Map<Int, Double>) {
        val message = getString(R.string.cart_contains_different_restaurant, restaurantName)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_cart))
            .setMessage(message)
            .setPositiveButton(getString(R.string.clear_cart_and_add)) { _, _ ->
                val unitRounded = kotlin.math.round(productDisplayPrice * 100.0) / 100.0
                val addonRounded = addonPriceOverrides.mapValues { kotlin.math.round(it.value * 100.0) / 100.0 }
                Log.d("MoneyLog", "[MealDetails] CLEAR_CART_AND_ADD product id=${p.id} unitPriceOverride=$unitRounded addonPriceOverrides=$addonRounded quantity=$quantity")
                cartViewModel.clearCart()
                cartViewModel.addToCart(p, selectedAddons, unitPriceOverride = unitRounded, addonPriceOverrides = addonRounded)
                if (quantity > 1) {
                    cartViewModel.updateQuantity(p.id, selectedAddons.map { it.id }, quantity)
                }
                Toast.makeText(this, getString(R.string.cart_cleared_and_item_added), Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    companion object {
        private const val EXTRA_PRODUCT = "product"
        private const val EXTRA_CURRENCY_CODE = "currency_code"
        private const val EXTRA_CURRENCY_SYMBOL_POSITION = "currency_symbol_position"
        private const val EXTRA_RESTAURANT_NAME = "restaurant_name"

        fun newIntent(
            context: Context,
            product: Product,
            currencyCode: String?,
            currencySymbolPosition: String?,
            restaurantName: String
        ): Intent {
            return Intent(context, MealDetailsActivity::class.java).apply {
                putExtra(EXTRA_PRODUCT, product)
                putExtra(EXTRA_CURRENCY_CODE, currencyCode)
                putExtra(EXTRA_CURRENCY_SYMBOL_POSITION, currencySymbolPosition)
                putExtra(EXTRA_RESTAURANT_NAME, restaurantName)
            }
        }
    }
}
