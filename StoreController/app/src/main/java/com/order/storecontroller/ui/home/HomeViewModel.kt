package com.order.storecontroller.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.order.storecontroller.data.model.Order
import com.order.storecontroller.service.FirebaseOrderService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeViewModel : ViewModel() {

    private val firebaseOrderService = FirebaseOrderService()

    private val _allOrders = MutableLiveData<List<Order>>(emptyList())
    val allOrders: LiveData<List<Order>> = _allOrders

    private val _filterWebsiteIds = MutableLiveData<Set<Int>>(emptySet())
    val filterWebsiteIds: LiveData<Set<Int>> = _filterWebsiteIds

    private val _filterStatus = MutableLiveData<String?>(null)
    val filterStatus: LiveData<String?> = _filterStatus

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Event: play sound when a new order is received (not on initial load). */
    private val _newOrderReceived = MutableLiveData<Boolean>()
    val newOrderReceived: LiveData<Boolean> = _newOrderReceived

    private var previousOrderKeys: Set<String> = emptySet()
    private var listenerJob: Job? = null

    init {
        startListening()
    }

    fun startListening() {
        listenerJob?.cancel()
        listenerJob = null
        previousOrderKeys = emptySet()
        _isLoading.value = true
        _error.value = null
        listenerJob = firebaseOrderService.listenToOrders(websiteIds = _filterWebsiteIds.value.orEmpty())
            .onEach { list ->
                val currentKeys = list.map { "${it.website_id}_${it.order_number}" }.toSet()
                val newKeys = currentKeys - previousOrderKeys
                if (newKeys.isNotEmpty() && previousOrderKeys.isNotEmpty()) {
                    _newOrderReceived.value = true
                }
                previousOrderKeys = currentKeys
                _allOrders.value = list
                _isLoading.value = false
                _error.value = null
            }
            .catch { e ->
                _error.value = e.message
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    fun onNewOrderSoundPlayed() {
        _newOrderReceived.value = false
    }

    fun setWebsiteFilter(websiteIds: Set<Int>) {
        _filterWebsiteIds.value = websiteIds
        startListening()
    }

    fun setStatusFilter(status: String?) {
        _filterStatus.value = status
    }

    fun getFilteredOrders(): List<Order> {
        val all = _allOrders.value ?: return emptyList()
        val websiteIds = _filterWebsiteIds.value
        val status = _filterStatus.value
        return all
            .filter { if (websiteIds.isEmpty()) true else it.website_id in websiteIds }
            .filter { if (status.isNullOrEmpty()) true else it.status.equals(status, ignoreCase = true) }
    }
}
