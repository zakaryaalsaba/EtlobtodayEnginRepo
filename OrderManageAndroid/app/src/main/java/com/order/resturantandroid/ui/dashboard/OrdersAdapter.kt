package com.order.resturantandroid.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.order.resturantandroid.R
import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.databinding.ItemOrderBinding
import com.order.resturantandroid.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class OrdersAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding, onOrderClick)
    }
    
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class OrderViewHolder(
        private val binding: ItemOrderBinding,
        private val onOrderClick: (Order) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            binding.apply {
                tvOrderNumber.text = order.orderNumber ?: "N/A"
                tvCustomerName.text = order.customerName ?: "N/A"
                tvOrderType.text = (order.orderType ?: "").replaceFirstChar { it.uppercaseChar() }
                tvStatus.text = (order.status ?: "").replaceFirstChar { it.uppercaseChar() }
                
                // Format total amount with currency from database
                android.util.Log.d("OrdersAdapter", "Order currency: ${order.currencyCode}, position: ${order.currencySymbolPosition}, total: ${order.totalAmount}")
                val formattedTotal = CurrencyFormatter.formatAmount(
                    order.totalAmount ?: "0.00",
                    order.currencyCode,
                    order.currencySymbolPosition
                )
                android.util.Log.d("OrdersAdapter", "Formatted total: $formattedTotal")
                tvTotalAmount.text = formattedTotal
                
                // Format time
                try {
                    val createdAt = order.createdAt ?: ""
                    if (createdAt.isNotEmpty()) {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        val date = inputFormat.parse(createdAt)
                        tvOrderTime.text = date?.let { outputFormat.format(it) } ?: createdAt
                    } else {
                        tvOrderTime.text = "N/A"
                    }
                } catch (e: Exception) {
                    tvOrderTime.text = order.createdAt ?: "N/A"
                }
                
                // Items summary
                try {
                    val itemsList = order.getItemsList()
                    val itemsCount = itemsList.size
                    tvItemsCount.text = "$itemsCount ${if (itemsCount == 1) "item" else "items"}"
                } catch (e: Exception) {
                    tvItemsCount.text = "0 items"
                }
                
                // Payment method display
                val paymentMethodLayout = root.findViewById<android.view.ViewGroup>(R.id.layoutPaymentMethod)
                val tvPaymentMethod = root.findViewById<android.widget.TextView>(R.id.tvPaymentMethod)
                
                if (order.paymentMethod != null && order.paymentMethod.isNotEmpty()) {
                    val paymentMethod = order.paymentMethod.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                    tvPaymentMethod?.text = paymentMethod
                    paymentMethodLayout?.visibility = android.view.View.VISIBLE
                } else {
                    paymentMethodLayout?.visibility = android.view.View.GONE
                }
                
                // Status chip styling
                val status = (order.status ?: "").lowercase()
                tvStatus.text = status.replaceFirstChar { it.uppercaseChar() }
                
                val statusColor = when (status) {
                    "pending" -> android.graphics.Color.parseColor("#F59E0B")
                    "confirmed", "preparing" -> android.graphics.Color.parseColor("#3B82F6")
                    "ready" -> android.graphics.Color.parseColor("#10B981")
                    "completed" -> android.graphics.Color.parseColor("#059669")
                    "cancelled" -> android.graphics.Color.parseColor("#EF4444")
                    else -> android.graphics.Color.parseColor("#6B7280")
                }
                
                // Set chip background color with alpha
                val chipColor = android.graphics.Color.argb(30, 
                    android.graphics.Color.red(statusColor),
                    android.graphics.Color.green(statusColor),
                    android.graphics.Color.blue(statusColor)
                )
                tvStatus.setChipBackgroundColorResource(android.R.color.transparent)
                tvStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(chipColor)
                tvStatus.setTextColor(statusColor)
                
                root.setOnClickListener {
                    onOrderClick(order)
                }
            }
        }
    }
    
    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}

