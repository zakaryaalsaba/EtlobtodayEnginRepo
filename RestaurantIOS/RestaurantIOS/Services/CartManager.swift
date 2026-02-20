//
//  CartManager.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

class CartManager: ObservableObject {
    static let shared = CartManager()
    
    @Published var items: [CartItem] = []
    @Published var restaurantId: Int?
    
    private init() {}
    
    /// Current restaurant id when cart is non-empty (for “different restaurant” confirmation).
    var currentRestaurantId: Int? { restaurantId }
    
    func addItem(_ product: Product, restaurantId: Int, unitPriceOverride: Double? = nil, selectedAddons: [ProductAddon] = []) {
        if restaurantId != self.restaurantId, self.restaurantId != nil {
            clearCart()
        }
        self.restaurantId = restaurantId
        let addonIds = selectedAddons.map(\.id).sorted()
        if let index = items.firstIndex(where: { item in
            item.product.id == product.id && item.selectedAddons.map(\.id).sorted() == addonIds
        }) {
            items[index].quantity += 1
        } else {
            items.append(CartItem(product: product, quantity: 1, unitPriceOverride: unitPriceOverride, selectedAddons: selectedAddons))
        }
    }
    
    /// Replace cart with a single item (after user confirms “clear and add”).
    func clearCartAndAdd(_ product: Product, restaurantId: Int, unitPriceOverride: Double? = nil, selectedAddons: [ProductAddon] = []) {
        clearCart()
        addItem(product, restaurantId: restaurantId, unitPriceOverride: unitPriceOverride, selectedAddons: selectedAddons)
    }
    
    func updateQuantity(productId: Int, quantity: Int) {
        updateQuantity(productId: productId, addonIds: [], quantity: quantity)
    }
    
    func updateQuantity(productId: Int, addonIds: [Int], quantity: Int) {
        let sorted = addonIds.sorted()
        guard quantity > 0 else {
            items.removeAll { item in
                item.product.id == productId && item.selectedAddons.map(\.id).sorted() == sorted
            }
            if items.isEmpty { restaurantId = nil }
            return
        }
        if let index = items.firstIndex(where: { item in
            item.product.id == productId && item.selectedAddons.map(\.id).sorted() == sorted
        }) {
            items[index].quantity = quantity
        }
    }
    
    /// Quantity in cart for this product (0 if not in cart).
    func quantity(for productId: Int) -> Int {
        items.first(where: { $0.product.id == productId })?.quantity ?? 0
    }
    
    /// Remove cart line matching product + addon set (like Android removeFromCart).
    func removeFromCart(productId: Int, addonIds: [Int] = []) {
        let sorted = addonIds.sorted()
        items.removeAll { item in
            item.product.id == productId && item.selectedAddons.map(\.id).sorted() == sorted
        }
        if items.isEmpty { restaurantId = nil }
    }
    
    func removeItem(_ product: Product) {
        items.removeAll { $0.product.id == product.id }
        if items.isEmpty {
            restaurantId = nil
        }
    }
    
    /// Total item count (sum of quantities) for display.
    var itemCount: Int {
        items.reduce(0) { $0 + $1.quantity }
    }
    
    func updateQuantity(_ product: Product, quantity: Int) {
        if quantity <= 0 {
            removeItem(product)
        } else {
            if let index = items.firstIndex(where: { $0.product.id == product.id }) {
                items[index].quantity = quantity
            }
        }
    }
    
    func clearCart() {
        items.removeAll()
        restaurantId = nil
    }
    
    var total: Double {
        items.reduce(0) { $0 + $1.subtotal }
    }
    
    var isEmpty: Bool {
        items.isEmpty
    }
    
    func getItemsForOrder() -> [OrderItemRequest] {
        return items.map { OrderItemRequest(product_id: $0.product.id, quantity: $0.quantity) }
    }
}

