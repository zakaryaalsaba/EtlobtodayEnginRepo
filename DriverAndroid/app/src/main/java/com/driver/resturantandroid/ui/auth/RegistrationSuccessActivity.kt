package com.driver.resturantandroid.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.driver.resturantandroid.databinding.ActivityRegistrationSuccessBinding
import com.driver.resturantandroid.ui.auth.LoginActivity

class RegistrationSuccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationSuccessBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnGoToLogin.setOnClickListener {
            navigateToLogin()
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        // Prevent going back to registration form
        navigateToLogin()
    }
}

