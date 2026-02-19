package com.driver.resturantandroid.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.driver.resturantandroid.R
import com.driver.resturantandroid.data.model.Order
import com.driver.resturantandroid.databinding.ItemOrderHistoryBinding
import com.driver.resturantandroid.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class OrderHistoryAdapter : ListAdapter<Order, OrderHistoryAdapter.OrderViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class OrderViewHolder(
        private val binding: ItemOrderHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            val ctx = binding.root.context
            binding.apply {
                tvOrderNumber.text = "Order #${order.order_number}"
                tvCustomerName.text = order.customer_name
                
                val formattedAmount = CurrencyFormatter.formatAmount(
                    order.total_amount,
                    order.currency_code,
                    order.currency_symbol_position
                )
                tvTotalAmount.text = formattedAmount
                
                tvStatus.text = order.status.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                    val date = inputFormat.parse(order.created_at)
                    tvDate.text = date?.let { outputFormat.format(it) } ?: order.created_at
                } catch (e: Exception) {
                    tvDate.text = order.created_at
                }
                
                // Payment method
                val paymentLabel = order.payment_method?.trim()?.takeIf { it.isNotEmpty() }
                    ?.replace("_", " ")
                    ?.replaceFirstChar { it.uppercaseChar() }
                if (paymentLabel != null) {
                    tvPaymentMethod.visibility = View.VISIBLE
                    tvPaymentMethod.text = ctx.getString(R.string.payment_label, paymentLabel)
                } else {
                    tvPaymentMethod.visibility = View.GONE
                }
                
                // Delivery fee
                val deliveryFeeValue = order.delivery_fees?.toDoubleOrNull() ?: 0.0
                if (deliveryFeeValue > 0 && order.delivery_fees != null) {
                    val formattedFee = CurrencyFormatter.formatAmount(
                        order.delivery_fees,
                        order.currency_code,
                        order.currency_symbol_position
                    )
                    tvDeliveryFee.visibility = View.VISIBLE
                    tvDeliveryFee.text = ctx.getString(R.string.delivery_fee_label, formattedFee)
                } else {
                    tvDeliveryFee.visibility = View.GONE
                }
                
                // Rider tip (show only if any)
                val tipValue = order.tip?.toDoubleOrNull() ?: 0.0
                if (tipValue > 0 && order.tip != null) {
                    val formattedTip = CurrencyFormatter.formatAmount(
                        order.tip!!,
                        order.currency_code,
                        order.currency_symbol_position
                    )
                    tvRiderTip.visibility = View.VISIBLE
                    tvRiderTip.text = ctx.getString(R.string.rider_tip, formattedTip)
                } else {
                    tvRiderTip.visibility = View.GONE
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

