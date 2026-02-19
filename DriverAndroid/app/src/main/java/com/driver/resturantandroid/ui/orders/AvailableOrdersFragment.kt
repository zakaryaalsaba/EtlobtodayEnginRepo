package com.driver.resturantandroid.ui.orders

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.driver.resturantandroid.R
import com.driver.resturantandroid.databinding.FragmentAvailableOrdersBinding
import com.driver.resturantandroid.util.SessionManager
import com.driver.resturantandroid.util.SoundHelper
import com.driver.resturantandroid.viewmodel.DriverStatusViewModel
import com.driver.resturantandroid.viewmodel.OrdersViewModel

class AvailableOrdersFragment : Fragment() {
    companion object {
        private const val TAG = "AvailableOrdersFragment"
    }
    private var _binding: FragmentAvailableOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersViewModel: OrdersViewModel
    private lateinit var driverStatusViewModel: DriverStatusViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var ordersAdapter: AvailableOrdersAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvailableOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "👁️ onViewCreated called")
        sessionManager = SessionManager(requireContext())
        ordersViewModel = ViewModelProvider(this)[OrdersViewModel::class.java]
        driverStatusViewModel = ViewModelProvider(requireActivity())[DriverStatusViewModel::class.java]
        Log.d(TAG, "✅ ViewModels initialized")
        
        setupRecyclerView()
        setupObservers()
        setupOnlineStatusObserver()
        loadOrdersIfOnline()
        
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "🔄 Swipe refresh triggered")
            loadOrdersIfOnline()
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "▶️ onResume called")
        loadOrdersIfOnline()
    }
    
    private fun setupRecyclerView() {
        ordersAdapter = AvailableOrdersAdapter(
            onAcceptClick = { order ->
                acceptOrder(order.id)
            },
            onRejectClick = { order ->
                rejectOrder(order.id)
            }
        )
        
        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
        }
    }
    
    private fun setupObservers() {
        Log.d(TAG, "👀 Setting up LiveData observers")
        
        ordersViewModel.availableOrders.observe(viewLifecycleOwner) { orders ->
            Log.d(TAG, "📬 LiveData observer triggered with ${orders.size} orders")
            
            // Use ViewModel's isOnline so we stay in sync when user toggles (sessionManager updates after API success)
            val isOnline = driverStatusViewModel.isOnline.value == true
            
            if (orders.isEmpty()) {
                Log.d(TAG, "📭 No orders - showing empty state (online: $isOnline)")
                binding.recyclerViewOrders.visibility = View.GONE
                updateEmptyStateCards(isOnline = isOnline, hasOrders = false)
            } else {
                Log.d(TAG, "📦 ${orders.size} orders received - updating RecyclerView")
                orders.forEach { order ->
                    Log.d(TAG, "  → Order: ${order.order_number}, Status: ${order.status}, Type: ${order.order_type}")
                }
                binding.cardEmptyStateOnline.visibility = View.GONE
                binding.cardEmptyStateOffline.visibility = View.GONE
                binding.recyclerViewOrders.visibility = View.VISIBLE
                ordersAdapter.submitList(orders)
                Log.d(TAG, "✅ RecyclerView updated with ${orders.size} orders")
            }
        }
        
        ordersViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "⏳ Loading state changed: $isLoading")
            binding.swipeRefresh.isRefreshing = isLoading
        }
        
        ordersViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "❌ Error observed: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        
        // When online status changes, always refresh empty state so card updates (online→offline and offline→online)
        driverStatusViewModel.isOnline.observe(viewLifecycleOwner) { isOnline ->
            val hasOrders = !(ordersViewModel.availableOrders.value.isNullOrEmpty())
            if (!hasOrders) {
                binding.recyclerViewOrders.visibility = View.GONE
                updateEmptyStateCards(isOnline = (isOnline == true), hasOrders = false)
            }
        }
        
        // Play sound when a new order is received
        Log.d(TAG, "🔔 Setting up newOrderReceived observer")
        ordersViewModel.newOrderReceived.observe(viewLifecycleOwner) { shouldPlay ->
            Log.d(TAG, "🔔 newOrderReceived observer triggered: shouldPlay=$shouldPlay")
            if (shouldPlay == true) {
                Log.d(TAG, "🔔 ✅ New order received - calling SoundHelper.playOrderReceivedSound()")
                try {
                    SoundHelper.playOrderReceivedSound(requireContext())
                    Log.d(TAG, "🔔 ✅ SoundHelper.playOrderReceivedSound() called successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception calling SoundHelper: ${e.message}", e)
                    e.printStackTrace()
                }
                // Reset the event flag
                ordersViewModel.onNewOrderSoundPlayed()
                Log.d(TAG, "🔔 ✅ onNewOrderSoundPlayed() called")
            } else {
                Log.d(TAG, "🔕 newOrderReceived is false or null - not playing sound")
            }
        }
        Log.d(TAG, "🔔 ✅ newOrderReceived observer set up")
        
        Log.d(TAG, "✅ All observers set up")
    }
    
    /** Updates the empty state cards based on online status and whether there are orders. */
    private fun updateEmptyStateCards(isOnline: Boolean, hasOrders: Boolean) {
        if (hasOrders) return
        binding.cardEmptyStateOnline.visibility = if (isOnline) View.VISIBLE else View.GONE
        binding.cardEmptyStateOffline.visibility = if (isOnline) View.GONE else View.VISIBLE
    }
    
    /** Observes driver online status: start Firebase when online, stop and clear when offline. */
    private fun setupOnlineStatusObserver() {
        driverStatusViewModel.isOnline.observe(viewLifecycleOwner) { isOnline ->
            val token = sessionManager.getAuthToken()
            if (isOnline == true && token != null) {
                Log.d(TAG, "🟢 Driver is online - starting Firebase order listener")
                ordersViewModel.startListeningToFirebaseOrders(token)
            } else {
                Log.d(TAG, "🔴 Driver is offline - stopping Firebase listener, orders cleared")
                ordersViewModel.stopListeningToFirebaseOrders()
            }
        }
    }

    /** Start Firebase listener only when driver is online; otherwise stop listener and show no orders. */
    private fun loadOrdersIfOnline() {
        if (!sessionManager.isOnline()) {
            Log.d(TAG, "⏸️ Driver offline - Firebase not started; available orders empty")
            ordersViewModel.stopListeningToFirebaseOrders()
            return
        }
        val token = sessionManager.getAuthToken()
        if (token != null) {
            Log.d(TAG, "✅ Driver online, starting Firebase listener")
            ordersViewModel.startListeningToFirebaseOrders(token)
        } else {
            Log.e(TAG, "❌ No auth token found!")
        }
    }
    
    private fun acceptOrder(orderId: Int) {
        val token = sessionManager.getAuthToken()
        if (token != null) {
            ordersViewModel.acceptOrder(
                orderId,
                token,
                onSuccess = {
                    Toast.makeText(requireContext(), "Order accepted!", Toast.LENGTH_SHORT).show()
                    // Take driver to Active Delivery (order data loaded from MySQL as now)
                    findNavController().navigate(R.id.nav_active_delivery)
                },
                onError = { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun rejectOrder(orderId: Int) {
        val token = sessionManager.getAuthToken()
        if (token != null) {
            ordersViewModel.rejectOrder(
                orderId,
                token,
                onSuccess = {
                    Toast.makeText(requireContext(), "Order rejected", Toast.LENGTH_SHORT).show()
                    // Order is removed from list in ViewModel.removeOrderFromAvailable
                },
                onError = { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

