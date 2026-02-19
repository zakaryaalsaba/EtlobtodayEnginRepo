package com.order.resturantandroid.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.order.resturantandroid.data.repository.AuthRepository
import com.order.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository()
    private val sessionManager = SessionManager(application)
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            repository.login(email, password).fold(
                onSuccess = { authResponse ->
                    // Save session
                    sessionManager.saveAuthToken(authResponse.token)
                    sessionManager.saveAdminInfo(
                        authResponse.admin.id,
                        authResponse.admin.websiteId,
                        authResponse.admin.email,
                        authResponse.admin.name,
                        authResponse.admin.restaurantName
                    )
                    _loginState.value = LoginState.Success
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Login failed")
                }
            )
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

