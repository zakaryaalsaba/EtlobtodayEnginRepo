package com.order.resturantandroid.ui.delivery

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.order.resturantandroid.R
import com.order.resturantandroid.data.remote.DeliveryZone
import com.order.resturantandroid.data.remote.RetrofitClient
import com.order.resturantandroid.data.remote.RequestDriverBody
import com.order.resturantandroid.databinding.ActivityRequestDeliveryBinding
import com.order.resturantandroid.util.LocaleHelper
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RequestDeliveryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequestDeliveryBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var zonesAdapter: ZonesAdapter

    /** Current zones list used to find position for notifyItemChanged */
    private var currentZones: List<DeliveryZone> = emptyList()
    /** zoneId -> "idle" | "cancel_5".."cancel_1" | "processing" */
    private val zoneButtonState = mutableMapOf<Int, String>()
    private val countdownJobs = mutableMapOf<Int, Job>()
    /** Restaurant display name (language-based) for zone cards */
    private var restaurantDisplayName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle.text = getString(R.string.request_delivery)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        setupRecyclerView()
        loadZones()
    }
    
    private fun setupRecyclerView() {
        zonesAdapter = ZonesAdapter(
            getButtonLabel = { zoneId -> getButtonLabel(zoneId) },
            getButtonState = { zoneId -> getButtonState(zoneId) },
            getRestaurantDisplayName = { restaurantDisplayName },
            onRequestDriverClick = { zone -> onRequestDriverClick(zone) },
            onCancelClick = { zone -> onCancelCountdownClick(zone) }
        )
        binding.recyclerViewZones.apply {
            layoutManager = LinearLayoutManager(this@RequestDeliveryActivity)
            adapter = zonesAdapter
        }
    }
    
    private fun getButtonLabel(zoneId: Int): String {
        val state = zoneButtonState[zoneId] ?: return getString(R.string.request_driver)
        return when {
            state == "processing" -> getString(R.string.processing)
            state.startsWith("cancel_") -> getString(R.string.cancel_countdown, state.removePrefix("cancel_").toIntOrNull() ?: 0)
            else -> getString(R.string.request_driver)
        }
    }
    
    private fun getButtonState(zoneId: Int): String {
        val state = zoneButtonState[zoneId] ?: return "idle"
        return when {
            state.startsWith("cancel_") -> "cancel"
            state == "processing" -> "processing"
            else -> "idle"
        }
    }
    
    private fun notifyZoneItem(zoneId: Int) {
        val pos = currentZones.indexOfFirst { it.id == zoneId }
        if (pos >= 0) zonesAdapter.notifyItemChanged(pos)
    }
    
    private fun onRequestDriverClick(zone: DeliveryZone) {
        countdownJobs[zone.id]?.cancel()
        zoneButtonState[zone.id] = "cancel_5"
        notifyZoneItem(zone.id)
        
        countdownJobs[zone.id] = lifecycleScope.launch {
            for (sec in 5 downTo 1) {
                zoneButtonState[zone.id] = "cancel_$sec"
                notifyZoneItem(zone.id)
                if (sec > 1) delay(1000L)
            }
            zoneButtonState[zone.id] = "processing"
            notifyZoneItem(zone.id)
            requestDriverApi(zone)
        }
    }
    
    private fun onCancelCountdownClick(zone: DeliveryZone) {
        countdownJobs[zone.id]?.cancel()
        countdownJobs.remove(zone.id)
        zoneButtonState.remove(zone.id)
        notifyZoneItem(zone.id)
    }
    
    private fun requestDriverApi(zone: DeliveryZone) {
        val token = sessionManager.getAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.requestDriver(
                    RequestDriverBody(zone.id),
                    "Bearer $token"
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@RequestDeliveryActivity,
                        "Driver requested for ${zone.zoneNameEn ?: zone.zoneNameAr ?: "zone"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    zoneButtonState.remove(zone.id)
                    notifyZoneItem(zone.id)
                } else {
                    zoneButtonState.remove(zone.id)
                    notifyZoneItem(zone.id)
                    val msg = response.errorBody()?.string()?.let { body ->
                        try { org.json.JSONObject(body).optString("error", response.message()) } catch (_: Exception) { response.message() }
                    } ?: response.message()
                    Toast.makeText(this@RequestDeliveryActivity, msg ?: "Request failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                zoneButtonState.remove(zone.id)
                notifyZoneItem(zone.id)
                Toast.makeText(this@RequestDeliveryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadZones() {
        val token = sessionManager.getAuthToken()
        
        if (token == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getRestaurantZones("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val deliveryCompany = data.deliveryCompany
                    val zones = data.zones
                    val restaurant = data.restaurant
                    
                    restaurantDisplayName = when (LocaleHelper.getPersistedLocale(this@RequestDeliveryActivity)) {
                        "ar" -> (restaurant?.restaurantNameAr?.takeIf { it.isNotBlank() } ?: restaurant?.restaurantName) ?: ""
                        else -> (restaurant?.restaurantName?.takeIf { it.isNotBlank() } ?: restaurant?.restaurantNameAr) ?: ""
                    }
                    
                    if (deliveryCompany != null) {
                        // Show delivery company name as title
                        binding.tvDeliveryCompanyTitle.text = deliveryCompany.companyName
                        binding.tvDeliveryCompanyTitle.visibility = android.view.View.VISIBLE
                        
                        // Show zones if available
                        if (zones.isNotEmpty()) {
                            currentZones = zones
                            zonesAdapter.submitList(zones)
                            binding.recyclerViewZones.visibility = android.view.View.VISIBLE
                            binding.emptyState.visibility = android.view.View.GONE
                        } else {
                            binding.recyclerViewZones.visibility = android.view.View.GONE
                            binding.emptyState.visibility = android.view.View.VISIBLE
                            binding.tvEmptyState.text = "No zones available for this delivery company"
                        }
                    } else {
                        // No delivery company assigned
                        binding.tvDeliveryCompanyTitle.visibility = android.view.View.GONE
                        binding.recyclerViewZones.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        binding.tvEmptyState.text = "No delivery company assigned"
                    }
                } else {
                    Toast.makeText(
                        this@RequestDeliveryActivity,
                        response.message() ?: "Failed to load zones",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.emptyState.visibility = android.view.View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@RequestDeliveryActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.emptyState.visibility = android.view.View.VISIBLE
            }
        }
    }
}
