package com.mnsf.resturantandroid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mnsf.resturantandroid.data.model.CartItem
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.data.model.ProductAddon
import com.mnsf.resturantandroid.util.CartManager

class CartViewModel : ViewModel() {
    
    private val _cartItems = MutableLiveData<MutableList<CartItem>>()
    val cartItems: LiveData<MutableList<CartItem>> = _cartItems
    
    init {
        // Initialize with current cart items from CartManager
        updateCartItems()
    }
    
    private fun updateCartItems() {
        _cartItems.value = CartManager.getCartItems()
    }
    
    /**
     * @param unitPriceOverride When non-null, used as product unit price (e.g. discounted by offer).
     * @param addonPriceOverrides When non-null, addon id -> price for subtotal (e.g. offer discount).
     */
    fun addToCart(
        product: Product,
        addons: List<ProductAddon> = emptyList(),
        unitPriceOverride: Double? = null,
        addonPriceOverrides: Map<Int, Double>? = null
    ) {
        CartManager.addToCart(product, addons, unitPriceOverride, addonPriceOverrides)
        updateCartItems()
        notifyCartChanged()
    }
    
    fun removeFromCart(productId: Int, addonIds: List<Int> = emptyList()) {
        CartManager.removeFromCart(productId, addonIds)
        updateCartItems()
        notifyCartChanged()
    }
    
    fun updateQuantity(productId: Int, addonIds: List<Int>, quantity: Int) {
        CartManager.updateQuantity(productId, addonIds, quantity)
        updateCartItems()
        notifyCartChanged()
    }
    
    private fun notifyCartChanged() {
        // Create a new list reference to trigger LiveData observer
        _cartItems.value = CartManager.getCartItems().toMutableList()
    }
    
    fun getTotalPrice(): Double {
        return CartManager.getTotalPrice()
    }
    
    fun getItemCount(): Int {
        return CartManager.getItemCount()
    }
    
    fun isEmpty(): Boolean {
        return CartManager.isEmpty()
    }
    
    fun clearCart() {
        CartManager.clearCart()
        updateCartItems()
        notifyCartChanged()
    }
    
    fun getCartItemsForRestaurant(restaurantId: Int): List<CartItem> {
        return CartManager.getCartItemsForRestaurant(restaurantId)
    }
    
    /**
     * Get the current restaurant ID in the cart
     */
    fun getCurrentRestaurantId(): Int? {
        return CartManager.getCurrentRestaurantId()
    }
    
    /**
     * Check if adding this product would require clearing the cart
     */
    fun requiresCartClear(product: Product): Boolean {
        return CartManager.hasItemsFromDifferentRestaurant(product.website_id)
    }
    
    /**
     * Clear cart and add a new product (with optional add-ons and price overrides for offers).
     */
    fun clearCartAndAdd(
        product: Product,
        addons: List<ProductAddon> = emptyList(),
        unitPriceOverride: Double? = null,
        addonPriceOverrides: Map<Int, Double>? = null
    ) {
        CartManager.clearCart()
        CartManager.addToCart(product, addons, unitPriceOverride, addonPriceOverrides)
        updateCartItems()
        notifyCartChanged()
    }
    
    // Refresh cart items from CartManager (useful when returning to activity)
    fun refreshCart() {
        updateCartItems()
        notifyCartChanged()
    }
}

