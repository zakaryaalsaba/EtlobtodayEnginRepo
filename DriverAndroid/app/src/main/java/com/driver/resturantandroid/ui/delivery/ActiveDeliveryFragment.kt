package com.driver.resturantandroid.ui.delivery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.driver.resturantandroid.R
import com.driver.resturantandroid.data.model.Order
import com.driver.resturantandroid.databinding.FragmentActiveDeliveryBinding
import com.driver.resturantandroid.repository.OrderRepository
import com.driver.resturantandroid.util.CurrencyFormatter
import com.driver.resturantandroid.util.NavigationHelper
import com.driver.resturantandroid.util.SessionManager
import com.driver.resturantandroid.viewmodel.OrdersViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ActiveDeliveryFragment : Fragment() {
    private var _binding: FragmentActiveDeliveryBinding? = null
    private val binding get() = _binding!!
    private lateinit var ordersViewModel: OrdersViewModel
    private lateinit var sessionManager: SessionManager
    private val orderRepository = OrderRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveDeliveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        ordersViewModel = ViewModelProvider(this)[OrdersViewModel::class.java]

        setupClickListeners()
        setupObservers()
        loadActiveOrder()
    }

    override fun onResume() {
        super.onResume()
        loadActiveOrder()
    }

    private fun cardRoot(): View? = binding.layoutActiveOrder.getChildAt(0)

    private fun setupClickListeners() {
        val root = cardRoot() ?: return
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNavigatePickup)?.setOnClickListener {
            ordersViewModel.assignedOrders.value?.firstOrNull()?.let { order -> navigateToPickup(order) }
        }
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNavigateDelivery)?.setOnClickListener {
            ordersViewModel.assignedOrders.value?.firstOrNull()?.let { order -> navigateToDelivery(order) }
        }
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkArrived)?.setOnClickListener {
            updateStatus("arrived_at_pickup")
        }
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkPickedUp)?.setOnClickListener {
            updateStatus("picked_up")
        }
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkDelivered)?.setOnClickListener {
            updateStatus("delivered")
        }
    }

    private fun setupObservers() {
        ordersViewModel.assignedOrders.observe(viewLifecycleOwner) { orders ->
            val activeOrder = orders.firstOrNull()
            if (activeOrder != null && !activeOrder.order_number.isNullOrEmpty()) {
                displayOrder(activeOrder)
                binding.layoutNoActiveOrder.visibility = View.GONE
                binding.layoutActiveOrder.visibility = View.VISIBLE
            } else {
                binding.layoutNoActiveOrder.visibility = View.VISIBLE
                binding.layoutActiveOrder.visibility = View.GONE
            }
        }

        ordersViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadActiveOrder() {
        val token = sessionManager.getAuthToken()
        if (token != null) {
            ordersViewModel.loadAssignedOrders(token)
        }
    }

    private fun displayOrder(order: Order) {
        val cardRoot = cardRoot() ?: return
        val res = requireContext().resources

        android.util.Log.d("ActiveDelivery", "Displaying order: ${order.order_number}, status: ${order.status}")

        // Safety check: if order_number is null, don't display (order data incomplete)
        if (order.order_number.isNullOrEmpty()) {
            android.util.Log.e("ActiveDelivery", "Order data incomplete, skipping display")
            return
        }

        cardRoot.findViewById<android.widget.TextView>(R.id.tvOrderNumber)?.text =
            res.getString(R.string.order_number, order.order_number)
        cardRoot.findViewById<android.widget.TextView>(R.id.tvDeliveryAddress)?.text =
            order.customer_address ?: res.getString(R.string.delivery_location)

        // Handle null total_amount safely
        val totalAmount = order.total_amount ?: "0.00"
        val formattedAmount = CurrencyFormatter.formatAmount(
            totalAmount,
            order.currency_code,
            order.currency_symbol_position
        )
        cardRoot.findViewById<android.widget.TextView>(R.id.tvTotalAmount)?.text = formattedAmount

        val deliveryFeesCard = cardRoot.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardDeliveryFees)
        val tvDeliveryFees = cardRoot.findViewById<android.widget.TextView>(R.id.tvDeliveryFees)
        if (order.delivery_fees != null && order.delivery_fees.isNotEmpty()) {
            val value = order.delivery_fees.toDoubleOrNull() ?: 0.0
            if (value > 0) {
                tvDeliveryFees?.text = CurrencyFormatter.formatAmount(
                    order.delivery_fees,
                    order.currency_code,
                    order.currency_symbol_position
                )
                deliveryFeesCard?.visibility = View.VISIBLE
            } else {
                deliveryFeesCard?.visibility = View.GONE
            }
        } else {
            deliveryFeesCard?.visibility = View.GONE
        }

        val cardTip = cardRoot.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardTip)
        val tvTip = cardRoot.findViewById<android.widget.TextView>(R.id.tvTip)
        if (order.tip != null && order.tip.isNotEmpty()) {
            val tipValue = order.tip.toDoubleOrNull() ?: 0.0
            if (tipValue > 0) {
                tvTip?.text = CurrencyFormatter.formatAmount(
                    order.tip,
                    order.currency_code,
                    order.currency_symbol_position
                )
                cardTip?.visibility = View.VISIBLE
            } else {
                cardTip?.visibility = View.GONE
            }
        } else {
            cardTip?.visibility = View.GONE
        }

        val cardPaymentMethod = cardRoot.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPaymentMethod)
        val tvPaymentMethod = cardRoot.findViewById<android.widget.TextView>(R.id.tvPaymentMethod)
        if (order.payment_method != null && order.payment_method.isNotEmpty()) {
            tvPaymentMethod?.text = order.payment_method.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
            cardPaymentMethod?.visibility = View.VISIBLE
        } else {
            cardPaymentMethod?.visibility = View.GONE
        }

        cardRoot.findViewById<com.google.android.material.chip.Chip>(R.id.chipStatus)?.text =
            order.status.replace("_", " ").replaceFirstChar { it.uppercaseChar() }

        if (order.restaurant != null) {
            cardRoot.findViewById<android.widget.TextView>(R.id.tvRestaurantName)?.text =
                order.restaurant.name ?: res.getString(R.string.restaurant)
            cardRoot.findViewById<android.widget.TextView>(R.id.tvPickupAddress)?.text =
                order.restaurant.address ?: res.getString(R.string.restaurant_location)
        } else {
            cardRoot.findViewById<android.widget.TextView>(R.id.tvRestaurantName)?.text = res.getString(R.string.restaurant)
            cardRoot.findViewById<android.widget.TextView>(R.id.tvPickupAddress)?.text = res.getString(R.string.restaurant_location)
        }

        updateButtonVisibility(cardRoot, order.status)
    }

    /**
     * Flow: Navigate to Restaurant (enabled after accept) -> Mark Arrived at Pickup -> Mark Picked Up -> Navigate to Customer -> Mark Delivered.
     * MySQL statuses: arrived_at_pickup, picked_up, delivered (via PUT /api/orders/:orderId/status).
     */
    private fun updateButtonVisibility(cardRoot: View, status: String) {
        val btnNavigatePickup = cardRoot.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNavigatePickup)
        val btnNavigateDelivery = cardRoot.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNavigateDelivery)
        val btnMarkArrived = cardRoot.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkArrived)
        val btnMarkPickedUp = cardRoot.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkPickedUp)
        val btnMarkDelivered = cardRoot.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkDelivered)

        android.util.Log.d("ActiveDelivery", "Updating button visibility for status: $status")

        val isCompleted = status == "completed" || status == "delivered"

        // Navigate to Restaurant: Available from start (when order is accepted) until completed
        btnNavigatePickup?.isEnabled = !isCompleted
        btnNavigatePickup?.visibility = if (isCompleted) View.GONE else View.VISIBLE

        // Navigate to Customer: Only visible and enabled after order is picked up (status = "picked_up")
        val showNavigateToCustomer = status == "picked_up" && !isCompleted
        btnNavigateDelivery?.isEnabled = showNavigateToCustomer
        btnNavigateDelivery?.visibility = if (showNavigateToCustomer) View.VISIBLE else View.GONE
        android.util.Log.d("ActiveDelivery", "Navigate to Customer: enabled=$showNavigateToCustomer, visible=${showNavigateToCustomer}")

        // Flow: Mark Arrived at Pickup -> Mark Picked Up -> Mark Delivered
        // Mark Arrived: before driver has arrived (accepted_by_driver/confirmed/preparing/ready)
        val canMarkArrived = status == "accepted_by_driver" || status == "confirmed" || status == "preparing" || status == "ready"
        btnMarkArrived?.isEnabled = canMarkArrived
        android.util.Log.d("ActiveDelivery", "Mark Arrived: enabled=$canMarkArrived (status=$status)")
        
        // Mark Picked Up: only after driver has marked "Arrived at Pickup" (MySQL: arrived_at_pickup)
        val canMarkPickedUp = status == "arrived_at_pickup"
        btnMarkPickedUp?.isEnabled = canMarkPickedUp
        android.util.Log.d("ActiveDelivery", "Mark Picked Up: enabled=$canMarkPickedUp (status=$status)")
        
        // Mark Delivered: only when order is picked up (MySQL: picked_up) - enabled when ready to deliver
        val canMarkDelivered = status == "picked_up"
        btnMarkDelivered?.isEnabled = canMarkDelivered
        android.util.Log.d("ActiveDelivery", "Mark Delivered: enabled=$canMarkDelivered (status=$status)")
    }

    private fun navigateToPickup(order: Order) {
        if (order.restaurant != null) {
            if (order.restaurant.latitude != null && order.restaurant.longitude != null) {
                NavigationHelper.navigateToCoordinates(
                    requireContext(),
                    order.restaurant.latitude!!,
                    order.restaurant.longitude!!
                )
            } else {
                val address = order.restaurant.address ?: getString(R.string.restaurant_location)
                NavigationHelper.navigateToAddress(requireContext(), address)
            }
        } else {
            lifecycleScope.launch {
                orderRepository.getRestaurant(order.website_id)
                    .onSuccess { restaurant ->
                        if (restaurant.latitude != null && restaurant.longitude != null) {
                            NavigationHelper.navigateToCoordinates(
                                requireContext(),
                                restaurant.latitude,
                                restaurant.longitude
                            )
                        } else {
                            val address = restaurant.address ?: getString(R.string.restaurant_location)
                            NavigationHelper.navigateToAddress(requireContext(), address)
                        }
                    }
                    .onFailure {
                        NavigationHelper.navigateToAddress(requireContext(), getString(R.string.restaurant_location))
                    }
            }
        }
    }

    private fun navigateToDelivery(order: Order) {
        if (order.delivery_latitude != null && order.delivery_longitude != null) {
            NavigationHelper.navigateToCoordinates(
                requireContext(),
                order.delivery_latitude,
                order.delivery_longitude
            )
        } else {
            val address = order.customer_address ?: getString(R.string.delivery_location)
            NavigationHelper.navigateToAddress(requireContext(), address)
        }
    }

    private fun updateStatus(status: String) {
        val token = sessionManager.getAuthToken()
        val order = ordersViewModel.assignedOrders.value?.firstOrNull()

        if (token != null && order != null) {
            android.util.Log.d("ActiveDelivery", "Updating order ${order.id} status to: $status")
            ordersViewModel.updateOrderStatus(
                order.id,
                status,
                token,
                onSuccess = {
                    android.util.Log.d("ActiveDelivery", "Status update successful, refreshing order...")
                    
                    // If order was marked as delivered, show success dialog and navigate to Available Orders
                    if (status == "delivered") {
                        showDeliveryCompletedDialog()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.status_updated), Toast.LENGTH_SHORT).show()
                        // Give a small delay to ensure database update is committed before refreshing
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            loadActiveOrder()
                        }, 500)
                    }
                },
                onError = { error ->
                    android.util.Log.e("ActiveDelivery", "Status update failed: $error")
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            android.util.Log.e("ActiveDelivery", "Cannot update status: token=${token != null}, order=${order != null}")
        }
    }

    private fun showDeliveryCompletedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delivery_completed_title))
            .setMessage(getString(R.string.delivery_completed_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                // Navigate to Available Orders after user dismisses dialog
                findNavController().navigate(R.id.nav_orders)
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
