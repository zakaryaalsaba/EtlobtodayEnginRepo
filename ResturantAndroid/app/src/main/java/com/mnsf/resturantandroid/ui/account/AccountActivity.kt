package com.mnsf.resturantandroid.ui.account

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.databinding.ActivityAccountBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.network.UpdateCustomerProfileRequest
import com.mnsf.resturantandroid.util.LocaleHelper
import com.mnsf.resturantandroid.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AccountActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAccountBinding
    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    
    companion object {
        private const val REQUEST_CODE_CAMERA = 100
        private const val REQUEST_CODE_GALLERY = 101
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase)))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        setupToolbar()
        setupClickListeners()
        loadCustomerProfile()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.account)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            displaySelectedImage(it)
            uploadProfilePicture(it)
        }
    }
    
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            displaySelectedImage(cameraImageUri!!)
            uploadProfilePicture(cameraImageUri!!)
        }
    }
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, getString(R.string.storage_permission_required), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
        
        binding.ivProfilePicture.setOnClickListener {
            showImageSourceDialog()
        }
    }
    
    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery)
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_profile_picture))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> checkStoragePermissionAndOpen()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun checkStoragePermissionAndOpen() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                storagePermissionLauncher.launch(permission)
            }
        }
    }
    
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = File(getExternalFilesDir(null), "profile_photo_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        cameraLauncher.launch(cameraImageUri)
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    private fun displaySelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_account_circle)
            .error(R.drawable.ic_account_circle)
            .circleCrop()
            .into(binding.ivProfilePicture)
    }
    
    private fun uploadProfilePicture(uri: Uri) {
        val customerId = sessionManager.getCustomerId()
        val token = sessionManager.getAuthToken()
        
        if (customerId == -1 || token == null) {
            Toast.makeText(this, getString(R.string.error_uploading_image), Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                android.util.Log.d("AccountActivity", "Starting profile picture upload for customer $customerId")
                
                // Convert URI to File
                val file = withContext(Dispatchers.IO) {
                    uriToFile(uri)
                }
                
                if (file == null || !file.exists()) {
                    android.util.Log.e("AccountActivity", "Failed to create file from URI: $uri")
                    Toast.makeText(
                        this@AccountActivity,
                        getString(R.string.error_reading_image),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }
                
                android.util.Log.d("AccountActivity", "File created: ${file.absolutePath}, size: ${file.length()} bytes")
                
                // Determine MIME type from file extension
                val mimeType = when {
                    file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    file.name.endsWith(".gif", ignoreCase = true) -> "image/gif"
                    file.name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                    else -> "image/jpeg" // Default to JPEG
                }
                
                android.util.Log.d("AccountActivity", "MIME type: $mimeType for file: ${file.name}")
                
                // Create request body with specific MIME type
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
                
                android.util.Log.d("AccountActivity", "Uploading file: ${file.name}")
                
                // Upload image
                val response = RetrofitClient.apiService.uploadProfilePicture(
                    customerId,
                    body,
                    "Bearer $token"
                )
                
                android.util.Log.d("AccountActivity", "Upload response: code=${response.code()}, success=${response.isSuccessful}")
                
                if (response.isSuccessful && response.body() != null) {
                    val customer = response.body()!!.customer
                    
                    android.util.Log.d("AccountActivity", "Upload successful! Profile picture URL: ${customer.profile_picture_url}")
                    
                    // Update session
                    sessionManager.saveCustomerInfo(
                        customer.id,
                        customer.name,
                        customer.email,
                        customer.phone
                    )
                    
                    // Reload profile picture from URL
                    if (!customer.profile_picture_url.isNullOrEmpty()) {
                        android.util.Log.d("AccountActivity", "Loading profile picture from URL: ${customer.profile_picture_url}")
                        Glide.with(this@AccountActivity)
                            .load(customer.profile_picture_url)
                            .placeholder(R.drawable.ic_account_circle)
                            .error(R.drawable.ic_account_circle)
                            .circleCrop()
                            .into(binding.ivProfilePicture)
                    } else {
                        android.util.Log.w("AccountActivity", "Profile picture URL is empty after upload")
                    }
                    
                    Toast.makeText(
                        this@AccountActivity,
                        getString(R.string.profile_picture_uploaded_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AccountActivity", "Upload failed: code=${response.code()}, body=$errorBody")
                    
                    val errorMessage = if (errorBody != null) {
                        try {
                            val errorJson = org.json.JSONObject(errorBody)
                            errorJson.optString("error", response.message() ?: getString(R.string.error_uploading_image))
                        } catch (e: Exception) {
                            response.message() ?: getString(R.string.error_uploading_image)
                        }
                    } else {
                        response.message() ?: getString(R.string.error_uploading_image)
                    }
                    
                    Toast.makeText(
                        this@AccountActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AccountActivity", "Exception uploading profile picture", e)
                e.printStackTrace()
                Toast.makeText(
                    this@AccountActivity,
                    "${getString(R.string.error_uploading_image)}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            
            // Get MIME type from content resolver
            val mimeType = contentResolver.getType(uri)
            android.util.Log.d("AccountActivity", "URI MIME type: $mimeType")
            
            // Determine file extension from MIME type
            val extension = when {
                mimeType?.contains("jpeg", ignoreCase = true) == true || mimeType?.contains("jpg", ignoreCase = true) == true -> ".jpg"
                mimeType?.contains("png", ignoreCase = true) == true -> ".png"
                mimeType?.contains("gif", ignoreCase = true) == true -> ".gif"
                mimeType?.contains("webp", ignoreCase = true) == true -> ".webp"
                else -> ".jpg" // Default to jpg
            }
            
            val file = File(cacheDir, "profile_${System.currentTimeMillis()}$extension")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            android.util.Log.d("AccountActivity", "File created: ${file.absolutePath}, extension: $extension")
            file
        } catch (e: Exception) {
            android.util.Log.e("AccountActivity", "Error converting URI to file", e)
            e.printStackTrace()
            null
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload profile when returning to this activity to show updated profile picture
        loadCustomerProfile()
    }
    
    private fun loadCustomerProfile() {
        val customerId = sessionManager.getCustomerId()
        val token = sessionManager.getAuthToken()
        
        android.util.Log.d("AccountActivity", "Loading profile for customer ID: $customerId")
        
        if (customerId == -1 || token == null) {
            android.util.Log.e("AccountActivity", "Invalid session: customerId=$customerId, token=${if (token != null) "exists" else "null"}")
            Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCustomerProfile(
                    customerId,
                    "Bearer $token"
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val customer = response.body()!!.customer
                    
                    // Populate fields
                    binding.etName.setText(customer.name)
                    binding.etEmail.setText(customer.email)
                    binding.etPhone.setText(customer.phone ?: "")
                    binding.etAddress.setText(customer.address ?: "")
                    
                    // Load profile picture if available
                    if (!customer.profile_picture_url.isNullOrEmpty()) {
                        Glide.with(this@AccountActivity)
                            .load(customer.profile_picture_url)
                            .placeholder(R.drawable.ic_account_circle)
                            .error(R.drawable.ic_account_circle)
                            .circleCrop()
                            .into(binding.ivProfilePicture)
                    } else {
                        binding.ivProfilePicture.setImageResource(R.drawable.ic_account_circle)
                    }
                } else {
                    // Get error message from response body if available
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            // Try to parse JSON error
                            val errorJson = org.json.JSONObject(errorBody)
                            errorJson.optString("error", response.message() ?: getString(R.string.error_loading_profile))
                        } catch (e: Exception) {
                            response.message() ?: getString(R.string.error_loading_profile)
                        }
                    } else {
                        response.message() ?: getString(R.string.error_loading_profile)
                    }
                    
                    android.util.Log.e("AccountActivity", "Error loading profile: ${response.code()} - $errorMessage")
                    Toast.makeText(
                        this@AccountActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // If customer not found, show more helpful message
                    if (response.code() == 404) {
                        android.util.Log.e("AccountActivity", "Customer ID $customerId not found in database")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AccountActivity", "Exception loading profile", e)
                Toast.makeText(
                    this@AccountActivity,
                    "${getString(R.string.error_loading_profile)}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun saveProfile() {
        val customerId = sessionManager.getCustomerId()
        val token = sessionManager.getAuthToken()
        
        if (customerId == -1 || token == null) {
            Toast.makeText(this, getString(R.string.error_saving_profile), Toast.LENGTH_SHORT).show()
            return
        }
        
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        
        // Basic validation
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.name_required)
            return
        }
        
        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.email_required)
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val request = UpdateCustomerProfileRequest(
                    name = name,
                    email = email,
                    phone = if (phone.isNotEmpty()) phone else null,
                    address = if (address.isNotEmpty()) address else null,
                    profile_picture_url = null // TODO: Upload image and get URL
                )
                
                val response = RetrofitClient.apiService.updateCustomerProfile(
                    customerId,
                    request,
                    "Bearer $token"
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val customer = response.body()!!.customer
                    
                    // Update session
                    sessionManager.saveCustomerInfo(
                        customer.id,
                        customer.name,
                        customer.email,
                        customer.phone
                    )
                    
                    Toast.makeText(
                        this@AccountActivity,
                        getString(R.string.profile_updated_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Update MainActivity drawer header if it's still in memory
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@AccountActivity,
                        response.message() ?: getString(R.string.error_saving_profile),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AccountActivity,
                    getString(R.string.error_saving_profile),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }
}

