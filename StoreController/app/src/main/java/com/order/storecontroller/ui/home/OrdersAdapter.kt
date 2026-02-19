package com.order.storecontroller.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.order.storecontroller.R
import com.order.storecontroller.data.model.Order
import com.order.storecontroller.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Locale

class OrdersAdapter : ListAdapter<Order, OrdersAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            val ctx = binding.root.context
            binding.orderNumberText.text = ctx.getString(R.string.order_number, order.order_number)
            binding.storeNameText.text = order.restaurant?.name?.takeIf { it.isNotBlank() }
                ?: ctx.getString(R.string.store_name)
            binding.customerText.text = "${ctx.getString(R.string.customer)}: ${order.customer_name}"
            val phoneStr = order.customer_phone.takeIf { it.isNotBlank() }?.let {
                "${ctx.getString(R.string.phone)}: $it"
            } ?: ""
            binding.phoneText.text = phoneStr
            binding.phoneText.visibility = if (phoneStr.isNotEmpty()) View.VISIBLE else View.GONE
            binding.addressText.text = order.customer_address?.takeIf { it.isNotBlank() }?.let {
                "${ctx.getString(R.string.address)}: $it"
            } ?: ""
            binding.addressText.visibility = if (binding.addressText.text.isNotEmpty()) View.VISIBLE else View.GONE
            binding.totalText.text = order.total_amount
            binding.statusChip.text = statusLabel(order.status)
            binding.orderTypeChip.text = orderTypeLabel(ctx, order.order_type)
            val count = order.items?.size ?: 0
            binding.itemsCountText.text = ctx.getString(R.string.items_count, count)
            binding.createdAtText.text = formatTime(order.created_at)
            binding.paymentMethodText.text = order.payment_method?.takeIf { it.isNotBlank() }
                ?.let { "${ctx.getString(R.string.payment_method)}: $it" } ?: ""
            binding.paymentStatusText.text = order.payment_status?.takeIf { it.isNotBlank() } ?: ""
            binding.deliveryFeesText.visibility = View.GONE
            order.delivery_fees?.takeIf { it.isNotBlank() && it != "0" }?.let { fee ->
                binding.deliveryFeesText.text = "${ctx.getString(R.string.delivery_fees)}: $fee"
                binding.deliveryFeesText.visibility = View.VISIBLE
            }
            binding.tipText.visibility = View.GONE
            order.tip?.takeIf { it.isNotBlank() && it != "0" }?.let { tip ->
                binding.tipText.text = "${ctx.getString(R.string.tip)}: $tip"
                binding.tipText.visibility = View.VISIBLE
            }
            // Populate order items
            binding.orderItemsContainer.removeAllViews()
            order.items?.forEach { item ->
                val line = TextView(ctx).apply {
                    text = ctx.getString(R.string.order_item_line, item.quantity, item.product_name, item.subtotal)
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant))
                    setPadding(0, 2, 0, 2)
                }
                binding.orderItemsContainer.addView(line)
            }
        }

        private fun formatTime(createdAt: String): String {
            if (createdAt.isBlank()) return ""
            val out = SimpleDateFormat("HH:mm", Locale.getDefault())
            return try {
                val date = when {
                    createdAt.length >= 19 && createdAt[10] == 'T' ->
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(createdAt.take(19))
                    createdAt.length >= 19 ->
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(createdAt.take(19))
                    else -> null
                }
                if (date != null) out.format(date) else createdAt.take(16)
            } catch (_: Exception) {
                createdAt.take(16)
            }
        }

        private fun statusLabel(status: String): String {
            val ctx = binding.root.context
            return when (status.lowercase()) {
                "pending" -> ctx.getString(R.string.status_pending)
                "confirmed" -> ctx.getString(R.string.status_confirmed)
                "preparing" -> ctx.getString(R.string.status_preparing)
                "ready" -> ctx.getString(R.string.status_ready)
                "completed" -> ctx.getString(R.string.status_completed)
                "delivered" -> ctx.getString(R.string.status_delivered)
                "cancelled" -> ctx.getString(R.string.status_cancelled)
                else -> status
            }
        }

        private fun orderTypeLabel(ctx: android.content.Context, type: String): String {
            return when (type.trim().lowercase()) {
                "delivery" -> ctx.getString(R.string.order_type_delivery)
                "pickup" -> ctx.getString(R.string.order_type_pickup)
                "dine_in", "dine-in" -> ctx.getString(R.string.order_type_dine_in)
                else -> type
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.website_id == b.website_id && a.order_number == b.order_number
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}
