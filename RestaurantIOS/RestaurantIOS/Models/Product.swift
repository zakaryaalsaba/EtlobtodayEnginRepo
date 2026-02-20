//
//  Product.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct Product: Codable, Identifiable {
    let id: Int
    let website_id: Int
    let name: String
    let description: String?
    let price: Double
    let category: String?
    let image_url: String?
    let is_available: Bool
    let addon_required: Bool?
    let created_at: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case website_id
        case name
        case description
        case price
        case category
        case image_url
        case is_available
        case addon_required
        case created_at
    }
    
    // Custom decoding to handle boolean from MySQL (0/1)
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        website_id = try container.decode(Int.self, forKey: .website_id)
        name = try container.decode(String.self, forKey: .name)
        description = try container.decodeIfPresent(String.self, forKey: .description)
        
        // Handle price - can be a string or number
        if let doubleValue = try? container.decode(Double.self, forKey: .price) {
            price = doubleValue
        } else if let stringValue = try? container.decode(String.self, forKey: .price), let doubleValue = Double(stringValue) {
            price = doubleValue
        } else {
            price = 0.0 // Default to 0 if can't decode
        }
        
        category = try container.decodeIfPresent(String.self, forKey: .category)
        image_url = try container.decodeIfPresent(String.self, forKey: .image_url)
        created_at = try container.decodeIfPresent(String.self, forKey: .created_at)
        
        // Handle is_available as Int (0/1) or Bool
        if let boolValue = try? container.decode(Bool.self, forKey: .is_available) {
            is_available = boolValue
        } else if let intValue = try? container.decode(Int.self, forKey: .is_available) {
            is_available = intValue == 1
        } else {
            is_available = true
        }
        // addon_required: optional Bool/Int
        if let boolValue = try? container.decode(Bool.self, forKey: .addon_required) {
            addon_required = boolValue
        } else if let intValue = try? container.decode(Int.self, forKey: .addon_required) {
            addon_required = intValue == 1
        } else {
            addon_required = nil
        }
    }
}

struct ProductsResponse: Codable {
    let products: [Product]
}

// MARK: - Product Addon (for meals with customization)
struct ProductAddon: Codable, Identifiable {
    let id: Int
    let product_id: Int
    let name: String
    let name_ar: String?
    let description: String?
    let description_ar: String?
    let image_url: String?
    let price: Double
    let is_required: Bool
    let display_order: Int
    
    enum CodingKeys: String, CodingKey {
        case id, product_id, name, name_ar, description, description_ar, image_url, price, is_required, display_order
    }
    
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        product_id = try c.decode(Int.self, forKey: .product_id)
        name = try c.decode(String.self, forKey: .name)
        name_ar = try c.decodeIfPresent(String.self, forKey: .name_ar)
        description = try c.decodeIfPresent(String.self, forKey: .description)
        description_ar = try c.decodeIfPresent(String.self, forKey: .description_ar)
        image_url = try c.decodeIfPresent(String.self, forKey: .image_url)
        if let d = try? c.decode(Double.self, forKey: .price) { price = d }
        else if let s = try? c.decode(String.self, forKey: .price), let d = Double(s) { price = d }
        else { price = 0 }
        if let b = try? c.decode(Bool.self, forKey: .is_required) { is_required = b }
        else if let i = try? c.decode(Int.self, forKey: .is_required) { is_required = i == 1 }
        else { is_required = false }
        display_order = try c.decodeIfPresent(Int.self, forKey: .display_order) ?? 0
    }
}

struct ProductAddonsResponse: Codable {
    let addons: [ProductAddon]
    let addon_required: Bool
    let addon_required_min: Int?
    
    enum CodingKeys: String, CodingKey {
        case addons, addon_required, addon_required_min
    }
    
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        addons = try c.decode([ProductAddon].self, forKey: .addons)
        if let b = try? c.decode(Bool.self, forKey: .addon_required) {
            addon_required = b
        } else if let i = try? c.decode(Int.self, forKey: .addon_required) {
            addon_required = i == 1
        } else {
            addon_required = false
        }
        addon_required_min = try c.decodeIfPresent(Int.self, forKey: .addon_required_min)
    }
    
    init(addons: [ProductAddon], addon_required: Bool, addon_required_min: Int?) {
        self.addons = addons
        self.addon_required = addon_required
        self.addon_required_min = addon_required_min
    }
}
