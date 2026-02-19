package com.mnsf.resturantandroid.ui.restaurant

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.graphics.Color
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.data.model.ProductAddon
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.I18nHelper

class ProductAdapter(
    private val onAddToCart: (Product, List<ProductAddon>) -> Unit,
    private val onProductClick: ((Product) -> Unit)? = null,
    private val currencyCode: String? = null,
    private val currencySymbolPosition: String? = null
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view, onAddToCart, onProductClick, currencyCode, currencySymbolPosition)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        itemView: View,
        private val onAddToCart: (Product, List<ProductAddon>) -> Unit,
        private val onProductClick: ((Product) -> Unit)?,
        private val currencyCode: String?,
        private val currencySymbolPosition: String?
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvProductName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tvProductDescription)
        private val priceTextView: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val imageImageView: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val btnMealDetails: View = itemView.findViewById(R.id.btnMealDetails)

        fun bind(product: Product) {
            val context = itemView.context
            nameTextView.text = product.name
            descriptionTextView.text = product.description ?: context.getString(R.string.no_description_available)

            val formattedPrice = CurrencyFormatter.formatPrice(
                product.price,
                currencyCode,
                currencySymbolPosition
            )
            priceTextView.text = context.getString(R.string.price) + ": $formattedPrice"

            product.image_url?.let { imageUrl ->
                val emulatorUrl = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(imageUrl)
                    .replace("localhost", "10.0.2.2")
                Glide.with(itemView.context)
                    .load(emulatorUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(imageImageView)
            } ?: imageImageView.setImageResource(R.drawable.ic_launcher_foreground)

            itemView.setOnClickListener { onProductClick?.invoke(product) }
            btnMealDetails.setOnClickListener { onProductClick?.invoke(product) }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}

/** Required add-ons: uses item_addon_required (line separator). Radio when choose-1, checkbox otherwise. Supports discounted addon price (strikethrough original). */
class AddonAdapter(
    private val currencyCode: String? = null,
    private val currencySymbolPosition: String? = null,
    private val onSelectionChanged: (() -> Unit)? = null,
    private var addonPriceResolver: ((ProductAddon) -> Pair<Double, Double?>)? = null
) : ListAdapter<ProductAddon, AddonAdapter.AddonViewHolder>(AddonDiffCallback()) {

    private val selectedAddons = mutableSetOf<Int>()
    private var addonRequired: Boolean = false
    private var addonRequiredMin: Int? = null

    fun setAddonPriceResolver(resolver: ((ProductAddon) -> Pair<Double, Double?>)?) {
        addonPriceResolver = resolver
        notifyDataSetChanged()
    }

    fun setRequiredInfo(required: Boolean, requiredMin: Int?) {
        addonRequired = required
        addonRequiredMin = requiredMin
    }

    fun getSelectedAddons(): List<ProductAddon> {
        return currentList.filter { selectedAddons.contains(it.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon_required, parent, false)
        return AddonViewHolder(view, currencyCode, currencySymbolPosition) { addonId, isChecked ->
            val useRadio = (addonRequiredMin == 1)
            if (useRadio && isChecked) {
                selectedAddons.clear()
                selectedAddons.add(addonId)
                notifyDataSetChanged()
            } else if (!useRadio) {
                if (isChecked) selectedAddons.add(addonId)
                else selectedAddons.remove(addonId)
            }
            onSelectionChanged?.invoke()
        }
    }

    override fun onBindViewHolder(holder: AddonViewHolder, position: Int) {
        holder.bind(getItem(position), addonRequired, addonRequiredMin, selectedAddons.contains(getItem(position).id), addonPriceResolver)
        (holder.itemView.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.apply {
            topMargin = 0
            bottomMargin = 0
        }
    }

    class AddonViewHolder(
        itemView: View,
        private val currencyCode: String?,
        private val currencySymbolPosition: String?,
        private val onCheckedChange: (Int, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val radio: MaterialRadioButton = itemView.findViewById(R.id.radioAddon)
        private val checkbox: MaterialCheckBox = itemView.findViewById(R.id.checkboxAddon)
        private val nameTextView: TextView = itemView.findViewById(R.id.tvAddonName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tvAddonDescription)
        private val originalPriceTextView: TextView = itemView.findViewById(R.id.tvAddonOriginalPrice)
        private val priceTextView: TextView = itemView.findViewById(R.id.tvAddonPrice)

        fun bind(addon: ProductAddon, addonRequired: Boolean, addonRequiredMin: Int?, isSelected: Boolean, priceResolver: ((ProductAddon) -> Pair<Double, Double?>)?) {
            nameTextView.text = I18nHelper.getAddonNameDisplay(addon, itemView.context)
            val desc = I18nHelper.getAddonDescriptionDisplay(addon, itemView.context)
            descriptionTextView.text = desc
            descriptionTextView.visibility = if (desc.isNullOrBlank()) View.GONE else View.VISIBLE

            val (displayPrice, originalForStrike) = priceResolver?.invoke(addon)?.let { it }
                ?: Pair(addon.price, null as Double?)
            if (displayPrice > 0 || originalForStrike != null) {
                priceTextView.visibility = View.VISIBLE
                val formattedDisplay = CurrencyFormatter.formatPrice(displayPrice, currencyCode, currencySymbolPosition)
                priceTextView.text = "+$formattedDisplay"
                if (originalForStrike != null && originalForStrike > displayPrice) {
                    originalPriceTextView.visibility = View.VISIBLE
                    originalPriceTextView.text = "+" + CurrencyFormatter.formatPrice(originalForStrike, currencyCode, currencySymbolPosition)
                    originalPriceTextView.paintFlags = originalPriceTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    originalPriceTextView.visibility = View.GONE
                }
            } else {
                priceTextView.visibility = View.GONE
                originalPriceTextView.visibility = View.GONE
            }

            val useRadio = (addonRequiredMin == 1)
            radio.visibility = if (useRadio) View.VISIBLE else View.GONE
            checkbox.visibility = if (useRadio) View.GONE else View.VISIBLE

            val control = if (useRadio) radio else checkbox
            control.setOnCheckedChangeListener(null)
            control.isChecked = isSelected
            control.setOnCheckedChangeListener { _, isChecked -> onCheckedChange(addon.id, isChecked) }

            itemView.setOnClickListener { control.isChecked = !control.isChecked }
        }
    }

    class AddonDiffCallback : DiffUtil.ItemCallback<ProductAddon>() {
        override fun areItemsTheSame(oldItem: ProductAddon, newItem: ProductAddon): Boolean =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProductAddon, newItem: ProductAddon): Boolean =
            oldItem == newItem
    }
}

/** Optional add-ons: horizontal list. Free add-on: max 1, + becomes "1". Paid add-on: - qty + stepper. */
class OptionalAddonAdapter(
    private val priceResolver: (ProductAddon) -> Double,
    private val onSelectionChanged: (() -> Unit)? = null
) : ListAdapter<ProductAddon, OptionalAddonAdapter.OptionalViewHolder>(AddonAdapter.AddonDiffCallback()) {

    /** addon id -> quantity (0 = not selected). Free add-ons max 1. */
    private val quantities = mutableMapOf<Int, Int>()

    fun getSelectedAddons(): List<ProductAddon> = currentList.flatMap { addon ->
        List(quantities.getOrDefault(addon.id, 0)) { addon }
    }

    private fun changeQuantity(addonId: Int, delta: Int) {
        val addon = currentList.find { it.id == addonId } ?: return
        val isFree = priceResolver(addon) == 0.0
        val current = quantities.getOrDefault(addonId, 0)
        val newQty = when {
            delta > 0 && isFree -> (current + 1).coerceAtMost(1)
            delta > 0 && !isFree -> current + 1
            delta < 0 -> (current - 1).coerceAtLeast(0)
            else -> current
        }
        if (newQty != current) {
            if (newQty == 0) quantities.remove(addonId) else quantities[addonId] = newQty
            val pos = currentList.indexOfFirst { it.id == addonId }
            if (pos >= 0) notifyItemChanged(pos)
            onSelectionChanged?.invoke()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon_optional, parent, false)
        return OptionalViewHolder(view) { addonId, delta -> changeQuantity(addonId, delta) }
    }

    override fun onBindViewHolder(holder: OptionalViewHolder, position: Int) {
        val addon = getItem(position)
        val qty = quantities.getOrDefault(addon.id, 0)
        val isFree = priceResolver(addon) == 0.0
        holder.bind(addon, qty, isFree)
    }

    class OptionalViewHolder(
        itemView: View,
        private val onQuantityChange: (addonId: Int, delta: Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivAddonImage: ImageView = itemView.findViewById(R.id.ivAddonImage)
        private val tvAddonName: TextView = itemView.findViewById(R.id.tvAddonName)
        private val ivPlus: View = itemView.findViewById(R.id.ivPlus)
        private val tvFreeQuantityOne: View = itemView.findViewById(R.id.tvFreeQuantityOne)
        private val layoutStepper: View = itemView.findViewById(R.id.layoutStepper)
        private val btnDecrease: View = itemView.findViewById(R.id.btnDecrease)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnIncrease: View = itemView.findViewById(R.id.btnIncrease)

        fun bind(addon: ProductAddon, quantity: Int, isFree: Boolean) {
            (itemView as? MaterialCardView)?.setCardBackgroundColor(Color.TRANSPARENT)
            tvAddonName.text = I18nHelper.getAddonNameDisplay(addon, itemView.context)
            val id = addon.id

            when {
                isFree && quantity == 0 -> {
                    ivPlus.visibility = View.VISIBLE
                    tvFreeQuantityOne.visibility = View.GONE
                    layoutStepper.visibility = View.GONE
                    ivPlus.setOnClickListener { onQuantityChange(id, 1) }
                }
                isFree && quantity == 1 -> {
                    ivPlus.visibility = View.GONE
                    tvFreeQuantityOne.visibility = View.VISIBLE
                    layoutStepper.visibility = View.GONE
                    tvFreeQuantityOne.setOnClickListener { onQuantityChange(id, -1) }
                }
                !isFree && quantity == 0 -> {
                    ivPlus.visibility = View.VISIBLE
                    tvFreeQuantityOne.visibility = View.GONE
                    layoutStepper.visibility = View.GONE
                    ivPlus.setOnClickListener { onQuantityChange(id, 1) }
                }
                else -> {
                    ivPlus.visibility = View.GONE
                    tvFreeQuantityOne.visibility = View.GONE
                    layoutStepper.visibility = View.VISIBLE
                    tvQuantity.text = quantity.toString()
                    btnDecrease.setOnClickListener { onQuantityChange(id, -1) }
                    btnIncrease.setOnClickListener { onQuantityChange(id, 1) }
                }
            }
            if (addon.image_url != null) {
                val url = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(addon.image_url!!)
                    .replace("localhost", "10.0.2.2")
                Glide.with(itemView.context)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(ivAddonImage)
            } else {
                ivAddonImage.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }
}