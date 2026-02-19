package com.driver.resturantandroid.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driver.resturantandroid.data.model.Driver
import com.driver.resturantandroid.repository.DriverRepository
import kotlinx.coroutines.launch
import java.io.File

class ProfileViewModel : ViewModel() {
    private val repository = DriverRepository()
    
    private val _driver = MutableLiveData<Driver?>()
    val driver: LiveData<Driver?> = _driver
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun loadProfile(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.getDriverProfile(token)
                .onSuccess { driver ->
                    _driver.value = driver
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                }
        }
    }
    
    fun updateProfile(name: String, phone: String?, token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.updateDriverProfile(name, phone, token)
                .onSuccess { driver ->
                    _driver.value = driver
                    _updateSuccess.value = true
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                }
        }
    }
    
    fun clearUpdateSuccess() {
        _updateSuccess.value = false
    }
    
    fun uploadProfileImage(imageFile: File, token: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.uploadProfileImage(imageFile, token)
                .onSuccess { driver ->
                    _driver.value = driver
                    onComplete()
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                }
        }
    }
}
