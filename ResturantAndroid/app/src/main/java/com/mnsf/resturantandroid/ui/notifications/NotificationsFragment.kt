package com.mnsf.resturantandroid.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.FragmentNotificationsBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.NotificationRepository
import com.mnsf.resturantandroid.ui.order.OrderConfirmationActivity
import com.mnsf.resturantandroid.util.SessionManager

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            Log.d("NotificationsFragment", "onCreateView: Starting")
            sessionManager = SessionManager(requireContext())
            
            val notificationRepository = NotificationRepository(RetrofitClient.apiService, sessionManager)
            notificationsViewModel = ViewModelProvider(
                this,
                NotificationsViewModelFactory(notificationRepository, sessionManager)
            )[NotificationsViewModel::class.java]

            _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
            val root: View = binding.root

            setupRecyclerView()
            setupObservers()
            setupClickListeners()
            
            if (sessionManager.isLoggedIn()) {
                notificationsViewModel.loadNotifications()
            } else {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewNotifications.visibility = View.GONE
            }
            
            return root
        } catch (e: Exception) {
            Log.e("NotificationsFragment", "onCreateView: Error", e)
            throw e
        }
    }
    
    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onItemClick = { notification ->
                // Navigate to order details if order_id is available
                notification.orderId?.let { orderId ->
                    // You can navigate to order details here
                    // For now, we'll just mark it as read
                    notificationsViewModel.markAsRead(notification.id)
                } ?: run {
                    // Just mark as read if no order
                    notificationsViewModel.markAsRead(notification.id)
                }
            }
        )
        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotifications.adapter = notificationAdapter
    }
    
    private fun setupObservers() {
        notificationsViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            // Update notification count
            val unreadCount = notifications.count { !it.isRead }
            if (unreadCount > 0) {
                binding.tvNotificationCount.text = 
                    resources.getQuantityString(
                        R.plurals.notification_count,
                        unreadCount,
                        unreadCount
                    )
            } else {
                binding.tvNotificationCount.text = getString(R.string.all_read)
            }
            
            if (notifications.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewNotifications.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.recyclerViewNotifications.visibility = View.VISIBLE
                notificationAdapter.submitList(notifications)
            }
        }
        
        notificationsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        notificationsViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("NotificationsFragment", "Error: $it")
                // You can show a toast or error message here
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnMarkAllRead.setOnClickListener {
            notificationsViewModel.markAllAsRead()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class NotificationsViewModelFactory(
    private val notificationRepository: NotificationRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(notificationRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}