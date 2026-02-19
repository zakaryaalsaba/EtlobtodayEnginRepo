package com.driver.resturantandroid.ui.auth

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.driver.resturantandroid.databinding.ActivityRegisterBinding
import com.driver.resturantandroid.repository.DriverRepository
import com.driver.resturantandroid.ui.auth.RegistrationSuccessActivity
import com.driver.resturantandroid.util.LocaleHelper
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val driverRepository = DriverRepository()
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Driver Account"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            register()
        }
    }
    
    private fun register() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Validation
        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        } else {
            binding.tilName.error = null
        }
        
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        } else {
            binding.tilEmail.error = null
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        } else {
            binding.tilPassword.error = null
        }
        
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return
        } else {
            binding.tilPassword.error = null
        }
        
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        } else {
            binding.tilConfirmPassword.error = null
        }
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRegister.isEnabled = false
        
        lifecycleScope.launch {
            val result = driverRepository.register(
                name,
                email,
                password,
                if (phone.isEmpty()) null else phone
            )
            
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnRegister.isEnabled = true
            
            result.onSuccess { response ->
                // Navigate to success activity
                val intent = Intent(this@RegisterActivity, RegistrationSuccessActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { error ->
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

