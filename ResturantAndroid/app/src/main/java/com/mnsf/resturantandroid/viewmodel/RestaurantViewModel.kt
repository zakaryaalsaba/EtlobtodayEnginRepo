package com.mnsf.resturantandroid.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.data.model.Restaurant
import com.mnsf.resturantandroid.repository.RestaurantRepository
import kotlinx.coroutines.launch

class RestaurantViewModel(private val restaurantRepository: RestaurantRepository) : ViewModel() {
    
    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> = _restaurants
    
    private val _filteredRestaurants = MutableLiveData<List<Restaurant>>()
    val filteredRestaurants: LiveData<List<Restaurant>> = _filteredRestaurants
    
    private val _selectedRestaurant = MutableLiveData<Restaurant?>()
    val selectedRestaurant: LiveData<Restaurant?> = _selectedRestaurant
    
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private var currentSearchQuery: String = ""
    private var currentOrderType: String = "delivery"

    private val _offers = MutableLiveData<List<Offer>>(emptyList())
    val offers: LiveData<List<Offer>> = _offers

    private val _restaurantOffers = MutableLiveData<List<Offer>>(emptyList())
    val restaurantOffers: LiveData<List<Offer>> = _restaurantOffers
    
    /** Apply both search and order-type filter. Order type uses restaurant_websites order_type_*_enabled. */
    private fun applyFilters() {
        val list = _restaurants.value ?: emptyList()
        val byOrderType = list.filter { r ->
            when (currentOrderType) {
                "pickup" -> r.order_type_pickup_enabled != false
                else -> r.order_type_delivery_enabled != false
            }
        }
        val bySearch = if (currentSearchQuery.isBlank()) byOrderType else byOrderType.filter {
            it.restaurant_name.contains(currentSearchQuery, ignoreCase = true) ||
            it.description?.contains(currentSearchQuery, ignoreCase = true) == true ||
            it.address?.contains(currentSearchQuery, ignoreCase = true) == true
        }
        _filteredRestaurants.value = bySearch
    }
    
    /** Set order type (delivery / pickup) and refilter. Used by home screen toggle. */
    fun setOrderType(type: String) {
        if (type != currentOrderType) {
            currentOrderType = if (type == "pickup") "pickup" else "delivery"
            applyFilters()
        }
    }
    
    fun loadRestaurants() {
        Log.e("LoginFlow", "RestaurantViewModel.loadRestaurants: CALLED")
        viewModelScope.launch {
            try {
                Log.d("RestaurantViewModel", "loadRestaurants: Starting")
                Log.e("LoginFlow", "RestaurantViewModel.loadRestaurants: Inside coroutine")
                _isLoading.value = true
                _error.value = null
                
                val result = restaurantRepository.getRestaurants()
                Log.e("LoginFlow", "RestaurantViewModel.loadRestaurants: Repository call completed, result=${result.isSuccess}")
                Log.d("RestaurantViewModel", "loadRestaurants: Repository call completed")
                _isLoading.value = false
                
                result.onSuccess { restaurantList ->
                    try {
                        Log.d("RestaurantViewModel", "loadRestaurants: Success - received ${restaurantList.size} restaurants")
                        Log.e("LoginFlow", "RestaurantViewModel.loadRestaurants: Success - ${restaurantList.size} restaurants")
                        _restaurants.value = restaurantList
                        applyFilters()
                        Log.d("RestaurantViewModel", "loadRestaurants: LiveData updated")
                        Log.e("LoginFlow", "RestaurantViewModel.loadRestaurants: LiveData updated, filteredRestaurants should trigger observer")
                    } catch (e: Exception) {
                        Log.e("RestaurantViewModel", "loadRestaurants: Error updating LiveData", e)
                        Log.e("LoginFlow", "RestaurantViewModel.loadRestaurants: Error updating LiveData - ${e.message}")
                        _error.value = "Error processing restaurants: ${e.message}"
                    }
                }.onFailure { exception ->
                    Log.e("RestaurantViewModel", "loadRestaurants: Failed", exception)
                    Log.e("LoginFlow", "RestaurantViewModel.loadRestaurants: Failed - ${exception.message}")
                    _error.value = exception.message ?: "Failed to load restaurants"
                }
                loadOffers()
            } catch (e: Exception) {
                Log.e("RestaurantViewModel", "loadRestaurants: Fatal error", e)
                _isLoading.value = false
                _error.value = "Fatal error: ${e.message}"
            }
        }
    }

    fun loadOffers() {
        viewModelScope.launch {
            try {
                val result = restaurantRepository.getOffersList()
                result.onSuccess { list -> _offers.value = list }
                result.onFailure { _ -> _offers.value = emptyList() }
            } catch (e: Exception) {
                _offers.value = emptyList()
            }
        }
    }
    
    fun searchRestaurants(query: String) {
        currentSearchQuery = query.trim()
        applyFilters()
    }
    
    fun loadRestaurant(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = restaurantRepository.getRestaurant(id)
            _isLoading.value = false
            
            result.onSuccess { restaurant ->
                _selectedRestaurant.value = restaurant
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to load restaurant"
            }
        }
    }
    
    fun loadProducts(websiteId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = restaurantRepository.getProducts(websiteId)
            _isLoading.value = false
            
            result.onSuccess { productList ->
                _products.value = productList
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to load menu"
            }
        }
    }

    fun loadRestaurantOffers(websiteId: Int) {
        viewModelScope.launch {
            val result = restaurantRepository.getOffersByWebsiteId(websiteId)
            result.onSuccess { list -> _restaurantOffers.value = list }
            result.onFailure { _ -> _restaurantOffers.value = emptyList() }
        }
    }
    
    fun getProductsByCategory(): Map<String, List<Product>> {
        val productList = _products.value ?: emptyList()
        return productList.groupBy { it.category ?: "Other" }
    }
}

