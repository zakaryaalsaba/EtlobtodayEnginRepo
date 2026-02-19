package com.mnsf.resturantandroid.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.databinding.ItemOfferHorizontalBinding
import com.mnsf.resturantandroid.utils.UrlHelper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class OfferHorizontalAdapter(
    private val onOfferClick: (Offer) -> Unit
) : ListAdapter<Offer, OfferHorizontalAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOfferHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onOfferClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemOfferHorizontalBinding,
        private val onOfferClick: (Offer) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(offer: Offer) {
            val imageUrl = offer.first_product_image_url?.takeIf { it.isNotBlank() }
            if (!imageUrl.isNullOrBlank()) {
                val url = UrlHelper.convertUrlForAndroid(imageUrl)
                Glide.with(binding.root.context)
                    .load(url)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivOfferProduct)
            } else {
                Glide.with(binding.root.context)
                    .load(R.mipmap.ic_launcher)
                    .centerCrop()
                    .into(binding.ivOfferProduct)
            }
            binding.tvOfferTitle.text = offer.title
            binding.tvOfferDescription.text = offer.description
            binding.tvOfferDescription.visibility = if (offer.description.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.tvRestaurantName.text = offer.restaurant_name ?: ""
            // Show "For one day only" or "For X days only" from (valid_until - valid_from)
            val validFrom = offer.valid_from
            val validUntil = offer.valid_until
            if (!validFrom.isNullOrBlank() && !validUntil.isNullOrBlank()) {
                try {
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val from = fmt.parse(validFrom)
                    val until = fmt.parse(validUntil)
                    if (from != null && until != null) {
                        val days = TimeUnit.MILLISECONDS.toDays(until.time - from.time).toInt() + 1
                        binding.tvValidUntil.text = if (days <= 1) {
                            binding.root.context.getString(R.string.offer_for_one_day_only)
                        } else {
                            binding.root.context.getString(R.string.offer_for_days_only, days)
                        }
                    } else {
                        binding.tvValidUntil.text = ""
                    }
                } catch (_: Exception) {
                    binding.tvValidUntil.text = ""
                }
            } else {
                binding.tvValidUntil.text = ""
            }
            binding.root.setOnClickListener { onOfferClick(offer) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Offer>() {
        override fun areItemsTheSame(old: Offer, new: Offer) = old.id == new.id
        override fun areContentsTheSame(old: Offer, new: Offer) = old == new
    }
}
