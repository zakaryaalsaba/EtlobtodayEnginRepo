package com.order.resturantandroid.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.order.resturantandroid.data.model.OrderItem
import com.order.resturantandroid.databinding.ItemOrderItemBinding
import com.order.resturantandroid.util.CurrencyFormatter

class OrderItemsAdapter(
    private val currencyCode: String? = "USD",
    private val currencySymbolPosition: String? = "before"
) : ListAdapter<OrderItem, OrderItemsAdapter.ItemViewHolder>(ItemDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemOrderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding, currencyCode, currencySymbolPosition)
    }
    
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ItemViewHolder(
        private val binding: ItemOrderItemBinding,
        private val currencyCode: String?,
        private val currencySymbolPosition: String?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: OrderItem) {
            binding.apply {
                tvItemName.text = item.productName
                tvQuantity.text = "x${item.quantity}"
                
                android.util.Log.d("OrderItemsAdapter", "Formatting with currency: $currencyCode, position: $currencySymbolPosition")
                
                // Format price with currency
                val formattedPrice = CurrencyFormatter.formatAmount(
                    item.productPrice,
                    currencyCode,
                    currencySymbolPosition
                )
                android.util.Log.d("OrderItemsAdapter", "Price: ${item.productPrice} -> $formattedPrice")
                tvPrice.text = formattedPrice
                
                // Format subtotal with currency
                val formattedSubtotal = CurrencyFormatter.formatAmount(
                    item.subtotal,
                    currencyCode,
                    currencySymbolPosition
                )
                android.util.Log.d("OrderItemsAdapter", "Subtotal: ${item.subtotal} -> $formattedSubtotal")
                tvSubtotal.text = formattedSubtotal
            }
        }
    }
    
    class ItemDiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
            return oldItem == newItem
        }
    }
}

