package com.mnsf.resturantandroid.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnsf.resturantandroid.data.model.Notification
import com.mnsf.resturantandroid.repository.NotificationRepository
import com.mnsf.resturantandroid.util.SessionManager
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadNotifications() {
        val customerId = sessionManager.getCustomerId()
        if (customerId == -1) {
            _error.value = "Not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            notificationRepository.getNotifications(customerId)
                .onSuccess { notificationList ->
                    _notifications.value = notificationList
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load notifications"
                    _isLoading.value = false
                }
        }
    }

    fun markAsRead(notificationId: Int) {
        val customerId = sessionManager.getCustomerId()
        if (customerId == -1) return

        viewModelScope.launch {
            notificationRepository.markAsRead(customerId, notificationId)
                .onSuccess {
                    // Update local notification list
                    val currentNotifications = _notifications.value?.toMutableList()
                    currentNotifications?.find { it.id == notificationId }?.let { notification ->
                        val index = currentNotifications.indexOf(notification)
                        currentNotifications[index] = notification.copy(isRead = true)
                        _notifications.value = currentNotifications
                    }
                }
                .onFailure { exception ->
                    android.util.Log.e("NotificationsViewModel", "Failed to mark as read", exception)
                }
        }
    }

    fun markAllAsRead() {
        val customerId = sessionManager.getCustomerId()
        if (customerId == -1) return

        viewModelScope.launch {
            notificationRepository.markAllAsRead(customerId)
                .onSuccess {
                    // Update all notifications to read
                    val currentNotifications = _notifications.value?.map { it.copy(isRead = true) }
                    _notifications.value = currentNotifications
                }
                .onFailure { exception ->
                    android.util.Log.e("NotificationsViewModel", "Failed to mark all as read", exception)
                }
        }
    }

    fun getUnreadCount(): Int {
        return _notifications.value?.count { !it.isRead } ?: 0
    }
}