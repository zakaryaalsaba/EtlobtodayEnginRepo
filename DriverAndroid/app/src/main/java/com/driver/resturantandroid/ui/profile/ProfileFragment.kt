package com.driver.resturantandroid.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.driver.resturantandroid.R
import com.driver.resturantandroid.databinding.FragmentProfileBinding
import com.driver.resturantandroid.util.SessionManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null
    private var tempImageFile: File? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImageFromUri(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Populate fields immediately from SessionManager
        populateFromSession()
        
        setupObservers()
        setupClickListeners()
        
        // Then load fresh data from API
        loadProfile()
    }
    
    private fun populateFromSession() {
        // Populate fields from session manager immediately for instant display
        val driverName = sessionManager.getDriverName()
        val driverEmail = sessionManager.getDriverEmail()
        val driverPhone = sessionManager.getDriverPhone()
        
        if (driverName != null) {
            binding.etName.setText(driverName)
            binding.tvProfileName.text = driverName
        }
        
        if (driverEmail != null) {
            binding.etEmail.setText(driverEmail)
            binding.tvProfileEmail.text = driverEmail
        }
        
        if (driverPhone != null) {
            binding.etPhone.setText(driverPhone)
        }
    }
    
    private fun setupObservers() {
        viewModel.driver.observe(viewLifecycleOwner) { driver ->
            driver?.let {
                // Update all fields with fresh data from API
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)
                binding.etPhone.setText(it.phone ?: "")
                binding.tvProfileName.text = it.name
                binding.tvProfileEmail.text = it.email
                
                // Load profile image if available
                if (it.image_url != null && it.image_url.isNotEmpty()) {
                    binding.ivProfileImage.load(it.image_url) {
                        placeholder(R.mipmap.ic_launcher_round)
                        error(R.mipmap.ic_launcher_round)
                        crossfade(true)
                    }
                } else {
                    // Reset to default if no image
                    binding.ivProfileImage.setImageResource(R.mipmap.ic_launcher_round)
                }
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
        }
        
        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_updated),
                    Toast.LENGTH_SHORT
                ).show()
                // Update session manager with new data
                binding.etName.text?.toString()?.let { name ->
                    sessionManager.updateDriverName(name)
                }
                binding.etPhone.text?.toString()?.let { phone ->
                    sessionManager.updateDriverPhone(phone)
                }
                // Reload profile to get updated image URL
                loadProfile()
                viewModel.clearUpdateSuccess()
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(
                    requireContext(),
                    it,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun loadProfile() {
        val token = sessionManager.getAuthToken()
        if (token != null) {
            // Show loading indicator
            binding.progressBar.visibility = View.VISIBLE
            viewModel.loadProfile(token)
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun setupClickListeners() {
        // Avatar click to select image
        binding.cardAvatar.setOnClickListener {
            openImagePicker()
        }
        
        binding.fabEditImage.setOnClickListener {
            openImagePicker()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            val phone = binding.etPhone.text?.toString()?.trim()
            
            if (name.isEmpty()) {
                binding.tilName.error = getString(R.string.name) + " " + getString(R.string.is_required)
                return@setOnClickListener
            }
            
            binding.tilName.error = null
            
            val token = sessionManager.getAuthToken()
            if (token != null) {
                // If image is selected, upload it first, then update profile
                if (selectedImageUri != null && tempImageFile != null && tempImageFile!!.exists()) {
                    viewModel.uploadProfileImage(tempImageFile!!, token) {
                        // After image upload completes, update profile
                        viewModel.updateProfile(name, phone?.takeIf { it.isNotEmpty() }, token)
                    }
                } else {
                    // Just update profile without image
                    viewModel.updateProfile(name, phone?.takeIf { it.isNotEmpty() }, token)
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }
    
    private fun loadImageFromUri(uri: Uri) {
        try {
            // Display the selected image
            binding.ivProfileImage.load(uri) {
                placeholder(R.mipmap.ic_launcher_round)
                error(R.mipmap.ic_launcher_round)
            }
            
            // Save to temp file for upload
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            tempImageFile = File(requireContext().cacheDir, "profile_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempImageFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.failed_to_load_image) + ": ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
