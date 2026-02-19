package com.mnsf.resturantandroid.ui.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityNewAddressBinding
import android.util.Log
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import com.mnsf.resturantandroid.data.model.Region
import com.mnsf.resturantandroid.data.model.Area
import com.mnsf.resturantandroid.data.model.DeliveryZone
import com.mnsf.resturantandroid.network.CreateAddressRequest
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.util.CurrencyFormatter
import com.mnsf.resturantandroid.util.I18nHelper
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.LocationHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import com.mnsf.resturantandroid.ui.restaurant.RestaurantViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewAddressActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNewAddressBinding
    private var restaurantId: Int = -1
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var restaurantViewModel: RestaurantViewModel
    
    // Regions, Areas, and Zones for delivery company
    private var regions: List<Region> = emptyList()
    private var areas: List<Area> = emptyList()
    private var zones: List<DeliveryZone> = emptyList()
    private var selectedRegion: Region? = null
    private var selectedArea: Area? = null
    private var selectedZone: DeliveryZone? = null
    private var hasDeliveryCompany: Boolean = false

    companion object {
        fun newIntent(
            context: Context,
            restaurantId: Int,
            latitude: Double,
            longitude: Double
        ): Intent {
            return Intent(context, NewAddressActivity::class.java).apply {
                putExtra("restaurant_id", restaurantId)
                putExtra("latitude", latitude)
                putExtra("longitude", longitude)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        restaurantId = intent.getIntExtra("restaurant_id", -1)
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        // restaurantId == -1 allowed when opening from Home (choose location only); on save we just finish()

        // Setup restaurant view model
        val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
        restaurantViewModel = ViewModelProvider(
            this,
            RestaurantViewModelFactory(restaurantRepository)
        )[RestaurantViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }
        
        // Load restaurant info to check for delivery company
        if (restaurantId != -1) {
            loadRestaurantAndSetupDeliveryCompany()
        }

        mapView = MapView(this).apply {
            onCreate(savedInstanceState)
            getMapAsync(this@NewAddressActivity)
        }
        binding.mapContainer.addView(mapView, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)

        sessionManager.getCustomerPhone()?.let { binding.etPhone.setText(it) }

        // Load address from location (only if not using delivery company)
        if (restaurantId == -1 || !hasDeliveryCompany) {
            lifecycleScope.launch {
                val areaStreet = withContext(Dispatchers.IO) {
                    LocationHelper.getAddressFromLocation(this@NewAddressActivity, latitude, longitude)
                }
                areaStreet?.let { addr ->
                    runOnUiThread {
                        binding.tvArea.text = addr
                        binding.etStreet.setText(addr)
                    }
                } ?: runOnUiThread {
                    binding.tvArea.text = getString(R.string.area)
                }
            }
        }

        binding.btnChangeArea.setOnClickListener {
            // Go back to Confirm Location (map) to pick a new location
            startActivity(ConfirmLocationActivity.newIntent(this, restaurantId))
            finish()
        }

        binding.btnSaveAddress.setOnClickListener { saveAddressAndContinue() }

        // Show/hide form fields based on address type (Apartment / House / Office)
        updateFormVisibilityForAddressType()
        binding.chipGroupType.setOnCheckedChangeListener { _, _ ->
            updateFormVisibilityForAddressType()
        }
    }

    private fun updateFormVisibilityForAddressType() {
        when (binding.chipGroupType.checkedChipId) {
            R.id.chipApartment -> {
                binding.layoutApartmentFields.visibility = View.VISIBLE
                binding.layoutHouseFields.visibility = View.GONE
                binding.layoutOfficeFields.visibility = View.GONE
            }
            R.id.chipHouse -> {
                binding.layoutApartmentFields.visibility = View.GONE
                binding.layoutHouseFields.visibility = View.VISIBLE
                binding.layoutOfficeFields.visibility = View.GONE
            }
            R.id.chipOffice -> {
                binding.layoutApartmentFields.visibility = View.GONE
                binding.layoutHouseFields.visibility = View.GONE
                binding.layoutOfficeFields.visibility = View.VISIBLE
            }
            else -> {
                binding.layoutApartmentFields.visibility = View.VISIBLE
                binding.layoutHouseFields.visibility = View.GONE
                binding.layoutOfficeFields.visibility = View.GONE
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val latLng = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        map.addMarker(MarkerOptions().position(latLng))
        map.uiSettings.isZoomControlsEnabled = false
    }

    private fun getSelectedAddressType(): String {
        return when (binding.chipGroupType.checkedChipId) {
            R.id.chipApartment -> "apartment"
            R.id.chipHouse -> "house"
            R.id.chipOffice -> "office"
            else -> "apartment"
        }
    }

    /** Returns (buildingName, apartmentNumber, floor) for the currently selected address type. */
    private fun getTypeSpecificFields(): Triple<String, String, String> {
        return when (binding.chipGroupType.checkedChipId) {
            R.id.chipApartment -> Triple(
                binding.etBuildingName.text.toString().trim(),
                binding.etAptNumber.text.toString().trim(),
                binding.etFloor.text.toString().trim()
            )
            R.id.chipHouse -> Triple(
                binding.etHouseNumber.text.toString().trim(),
                "",
                ""
            )
            R.id.chipOffice -> Triple(
                binding.etCompany.text.toString().trim(),
                "",
                binding.etOfficeFloor.text.toString().trim()
            )
            else -> Triple(
                binding.etBuildingName.text.toString().trim(),
                binding.etAptNumber.text.toString().trim(),
                binding.etFloor.text.toString().trim()
            )
        }
    }

    private fun saveAddressAndContinue() {
        // Validate region, area, and zone selection if using delivery company
        if (hasDeliveryCompany) {
            if (selectedRegion == null) {
                Toast.makeText(this, getString(R.string.please_select_region), Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedArea == null) {
                Toast.makeText(this, getString(R.string.please_select_area), Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedZone == null) {
                Toast.makeText(this, getString(R.string.please_select_zone), Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        val (building, apt, floor) = getTypeSpecificFields()
        val street = binding.etStreet.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val extra = binding.etAdditionalDirections.text.toString().trim()
        val label = binding.etAddressLabel.text.toString().trim()
        
        // For delivery company: use zone name as area; otherwise use tvArea text
        val area = if (hasDeliveryCompany && selectedZone != null) {
            I18nHelper.getZoneNameDisplay(selectedZone!!, this)
        } else {
            binding.tvArea.text?.toString()?.trim().orEmpty()
        }

        if (!hasDeliveryCompany && street.isBlank()) {
            Toast.makeText(this, getString(R.string.street), Toast.LENGTH_SHORT).show()
            return
        }

        val parts = mutableListOf<String>()
        if (street.isNotEmpty()) parts.add(street)
        if (building.isNotEmpty()) parts.add(building)
        if (apt.isNotEmpty()) parts.add(getString(R.string.apt_number) + " " + apt)
        if (floor.isNotEmpty()) parts.add(getString(R.string.floor) + " " + floor)
        if (extra.isNotEmpty()) parts.add(extra)
        val fullAddress = parts.joinToString(", ")

        val customerId = sessionManager.getCustomerId()
        val token = sessionManager.getAuthToken()

        if (customerId != -1) {
            sessionManager.saveCustomerInfo(
                customerId,
                sessionManager.getCustomerName() ?: "",
                sessionManager.getCustomerEmail(),
                if (phone.isNotEmpty()) phone else sessionManager.getCustomerPhone(),
                fullAddress
            )
        }

        // Call API to save address when customer is logged in
        if (customerId != -1 && !token.isNullOrBlank()) {
            binding.btnSaveAddress.isEnabled = false
            lifecycleScope.launch {
                try {
                    val request = CreateAddressRequest(
                        area = area.ifBlank { null },
                        region_id = selectedRegion?.id,
                        region_name = selectedRegion?.let { I18nHelper.getRegionNameDisplay(it, this@NewAddressActivity) },
                        area_id = selectedArea?.id,
                        area_name = selectedArea?.let { I18nHelper.getAreaNameDisplay(it, this@NewAddressActivity) },
                        zone_id = selectedZone?.id,
                        zone_name = selectedZone?.let { I18nHelper.getZoneNameDisplay(it, this@NewAddressActivity) },
                        zone_price = selectedZone?.price,
                        latitude = latitude,
                        longitude = longitude,
                        address_type = getSelectedAddressType(),
                        building_name = building.ifBlank { null },
                        apartment_number = apt.ifBlank { null },
                        floor = floor.ifBlank { null },
                        street = street,
                        phone_number = phone.ifBlank { null },
                        additional_directions = extra.ifBlank { null },
                        address_label = label.ifBlank { null },
                        is_default = true
                    )
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.createAddress(
                            customerId,
                            request,
                            "Bearer $token"
                        )
                    }
                    if (response.isSuccessful) {
                        runOnUiThread {
                            sessionManager.saveDeliveryLabel(getSelectedAddressType().replaceFirstChar { it.uppercase() })
                            // Pass zone_price to checkout via intent extra (will be loaded from address in checkout)
                            navigateToCheckout()
                        }
                    } else {
                        runOnUiThread {
                            binding.btnSaveAddress.isEnabled = true
                            Toast.makeText(this@NewAddressActivity, getString(R.string.error_saving_profile), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.btnSaveAddress.isEnabled = true
                        Toast.makeText(this@NewAddressActivity, getString(R.string.error_saving_profile), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            navigateToCheckout()
        }
    }

    private fun navigateToCheckout() {
        if (restaurantId == -1) {
            finish()
            return
        }
        startActivity(Intent(this, CheckoutActivity::class.java).apply {
            putExtra("restaurant_id", restaurantId)
        })
        finish()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
    
    /** Load restaurant info and check if delivery company is approved, then setup region/zone selection. */
    private fun loadRestaurantAndSetupDeliveryCompany() {
        restaurantViewModel.loadRestaurant(restaurantId)
        restaurantViewModel.selectedRestaurant.observe(this) { restaurant ->
            restaurant?.let {
                hasDeliveryCompany = it.delivery_company_id != null && it.delivery_company_id > 0
                if (hasDeliveryCompany) {
                    // Show region/zone selection, hide area card
                    binding.layoutRegionZone.visibility = View.VISIBLE
                    binding.cardArea.visibility = View.GONE
                    loadRegions()
                } else {
                    // Show area card, hide region/zone selection
                    binding.layoutRegionZone.visibility = View.GONE
                    binding.cardArea.visibility = View.VISIBLE
                }
            }
        }
    }
    
    /** Load regions for the restaurant's delivery company. */
    private fun loadRegions() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getRestaurantRegions(restaurantId)
                }
                if (response.isSuccessful) {
                    regions = response.body()?.regions ?: emptyList()
                    runOnUiThread {
                        setupRegionDropdown()
                    }
                }
            } catch (e: Exception) {
                Log.e("NewAddressActivity", "Failed to load regions", e)
            }
        }
    }
    
    /** Setup region dropdown adapter. */
    private fun setupRegionDropdown() {
        val regionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            regions.map { region ->
                I18nHelper.getRegionNameDisplay(region, this)
            }
        )
        binding.autoCompleteRegion.setAdapter(regionAdapter)
        binding.autoCompleteRegion.setOnItemClickListener { _, _, position, _ ->
            selectedRegion = regions[position]
            // Clear area and zone selections when region changes
            selectedArea = null
            selectedZone = null
            binding.autoCompleteArea.setText("")
            binding.autoCompleteZone.setText("")
            binding.layoutArea.isEnabled = false
            binding.autoCompleteArea.isEnabled = false
            binding.layoutZone.isEnabled = false
            binding.autoCompleteZone.isEnabled = false
            // Load areas for selected region
            loadAreasForRegion(selectedRegion!!.id)
        }
    }
    
    /** Load areas filtered by selected region. */
    private fun loadAreasForRegion(regionId: Int) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getRestaurantAreas(restaurantId, regionId)
                }
                if (response.isSuccessful) {
                    areas = response.body()?.areas ?: emptyList()
                    runOnUiThread {
                        setupAreaDropdown()
                        binding.layoutArea.isEnabled = true
                        binding.autoCompleteArea.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e("NewAddressActivity", "Failed to load areas", e)
            }
        }
    }
    
    /** Setup area dropdown adapter. */
    private fun setupAreaDropdown() {
        val areaAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            areas.map { area ->
                I18nHelper.getAreaNameDisplay(area, this)
            }
        )
        binding.autoCompleteArea.setAdapter(areaAdapter)
        binding.autoCompleteArea.setOnItemClickListener { _, _, position, _ ->
            selectedArea = areas[position]
            // Clear zone selection when area changes
            selectedZone = null
            binding.autoCompleteZone.setText("")
            binding.layoutZone.isEnabled = false
            binding.autoCompleteZone.isEnabled = false
            // Load zones for selected area
            loadZonesForArea(selectedArea!!.id)
        }
    }
    
    /** Load zones filtered by selected area. */
    private fun loadZonesForArea(areaId: Int) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getRestaurantZones(restaurantId, areaId)
                }
                if (response.isSuccessful) {
                    zones = response.body()?.zones ?: emptyList()
                    runOnUiThread {
                        setupZoneDropdown()
                        binding.layoutZone.isEnabled = true
                        binding.autoCompleteZone.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e("NewAddressActivity", "Failed to load zones", e)
            }
        }
    }
    
    /** Setup zone dropdown adapter. */
    private fun setupZoneDropdown() {
        val zoneAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            zones.map { zone ->
                I18nHelper.getZoneNameDisplay(zone, this)
            }
        )
        binding.autoCompleteZone.setAdapter(zoneAdapter)
        binding.autoCompleteZone.setOnItemClickListener { _, _, position, _ ->
            selectedZone = zones[position]
        }
    }
}
