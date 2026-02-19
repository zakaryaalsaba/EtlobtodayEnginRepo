package com.mnsf.resturantandroid.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityLoginBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.AuthRepository
import com.mnsf.resturantandroid.ui.MainActivity
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.util.DeviceTokenHelper
import com.mnsf.resturantandroid.util.LocationHelper
import com.mnsf.resturantandroid.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d("LoginActivity", "onCreate: Starting LoginActivity")
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("LoginActivity", "onCreate: Binding inflated and content view set")
            
            sessionManager = SessionManager(this)
            Log.d("LoginActivity", "onCreate: SessionManager created")
            
            authRepository = AuthRepository(RetrofitClient.apiService, sessionManager)
            Log.d("LoginActivity", "onCreate: AuthRepository created")
            
            viewModel = ViewModelProvider(this, AuthViewModelFactory(authRepository))[AuthViewModel::class.java]
            Log.d("LoginActivity", "onCreate: ViewModel created")
            
            // Check if already logged in
            if (sessionManager.isLoggedIn()) {
                Log.d("LoginActivity", "onCreate: User already logged in, navigating to MainActivity")
                navigateToMain()
                return
            }
            
            Log.e("LoginFlow", "app just loaded")
            Toast.makeText(this, "app just loaded", Toast.LENGTH_SHORT).show()
            setupObservers()
            setupClickListeners()
            // Quick test: pre-fill test credentials (remove in production)
            binding.etEmail.setText("t@t.com")
            binding.etPassword.setText("123456")
            Log.d("LoginActivity", "onCreate: Completed successfully")
        } catch (e: Exception) {
            Log.e("LoginActivity", "onCreate: Fatal error", e)
            e.printStackTrace()
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }
        
        viewModel.authState.observe(this) { state ->
            try {
                Log.d("LoginActivity", "authState observer: State received: ${state.javaClass.simpleName}")
                when (state) {
                    is AuthViewModel.AuthState.Success -> {
                        Log.w("LoginBtn", "welcome zak")
                        Log.d("LoginActivity", "authState observer: Login successful, navigating to MainActivity")
                        Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                        
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
                                lifecycleScope.launch {
                                    LocationHelper.getCurrentLocation(this@LoginActivity)?.let { location ->
                                        val address = LocationHelper.getAddressFromLocation(
                                            this@LoginActivity,
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
                        Log.e("LoginActivity", "authState observer: Login error: ${state.message}")
                        binding.tvError.text = state.message
                        binding.tvError.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "authState observer: Error handling state", e)
                e.printStackTrace()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            Log.e("LoginFlow", "button just clicked")
            Toast.makeText(this, "button just clicked", Toast.LENGTH_SHORT).show()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            Log.d("LoginActivity", "Login attempt: email=${if (email.isEmpty()) "(empty)" else email.take(3) + "***"}")
            if (email.isEmpty() || password.isEmpty()) {
                binding.tvError.text = getString(R.string.fill_all_fields)
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            
            binding.tvError.visibility = View.GONE
            viewModel.login(email, password)
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        binding.btnPhoneAuth.setOnClickListener {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
        }
    }
    
    private fun navigateToMain() {
        try {
            Log.e("LoginFlow", "navigateToMain: about to start MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Log.e("LoginFlow", "navigateToMain: startActivity(MainActivity) called, now finish()")
            finish()
        } catch (e: Exception) {
            Log.e("LoginActivity", "navigateToMain: Error navigating to MainActivity", e)
            e.printStackTrace()
            Toast.makeText(this, "Error navigating: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

class AuthViewModelFactory(private val authRepository: AuthRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

