package com.order.resturantandroid.ui.statistics

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import com.order.resturantandroid.R
import com.order.resturantandroid.databinding.ActivityStatisticsBinding
import com.order.resturantandroid.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private val viewModel: StatisticsViewModel by viewModels {
        AndroidViewModelFactory.getInstance(application)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupObservers()
        setupDateFilter()
        
        // Load statistics
        viewModel.loadStatistics()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.toolbarTitle.text = getString(R.string.statistics)
    }
    
    private fun setupObservers() {
        viewModel.statistics.observe(this) { stats ->
            stats?.let {
                binding.tvTotalOrders.text = it.totalOrders.toString()
                
                val formattedRevenue = CurrencyFormatter.formatAmount(
                    it.totalRevenue.toString(),
                    it.currencyCode,
                    it.currencySymbolPosition
                )
                binding.tvTotalRevenue.text = formattedRevenue
                
                displayOrdersByStatus(it.ordersByStatus)
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // You can add a progress indicator here if needed
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun displayOrdersByStatus(ordersByStatus: Map<String, Int>) {
        val container = binding.statusContainer
        container.removeAllViews()
        
        if (ordersByStatus.isEmpty()) {
            return
        }
        
        val statusLabels = mapOf(
            "pending" to getString(R.string.status_pending),
            "confirmed" to getString(R.string.status_confirmed),
            "preparing" to getString(R.string.status_preparing),
            "ready" to getString(R.string.status_ready),
            "completed" to getString(R.string.status_completed),
            "cancelled" to getString(R.string.status_cancelled)
        )
        
        ordersByStatus.forEach { (status, count) ->
            val statusLabel = statusLabels[status.lowercase()] ?: status.replaceFirstChar { it.uppercaseChar() }
            
            val cardView = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_8)
                }
                radius = resources.getDimension(R.dimen.card_corner_radius_medium)
                cardElevation = resources.getDimension(R.dimen.card_elevation_low)
                setCardBackgroundColor(resources.getColor(R.color.surface_variant, theme))
                
                val layout = android.widget.LinearLayout(this@StatisticsActivity).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(
                        resources.getDimensionPixelSize(R.dimen.spacing_16),
                        resources.getDimensionPixelSize(R.dimen.spacing_12),
                        resources.getDimensionPixelSize(R.dimen.spacing_16),
                        resources.getDimensionPixelSize(R.dimen.spacing_12)
                    )
                }
                
                val statusTextView = android.widget.TextView(this@StatisticsActivity).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    text = statusLabel
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_primary, theme))
                }
                
                val countTextView = android.widget.TextView(this@StatisticsActivity).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = count.toString()
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(resources.getColor(R.color.primary, theme))
                }
                
                layout.addView(statusTextView)
                layout.addView(countTextView)
                addView(layout)
            }
            
            container.addView(cardView)
        }
    }
    
    private fun setupDateFilter() {
        binding.btnFilterDate.setOnClickListener {
            showDateRangePicker()
        }
        
        binding.btnClearFilter.setOnClickListener {
            viewModel.clearDateFilter()
            binding.btnFilterDate.text = getString(R.string.filter_by_date)
            binding.btnClearFilter.visibility = android.view.View.GONE
        }
    }
    
    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_date_range))
            .build()
        
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            selection?.let {
                val startDate = it.first
                val endDate = it.second
                
                // Set end of day for end date
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = endDate ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endOfDay = calendar.timeInMillis
                
                viewModel.setDateFilter(startDate, endOfDay)
                
                // Update button text
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val startStr = startDate?.let { dateFormat.format(Date(it)) } ?: ""
                val endStr = endOfDay.let { dateFormat.format(Date(it)) }
                binding.btnFilterDate.text = "$startStr - $endStr"
                binding.btnClearFilter.visibility = android.view.View.VISIBLE
            }
        }
        
        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }
}
