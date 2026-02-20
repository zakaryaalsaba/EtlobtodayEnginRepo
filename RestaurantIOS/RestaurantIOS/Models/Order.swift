//
//  Order.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct OrderItem: Codable, Identifiable {
    let id: Int?
    let product_id: Int
    let product_name: String
    let product_price: Double
    let quantity: Int
    let subtotal: Double
    
    enum CodingKeys: String, CodingKey {
        case id
        case product_id
        case product_name
        case product_price
        case quantity
        case subtotal
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        id = try container.decodeIfPresent(Int.self, forKey: .id)
        product_id = try container.decode(Int.self, forKey: .product_id)
        product_name = try container.decode(String.self, forKey: .product_name)
        quantity = try container.decode(Int.self, forKey: .quantity)
        
        // Handle product_price - can be a string or number
        if let doubleValue = try? container.decode(Double.self, forKey: .product_price) {
            product_price = doubleValue
        } else if let stringValue = try? container.decode(String.self, forKey: .product_price), let doubleValue = Double(stringValue) {
            product_price = doubleValue
        } else {
            product_price = 0.0
        }
        
        // Handle subtotal - can be a string or number
        if let doubleValue = try? container.decode(Double.self, forKey: .subtotal) {
            subtotal = doubleValue
        } else if let stringValue = try? container.decode(String.self, forKey: .subtotal), let doubleValue = Double(stringValue) {
            subtotal = doubleValue
        } else {
            subtotal = 0.0
        }
    }
    
    // Manual initializer for creating OrderItem instances
    init(
        id: Int? = nil,
        product_id: Int,
        product_name: String,
        product_price: Double,
        quantity: Int,
        subtotal: Double
    ) {
        self.id = id
        self.product_id = product_id
        self.product_name = product_name
        self.product_price = product_price
        self.quantity = quantity
        self.subtotal = subtotal
    }
}

struct Order: Codable, Identifiable {
    let id: Int?
    let website_id: Int
    let customer_id: Int?
    let order_number: String
    let customer_name: String
    let customer_email: String?
    let customer_phone: String
    let customer_address: String?
    let order_type: String?
    let status: String
    let total_amount: Double
    let payment_method: String?
    let payment_status: String?
    let notes: String?
    let created_at: String?
    let items: [OrderItem]?
    
    enum CodingKeys: String, CodingKey {
        case id
        case website_id
        case customer_id
        case order_number
        case customer_name
        case customer_email
        case customer_phone
        case customer_address
        case order_type
        case status
        case total_amount
        case payment_method
        case payment_status
        case notes
        case created_at
        case items
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        id = try container.decodeIfPresent(Int.self, forKey: .id)
        website_id = try container.decode(Int.self, forKey: .website_id)
        customer_id = try container.decodeIfPresent(Int.self, forKey: .customer_id)
        order_number = try container.decode(String.self, forKey: .order_number)
        customer_name = try container.decode(String.self, forKey: .customer_name)
        customer_email = try container.decodeIfPresent(String.self, forKey: .customer_email)
        customer_phone = try container.decode(String.self, forKey: .customer_phone)
        customer_address = try container.decodeIfPresent(String.self, forKey: .customer_address)
        order_type = try container.decodeIfPresent(String.self, forKey: .order_type)
        status = try container.decode(String.self, forKey: .status)
        payment_method = try container.decodeIfPresent(String.self, forKey: .payment_method)
        payment_status = try container.decodeIfPresent(String.self, forKey: .payment_status)
        notes = try container.decodeIfPresent(String.self, forKey: .notes)
        created_at = try container.decodeIfPresent(String.self, forKey: .created_at)
        items = try container.decodeIfPresent([OrderItem].self, forKey: .items)
        
        // Handle total_amount - can be a string or number
        if let doubleValue = try? container.decode(Double.self, forKey: .total_amount) {
            total_amount = doubleValue
        } else if let stringValue = try? container.decode(String.self, forKey: .total_amount), let doubleValue = Double(stringValue) {
            total_amount = doubleValue
        } else {
            total_amount = 0.0
        }
    }
    
    // Manual initializer for creating Order instances (for previews, testing, etc.)
    init(
        id: Int? = nil,
        website_id: Int,
        customer_id: Int? = nil,
        order_number: String,
        customer_name: String,
        customer_email: String? = nil,
        customer_phone: String,
        customer_address: String? = nil,
        order_type: String? = nil,
        status: String,
        total_amount: Double,
        payment_method: String? = nil,
        payment_status: String? = nil,
        notes: String? = nil,
        created_at: String? = nil,
        items: [OrderItem]? = nil
    ) {
        self.id = id
        self.website_id = website_id
        self.customer_id = customer_id
        self.order_number = order_number
        self.customer_name = customer_name
        self.customer_email = customer_email
        self.customer_phone = customer_phone
        self.customer_address = customer_address
        self.order_type = order_type
        self.status = status
        self.total_amount = total_amount
        self.payment_method = payment_method
        self.payment_status = payment_status
        self.notes = notes
        self.created_at = created_at
        self.items = items
    }
}

struct CreateOrderRequest: Codable {
    let website_id: Int
    let customer_id: Int?
    let customer_name: String
    let customer_email: String?
    let customer_phone: String
    let customer_address: String?
    let order_type: String
    let payment_method: String
    let delivery_latitude: Double?
    let delivery_longitude: Double?
    let delivery_instructions: String?
    let tip: Double?
    let items: [OrderItemRequest]
    let notes: String?
    let total_amount: Double?
}

struct OrderItemRequest: Codable {
    let product_id: Int
    let quantity: Int
}

struct OrdersResponse: Codable {
    let orders: [Order]
}

struct OrderResponse: Codable {
    let order: Order
}

