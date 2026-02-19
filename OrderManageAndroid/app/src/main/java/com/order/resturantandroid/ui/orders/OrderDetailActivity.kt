package com.order.resturantandroid.ui.orders

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.order.resturantandroid.R
import com.order.resturantandroid.databinding.ActivityOrderDetailBinding
import com.order.resturantandroid.util.SessionManager
import com.order.resturantandroid.util.CurrencyFormatter
import kotlinx.coroutines.launch

class OrderDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderDetailBinding
    private val viewModel: OrderDetailViewModel by viewModels {
        AndroidViewModelFactory.getInstance(application)
    }
    private lateinit var sessionManager: SessionManager
    private lateinit var itemsAdapter: OrderItemsAdapter
    private var orderId: Int = -1
    private var orderNumber: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityOrderDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
        sessionManager = SessionManager(this)
        orderId = intent.getIntExtra("order_id", -1)
        orderNumber = intent.getStringExtra("order_number")
        
        if (orderNumber.isNullOrBlank() && orderId == -1) {
            android.util.Log.e("OrderDetailActivity", "Invalid order ID or number received")
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        android.util.Log.d("OrderDetailActivity", "Loading order with ID: $orderId, Number: $orderNumber")
            
            setupToolbar()
            setupRecyclerView() // Initialize with default currency, will be updated when order loads
            setupObservers()
            setupClickListeners()
            
            // Load order using order number if available, otherwise we'll need to get it from the order list
            if (!orderNumber.isNullOrBlank()) {
                viewModel.loadOrder(orderNumber!!)
            } else {
                Toast.makeText(this, "Order number not available", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("OrderDetailActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Hide default title
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Set custom centered title
        binding.toolbarTitle.text = getString(R.string.order_details)
    }
    
    private fun setupRecyclerView(order: com.order.resturantandroid.data.model.Order? = null) {
        itemsAdapter = OrderItemsAdapter(
            currencyCode = order?.currencyCode ?: "USD",
            currencySymbolPosition = order?.currencySymbolPosition ?: "before"
        )
        binding.recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(this@OrderDetailActivity)
            adapter = itemsAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.order.observe(this) { order ->
            try {
                order?.let {
                    displayOrder(it)
                } ?: run {
                    Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error displaying order: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            try {
                binding.progressBar.visibility = if (isLoading == true) android.view.View.VISIBLE else android.view.View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.statusUpdateSuccess.observe(this) { success ->
            if (success == true) {
                Toast.makeText(this, getString(R.string.status_updated), Toast.LENGTH_SHORT).show()
                if (!orderNumber.isNullOrBlank()) {
                    viewModel.loadOrder(orderNumber!!)
                }
            }
        }
    }
    
    private fun displayOrder(order: com.order.resturantandroid.data.model.Order) {
        binding.apply {
            try {
                tvOrderNumber.text = order.orderNumber ?: "N/A"
                tvCustomerName.text = order.customerName ?: "N/A"
                tvCustomerPhone.text = order.customerPhone ?: getString(R.string.not_available)
                tvCustomerAddress.text = order.customerAddress ?: getString(R.string.not_available)
                tvOrderType.text = (order.orderType ?: "").replaceFirstChar { it.uppercaseChar() }
                
                // Payment method display
                val tvPaymentMethod = binding.root.findViewById<android.widget.TextView>(R.id.tvPaymentMethod)
                if (order.paymentMethod != null && order.paymentMethod.isNotEmpty()) {
                    val paymentMethod = order.paymentMethod.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                    tvPaymentMethod?.text = paymentMethod
                } else {
                    tvPaymentMethod?.text = getString(R.string.not_available)
                }
                
                // Notes card visibility
                val cardNotes = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardNotes)
                if (order.notes != null && order.notes.isNotEmpty() && order.notes != getString(R.string.no_notes)) {
                    cardNotes?.visibility = android.view.View.VISIBLE
                } else {
                    cardNotes?.visibility = android.view.View.GONE
                }
                
                // Status chip styling
                val status = (order.status ?: "").lowercase()
                tvStatus.text = status.replaceFirstChar { it.uppercaseChar() }
                
                val statusColor = when (status) {
                    "pending" -> android.graphics.Color.parseColor("#F59E0B")
                    "confirmed", "preparing" -> android.graphics.Color.parseColor("#3B82F6")
                    "ready" -> android.graphics.Color.parseColor("#10B981")
                    "completed" -> android.graphics.Color.parseColor("#059669")
                    "cancelled" -> android.graphics.Color.parseColor("#EF4444")
                    else -> android.graphics.Color.parseColor("#6B7280")
                }
                
                // Set chip background color with alpha
                val chipColor = android.graphics.Color.argb(30, 
                    android.graphics.Color.red(statusColor),
                    android.graphics.Color.green(statusColor),
                    android.graphics.Color.blue(statusColor)
                )
                tvStatus.setChipBackgroundColorResource(android.R.color.transparent)
                tvStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(chipColor)
                tvStatus.setTextColor(statusColor)
                
                // Display order breakdown
                val currencyCode = order.currencyCode ?: "USD"
                val symbolPosition = order.currencySymbolPosition ?: "before"
                
                // Subtotal (sum of all item subtotals)
                val subtotal = order.getSubtotal()
                val formattedSubtotal = CurrencyFormatter.formatAmount(
                    subtotal.toString(),
                    currencyCode,
                    symbolPosition
                )
                tvSubtotal.text = formattedSubtotal
                
                // Tax
                val tax = order.tax?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0
                if (tax > 0) {
                    val formattedTax = CurrencyFormatter.formatAmount(
                        tax.toString(),
                        currencyCode,
                        symbolPosition
                    )
                    tvTax.text = formattedTax
                    layoutTax.visibility = android.view.View.VISIBLE
                } else {
                    layoutTax.visibility = android.view.View.GONE
                }
                
                // Delivery Fee
                val deliveryFee = order.deliveryFees?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0
                if (deliveryFee > 0) {
                    val formattedDeliveryFee = CurrencyFormatter.formatAmount(
                        deliveryFee.toString(),
                        currencyCode,
                        symbolPosition
                    )
                    tvDeliveryFee.text = formattedDeliveryFee
                    layoutDeliveryFee.visibility = android.view.View.VISIBLE
                } else {
                    layoutDeliveryFee.visibility = android.view.View.GONE
                }
                
                // Total amount
                android.util.Log.d("OrderDetailActivity", "Order currency: $currencyCode, position: $symbolPosition, total: ${order.totalAmount}")
                val formattedTotal = CurrencyFormatter.formatAmount(
                    order.totalAmount ?: "0.00",
                    currencyCode,
                    symbolPosition
                )
                android.util.Log.d("OrderDetailActivity", "Formatted total: $formattedTotal")
                tvTotalAmount.text = formattedTotal
                
                tvNotes.text = order.notes ?: getString(R.string.no_notes)
                
                // Update adapter with currency info from order
                itemsAdapter = OrderItemsAdapter(
                    currencyCode = order.currencyCode ?: "USD",
                    currencySymbolPosition = order.currencySymbolPosition ?: "before"
                )
                binding.recyclerViewItems.adapter = itemsAdapter
                
                // Display items
                itemsAdapter.submitList(order.getItemsList())
                
                // Update button visibility based on status
                updateButtonVisibility(order.status ?: "pending")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@OrderDetailActivity, "Error displaying order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateButtonVisibility(status: String) {
        binding.apply {
            try {
                when ((status ?: "").lowercase()) {
                    "pending" -> {
                        btnAccept.visibility = android.view.View.VISIBLE
                        btnReject.visibility = android.view.View.VISIBLE
                        btnUpdateStatus.visibility = android.view.View.GONE
                    }
                    "cancelled", "completed" -> {
                        btnAccept.visibility = android.view.View.GONE
                        btnReject.visibility = android.view.View.GONE
                        btnUpdateStatus.visibility = android.view.View.GONE
                    }
                    else -> {
                        btnAccept.visibility = android.view.View.GONE
                        btnReject.visibility = android.view.View.GONE
                        btnUpdateStatus.visibility = android.view.View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Default to showing update status button
                btnAccept.visibility = android.view.View.GONE
                btnReject.visibility = android.view.View.GONE
                btnUpdateStatus.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnAccept.setOnClickListener {
            viewModel.updateOrderStatus(orderId, "confirmed")
        }
        
        binding.btnReject.setOnClickListener {
            showRejectDialog()
        }
        
        binding.btnUpdateStatus.setOnClickListener {
            showStatusUpdateDialog()
        }
        
        binding.btnPrint.setOnClickListener {
            viewModel.order.value?.let { order ->
                // TODO: Implement printing
                Toast.makeText(this, "Printing feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showRejectDialog() {
        // First show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle(R.string.reject_order)
            .setMessage(R.string.reject_order_confirmation)
            .setPositiveButton(R.string.reject_order) { _, _ ->
                // After confirmation, show reason selection
                showRejectReasonDialog()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showRejectReasonDialog() {
        val reasons = arrayOf("Out of stock", "Kitchen closed", "Other")
        AlertDialog.Builder(this)
            .setTitle(R.string.reject_order)
            .setItems(reasons) { _, which ->
                viewModel.updateOrderStatus(orderId, "cancelled")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showStatusUpdateDialog() {
        val statuses = arrayOf(
            getString(R.string.status_confirmed),
            getString(R.string.status_preparing),
            getString(R.string.status_ready),
            getString(R.string.status_completed)
        )
        
        AlertDialog.Builder(this)
            .setTitle(R.string.update_status)
            .setItems(statuses) { _, which ->
                val status = when (which) {
                    0 -> "confirmed"
                    1 -> "preparing"
                    2 -> "ready"
                    3 -> "completed"
                    else -> return@setItems
                }
                viewModel.updateOrderStatus(orderId, status)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

