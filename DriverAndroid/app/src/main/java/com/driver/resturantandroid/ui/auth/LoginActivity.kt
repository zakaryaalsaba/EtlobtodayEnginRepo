package com.driver.resturantandroid.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.driver.resturantandroid.R
import com.driver.resturantandroid.databinding.ActivityLoginBinding
import com.driver.resturantandroid.repository.DriverRepository
import com.driver.resturantandroid.MainActivity
import com.driver.resturantandroid.ui.auth.RegisterActivity
import com.driver.resturantandroid.util.SessionManager
import com.driver.resturantandroid.util.FCMTokenManager
import com.driver.resturantandroid.util.NotificationHelper
import com.driver.resturantandroid.util.LocaleHelper
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val driverRepository = DriverRepository()
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Setup notifications (permission + channel)
        NotificationHelper.setupNotifications(this)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            login()
        }
        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnLogin.isEnabled = false
        
        lifecycleScope.launch {
            val result = driverRepository.login(email, password)
            
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnLogin.isEnabled = true
            
            result.onSuccess { response ->
                if (response.token == null) {
                    Toast.makeText(
                        this@LoginActivity,
                        response.message ?: "Your account is pending approval. Please wait for administrator approval.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                sessionManager.saveDriverSession(
                    response.driver.id,
                    response.driver.name,
                    response.driver.email,
                    response.token
                )
                
                // Register FCM token for push notifications
                FCMTokenManager.getToken(this@LoginActivity)
                
                navigateToMain()
            }.onFailure { error ->
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

