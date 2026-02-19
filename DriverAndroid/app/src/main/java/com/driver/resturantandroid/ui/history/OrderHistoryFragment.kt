package com.driver.resturantandroid.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.driver.resturantandroid.databinding.FragmentOrderHistoryBinding
import com.driver.resturantandroid.util.SessionManager
import com.driver.resturantandroid.viewmodel.OrdersViewModel

class OrderHistoryFragment : Fragment() {
    private var _binding: FragmentOrderHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersViewModel: OrdersViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var historyAdapter: OrderHistoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        ordersViewModel = ViewModelProvider(this)[OrdersViewModel::class.java]
        
        setupRecyclerView()
        setupObservers()
        loadHistory()
        
        binding.swipeRefresh.setOnRefreshListener {
            loadHistory()
        }
    }
    
    private fun setupRecyclerView() {
        historyAdapter = OrderHistoryAdapter()
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }
    
    private fun setupObservers() {
        ordersViewModel.orderHistory.observe(viewLifecycleOwner) { orders ->
            if (orders.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.VISIBLE
                historyAdapter.submitList(orders)
            }
        }
        
        ordersViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }
    
    private fun loadHistory() {
        val token = sessionManager.getAuthToken()
        if (token != null) {
            ordersViewModel.loadOrderHistory(token)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

