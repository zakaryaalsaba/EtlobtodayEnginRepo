package com.order.resturantandroid.ui.dashboard

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.order.resturantandroid.R
import com.order.resturantandroid.data.model.Order
import com.order.resturantandroid.databinding.ItemNewSpotlightOrderBinding
import com.order.resturantandroid.service.GlobalOrderAlertManager
import com.order.resturantandroid.util.ENGLISH_NUMBER_LOCALE
import com.order.resturantandroid.util.formatElapsedMmSs
import com.order.resturantandroid.util.parseOrderCreatedAtMillis
import com.order.resturantandroid.util.withEnglishDigits

class NewSpotlightOrdersAdapter(
    private val onConfirm: (Order) -> Unit,
    private val onViewDetails: (Order) -> Unit
) : ListAdapter<Order, NewSpotlightOrdersAdapter.SpotlightVH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotlightVH {
        val binding = ItemNewSpotlightOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpotlightVH(binding, onConfirm, onViewDetails)
    }

    override fun onBindViewHolder(holder: SpotlightVH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: SpotlightVH) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    class SpotlightVH(
        private val binding: ItemNewSpotlightOrderBinding,
        private val onConfirm: (Order) -> Unit,
        private val onViewDetails: (Order) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val handler = Handler(Looper.getMainLooper())
        private var ticker: Runnable? = null

        fun unbind() {
            ticker?.let { handler.removeCallbacks(it) }
            ticker = null
        }

        fun bind(order: Order) {
            unbind()
            binding.tvSpotOrderNumber.text = "#${order.orderNumber.withEnglishDigits()}"
            val count = order.getItemsList().size
            val qtyTemplate = binding.root.resources.getQuantityString(R.plurals.items_count, count)
            binding.tvSpotOrderItems.text = String.format(ENGLISH_NUMBER_LOCALE, qtyTemplate, count)

            val createdMs = parseOrderCreatedAtMillis(order.createdAt)
            if (createdMs != null) {
                val r = object : Runnable {
                    override fun run() {
                        val elapsed = System.currentTimeMillis() - createdMs
                        binding.tvSpotOrderElapsed.text = formatElapsedMmSs(elapsed)
                        handler.postDelayed(this, 1000L)
                    }
                }
                ticker = r
                handler.post(r)
            } else {
                binding.tvSpotOrderElapsed.text = "--:--"
            }

            binding.btnSpotConfirm.setOnClickListener {
                GlobalOrderAlertManager.stopActiveAlarm()
                onConfirm(order)
            }
            binding.btnSpotViewDetails.setOnClickListener {
                GlobalOrderAlertManager.stopActiveAlarm()
                onViewDetails(order)
            }
            binding.root.setOnClickListener {
                GlobalOrderAlertManager.stopActiveAlarm()
                onViewDetails(order)
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem == newItem
    }
}

