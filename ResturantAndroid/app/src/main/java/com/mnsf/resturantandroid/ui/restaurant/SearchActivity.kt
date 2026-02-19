package com.mnsf.resturantandroid.ui.restaurant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.databinding.ActivitySearchBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.I18nHelper
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.utils.UrlHelper
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var productAdapter: SearchProductAdapter
    private var restaurantId: Int = -1
    private var currencyCode: String? = null
    private var currencySymbolPosition: String? = null
    private var restaurantName: String = ""
    private var allProducts: List<Product> = emptyList()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getIntExtra(EXTRA_RESTAURANT_ID, -1)
        currencyCode = intent.getStringExtra(EXTRA_CURRENCY_CODE)
        currencySymbolPosition = intent.getStringExtra(EXTRA_CURRENCY_SYMBOL_POSITION)
        restaurantName = intent.getStringExtra(EXTRA_RESTAURANT_NAME) ?: ""

        if (restaurantId == -1) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize ViewModel first
        val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        restaurantViewModel = ViewModelProvider(
            this,
            RestaurantViewModelFactory(restaurantRepository)
        )[RestaurantViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupObservers()

        restaurantViewModel.loadProducts(restaurantId)
        restaurantViewModel.loadRestaurantOffers(restaurantId)
        
        // Auto-focus search field
        binding.etSearch.post {
            binding.etSearch.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.search_restaurants)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        productAdapter = SearchProductAdapter(
            onProductClick = { product ->
                startActivity(MealDetailsActivity.newIntent(
                    this,
                    product,
                    currencyCode,
                    currencySymbolPosition,
                    restaurantName
                ))
            },
            currencyCode = currencyCode,
            currencySymbolPosition = currencySymbolPosition
        )
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewProducts.adapter = productAdapter
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch()
            }
        })
    }

    private fun performSearch() {
        val query = binding.etSearch.text.toString().trim()
        filterProducts(query)
    }

    private fun filterProducts(query: String) {
        val filtered = if (query.isEmpty()) {
            allProducts.filter { it.is_available }
        } else {
            allProducts.filter { product ->
                product.is_available && (
                    I18nHelper.getProductNameDisplay(product, this).contains(query, ignoreCase = true) ||
                    I18nHelper.getProductDescriptionDisplay(product, this)?.contains(query, ignoreCase = true) == true ||
                    I18nHelper.getProductCategoryDisplay(product, this).contains(query, ignoreCase = true)
                )
            }
        }

        productAdapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty() && query.isNotEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textEmptyState.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerViewProducts.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun setupObservers() {
        restaurantViewModel.products.observe(this) { products ->
            allProducts = products
            val query = binding.etSearch.text.toString().trim()
            filterProducts(query)
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

    companion object {
        private const val EXTRA_RESTAURANT_ID = "restaurant_id"
        private const val EXTRA_CURRENCY_CODE = "currency_code"
        private const val EXTRA_CURRENCY_SYMBOL_POSITION = "currency_symbol_position"
        private const val EXTRA_RESTAURANT_NAME = "restaurant_name"

        fun newIntent(
            context: Context,
            restaurantId: Int,
            currencyCode: String?,
            currencySymbolPosition: String?,
            restaurantName: String
        ): Intent {
            return Intent(context, SearchActivity::class.java).apply {
                putExtra(EXTRA_RESTAURANT_ID, restaurantId)
                putExtra(EXTRA_CURRENCY_CODE, currencyCode)
                putExtra(EXTRA_CURRENCY_SYMBOL_POSITION, currencySymbolPosition)
                putExtra(EXTRA_RESTAURANT_NAME, restaurantName)
            }
        }
    }
}

// Adapter for search product list
class SearchProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val currencyCode: String?,
    private val currencySymbolPosition: String?
) : androidx.recyclerview.widget.ListAdapter<Product, SearchProductAdapter.ProductViewHolder>(
    ProductDiffCallback()
) {
    private var offers: List<Offer> = emptyList()
    
    fun setOffers(newOffers: List<Offer>) {
        if (offers != newOffers) {
            offers = newOffers
            notifyDataSetChanged()
        }
    }
    
    /** Best percent discount for this product (0.0–100.0) or null if none. */
    private fun getBestPercentOff(productId: Int): Double? {
        val percentOffers = offers.filter { offer ->
            (offer.offer_type?.trim()?.lowercase() == "percent_off") && (offer.value ?: 0.0) > 0
        }
        val applicable = percentOffers.filter { offer ->
            when (offer.offer_scope?.trim()?.lowercase()) {
                "selected_items" -> productId in offer.getSelectedProductIds()
                else -> true // "all_items", null, or blank = apply to all products
            }
        }
        return applicable.mapNotNull { it.value }.maxOrNull()
    }
    
    /** (displayPrice, originalPriceForStrikethrough?). If discounted, originalPriceForStrikethrough is non-null. */
    private fun getDisplayPrices(product: Product): Pair<Double, Double?> {
        val percent = getBestPercentOff(product.id)
        if (percent == null || percent <= 0.0) return Pair(product.price, null)
        val discounted = product.price * (1.0 - percent / 100.0)
        return Pair(discounted, product.price)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ProductViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_product, parent, false)
        return ProductViewHolder(view, onProductClick, currencyCode, currencySymbolPosition) { product ->
            getDisplayPrices(product)
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        itemView: android.view.View,
        private val onProductClick: (Product) -> Unit,
        private val currencyCode: String?,
        private val currencySymbolPosition: String?,
        private val getDisplayPrices: (Product) -> Pair<Double, Double?>
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        private val tvProductName: android.widget.TextView = itemView.findViewById(R.id.tvProductName)
        private val tvProductDescription: android.widget.TextView = itemView.findViewById(R.id.tvProductDescription)
        private val tvProductPrice: android.widget.TextView = itemView.findViewById(R.id.tvProductPrice)
        private val tvProductOriginalPrice: android.widget.TextView = itemView.findViewById(R.id.tvProductOriginalPrice)
        private val ivProductImage: com.google.android.material.imageview.ShapeableImageView =
            itemView.findViewById(R.id.ivProductImage)

        fun bind(product: Product) {
            val ctx = itemView.context

            // Name (i18n)
            tvProductName.text = I18nHelper.getProductNameDisplay(product, ctx)

            // Description (i18n)
            val displayDesc = I18nHelper.getProductDescriptionDisplay(product, ctx)
            if (!displayDesc.isNullOrBlank()) {
                tvProductDescription.text = displayDesc
                tvProductDescription.visibility = android.view.View.VISIBLE
            } else {
                tvProductDescription.visibility = android.view.View.GONE
            }

            // Price with offer discount
            val (displayPrice, originalPrice) = getDisplayPrices(product)
            tvProductPrice.text = CurrencyFormatter.formatPrice(
                displayPrice,
                currencyCode,
                currencySymbolPosition
            )
            if (originalPrice != null && originalPrice > displayPrice) {
                tvProductOriginalPrice.visibility = android.view.View.VISIBLE
                tvProductOriginalPrice.text = CurrencyFormatter.formatPrice(
                    originalPrice,
                    currencyCode,
                    currencySymbolPosition
                )
                tvProductOriginalPrice.paintFlags = tvProductOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvProductOriginalPrice.visibility = android.view.View.GONE
            }

            // Image
            product.image_url?.let { url ->
                val emulatorUrl = UrlHelper.convertUrlForAndroid(url)
                    .replace("localhost", "10.0.2.2")
                Glide.with(ctx)
                    .load(emulatorUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(ivProductImage)
            } ?: ivProductImage.setImageResource(R.drawable.ic_launcher_foreground)

            // Click listener
            itemView.setOnClickListener {
                onProductClick(product)
            }
        }
    }

    class ProductDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
