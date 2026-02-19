package com.mnsf.resturantandroid.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Restaurant

class RestaurantAdapter(
    private val onItemClick: (Restaurant) -> Unit
) : ListAdapter<Restaurant, RestaurantAdapter.RestaurantViewHolder>(RestaurantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RestaurantViewHolder(
        itemView: View,
        private val onItemClick: (Restaurant) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvRestaurantName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tvDescription)
        private val addressTextView: TextView = itemView.findViewById(R.id.tvAddress)
        private val logoImageView: ImageView = itemView.findViewById(R.id.ivLogo)
        private val chipFreeDelivery: Chip = itemView.findViewById(R.id.chipFreeDelivery)

        fun bind(restaurant: Restaurant) {
            try {
                val context = itemView.context
                nameTextView.text = restaurant.restaurant_name
                descriptionTextView.text = restaurant.description ?: context.getString(R.string.no_description_available)

                val addressLayout = itemView.findViewById<View>(R.id.layoutAddress)
                restaurant.address?.let { address ->
                    addressTextView.text = address
                    addressLayout?.visibility = View.VISIBLE
                } ?: run {
                    addressLayout?.visibility = View.GONE
                }

                val isFreeDelivery = (restaurant.delivery_fee ?: 0.0) == 0.0
                chipFreeDelivery.visibility = if (isFreeDelivery) View.VISIBLE else View.GONE
                if (isFreeDelivery) {
                    chipFreeDelivery.text = context.getString(R.string.free_delivery_badge)
                }

                restaurant.logo_url?.let { logoUrl ->
                    val emulatorUrl = com.mnsf.resturantandroid.utils.UrlHelper.convertUrlForAndroid(logoUrl)
                        .replace("localhost", "10.0.2.2")
                    Glide.with(itemView.context)
                        .load(emulatorUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .centerCrop()
                        .into(logoImageView)
                } ?: logoImageView.setImageResource(R.drawable.ic_launcher_foreground)

                itemView.setOnClickListener { onItemClick(restaurant) }
            } catch (e: Exception) {
                android.util.Log.e("RestaurantAdapter", "bind: Error binding restaurant ${restaurant.id}", e)
                e.printStackTrace()
            }
        }
    }

    class RestaurantDiffCallback : DiffUtil.ItemCallback<Restaurant>() {
        override fun areItemsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean {
            return oldItem == newItem
        }
    }
}

