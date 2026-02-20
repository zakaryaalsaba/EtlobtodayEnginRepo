//
//  Restaurant.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct Restaurant: Codable, Identifiable {
    let id: Int
    let restaurant_name: String
    let logo_url: String?
    let description: String?
    let address: String?
    let phone: String?
    let email: String?
    let primary_color: String?
    let secondary_color: String?
    let is_published: Bool
    let payment_methods: String? // JSON string
    let currency_code: String?
    let currency_symbol_position: String? // "before" or "after"
    let tax_enabled: Bool?
    let tax_rate: Double?
    let delivery_fee: Double?
    let order_type_dine_in_enabled: Bool?
    let order_type_pickup_enabled: Bool?
    let order_type_delivery_enabled: Bool?
    let delivery_company_id: Int?
    let delivery_time_min: Int?
    let delivery_time_max: Int?
    let created_at: String?
    
    
    // Additional fields from database (ignored during decoding)
    enum CodingKeys: String, CodingKey {
        case id
        case restaurant_name
        case logo_url
        case description
        case address
        case phone
        case email
        case primary_color
        case secondary_color
        case is_published
        // payment_methods handled separately due to type variability
        case currency_code
        case currency_symbol_position
        case tax_enabled
        case tax_rate
        case delivery_fee
        case order_type_dine_in_enabled
        case order_type_pickup_enabled
        case order_type_delivery_enabled
        case delivery_company_id
        case delivery_time_min
        case delivery_time_max
        case created_at
        // Ignore other fields that might be in the response
    }
    
    // Custom decoding to handle boolean from MySQL (0/1), string numbers, and missing fields
    // Note: payment_methods is excluded from CodingKeys to avoid type mismatch errors
    // (it can be either a string or dictionary in the JSON)
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Required fields
        id = try container.decode(Int.self, forKey: .id)
        restaurant_name = try container.decode(String.self, forKey: .restaurant_name)
        
        // Optional fields - use decodeIfPresent to handle missing or null fields
        logo_url = try container.decodeIfPresent(String.self, forKey: .logo_url)
        description = try container.decodeIfPresent(String.self, forKey: .description)
        address = try container.decodeIfPresent(String.self, forKey: .address)
        phone = try container.decodeIfPresent(String.self, forKey: .phone)
        email = try container.decodeIfPresent(String.self, forKey: .email)
        primary_color = try container.decodeIfPresent(String.self, forKey: .primary_color)
        secondary_color = try container.decodeIfPresent(String.self, forKey: .secondary_color)
        // Handle payment_methods separately - can be a string or dictionary
        // Access it through the decoder's userInfo or by creating a new container
        payment_methods = nil
        // We'll handle payment_methods by accessing the raw decoder
        // For now, set to nil - the app can work without it
        // TODO: Implement proper handling of dictionary case
        currency_code = try container.decodeIfPresent(String.self, forKey: .currency_code)
        currency_symbol_position = try container.decodeIfPresent(String.self, forKey: .currency_symbol_position)
        created_at = try container.decodeIfPresent(String.self, forKey: .created_at)
        // Handle boolean fields that might come as Int (0/1) from MySQL
        if let boolValue = try? container.decode(Bool.self, forKey: .tax_enabled) {
            tax_enabled = boolValue
        } else if let intValue = try? container.decode(Int.self, forKey: .tax_enabled) {
            tax_enabled = intValue == 1
        } else {
            tax_enabled = nil
        }
        
        // Numeric optional fields - handle both string and number formats
        if let doubleValue = try? container.decode(Double.self, forKey: .tax_rate) {
            tax_rate = doubleValue
        } else if let stringValue = try? container.decode(String.self, forKey: .tax_rate), let doubleValue = Double(stringValue) {
            tax_rate = doubleValue
        } else {
            tax_rate = nil
        }
        
        if let doubleValue = try? container.decode(Double.self, forKey: .delivery_fee) {
            delivery_fee = doubleValue
        } else if let stringValue = try? container.decode(String.self, forKey: .delivery_fee), let doubleValue = Double(stringValue) {
            delivery_fee = doubleValue
        } else {
            delivery_fee = nil
        }
        
        // Handle order type boolean fields
        if let boolValue = try? container.decode(Bool.self, forKey: .order_type_dine_in_enabled) {
            order_type_dine_in_enabled = boolValue
        } else if let intValue = try? container.decode(Int.self, forKey: .order_type_dine_in_enabled) {
            order_type_dine_in_enabled = intValue == 1
        } else {
            order_type_dine_in_enabled = nil
        }
        
        if let boolValue = try? container.decode(Bool.self, forKey: .order_type_pickup_enabled) {
            order_type_pickup_enabled = boolValue
        } else if let intValue = try? container.decode(Int.self, forKey: .order_type_pickup_enabled) {
            order_type_pickup_enabled = intValue == 1
        } else {
            order_type_pickup_enabled = nil
        }
        
        if let boolValue = try? container.decode(Bool.self, forKey: .order_type_delivery_enabled) {
            order_type_delivery_enabled = boolValue
        } else if let intValue = try? container.decode(Int.self, forKey: .order_type_delivery_enabled) {
            order_type_delivery_enabled = intValue == 1
        } else {
            order_type_delivery_enabled = nil
        }
        
        delivery_company_id = try container.decodeIfPresent(Int.self, forKey: .delivery_company_id)
        delivery_time_min = try container.decodeIfPresent(Int.self, forKey: .delivery_time_min)
        delivery_time_max = try container.decodeIfPresent(Int.self, forKey: .delivery_time_max)
        
        // Handle is_published as Int (0/1) or Bool - default to false if missing
        // Use decodeIfPresent first to check if key exists
        if container.contains(.is_published) {
            if let boolValue = try? container.decode(Bool.self, forKey: .is_published) {
                is_published = boolValue
            } else if let intValue = try? container.decode(Int.self, forKey: .is_published) {
                is_published = intValue == 1
            } else {
                is_published = false
            }
        } else {
            // Key doesn't exist, default to false
            is_published = false
        }
    }
    
    // Encoding support (for completeness, though we mainly decode)
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(restaurant_name, forKey: .restaurant_name)
        try container.encodeIfPresent(logo_url, forKey: .logo_url)
        try container.encodeIfPresent(description, forKey: .description)
        try container.encodeIfPresent(address, forKey: .address)
        try container.encodeIfPresent(phone, forKey: .phone)
        try container.encodeIfPresent(email, forKey: .email)
        try container.encodeIfPresent(primary_color, forKey: .primary_color)
        try container.encodeIfPresent(secondary_color, forKey: .secondary_color)
        try container.encode(is_published, forKey: .is_published)
        // payment_methods is not encoded (excluded from CodingKeys to avoid type issues)
        try container.encodeIfPresent(currency_code, forKey: .currency_code)
        try container.encodeIfPresent(currency_symbol_position, forKey: .currency_symbol_position)
        try container.encodeIfPresent(tax_enabled, forKey: .tax_enabled)
        try container.encodeIfPresent(tax_rate, forKey: .tax_rate)
        try container.encodeIfPresent(delivery_fee, forKey: .delivery_fee)
        try container.encodeIfPresent(order_type_dine_in_enabled, forKey: .order_type_dine_in_enabled)
        try container.encodeIfPresent(order_type_pickup_enabled, forKey: .order_type_pickup_enabled)
        try container.encodeIfPresent(order_type_delivery_enabled, forKey: .order_type_delivery_enabled)
        try container.encodeIfPresent(delivery_company_id, forKey: .delivery_company_id)
        try container.encodeIfPresent(delivery_time_min, forKey: .delivery_time_min)
        try container.encodeIfPresent(delivery_time_max, forKey: .delivery_time_max)
        try container.encodeIfPresent(created_at, forKey: .created_at)
    }
}

struct CliQServices: Codable {
    let enabled: Bool
    let phone: String?
    let name: String?
    
    enum CodingKeys: String, CodingKey {
        case enabled
        case phone
        case name
    }
    
    init(enabled: Bool = false, phone: String? = nil, name: String? = nil) {
        self.enabled = enabled
        self.phone = phone
        self.name = name
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        enabled = try container.decodeIfPresent(Bool.self, forKey: .enabled) ?? false
        phone = try container.decodeIfPresent(String.self, forKey: .phone)
        name = try container.decodeIfPresent(String.self, forKey: .name)
    }
}

struct PaymentMethods: Codable {
    let cashOnPickup: Bool
    let cashOnDelivery: Bool
    let creditCard: Bool
    let onlinePayment: Bool
    let mobilePayment: Bool
    let cliQServices: CliQServices?
    
    enum CodingKeys: String, CodingKey {
        case cashOnPickup
        case cashOnDelivery
        case creditCard
        case onlinePayment
        case mobilePayment
        case cliQServices
    }
    
    // Manual initializer for creating instances
    init(cashOnPickup: Bool = true, cashOnDelivery: Bool = true, creditCard: Bool = false, onlinePayment: Bool = false, mobilePayment: Bool = false, cliQServices: CliQServices? = nil) {
        self.cashOnPickup = cashOnPickup
        self.cashOnDelivery = cashOnDelivery
        self.creditCard = creditCard
        self.onlinePayment = onlinePayment
        self.mobilePayment = mobilePayment
        self.cliQServices = cliQServices
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        cashOnPickup = try container.decodeIfPresent(Bool.self, forKey: .cashOnPickup) ?? true
        cashOnDelivery = try container.decodeIfPresent(Bool.self, forKey: .cashOnDelivery) ?? true
        creditCard = try container.decodeIfPresent(Bool.self, forKey: .creditCard) ?? false
        onlinePayment = try container.decodeIfPresent(Bool.self, forKey: .onlinePayment) ?? false
        mobilePayment = try container.decodeIfPresent(Bool.self, forKey: .mobilePayment) ?? false
        cliQServices = try container.decodeIfPresent(CliQServices.self, forKey: .cliQServices)
    }
}

struct RestaurantsResponse: Codable {
    let websites: [Restaurant]
}

struct WebsiteResponse: Codable {
    let website: Restaurant
}

