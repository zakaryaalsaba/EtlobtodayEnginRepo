//
//  CartItem.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct CartItem: Identifiable {
    let id = UUID()
    let product: Product
    var quantity: Int
    let unitPriceOverride: Double?
    /// Selected addons (for products with addon_required). Subtotal includes addon prices per unit.
    let selectedAddons: [ProductAddon]
    
    init(product: Product, quantity: Int, unitPriceOverride: Double? = nil, selectedAddons: [ProductAddon] = []) {
        self.product = product
        self.quantity = quantity
        self.unitPriceOverride = unitPriceOverride
        self.selectedAddons = selectedAddons
    }
    
    var subtotal: Double {
        let unit = unitPriceOverride ?? product.price
        let addonsTotal = selectedAddons.reduce(0.0) { $0 + $1.price }
        return (unit + addonsTotal) * Double(quantity)
    }
}

