package com.driver.resturantandroid.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.driver.resturantandroid.R
import com.driver.resturantandroid.data.model.Order
import com.driver.resturantandroid.databinding.ItemAvailableOrderBinding
import com.driver.resturantandroid.util.NavigationHelper
import com.driver.resturantandroid.util.CurrencyFormatter

class AvailableOrdersAdapter(
    private val onAcceptClick: (Order) -> Unit,
    private val onRejectClick: (Order) -> Unit
) : ListAdapter<Order, AvailableOrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemAvailableOrderBinding.inflate(
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
        private val binding: ItemAvailableOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            binding.apply {
                val context = root.context
                val resources = context.resources
                // Currency from order (Firebase/API); default JOD for region
                val currencyCode = order.currency_code ?: "JOD"
                val symbolPosition = order.currency_symbol_position ?: "before"

                // Order number only, one line (customer info hidden)
                tvOrderNumber.text = resources.getString(R.string.order_number, order.order_number)
                tvDeliveryAddress.text = order.customer_address ?: resources.getString(R.string.delivery_location)

                // Total: single label "Total" in layout; amount with stored currency only
                val formattedTotal = CurrencyFormatter.formatAmount(
                    order.total_amount,
                    currencyCode,
                    symbolPosition
                )
                tvTotalAmount.text = formattedTotal

                // Payment method (under Total)
                val paymentMethodCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPaymentMethod)
                val tvPaymentMethod = binding.root.findViewById<android.widget.TextView>(R.id.tvPaymentMethod)
                if (order.payment_method != null && order.payment_method.isNotEmpty()) {
                    val paymentMethod = order.payment_method.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                    tvPaymentMethod?.text = paymentMethod
                    paymentMethodCard?.visibility = View.VISIBLE
                } else {
                    paymentMethodCard?.visibility = View.GONE
                }

                // Delivery fees with stored currency
                val deliveryFeesCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardDeliveryFees)
                val tvDeliveryFees = binding.root.findViewById<android.widget.TextView>(R.id.tvDeliveryFees)
                if (order.delivery_fees != null && order.delivery_fees.isNotEmpty()) {
                    val deliveryFeesValue = order.delivery_fees.toDoubleOrNull() ?: 0.0
                    if (deliveryFeesValue > 0) {
                        tvDeliveryFees?.text = CurrencyFormatter.formatAmount(
                            order.delivery_fees,
                            currencyCode,
                            symbolPosition
                        )
                        deliveryFeesCard?.visibility = View.VISIBLE
                    } else {
                        deliveryFeesCard?.visibility = View.GONE
                    }
                } else {
                    deliveryFeesCard?.visibility = View.GONE
                }

                // Tip with stored currency; fancy card (thank you Captain)
                val cardTip = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardTip)
                val tvTip = binding.root.findViewById<android.widget.TextView>(R.id.tvTip)
                if (order.tip != null && order.tip.isNotEmpty()) {
                    val tipValue = order.tip.toDoubleOrNull() ?: 0.0
                    if (tipValue > 0) {
                        tvTip?.text = CurrencyFormatter.formatAmount(
                            order.tip,
                            currencyCode,
                            symbolPosition
                        )
                        cardTip?.visibility = View.VISIBLE
                    } else {
                        cardTip?.visibility = View.GONE
                    }
                } else {
                    cardTip?.visibility = View.GONE
                }

                // Restaurant: icon in layout; name and address with location icon (no phone)
                tvRestaurantName.text = order.restaurant?.name ?: resources.getString(R.string.restaurant)
                tvPickupAddress.text = order.restaurant?.address ?: resources.getString(R.string.restaurant_location)

                // Distance/Est. Time card is hidden in layout (cardDistanceTime visibility gone)

                btnAccept.setOnClickListener { onAcceptClick(order) }
                btnReject.setOnClickListener { onRejectClick(order) }
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

