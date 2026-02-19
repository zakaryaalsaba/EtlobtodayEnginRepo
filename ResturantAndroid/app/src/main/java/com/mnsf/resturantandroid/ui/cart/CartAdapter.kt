package com.mnsf.resturantandroid.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.CartItem
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.util.CurrencyFormatter

class CartAdapter(
    private val onQuantityChange: (productId: Int, addonIds: List<Int>, quantity: Int) -> Unit,
    private val onRemove: (productId: Int, addonIds: List<Int>) -> Unit,
    private val onEditClick: (Product) -> Unit,
    private val currencyCode: String? = null,
    private val currencySymbolPosition: String? = null,
    private var offers: List<Offer> = emptyList()
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    fun setOffers(newOffers: List<Offer>) {
        if (offers != newOffers) {
            offers = newOffers
            notifyDataSetChanged()
        }
    }

    /** Calculate subtotal for display. Uses stored overrides when present (from offer at add-to-cart), else applies offers. Returns (subtotal, originalForStrikethrough?). */
    private fun getSubtotalWithOffers(cartItem: CartItem): Pair<Double, Double?> {
        val quantity = cartItem.quantity
        val hasStoredPrices = cartItem.unitPriceOverride != null || cartItem.addonPriceOverrides != null

        if (hasStoredPrices) {
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

        val productPrice = if (productPercent != null && productPercent > 0.0) {
            product.price * (1.0 - productPercent / 100.0)
        } else {
            product.price
        }

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
        val originalProductPrice = product.price
        val originalAddonsTotal = cartItem.selectedAddons.sumOf { it.price }
        val originalSubtotal = (originalProductPrice + originalAddonsTotal) * quantity

        return if (discountedSubtotal < originalSubtotal) Pair(discountedSubtotal, originalSubtotal) else Pair(originalSubtotal, null)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view, onQuantityChange, onRemove, onEditClick, currencyCode, currencySymbolPosition) { cartItem ->
            getSubtotalWithOffers(cartItem)
        }
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CartViewHolder(
        itemView: View,
        private val onQuantityChange: (productId: Int, addonIds: List<Int>, quantity: Int) -> Unit,
        private val onRemove: (productId: Int, addonIds: List<Int>) -> Unit,
        private val onEditClick: (Product) -> Unit,
        private val currencyCode: String?,
        private val currencySymbolPosition: String?,
        private val getSubtotalWithOffers: (CartItem) -> Pair<Double, Double?>
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvProductName)
        private val addonsTextView: TextView = itemView.findViewById(R.id.tvAddons)
        private val priceTextView: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val quantityTextView: TextView = itemView.findViewById(R.id.tvQuantity)
        private val subtotalTextView: TextView = itemView.findViewById(R.id.tvSubtotal)
        private val imageImageView: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val btnIncrease: View = itemView.findViewById(R.id.btnIncrease)
        private val btnDecrease: View = itemView.findViewById(R.id.btnDecrease)
        private val btnRemove: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnRemove)
        private val layoutEdit: View = itemView.findViewById(R.id.layoutEdit)

        fun bind(cartItem: CartItem) {
            val context = itemView.context
            val product = cartItem.product
            val addonIds = cartItem.addonIdsSorted()

            nameTextView.text = product.name

            if (cartItem.selectedAddons.isNotEmpty()) {
                addonsTextView.text = cartItem.selectedAddons.joinToString(", ") { it.name }
                addonsTextView.visibility = View.VISIBLE
            } else {
                addonsTextView.visibility = View.GONE
            }

            val formattedSubtotal = CurrencyFormatter.formatPrice(
                cartItem.getSubtotal(),
                currencyCode,
                currencySymbolPosition
            )
            quantityTextView.text = cartItem.quantity.toString()
            subtotalTextView.text = formattedSubtotal
            priceTextView.visibility = View.GONE

            product.image_url?.let { imageUrl ->
                val emulatorUrl = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(imageUrl)
                    .replace("localhost", "10.0.2.2")
                Glide.with(context)
                    .load(emulatorUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(imageImageView)
            } ?: imageImageView.setImageResource(R.drawable.ic_launcher_foreground)

            btnIncrease.setOnClickListener {
                onQuantityChange(product.id, addonIds, cartItem.quantity + 1)
            }

            btnDecrease.setOnClickListener {
                if (cartItem.quantity > 1) {
                    onQuantityChange(product.id, addonIds, cartItem.quantity - 1)
                } else {
                    onRemove(product.id, addonIds)
                }
            }

            layoutEdit.setOnClickListener { onEditClick(product) }

            btnRemove.setOnClickListener { onRemove(product.id, addonIds) }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.product.id == newItem.product.id &&
                oldItem.addonIdsSorted() == newItem.addonIdsSorted()
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.quantity == newItem.quantity &&
                oldItem.product == newItem.product &&
                oldItem.selectedAddons == newItem.selectedAddons
        }
    }
}

