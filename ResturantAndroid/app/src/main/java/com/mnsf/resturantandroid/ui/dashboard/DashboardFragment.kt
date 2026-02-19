package com.mnsf.resturantandroid.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.FragmentDashboardBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.ui.order.OrderHistoryActivity
import com.mnsf.resturantandroid.ui.order.OrderAdapter
import com.mnsf.resturantandroid.ui.order.RestaurantCurrencyInfo
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.viewmodel.OrderViewModel
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var orderViewModel: OrderViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var restaurantRepository: RestaurantRepository
    private val restaurantCurrencyMap = mutableMapOf<Int, RestaurantCurrencyInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            Log.d("DashboardFragment", "onCreateView: Starting")
            sessionManager = SessionManager(requireContext())
            Log.d("DashboardFragment", "onCreateView: SessionManager created")
            
            val orderRepository = OrderRepository(RetrofitClient.apiService, sessionManager)
            Log.d("DashboardFragment", "onCreateView: OrderRepository created")
            
            orderViewModel = ViewModelProvider(
                this,
                OrderViewModelFactory(orderRepository, sessionManager)
            )[OrderViewModel::class.java]
            Log.d("DashboardFragment", "onCreateView: OrderViewModel created")

            restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
            Log.d("DashboardFragment", "onCreateView: RestaurantRepository created")

            _binding = FragmentDashboardBinding.inflate(inflater, container, false)
            val root: View = binding.root
            Log.d("DashboardFragment", "onCreateView: Binding inflated")

            setupRecyclerView()
            Log.d("DashboardFragment", "onCreateView: RecyclerView setup complete")
            
            setupObservers()
            Log.d("DashboardFragment", "onCreateView: Observers setup complete")
            
            setupClickListeners()
            Log.d("DashboardFragment", "onCreateView: Click listeners setup complete")
            
            if (sessionManager.isLoggedIn()) {
                Log.d("DashboardFragment", "onCreateView: User logged in, loading orders")
                orderViewModel.loadOrders()
            } else {
                Log.d("DashboardFragment", "onCreateView: User not logged in")
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.tvRecentOrdersTitle.visibility = View.GONE
                binding.recyclerViewOrders.visibility = View.GONE
            }

            Log.d("DashboardFragment", "onCreateView: Completed successfully")
            return root
        } catch (e: Exception) {
            Log.e("DashboardFragment", "onCreateView: Fatal error", e)
            e.printStackTrace()
            throw e
        }
    }
    
    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onItemClick = { order ->
                // Navigate to order details
                val intent = Intent(requireContext(), com.mnsf.resturantandroid.ui.order.OrderConfirmationActivity::class.java)
                intent.putExtra("order", order as java.io.Serializable)
                startActivity(intent)
            },
            onReorderClick = null, // No reorder in dashboard, only in archive
            onTrackClick = { order ->
                // Navigate to order tracking
                val intent = Intent(requireContext(), com.mnsf.resturantandroid.ui.order.OrderTrackingActivity::class.java)
                intent.putExtra("order", order as java.io.Serializable)
                intent.putExtra("order_number", order.order_number)
                startActivity(intent)
            },
            restaurantCurrencyMap = emptyMap(),
            showReorderButton = false,
            showTrackButton = true
        )
        binding.recyclerViewOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewOrders.adapter = orderAdapter
    }
    
    private fun setupObservers() {
        orderViewModel.orders.observe(viewLifecycleOwner) { orders ->
            // Show only active orders (pending, confirmed, preparing, ready) - limit to 5 most recent
            val activeOrders = orders.filter { 
                it.status.lowercase() in listOf("pending", "confirmed", "preparing", "ready")
            }.take(5)
            
            if (activeOrders.isNotEmpty()) {
                // Show recent orders section
                binding.layoutEmptyState.visibility = View.GONE
                binding.tvRecentOrdersTitle.visibility = View.VISIBLE
                binding.recyclerViewOrders.visibility = View.VISIBLE
                orderAdapter.submitList(activeOrders)
                
                // Load currency info for restaurants
                loadRestaurantCurrencies(activeOrders.map { it.website_id }.distinct())
            } else {
                // Show empty state with encouragement message
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.tvRecentOrdersTitle.visibility = View.GONE
                binding.recyclerViewOrders.visibility = View.GONE
            }
        }
        
        orderViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        orderViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnViewAllOrders.setOnClickListener {
            startActivity(Intent(requireContext(), OrderHistoryActivity::class.java))
        }
    }
    
    private fun loadRestaurantCurrencies(websiteIds: List<Int>) {
        lifecycleScope.launch {
            websiteIds.forEach { websiteId ->
                // Check if we already have currency info for this restaurant
                if (!restaurantCurrencyMap.containsKey(websiteId)) {
                    try {
                        val result = restaurantRepository.getRestaurant(websiteId)
                        result.onSuccess { restaurant ->
                            restaurantCurrencyMap[websiteId] = RestaurantCurrencyInfo(
                                currencyCode = restaurant.currency_code,
                                currencySymbolPosition = restaurant.currency_symbol_position
                            )
                            // Update adapter with new currency map
                            orderAdapter.updateCurrencyMap(restaurantCurrencyMap.toMap())
                        }.onFailure {
                            // If restaurant fetch fails, use default currency
                            restaurantCurrencyMap[websiteId] = RestaurantCurrencyInfo(
                                currencyCode = null,
                                currencySymbolPosition = null
                            )
                            orderAdapter.updateCurrencyMap(restaurantCurrencyMap.toMap())
                        }
                    } catch (e: Exception) {
                        // If restaurant fetch fails, use default currency
                        restaurantCurrencyMap[websiteId] = RestaurantCurrencyInfo(
                            currencyCode = null,
                            currencySymbolPosition = null
                        )
                        orderAdapter.updateCurrencyMap(restaurantCurrencyMap.toMap())
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class OrderViewModelFactory(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderViewModel(orderRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}