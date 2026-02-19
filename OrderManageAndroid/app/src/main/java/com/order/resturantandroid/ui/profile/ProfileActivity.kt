package com.order.resturantandroid.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.messaging.FirebaseMessaging
import com.order.resturantandroid.R
import com.order.resturantandroid.data.remote.DeviceTokenRequest
import com.order.resturantandroid.data.remote.RetrofitClient
import com.order.resturantandroid.databinding.ActivityProfileBinding
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels {
        AndroidViewModelFactory.getInstance(application)
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(
                this,
                R.string.location_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            registerDeviceToken()
        } else {
            Toast.makeText(
                this,
                R.string.notification_permission_required,
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        setupToolbar()
        setupObservers()
        setupClickListeners()
        
        // Load restaurant profile
        viewModel.loadRestaurantProfile()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.toolbarTitle.text = getString(R.string.profile)
    }
    
    private fun setupObservers() {
        viewModel.restaurant.observe(this) { restaurant ->
            restaurant?.let {
                binding.etRestaurantName.setText(it.restaurantName ?: "")
                binding.etAddress.setText(it.address ?: "")
                binding.etPhone.setText(it.phone ?: "")
                binding.etEmail.setText(it.email ?: "")
                
                // Set location fields
                if (it.latitude != null) {
                    binding.etLatitude.setText(it.latitude.toString())
                } else {
                    binding.etLatitude.setText("")
                }
                if (it.longitude != null) {
                    binding.etLongitude.setText(it.longitude.toString())
                } else {
                    binding.etLongitude.setText("")
                }
                
                // Prompt to get location if it doesn't exist
                if (!it.hasLocation()) {
                    promptForLocation()
                }
                
                // Set publish switch
                val isPublished = it.getIsPublished()
                binding.switchPublish.isChecked = isPublished
                updatePublishStatusText(isPublished)
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnSave.isEnabled = isLoading != true
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.updateSuccess.observe(this) { success ->
            if (success == true) {
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.switchPublish.setOnCheckedChangeListener { _, isChecked ->
            updatePublishStatusText(isChecked)
        }
        
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
        
        binding.btnGetLocation.setOnClickListener {
            checkLocationPermissionAndGetLocation()
        }

        binding.btnEnableNotifications.setOnClickListener {
            enableNotificationsAndRegisterToken()
        }
    }

    private fun enableNotificationsAndRegisterToken() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        registerDeviceToken()
    }

    private fun registerDeviceToken() {
        registerDeviceTokenInternal(retryCount = 0)
    }

    private fun registerDeviceTokenInternal(retryCount: Int) {
        binding.btnEnableNotifications.isEnabled = false
        Log.d(TAG, "Requesting FCM token... (attempt ${retryCount + 1})")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                val ex = task.exception
                Log.e(TAG, "FCM token failed", ex)
                val isServiceNotAvailable = ex?.cause?.message?.contains("SERVICE_NOT_AVAILABLE", ignoreCase = true) == true
                        || ex?.message?.contains("SERVICE_NOT_AVAILABLE", ignoreCase = true) == true
                if (isServiceNotAvailable && retryCount < 1) {
                    Log.d(TAG, "SERVICE_NOT_AVAILABLE, retrying in 3 seconds...")
                    binding.btnEnableNotifications.isEnabled = true
                    android.os.Handler(mainLooper).postDelayed({
                        registerDeviceTokenInternal(retryCount + 1)
                    }, 3000)
                    return@addOnCompleteListener
                }
                binding.btnEnableNotifications.isEnabled = true
                val msg = when {
                    ex?.message?.contains("API key", ignoreCase = true) == true ->
                        getString(R.string.firebase_not_configured)
                    isServiceNotAvailable ->
                        getString(R.string.fcm_service_not_available)
                    ex?.message != null -> ex.message!!
                    else -> getString(R.string.notifications_enabled_failed)
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                return@addOnCompleteListener
            }
            val token = task.result
            if (token.isNullOrBlank()) {
                Log.e(TAG, "FCM token is null or empty")
                binding.btnEnableNotifications.isEnabled = true
                Toast.makeText(this, R.string.notifications_enabled_failed, Toast.LENGTH_LONG).show()
                return@addOnCompleteListener
            }
            Log.d(TAG, "FCM token received, length=${token.length}")
            val sessionManager = SessionManager(this)
            val authToken = sessionManager.getAuthToken()
            if (authToken == null) {
                Log.e(TAG, "No auth token - user may need to log in again")
                binding.btnEnableNotifications.isEnabled = true
                Toast.makeText(this, getString(R.string.notifications_enabled_failed) + " (session)", Toast.LENGTH_LONG).show()
                return@addOnCompleteListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Calling API to register device token...")
                    val response = RetrofitClient.apiService.updateDeviceToken(
                        DeviceTokenRequest(token, "android"),
                        "Bearer $authToken"
                    )
                    runOnUiThread {
                        binding.btnEnableNotifications.isEnabled = true
                        if (response.isSuccessful) {
                            Log.d(TAG, "Device token registered successfully")
                            Toast.makeText(
                                this@ProfileActivity,
                                R.string.notifications_enabled,
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.tvNotificationStatus.visibility = android.view.View.VISIBLE
                            binding.tvNotificationStatus.text = getString(R.string.notifications_enabled)
                        } else {
                            val code = response.code()
                            val body = response.errorBody()?.string() ?: response.message()
                            Log.e(TAG, "API error: code=$code body=$body")
                            val msg = body.take(80).ifBlank { getString(R.string.notifications_enabled_failed) }
                            Toast.makeText(this@ProfileActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering device token", e)
                    runOnUiThread {
                        binding.btnEnableNotifications.isEnabled = true
                        val msg = e.message ?: getString(R.string.notifications_enabled_failed)
                        Toast.makeText(this@ProfileActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ProfileNotifications"
    }
    
    private fun promptForLocation() {
        // Show a dialog suggesting to get location
        AlertDialog.Builder(this)
            .setTitle(R.string.location)
            .setMessage(R.string.location_permission_required)
            .setPositiveButton(R.string.get_current_location) { _, _ ->
                checkLocationPermissionAndGetLocation()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun checkLocationPermissionAndGetLocation() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        } else {
            // Request permission
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    private fun getCurrentLocation() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!fineLocationGranted && !coarseLocationGranted) {
            Toast.makeText(
                this,
                R.string.location_permission_required,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        binding.btnGetLocation.isEnabled = false
        
        val priority = if (fineLocationGranted) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        
        val cancellationTokenSource = CancellationTokenSource()
        
        fusedLocationClient.getCurrentLocation(
            priority,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            binding.btnGetLocation.isEnabled = true
            
            if (location != null) {
                binding.etLatitude.setText(location.latitude.toString())
                binding.etLongitude.setText(location.longitude.toString())
                Toast.makeText(
                    this,
                    R.string.location_updated,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    R.string.location_not_available,
                    Toast.LENGTH_LONG
                ).show()
            }
        }.addOnFailureListener { exception ->
            binding.btnGetLocation.isEnabled = true
            Toast.makeText(
                this,
                "${getString(R.string.location_not_available)}: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun updatePublishStatusText(isPublished: Boolean) {
        binding.tvPublishStatus.text = if (isPublished) {
            getString(R.string.published)
        } else {
            getString(R.string.unpublished)
        }
    }
    
    private fun saveProfile() {
        val restaurantName = binding.etRestaurantName.text?.toString()?.trim()
        val address = binding.etAddress.text?.toString()?.trim()
        val phone = binding.etPhone.text?.toString()?.trim()
        val email = binding.etEmail.text?.toString()?.trim()
        val isPublished = binding.switchPublish.isChecked
        
        // Get location values
        val latitudeStr = binding.etLatitude.text?.toString()?.trim()
        val longitudeStr = binding.etLongitude.text?.toString()?.trim()
        val latitude = latitudeStr?.toDoubleOrNull()
        val longitude = longitudeStr?.toDoubleOrNull()
        
        // Validate required fields
        if (restaurantName.isNullOrEmpty()) {
            Toast.makeText(this, "Restaurant name is required", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.updateRestaurantProfile(
            restaurantName = restaurantName,
            address = address?.takeIf { it.isNotEmpty() },
            phone = phone?.takeIf { it.isNotEmpty() },
            email = email?.takeIf { it.isNotEmpty() },
            isPublished = isPublished,
            latitude = latitude,
            longitude = longitude
        )
    }
}
