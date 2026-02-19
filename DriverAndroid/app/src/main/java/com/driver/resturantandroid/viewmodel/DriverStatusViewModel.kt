package com.driver.resturantandroid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driver.resturantandroid.repository.DriverRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DriverStatusViewModel : ViewModel() {
    private val driverRepository = DriverRepository()
    private val statusMutex = Mutex()

    private val _isOnline = MutableLiveData<Boolean>(false)
    val isOnline: LiveData<Boolean> = _isOnline

    private val _isUpdating = MutableLiveData<Boolean>(false)
    val isUpdating: LiveData<Boolean> = _isUpdating

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Pending status request from rapid toggles; we send at most one request at a time and one pending. */
    private var pendingStatus: Boolean? = null
    private var pendingToken: String? = null
    private var pendingOnSuccess: (() -> Unit)? = null
    private var pendingOnError: ((String) -> Unit)? = null

    fun setOnlineStatus(isOnline: Boolean, token: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            statusMutex.withLock {
                if (_isUpdating.value == true) {
                    pendingStatus = isOnline
                    pendingToken = token
                    pendingOnSuccess = onSuccess
                    pendingOnError = onError
                    return@launch
                }
            }
            runStatusUpdate(isOnline, token, onSuccess, onError)
        }
    }

    private suspend fun runStatusUpdate(
        isOnline: Boolean,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        statusMutex.withLock { _isUpdating.value = true }
        _error.value = null

        val result = driverRepository.updateDriverStatus(isOnline, token)

        statusMutex.withLock {
            _isUpdating.value = false
            result
                .onSuccess {
                    _isOnline.value = isOnline
                    onSuccess()
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to update status"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            val next = pendingStatus
            val nextToken = pendingToken
            val nextSuccess = pendingOnSuccess
            val nextError = pendingOnError
            pendingStatus = null
            pendingToken = null
            pendingOnSuccess = null
            pendingOnError = null
            if (next != null && nextToken != null && nextSuccess != null && nextError != null) {
                viewModelScope.launch { runStatusUpdate(next, nextToken, nextSuccess, nextError) }
            }
        }
    }

    fun initializeStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
    }
}

