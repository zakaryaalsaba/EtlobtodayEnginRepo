package com.order.resturantandroid.ui.delivery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.order.resturantandroid.R
import com.order.resturantandroid.data.remote.DeliveryZone
import com.order.resturantandroid.databinding.ItemZoneBinding

class ZonesAdapter(
    private val getButtonLabel: (zoneId: Int) -> String,
    private val getButtonState: (zoneId: Int) -> String, // "idle" | "cancel" | "processing"
    private val getRestaurantDisplayName: () -> String,
    private val onRequestDriverClick: (DeliveryZone) -> Unit,
    private val onCancelClick: (DeliveryZone) -> Unit
) : ListAdapter<DeliveryZone, ZonesAdapter.ZoneViewHolder>(ZoneDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val binding = ItemZoneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ZoneViewHolder(binding, getButtonLabel, getButtonState, getRestaurantDisplayName, onRequestDriverClick, onCancelClick)
    }
    
    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ZoneViewHolder(
        private val binding: ItemZoneBinding,
        private val getButtonLabel: (zoneId: Int) -> String,
        private val getButtonState: (zoneId: Int) -> String,
        private val getRestaurantDisplayName: () -> String,
        private val onRequestDriverClick: (DeliveryZone) -> Unit,
        private val onCancelClick: (DeliveryZone) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(zone: DeliveryZone) {
            binding.apply {
                btnRequestDriver.text = getButtonLabel(zone.id)
                btnRequestDriver.setOnClickListener {
                    when (getButtonState(zone.id)) {
                        "idle" -> onRequestDriverClick(zone)
                        "cancel" -> onCancelClick(zone)
                        else -> { /* processing: no-op */ }
                    }
                }
                // Zone name (prefer English, fallback to Arabic)
                val zoneName = zone.zoneNameEn ?: zone.zoneNameAr ?: "Zone ${zone.id}"
                tvZoneName.text = zoneName
                
                // Restaurant name (language-based)
                tvRestaurantName.text = getRestaurantDisplayName()
                
                // Price
                if (!zone.price.isNullOrEmpty()) {
                    layoutPrice.visibility = android.view.View.VISIBLE
                    tvPrice.text = zone.price
                } else {
                    layoutPrice.visibility = android.view.View.GONE
                }
                
                // Status
                val status = (zone.status ?: "active").lowercase()
                chipStatus.text = status.replaceFirstChar { it.uppercaseChar() }
                
                val statusColor = when (status) {
                    "active" -> android.graphics.Color.parseColor("#10B981")
                    "inactive" -> android.graphics.Color.parseColor("#6B7280")
                    else -> android.graphics.Color.parseColor("#6B7280")
                }
                
                val chipColor = android.graphics.Color.argb(30,
                    android.graphics.Color.red(statusColor),
                    android.graphics.Color.green(statusColor),
                    android.graphics.Color.blue(statusColor)
                )
                chipStatus.setChipBackgroundColorResource(android.R.color.transparent)
                chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(chipColor)
                chipStatus.setTextColor(statusColor)
            }
        }
    }
    
    class ZoneDiffCallback : DiffUtil.ItemCallback<DeliveryZone>() {
        override fun areItemsTheSame(oldItem: DeliveryZone, newItem: DeliveryZone): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: DeliveryZone, newItem: DeliveryZone): Boolean {
            return oldItem == newItem
        }
    }
}
