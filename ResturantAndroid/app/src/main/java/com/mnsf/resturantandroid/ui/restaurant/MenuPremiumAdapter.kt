package com.mnsf.resturantandroid.ui.restaurant

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.CartItem
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.I18nHelper

/**
 * Premium menu list item: Trending section header, Trending pair (2 products),
 * Category header, or Main product row.
 */
sealed class MenuPremiumItem {
    data class TrendingHeader(val title: String) : MenuPremiumItem()
    data class TrendingPair(val product1: Product, val product2: Product?) : MenuPremiumItem()
    data class CategoryHeader(val categoryName: String) : MenuPremiumItem()
    data class MainProduct(val product: Product) : MenuPremiumItem()
}

private const val VIEW_TYPE_TRENDING_HEADER = 0
private const val VIEW_TYPE_TRENDING_PAIR = 1
private const val VIEW_TYPE_CATEGORY_HEADER = 2
private const val VIEW_TYPE_MAIN_PRODUCT = 3

private const val TAG_MENU_PRICES = "RestaurantDetails"

/**
 * Premium menu adapter: Trending grid (2 per row), Main dishes (list style).
 * Uses DiffUtil for performance. Category positions for tab scroll.
 * Applies percent_off offers: shows original price strikethrough in red and discounted price.
 */
class MenuPremiumAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onAddToCartDirect: (Product) -> Unit,
    private val onQuantityChange: (productId: Int, addonIds: List<Int>, newQuantity: Int) -> Unit,
    private val currencyCode: String? = null,
    private val currencySymbolPosition: String? = null,
    private var offers: List<Offer> = emptyList()
) : ListAdapter<MenuPremiumItem, RecyclerView.ViewHolder>(MenuPremiumDiffCallback()) {

    /** Cart state: product id -> (first cart line for that product, total quantity). Updated when returning from MealDetails or when user taps +/-. */
    private var cartStateByProductId: Map<Int, Pair<CartItem, Int>> = emptyMap()

    fun setOffers(newOffers: List<Offer>) {
        if (offers != newOffers) {
            offers = newOffers
            Log.d(TAG_MENU_PRICES, "setOffers: ${newOffers.size} offer(s) applied to menu (percent_off=${newOffers.count { it.offer_type?.trim()?.lowercase() == "percent_off" }})")
            notifyDataSetChanged()
        }
    }

    fun setCartState(state: Map<Int, Pair<CartItem, Int>>) {
        cartStateByProductId = state
        notifyDataSetChanged()
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

    /** Category name -> first list position (for smooth scroll when tab selected). */
    fun getCategoryStartPositions(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        currentList.forEachIndexed { index, item ->
            if (item is MenuPremiumItem.CategoryHeader) {
                map[item.categoryName] = index
            }
        }
        return map
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MenuPremiumItem.TrendingHeader -> VIEW_TYPE_TRENDING_HEADER
        is MenuPremiumItem.TrendingPair -> VIEW_TYPE_TRENDING_PAIR
        is MenuPremiumItem.CategoryHeader -> VIEW_TYPE_CATEGORY_HEADER
        is MenuPremiumItem.MainProduct -> VIEW_TYPE_MAIN_PRODUCT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TRENDING_HEADER, VIEW_TYPE_CATEGORY_HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionHeaderViewHolder(v)
            }
            VIEW_TYPE_TRENDING_PAIR -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_trending_pair, parent, false)
                TrendingPairViewHolder(v, onProductClick, currencyCode, currencySymbolPosition) { getDisplayPrices(it) }
            }
            VIEW_TYPE_MAIN_PRODUCT -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_main_product, parent, false)
                MainProductViewHolder(v, onProductClick, onAddToCartDirect, onQuantityChange, currencyCode, currencySymbolPosition) { getDisplayPrices(it) }
            }
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MenuPremiumItem.TrendingHeader -> (holder as SectionHeaderViewHolder).bind(item.title)
            is MenuPremiumItem.CategoryHeader -> (holder as SectionHeaderViewHolder).bind(item.categoryName)
            is MenuPremiumItem.TrendingPair -> (holder as TrendingPairViewHolder).bind(item.product1, item.product2)
            is MenuPremiumItem.MainProduct -> (holder as MainProductViewHolder).bind(item.product, cartStateByProductId)
        }
    }

    class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSectionTitle: TextView = itemView.findViewById(R.id.tvSectionTitle)
        fun bind(title: String) {
            tvSectionTitle.text = title
        }
    }

    class TrendingPairViewHolder(
        itemView: View,
        private val onProductClick: (Product) -> Unit,
        private val currencyCode: String?,
        private val currencySymbolPosition: String?,
        private val priceResolver: (Product) -> Pair<Double, Double?>
    ) : RecyclerView.ViewHolder(itemView) {
        private val frame1: FrameLayout = itemView.findViewById(R.id.frameProduct1)
        private val frame2: FrameLayout = itemView.findViewById(R.id.frameProduct2)

        fun bind(product1: Product, product2: Product?) {
            frame1.removeAllViews()
            frame2.removeAllViews()
            val card1 = LayoutInflater.from(itemView.context)
                .inflate(R.layout.item_trending_product, frame1, false)
            bindTrendingCard(card1, product1)
            frame1.addView(card1)
            if (product2 != null) {
                val card2 = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_trending_product, frame2, false)
                bindTrendingCard(card2, product2)
                frame2.addView(card2)
            }
        }

        private fun bindTrendingCard(card: View, product: Product) {
            val iv = card.findViewById<ImageView>(R.id.ivProductImage)
            val name = card.findViewById<TextView>(R.id.tvProductName)
            val originalPrice = card.findViewById<TextView>(R.id.tvOriginalPrice)
            val price = card.findViewById<TextView>(R.id.tvProductPrice)
            name.text = I18nHelper.getProductNameDisplay(product, card.context)
            val (displayPrice, originalForStrike) = priceResolver(product)
            val nameDisplay = I18nHelper.getProductNameDisplay(product, card.context)
            if (originalForStrike != null && originalForStrike > displayPrice) {
                Log.d(TAG_MENU_PRICES, "Trending product id=${product.id} name=\"$nameDisplay\" original=$originalForStrike discounted=$displayPrice (strikethrough shown)")
            } else {
                Log.d(TAG_MENU_PRICES, "Trending product id=${product.id} name=\"$nameDisplay\" price=$displayPrice (no discount)")
            }
            price.text = CurrencyFormatter.formatPrice(displayPrice, currencyCode, currencySymbolPosition)
            if (originalForStrike != null && originalForStrike > displayPrice) {
                originalPrice.visibility = View.VISIBLE
                originalPrice.text = CurrencyFormatter.formatPrice(originalForStrike, currencyCode, currencySymbolPosition)
                originalPrice.paintFlags = originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                originalPrice.visibility = View.GONE
            }
            product.image_url?.let { url ->
                val u = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(url).replace("localhost", "10.0.2.2")
                Glide.with(card.context).load(u).placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground).centerCrop().into(iv)
            } ?: iv.setImageResource(R.drawable.ic_launcher_foreground)
            card.setOnClickListener { onProductClick(product) }
        }
    }

    class MainProductViewHolder(
        itemView: View,
        private val onProductClick: (Product) -> Unit,
        private val onAddToCartDirect: (Product) -> Unit,
        private val onQuantityChange: (productId: Int, addonIds: List<Int>, newQuantity: Int) -> Unit,
        private val currencyCode: String?,
        private val currencySymbolPosition: String?,
        private val priceResolver: (Product) -> Pair<Double, Double?>
    ) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tvProductName)
        private val desc: TextView = itemView.findViewById(R.id.tvProductDescription)
        private val originalPrice: TextView = itemView.findViewById(R.id.tvOriginalPrice)
        private val price: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val image: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val actionIcon: ImageView = itemView.findViewById(R.id.ivProductAction)
        private val layoutCartStepper: View = itemView.findViewById(R.id.layoutCartStepper)
        private val tvCartQuantity: TextView = itemView.findViewById(R.id.tvCartQuantity)
        private val btnCartMinus: View = itemView.findViewById(R.id.btnCartMinus)
        private val btnCartPlus: View = itemView.findViewById(R.id.btnCartPlus)

        fun bind(product: Product, cartStateByProductId: Map<Int, Pair<CartItem, Int>>) {
            val ctx = itemView.context
            name.text = I18nHelper.getProductNameDisplay(product, ctx)
            val displayDesc = I18nHelper.getProductDescriptionDisplay(product, ctx)
            desc.text = displayDesc?.take(80)?.plus("…").takeIf { (displayDesc?.length ?: 0) > 80 }
                ?: displayDesc ?: ctx.getString(R.string.no_description_available)
            desc.visibility = if (product.description.isNullOrBlank()) View.GONE else View.VISIBLE
            val (displayPrice, originalForStrike) = priceResolver(product)
            val nameDisplay = I18nHelper.getProductNameDisplay(product, ctx)
            if (originalForStrike != null && originalForStrike > displayPrice) {
                Log.d(TAG_MENU_PRICES, "Main product id=${product.id} name=\"$nameDisplay\" original=$originalForStrike discounted=$displayPrice (strikethrough shown)")
            } else {
                Log.d(TAG_MENU_PRICES, "Main product id=${product.id} name=\"$nameDisplay\" price=$displayPrice (no discount)")
            }
            price.text = CurrencyFormatter.formatPrice(displayPrice, currencyCode, currencySymbolPosition)
            if (originalForStrike != null && originalForStrike > displayPrice) {
                originalPrice.visibility = View.VISIBLE
                originalPrice.text = CurrencyFormatter.formatPrice(originalForStrike, currencyCode, currencySymbolPosition)
                originalPrice.paintFlags = originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                originalPrice.visibility = View.GONE
            }
            val cartState = cartStateByProductId[product.id]
            if (cartState != null && cartState.second > 0) {
                val (firstItem, totalQty) = cartState
                val addonIds = firstItem.addonIdsSorted()
                actionIcon.visibility = View.GONE
                layoutCartStepper.visibility = View.VISIBLE
                tvCartQuantity.text = totalQty.toString()
                btnCartMinus.setOnClickListener {
                    onQuantityChange(product.id, addonIds, totalQty - 1)
                }
                btnCartPlus.setOnClickListener {
                    onQuantityChange(product.id, addonIds, totalQty + 1)
                }
                layoutCartStepper.setOnClickListener { /* consume row click when tapping stepper area */ }
            } else {
                actionIcon.visibility = View.VISIBLE
                layoutCartStepper.visibility = View.GONE
                val hasAddons = product.addon_required == true
                actionIcon.setImageResource(
                    if (hasAddons) R.drawable.ic_chevron_right
                    else R.drawable.ic_add_24
                )
                actionIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.white))
                // For no-addon products, "+" opens the simple meal sheet; for addon products, opens full details
                actionIcon.setOnClickListener { onProductClick(product) }
            }
            // Row click: open details (full activity if addons, bottom sheet if no addons)
            itemView.setOnClickListener { onProductClick(product) }
            product.image_url?.let { url ->
                val u = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(url).replace("localhost", "10.0.2.2")
                Glide.with(itemView.context).load(u).placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground).centerCrop().into(image)
            } ?: image.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    class MenuPremiumDiffCallback : DiffUtil.ItemCallback<MenuPremiumItem>() {
        override fun areItemsTheSame(old: MenuPremiumItem, new: MenuPremiumItem): Boolean {
            return when {
                old is MenuPremiumItem.TrendingHeader && new is MenuPremiumItem.TrendingHeader -> old.title == new.title
                old is MenuPremiumItem.TrendingPair && new is MenuPremiumItem.TrendingPair ->
                    old.product1.id == new.product1.id && (old.product2?.id ?: -1) == (new.product2?.id ?: -1)
                old is MenuPremiumItem.CategoryHeader && new is MenuPremiumItem.CategoryHeader -> old.categoryName == new.categoryName
                old is MenuPremiumItem.MainProduct && new is MenuPremiumItem.MainProduct -> old.product.id == new.product.id
                else -> false
            }
        }
        override fun areContentsTheSame(old: MenuPremiumItem, new: MenuPremiumItem): Boolean = old == new
    }
}
