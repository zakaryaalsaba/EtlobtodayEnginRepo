package com.mnsf.resturantandroid.util

import android.util.Log
import com.mnsf.resturantandroid.data.model.CartItem
import com.mnsf.resturantandroid.data.model.Product
import com.mnsf.resturantandroid.data.model.ProductAddon

/**
 * Singleton CartManager to share cart state across activities
 */
object CartManager {
    private val _cartItems = mutableListOf<CartItem>()
    
    fun getCartItems(): MutableList<CartItem> {
        return _cartItems
    }
    
    fun getCartItemsList(): List<CartItem> {
        return _cartItems.toList()
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
        val addonIds = addons.map { it.id }.sorted()
        val existingItem = _cartItems.find { it.product.id == product.id && it.addonIdsSorted() == addonIds }

        if (existingItem != null) {
            existingItem.quantity++
            val q = existingItem.quantity
            val lineSubtotal = existingItem.getSubtotal()
            Log.d("MoneyLog", "[CartManager] addToCart MERGED product id=${product.id} name=${product.name} product.price=${product.price} unitPriceOverride=$unitPriceOverride addonIds=$addonIds addonPriceOverrides=$addonPriceOverrides quantity=$q lineSubtotal=$lineSubtotal cartTotal=${getTotalPrice()}")
        } else {
            val newItem = CartItem(product, 1, addons, unitPriceOverride, addonPriceOverrides)
            _cartItems.add(newItem)
            val lineSubtotal = newItem.getSubtotal()
            Log.d("MoneyLog", "[CartManager] addToCart NEW product id=${product.id} name=${product.name} product.price=${product.price} unitPriceOverride=$unitPriceOverride addons=${addons.map { "${it.id}:${it.price}" }} addonPriceOverrides=$addonPriceOverrides quantity=1 lineSubtotal=$lineSubtotal cartTotal=${getTotalPrice()}")
        }
    }
    
    fun removeFromCart(productId: Int, addonIds: List<Int> = emptyList()) {
        val sortedIds = addonIds.sorted()
        val removed = _cartItems.removeAll { it.product.id == productId && it.addonIdsSorted() == sortedIds }
        if (removed) Log.d("MoneyLog", "[CartManager] removeFromCart productId=$productId addonIds=$sortedIds cartTotal=${getTotalPrice()}")
    }
    
    fun updateQuantity(productId: Int, addonIds: List<Int>, quantity: Int) {
        val sortedIds = addonIds.sorted()
        val item = _cartItems.find { it.product.id == productId && it.addonIdsSorted() == sortedIds }
        
        if (item != null) {
            if (quantity <= 0) {
                _cartItems.remove(item)
                Log.d("MoneyLog", "[CartManager] updateQuantity REMOVED productId=$productId addonIds=$sortedIds cartTotal=${getTotalPrice()}")
            } else {
                item.quantity = quantity
                val lineSubtotal = item.getSubtotal()
                Log.d("MoneyLog", "[CartManager] updateQuantity productId=$productId addonIds=$sortedIds quantity=$quantity lineSubtotal=$lineSubtotal cartTotal=${getTotalPrice()}")
            }
        }
    }
    
    fun getTotalPrice(): Double {
        val total = kotlin.math.round(_cartItems.sumOf { it.getSubtotal() } * 100.0) / 100.0
        Log.d("MoneyLog", "[CartManager] getTotalPrice items=${_cartItems.size} total=$total")
        return total
    }
    
    fun getItemCount(): Int {
        return _cartItems.sumOf { it.quantity }
    }
    
    fun clearCart() {
        Log.d("MoneyLog", "[CartManager] clearCart (was ${_cartItems.size} items)")
        _cartItems.clear()
    }
    
    fun getCartItemsForRestaurant(restaurantId: Int): List<CartItem> {
        return _cartItems.filter { it.product.website_id == restaurantId }
    }
    
    /**
     * Get the restaurant ID of items currently in the cart
     * Returns null if cart is empty
     */
    fun getCurrentRestaurantId(): Int? {
        return _cartItems.firstOrNull()?.product?.website_id
    }
    
    /**
     * Check if cart has items from a different restaurant than the given product
     */
    fun hasItemsFromDifferentRestaurant(productWebsiteId: Int): Boolean {
        val currentRestaurantId = getCurrentRestaurantId()
        return currentRestaurantId != null && currentRestaurantId != productWebsiteId
    }
    
    /**
     * Check if cart is empty
     */
    fun isEmpty(): Boolean {
        return _cartItems.isEmpty()
    }
}

