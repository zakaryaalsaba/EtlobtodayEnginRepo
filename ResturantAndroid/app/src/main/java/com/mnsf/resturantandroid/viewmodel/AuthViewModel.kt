package com.mnsf.resturantandroid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnsf.resturantandroid.data.model.AuthResponse
import com.mnsf.resturantandroid.data.model.LoginRequest
import com.mnsf.resturantandroid.data.model.RegisterRequest
import com.mnsf.resturantandroid.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    sealed class AuthState {
        data class Success(val authResponse: AuthResponse) : AuthState()
        data class Error(val message: String) : AuthState()
    }
    
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.login(LoginRequest(email, password))
            _isLoading.value = false
            
            result.onSuccess { authResponse ->
                _authState.value = AuthState.Success(authResponse)
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Login failed")
            }
        }
    }
    
    fun register(name: String, email: String, password: String, phone: String? = null) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Name, email, and password are required")
            return
        }
        
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.register(RegisterRequest(name, email, password, phone))
            _isLoading.value = false
            
            result.onSuccess { authResponse ->
                _authState.value = AuthState.Success(authResponse)
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Registration failed")
            }
        }
    }
    
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
    
    fun logout() {
        authRepository.logout()
    }
}

