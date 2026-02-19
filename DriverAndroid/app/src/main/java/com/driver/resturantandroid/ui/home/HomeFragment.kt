package com.driver.resturantandroid.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.driver.resturantandroid.R
import com.driver.resturantandroid.databinding.FragmentHomeBinding
import com.driver.resturantandroid.util.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        sessionManager = SessionManager(requireContext())

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupUI()
        setupClickListeners()
        setupObservers()

        return root
    }

    private fun setupUI() {
        // Set greeting based on time of day
        val greeting = getGreeting()
        binding.tvGreeting.text = greeting

        // Set driver name
        val driverName = sessionManager.getDriverName() ?: "Driver"
        binding.tvDriverName.text = driverName

        // Set driver status
        val isOnline = sessionManager.isOnline()
        binding.tvDriverStatus.text = if (isOnline) getString(R.string.online) else getString(R.string.offline)
        binding.switchStatus.isChecked = isOnline

        // Stats will be updated from Firebase orders
        binding.tvTodayOrdersCount.text = "0"
        binding.tvTodayEarnings.text = "$0.00"
    }
    
    private fun setupObservers() {
        // Observe real-time orders from Firebase (read-only)
        homeViewModel.availableOrders.observe(viewLifecycleOwner) { orders ->
            updateStats(orders)
        }
        
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Can show loading indicator if needed
        }
        
        homeViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.widget.Toast.makeText(requireContext(), "Firebase error: $it", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Update stats based on Firebase orders (today's orders count).
     * Earnings calculation would require order completion status - can be enhanced later.
     */
    private fun updateStats(orders: List<com.driver.resturantandroid.data.model.Order>) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayOrders = orders.filter { order ->
            order.created_at.startsWith(today)
        }
        
        binding.tvTodayOrdersCount.text = todayOrders.size.toString()
        
        // Calculate today's earnings (only from completed/delivered orders)
        // Note: This is a simplified calculation - actual earnings might need to come from MySQL
        val todayEarnings = todayOrders
            .filter { it.status == "completed" || it.status == "delivered" }
            .sumOf { order ->
                order.delivery_fees?.toDoubleOrNull() ?: 0.0
            }
        binding.tvTodayEarnings.text = String.format("$%.2f", todayEarnings)
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    private fun setupClickListeners() {
        // Quick action buttons
        binding.btnViewOrders.setOnClickListener {
            findNavController().navigate(R.id.nav_orders)
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.nav_history)
        }

        // Status switch
        binding.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            binding.tvDriverStatus.text = if (isChecked) getString(R.string.online) else getString(R.string.offline)
            // Status change will be handled by MainActivity's switch
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}