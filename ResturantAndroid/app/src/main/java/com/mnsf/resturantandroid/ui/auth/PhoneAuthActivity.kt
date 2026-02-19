package com.mnsf.resturantandroid.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityPhoneAuthBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.AuthRepository
import com.mnsf.resturantandroid.ui.MainActivity
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.util.DeviceTokenHelper
import com.mnsf.resturantandroid.util.LocationHelper
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPhoneAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String = ""
    
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification completed (SMS code was automatically detected)
            Log.d(TAG, "onVerificationCompleted: Auto-verification successful")
            signInWithPhoneAuthCredential(credential)
        }
        
        override fun onVerificationFailed(e: FirebaseException) {
            Log.e(TAG, "onVerificationFailed: ${e.message}", e)
            binding.progressBar.visibility = View.GONE
            binding.btnSendCode.isEnabled = true
            binding.btnVerifyCode.isEnabled = true
            
            val errorMessage = when {
                e.message?.contains("not allowed") == true || 
                e.message?.contains("sign-in provider is disabled") == true -> {
                    getString(R.string.phone_auth_not_enabled)
                }
                e.message?.contains("invalid phone number") == true -> {
                    getString(R.string.invalid_phone_number)
                }
                e.message?.contains("quota") == true -> {
                    "SMS quota exceeded. Please try again later."
                }
                else -> {
                    getString(R.string.phone_verification_failed)
                }
            }
            
            binding.tvError.text = errorMessage
            binding.tvError.visibility = View.VISIBLE
            Toast.makeText(this@PhoneAuthActivity, errorMessage, Toast.LENGTH_LONG).show()
        }
        
        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d(TAG, "onCodeSent: Verification code sent")
            storedVerificationId = verificationId
            resendToken = token
            
            binding.progressBar.visibility = View.GONE
            binding.btnSendCode.isEnabled = true
            
            // Switch to OTP verification view
            showOtpVerificationView()
            
            Toast.makeText(
                this@PhoneAuthActivity,
                getString(R.string.verification_code_sent, phoneNumber),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)
        authRepository = AuthRepository(RetrofitClient.apiService, sessionManager)
        
        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        // Request location permission as soon as possible
        requestLocationPermissionIfNeeded()
        
        setupClickListeners()
        showPhoneInputView()
    }
    
    private fun requestLocationPermissionIfNeeded() {
        if (!LocationHelper.hasLocationPermission(this)) {
            LocationHelper.requestLocationPermissions(this, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted")
            } else {
                Log.w(TAG, "Location permission denied")
            }
        }
    }
    
    companion object {
        private const val TAG = "PhoneAuthActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    private fun setupClickListeners() {
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }
        
        binding.btnVerifyCode.setOnClickListener {
            verifyCode()
        }
        
        binding.btnResendCode.setOnClickListener {
            resendVerificationCode()
        }
        
        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun showPhoneInputView() {
        binding.llPhoneInput.visibility = View.VISIBLE
        binding.llOtpVerification.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }
    
    private fun showOtpVerificationView() {
        binding.llPhoneInput.visibility = View.GONE
        binding.llOtpVerification.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.etOtpCode.text?.clear()
    }
    
    private fun sendVerificationCode() {
        // User enters only the local Jordanian number (e.g., 7XXXXXXXX or 07XXXXXXXX)
        var localNumber = binding.etPhoneNumber.text.toString().trim()
        
        if (localNumber.isEmpty()) {
            binding.tvError.text = getString(R.string.invalid_phone_number)
            binding.tvError.visibility = View.VISIBLE
            return
        }
        
        // Remove leading 0 if present (e.g., 07XXXXXXXX -> 7XXXXXXXX)
        if (localNumber.startsWith("0")) {
            localNumber = localNumber.removePrefix("0")
        }
        
        // Build full E.164 number for Jordan: +9627XXXXXXXX
        phoneNumber = "+962$localNumber"
        
        // Basic length check (country code + local number)
        if (phoneNumber.length < 12) {
            binding.tvError.text = getString(R.string.invalid_phone_number)
            binding.tvError.visibility = View.VISIBLE
            return
        }
        
        binding.tvError.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendCode.isEnabled = false
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
        Log.d(TAG, "sendVerificationCode: Verification code request sent for $phoneNumber")
    }
    
    private fun resendVerificationCode() {
        if (phoneNumber.isEmpty() || resendToken == null) {
            Toast.makeText(this, getString(R.string.phone_verification_failed), Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnResendCode.isEnabled = false
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken!!)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
        Log.d(TAG, "resendVerificationCode: Resending verification code")
    }
    
    private fun verifyCode() {
        val code = binding.etOtpCode.text.toString().trim()
        
        if (code.isEmpty() || code.length < 6) {
            binding.tvError.text = getString(R.string.code_verification_failed)
            binding.tvError.visibility = View.VISIBLE
            return
        }
        
        storedVerificationId?.let { verificationId ->
            binding.progressBar.visibility = View.VISIBLE
            binding.btnVerifyCode.isEnabled = false
            binding.tvError.visibility = View.GONE
            
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        } ?: run {
            binding.tvError.text = getString(R.string.code_verification_failed)
            binding.tvError.visibility = View.VISIBLE
        }
    }
    
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        binding.progressBar.visibility = View.VISIBLE
        
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnVerifyCode.isEnabled = true
                
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Log.d(TAG, "signInWithPhoneAuthCredential: Firebase auth successful for ${user?.phoneNumber}")
                    
                    // Get Firebase ID token
                    user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val firebaseToken = tokenTask.result?.token
                            Log.d(TAG, "signInWithPhoneAuthCredential: Firebase ID token obtained")
                            
                            // Authenticate with backend using phone number and Firebase token
                            authenticateWithBackend(phoneNumber, firebaseToken)
                        } else {
                            Log.e(TAG, "signInWithPhoneAuthCredential: Failed to get Firebase ID token", tokenTask.exception)
                            binding.tvError.text = getString(R.string.phone_verification_failed)
                            binding.tvError.visibility = View.VISIBLE
                        }
                    }
                } else {
                    Log.e(TAG, "signInWithPhoneAuthCredential: Firebase auth failed", task.exception)
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            getString(R.string.code_verification_failed)
                        }
                        else -> {
                            getString(R.string.phone_verification_failed)
                        }
                    }
                    binding.tvError.text = errorMessage
                    binding.tvError.visibility = View.VISIBLE
                }
            }
    }
    
    private fun authenticateWithBackend(phone: String, firebaseToken: String?) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                
                // Get location and address BEFORE calling login (so it's saved immediately during login/registration)
                var latitude: Double? = null
                var longitude: Double? = null
                var address: String? = null
                
                if (LocationHelper.hasLocationPermission(this@PhoneAuthActivity)) {
                    LocationHelper.getCurrentLocation(this@PhoneAuthActivity)?.let { location ->
                        latitude = location.latitude
                        longitude = location.longitude
                        address = LocationHelper.getAddressFromLocation(
                            this@PhoneAuthActivity,
                            location.latitude,
                            location.longitude
                        )
                        Log.d(TAG, "authenticateWithBackend: Got location lat=$latitude, lng=$longitude, address=$address")
                    }
                } else {
                    Log.w(TAG, "authenticateWithBackend: Location permission not granted, requesting...")
                    requestLocationPermissionIfNeeded()
                }
                
                // Call backend API to authenticate with phone, Firebase token, location, and address
                val result = authRepository.loginWithPhone(phone, firebaseToken, latitude, longitude, address)
                
                binding.progressBar.visibility = View.GONE
                
                result.onSuccess { authResponse ->
                    Log.d(TAG, "authenticateWithBackend: Backend authentication successful")
                    Toast.makeText(
                        this@PhoneAuthActivity,
                        getString(R.string.phone_auth_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Send device token to backend (location already saved during login)
                    val customerId = sessionManager.getCustomerId()
                    val authToken = sessionManager.getAuthToken()
                    
                    if (customerId != null && authToken != null) {
                        DeviceTokenHelper.getAndSendDeviceToken(
                            apiService = RetrofitClient.apiService,
                            customerId = customerId,
                            token = authToken
                        )
                        
                        // If we have location but didn't send it in login (e.g., permission granted after), send it now
                        if (latitude == null && longitude == null && LocationHelper.hasLocationPermission(this@PhoneAuthActivity)) {
                            LocationHelper.getCurrentLocation(this@PhoneAuthActivity)?.let { location ->
                                val address = LocationHelper.getAddressFromLocation(
                                    this@PhoneAuthActivity,
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
                    
                    navigateToMain()
                }.onFailure { exception ->
                    Log.e(TAG, "authenticateWithBackend: Backend authentication failed", exception)
                    binding.tvError.text = exception.message ?: getString(R.string.phone_verification_failed)
                    binding.tvError.visibility = View.VISIBLE
                    
                    // Sign out from Firebase if backend auth fails
                    auth.signOut()
                }
            } catch (e: Exception) {
                Log.e(TAG, "authenticateWithBackend: Exception", e)
                binding.progressBar.visibility = View.GONE
                binding.tvError.text = e.message ?: getString(R.string.phone_verification_failed)
                binding.tvError.visibility = View.VISIBLE
                auth.signOut()
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

