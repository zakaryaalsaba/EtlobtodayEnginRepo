package com.mnsf.resturantandroid.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Order
import com.mnsf.resturantandroid.util.CurrencyFormatter

data class RestaurantCurrencyInfo(
    val currencyCode: String?,
    val currencySymbolPosition: String?
)

class OrderAdapter(
    private val onItemClick: (Order) -> Unit,
    private val onReorderClick: ((Order) -> Unit)? = null,
    private val onTrackClick: ((Order) -> Unit)? = null,
    private var restaurantCurrencyMap: Map<Int, RestaurantCurrencyInfo> = emptyMap(),
    private var showReorderButton: Boolean = false,
    private var showTrackButton: Boolean = true
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view, onItemClick, onReorderClick, onTrackClick)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position), restaurantCurrencyMap, showReorderButton, showTrackButton)
    }
    
    fun updateCurrencyMap(currencyMap: Map<Int, RestaurantCurrencyInfo>) {
        restaurantCurrencyMap = currencyMap
        notifyDataSetChanged()
    }
    
    fun setShowReorderButton(show: Boolean) {
        showReorderButton = show
        notifyDataSetChanged()
    }
    
    fun setShowTrackButton(show: Boolean) {
        showTrackButton = show
        notifyDataSetChanged()
    }

    class OrderViewHolder(
        itemView: View,
        private val onItemClick: (Order) -> Unit,
        private val onReorderClick: ((Order) -> Unit)?,
        private val onTrackClick: ((Order) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val orderNumberTextView: TextView = itemView.findViewById(R.id.tvOrderNumber)
        private val orderDateTextView: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val orderStatusTextView: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val totalAmountTextView: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val reorderButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnReorder)
        private val trackButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnTrackOrder)

        fun bind(order: Order, currencyMap: Map<Int, RestaurantCurrencyInfo>, showReorder: Boolean, showTrack: Boolean) {
            val context = itemView.context
            (itemView as? MaterialCardView)?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))

            orderNumberTextView.text = order.order_number
            orderDateTextView.text = order.created_at ?: ""
            
            // Format total amount with currency
            val currencyInfo = currencyMap[order.website_id]
            val formattedTotal = CurrencyFormatter.formatPrice(
                order.total_amount,
                currencyInfo?.currencyCode,
                currencyInfo?.currencySymbolPosition
            )
            totalAmountTextView.text = formattedTotal
            
            // Format status
            val statusText = when (order.status.lowercase()) {
                "pending" -> context.getString(R.string.status_pending)
                "confirmed" -> context.getString(R.string.status_confirmed)
                "preparing" -> context.getString(R.string.status_preparing)
                "ready" -> context.getString(R.string.status_ready)
                "completed" -> context.getString(R.string.status_completed)
                "cancelled" -> context.getString(R.string.status_cancelled)
                else -> order.status
            }
            orderStatusTextView.text = statusText
            
            // Set status color
            val statusColor = when (order.status.lowercase()) {
                "pending" -> android.graphics.Color.parseColor("#F59E0B")
                "confirmed" -> android.graphics.Color.parseColor("#3B82F6")
                "preparing" -> android.graphics.Color.parseColor("#8B5CF6")
                "ready" -> android.graphics.Color.parseColor("#10B981")
                "completed" -> android.graphics.Color.parseColor("#059669")
                "cancelled" -> android.graphics.Color.parseColor("#EF4444")
                else -> android.graphics.Color.parseColor("#6B7280")
            }
            orderStatusTextView.setTextColor(statusColor)
            
            // Show/hide track button for active orders
            val isActive = order.status.lowercase() in listOf("pending", "confirmed", "preparing", "ready")
            val isArchived = order.status.lowercase() in listOf("completed", "cancelled")
            
            if (showTrack && onTrackClick != null && isActive) {
                trackButton.visibility = android.view.View.VISIBLE
                trackButton.setOnClickListener {
                    onTrackClick.invoke(order)
                }
            } else {
                trackButton.visibility = android.view.View.GONE
            }
            
            // Show/hide reorder button for archived orders
            if (showReorder && onReorderClick != null && isArchived) {
                reorderButton.visibility = android.view.View.VISIBLE
                reorderButton.setOnClickListener {
                    onReorderClick?.invoke(order)
                }
            } else {
                reorderButton.visibility = android.view.View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(order)
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

