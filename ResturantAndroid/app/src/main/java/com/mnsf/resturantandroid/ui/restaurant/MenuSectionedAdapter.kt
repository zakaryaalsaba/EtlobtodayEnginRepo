package com.mnsf.resturantandroid.ui.restaurant

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
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.util.CurrencyFormatter

/**
 * List item for sectioned menu: either a category header or a product row.
 */
sealed class MenuListItem {
    data class CategoryHeader(val categoryName: String) : MenuListItem()
    data class ProductItem(val product: Product) : MenuListItem()
}

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_PRODUCT = 1

/**
 * Adapter that shows category headers and products. Used with TabLayout:
 * category positions can be used to scroll to section when tab is selected.
 */
class MenuSectionedAdapter(
    private val onProductClick: (Product) -> Unit,
    private val currencyCode: String? = null,
    private val currencySymbolPosition: String? = null
) : ListAdapter<MenuListItem, RecyclerView.ViewHolder>(MenuSectionDiffCallback()) {

    /** Map category name -> first position in the list (for scroll-to-tab). */
    fun getCategoryStartPositions(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        currentList.forEachIndexed { index, item ->
            if (item is MenuListItem.CategoryHeader) {
                map[item.categoryName] = index
            }
        }
        return map
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MenuListItem.CategoryHeader -> VIEW_TYPE_HEADER
            is MenuListItem.ProductItem -> VIEW_TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            VIEW_TYPE_PRODUCT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product, parent, false)
                ProductRowViewHolder(view, onProductClick, currencyCode, currencySymbolPosition)
            }
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MenuListItem.CategoryHeader -> (holder as CategoryHeaderViewHolder).bind(item.categoryName)
            is MenuListItem.ProductItem -> (holder as ProductRowViewHolder).bind(item.product)
        }
    }

    class CategoryHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        fun bind(name: String) {
            tvCategoryName.text = name
        }
    }

    class ProductRowViewHolder(
        itemView: View,
        private val onProductClick: (Product) -> Unit,
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

            itemView.setOnClickListener { onProductClick(product) }
            btnMealDetails.setOnClickListener { onProductClick(product) }
        }
    }

    class MenuSectionDiffCallback : DiffUtil.ItemCallback<MenuListItem>() {
        override fun areItemsTheSame(old: MenuListItem, new: MenuListItem): Boolean {
            return when {
                old is MenuListItem.CategoryHeader && new is MenuListItem.CategoryHeader ->
                    old.categoryName == new.categoryName
                old is MenuListItem.ProductItem && new is MenuListItem.ProductItem ->
                    old.product.id == new.product.id
                else -> false
            }
        }

        override fun areContentsTheSame(old: MenuListItem, new: MenuListItem): Boolean {
            return old == new
        }
    }
}
