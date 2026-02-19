package com.mnsf.resturantandroid.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityRegisterBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.AuthRepository
import com.mnsf.resturantandroid.ui.MainActivity
import android.content.Context
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.util.DeviceTokenHelper
import com.mnsf.resturantandroid.util.LocationHelper
import com.mnsf.resturantandroid.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        authRepository = AuthRepository(RetrofitClient.apiService, sessionManager)
        viewModel = ViewModelProvider(this, AuthViewModelFactory(authRepository))[AuthViewModel::class.java]
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
        }
        
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Success -> {
                    Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                    
                    // Send device token and location to backend
                    val customerId = sessionManager.getCustomerId()
                    val authToken = sessionManager.getAuthToken()
                    
                    if (customerId != null && authToken != null) {
                        // Send device token
                        DeviceTokenHelper.getAndSendDeviceToken(
                            apiService = RetrofitClient.apiService,
                            customerId = customerId,
                            token = authToken
                        )
                        
                        // Try to get and send location (optional)
                        if (LocationHelper.hasLocationPermission(this)) {
                            CoroutineScope(Dispatchers.Main).launch {
                                LocationHelper.getCurrentLocation(this@RegisterActivity)?.let { location ->
                                    val address = LocationHelper.getAddressFromLocation(
                                        this@RegisterActivity,
                                        location.latitude,
                                        location.longitude
                                    )
                                    DeviceTokenHelper.sendLocation(
                                        apiService = RetrofitClient.apiService,
                                        customerId = customerId,
                                        token = authToken,
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        address = address
                                    )
                                }
                            }
                        }
                    }
                    
                    navigateToMain()
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                binding.tvError.text = getString(R.string.fill_all_fields)
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                binding.tvError.text = getString(R.string.passwords_not_match)
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            
            binding.tvError.visibility = View.GONE
            viewModel.register(name, email, password, phone.takeIf { it.isNotEmpty() })
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

