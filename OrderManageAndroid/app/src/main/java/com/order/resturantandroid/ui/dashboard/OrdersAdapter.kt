package com.order.resturantandroid.ui.dashboard

import android.graphics.Color
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.order.resturantandroid.R
import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.databinding.ItemOrderBinding
import com.order.resturantandroid.util.CurrencyFormatter
import com.order.resturantandroid.util.ENGLISH_NUMBER_LOCALE
import com.order.resturantandroid.util.formatElapsedMmSs
import com.order.resturantandroid.util.formatOrderPlacedAt
import com.order.resturantandroid.util.parseOrderCreatedAtMillis
import com.order.resturantandroid.util.withEnglishDigits
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    override fun onViewRecycled(holder: OrderViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    class OrderViewHolder(
        private val binding: ItemOrderBinding,
        private val onOrderClick: (Order) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun unbind() {
            binding.chronometerElapsed.stop()
            binding.chronometerElapsed.setOnChronometerTickListener(null)
        }

        fun bind(order: Order) {
            unbind()

            binding.apply {
                tvOrderNumber.text = order.orderNumber?.takeIf { it.isNotBlank() }?.withEnglishDigits() ?: "—"
                tvCustomerName.text = order.customerName?.takeIf { it.isNotBlank() } ?: "—"
                tvOrderType.text = (order.orderType ?: "").replaceFirstChar { it.uppercaseChar() }

                val statusRaw = (order.status ?: "").lowercase(Locale.getDefault())
                tvStatus.text = statusRaw.replaceFirstChar { it.uppercaseChar() }

                val formattedTotal = CurrencyFormatter.formatAmount(
                    order.totalAmount ?: "0.00",
                    order.currencyCode,
                    order.currencySymbolPosition
                )
                tvTotalAmount.text = formattedTotal

                val createdMs = parseOrderCreatedAtMillis(order.createdAt)
                val placedLabel = formatOrderPlacedAt(order.createdAt, createdMs)
                tvOrderTime.text = placedLabel.ifBlank { "—" }

                try {
                    val itemsList = order.getItemsList()
                    val itemsCount = itemsList.size
                    val template = binding.root.resources.getQuantityString(R.plurals.items_count, itemsCount)
                    tvItemsCount.text = String.format(ENGLISH_NUMBER_LOCALE, template, itemsCount)
                } catch (e: Exception) {
                    val t = binding.root.resources.getQuantityString(R.plurals.items_count, 0)
                    tvItemsCount.text = String.format(ENGLISH_NUMBER_LOCALE, t, 0)
                }

                val paymentMethodLayout = root.findViewById<ViewGroup>(R.id.layoutPaymentMethod)
                val tvPaymentMethod = root.findViewById<android.widget.TextView>(R.id.tvPaymentMethod)
                if (!order.paymentMethod.isNullOrEmpty()) {
                    val paymentMethod = order.paymentMethod.replace("_", " ")
                        .replaceFirstChar { it.uppercaseChar() }
                    tvPaymentMethod?.text = paymentMethod
                    paymentMethodLayout?.visibility = View.VISIBLE
                } else {
                    paymentMethodLayout?.visibility = View.GONE
                }

                val statusColor = when (statusRaw) {
                    "pending" -> Color.parseColor("#F59E0B")
                    "confirmed", "preparing" -> Color.parseColor("#3B82F6")
                    "ready" -> Color.parseColor("#10B981")
                    "completed" -> Color.parseColor("#059669")
                    "cancelled" -> Color.parseColor("#EF4444")
                    else -> Color.parseColor("#6B7280")
                }
                val chipColor = Color.argb(
                    30,
                    Color.red(statusColor),
                    Color.green(statusColor),
                    Color.blue(statusColor)
                )
                tvStatus.setChipBackgroundColorResource(android.R.color.transparent)
                tvStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(chipColor)
                tvStatus.setTextColor(statusColor)

                setupElapsedTimer(chronometerElapsed, createdMs)

                root.setOnClickListener { onOrderClick(order) }
            }
        }

        private fun setupElapsedTimer(chronometer: Chronometer, createdMs: Long?) {
            if (createdMs == null) {
                chronometer.visibility = View.GONE
                return
            }
            chronometer.visibility = View.VISIBLE
            chronometer.base =
                SystemClock.elapsedRealtime() - (System.currentTimeMillis() - createdMs)

            val green = binding.root.context.getColor(R.color.success)
            val orange = binding.root.context.getColor(R.color.warning)
            val red = binding.root.context.getColor(R.color.error)

            fun applyElapsedColors(elapsedMs: Long) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
                val color = when {
                    minutes < 5L -> green
                    minutes < 15L -> orange
                    else -> red
                }
                chronometer.setTextColor(color)
            }

            chronometer.setOnChronometerTickListener { c ->
                val elapsed = SystemClock.elapsedRealtime() - c.base
                c.text = formatElapsedMmSs(elapsed)
                applyElapsedColors(elapsed)
            }
            val initialElapsed = SystemClock.elapsedRealtime() - chronometer.base
            chronometer.text = formatElapsedMmSs(initialElapsed)
            applyElapsedColors(initialElapsed)
            chronometer.start()
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
