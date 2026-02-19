package com.order.storecontroller.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.order.storecontroller.R
import com.order.storecontroller.databinding.FragmentHomeBinding
import com.order.storecontroller.util.SoundHelper

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var ordersAdapter: OrdersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ordersAdapter = OrdersAdapter()
        binding.recyclerOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOrders.adapter = ordersAdapter
        setupStatusChips()
        setupObservers()
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.startListening()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupStatusChips() {
        val statuses = listOf(
            null to getString(R.string.filter_all_statuses),
            "pending" to getString(R.string.status_pending),
            "confirmed" to getString(R.string.status_confirmed),
            "preparing" to getString(R.string.status_preparing),
            "ready" to getString(R.string.status_ready),
            "completed" to getString(R.string.status_completed),
            "delivered" to getString(R.string.status_delivered),
            "cancelled" to getString(R.string.status_cancelled)
        )
        statuses.forEach { (value, label) ->
            val chip = Chip(requireContext(), null, R.style.Widget_StoreController_FilterChip).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                setOnClickListener {
                    viewModel.setStatusFilter(value)
                    updateOrdersList()
                }
            }
            binding.statusChipGroup.addView(chip)
        }
        binding.statusChipGroup.check(binding.statusChipGroup.getChildAt(0).id)
    }

    private fun setupObservers() {
        viewModel.allOrders.observe(viewLifecycleOwner) {
            updateOrdersList()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), getString(R.string.error) + ": $it", Toast.LENGTH_SHORT).show() }
        }
        viewModel.newOrderReceived.observe(viewLifecycleOwner) { play ->
            if (play == true) {
                try {
                    SoundHelper.playOrderReceivedSound(requireContext())
                } catch (_: Exception) { }
                viewModel.onNewOrderSoundPlayed()
            }
        }
    }

    private fun updateOrdersList() {
        val list = viewModel.getFilteredOrders()
        ordersAdapter.submitList(list)
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerOrders.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
