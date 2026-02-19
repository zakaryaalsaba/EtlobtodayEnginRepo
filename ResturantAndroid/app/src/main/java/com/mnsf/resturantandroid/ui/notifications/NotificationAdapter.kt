package com.mnsf.resturantandroid.ui.notifications

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Notification
import com.mnsf.resturantandroid.databinding.ItemNotificationBinding
import com.mnsf.resturantandroid.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onItemClick: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            val context = binding.root.context
            (binding.root as? MaterialCardView)?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            val localizedContext = LocaleHelper.getLocalizedContext(context)
            val resources = localizedContext.resources
            
            binding.apply {
                // Translate title and message based on locale
                val (translatedTitle, translatedMessage) = translateNotification(
                    notification,
                    resources
                )
                
                tvTitle.text = translatedTitle
                tvMessage.text = translatedMessage
                
                // Show restaurant name if available
                if (!notification.restaurantName.isNullOrEmpty()) {
                    tvRestaurantName.text = notification.restaurantName
                    tvRestaurantName.visibility = View.VISIBLE
                } else {
                    tvRestaurantName.visibility = View.GONE
                }
                
                // Format date using localized context
                try {
                    val locale = localizedContext.resources.configuration.locales[0]
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", locale)
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", locale)
                    val date = inputFormat.parse(notification.createdAt)
                    date?.let {
                        tvDate.text = outputFormat.format(it)
                    } ?: run {
                        tvDate.text = notification.createdAt
                    }
                } catch (e: Exception) {
                    tvDate.text = notification.createdAt
                }
                
                // Show unread indicator
                if (notification.isRead) {
                    viewUnreadIndicator.visibility = View.GONE
                    root.alpha = 0.7f
                } else {
                    viewUnreadIndicator.visibility = View.VISIBLE
                    root.alpha = 1.0f
                }
                
                root.setOnClickListener {
                    onItemClick(notification)
                }
            }
        }
        
        private fun translateNotification(
            notification: Notification,
            resources: android.content.res.Resources
        ): Pair<String, String> {
            // If it's an order update notification and we have a status, translate it
            if (notification.type == "order_update" && !notification.status.isNullOrEmpty()) {
                val status = notification.status.lowercase()
                val orderNumber = notification.orderNumber ?: ""
                
                // Get status string resource ID
                val statusStringResId = when (status) {
                    "pending" -> R.string.status_pending
                    "confirmed" -> R.string.status_confirmed
                    "preparing" -> R.string.status_preparing
                    "ready" -> R.string.status_ready
                    "completed" -> R.string.status_completed
                    "cancelled" -> R.string.status_cancelled
                    else -> null
                }
                
                // Get message string resource ID
                val messageResId = when (status) {
                    "pending" -> R.string.notification_order_pending
                    "confirmed" -> R.string.notification_order_confirmed
                    "preparing" -> R.string.notification_order_preparing
                    "ready" -> R.string.notification_order_ready
                    "completed" -> R.string.notification_order_completed
                    "cancelled" -> R.string.notification_order_cancelled
                    else -> null
                }
                
                if (statusStringResId != null && messageResId != null) {
                    val statusText = resources.getString(statusStringResId)
                    val message = resources.getString(messageResId)
                    val title = resources.getString(R.string.notification_order_title, orderNumber, statusText)
                    return Pair(title, message)
                }
            }
            
            // Fallback to original title and message if translation not available
            return Pair(notification.title, notification.message)
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}

