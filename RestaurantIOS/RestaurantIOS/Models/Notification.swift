//
//  Notification.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct AppNotification: Codable, Identifiable {
    let id: Int
    let customer_id: Int?
    let order_id: Int?
    let website_id: Int?
    let title: String
    let message: String
    let type: String
    let status: String?
    let is_read: Bool
    let restaurant_name: String?
    let order_number: String?
    let created_at: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case customer_id
        case order_id
        case website_id
        case title
        case message
        case type
        case status
        case is_read
        case restaurant_name
        case order_number
        case created_at
    }
    
    // Custom decoding to handle boolean from MySQL (0/1)
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        customer_id = try container.decodeIfPresent(Int.self, forKey: .customer_id)
        order_id = try container.decodeIfPresent(Int.self, forKey: .order_id)
        website_id = try container.decodeIfPresent(Int.self, forKey: .website_id)
        title = try container.decode(String.self, forKey: .title)
        message = try container.decode(String.self, forKey: .message)
        type = try container.decodeIfPresent(String.self, forKey: .type) ?? "order_update"
        status = try container.decodeIfPresent(String.self, forKey: .status)
        restaurant_name = try container.decodeIfPresent(String.self, forKey: .restaurant_name)
        order_number = try container.decodeIfPresent(String.self, forKey: .order_number)
        created_at = try container.decode(String.self, forKey: .created_at)
        
        // Handle is_read as Int (0/1) or Bool
        if let boolValue = try? container.decode(Bool.self, forKey: .is_read) {
            is_read = boolValue
        } else if let intValue = try? container.decode(Int.self, forKey: .is_read) {
            is_read = intValue == 1
        } else {
            is_read = false
        }
    }
}

struct NotificationsResponse: Codable {
    let notifications: [AppNotification]
}

struct ApiResponse: Codable {
    let success: Bool
    let message: String
}

struct DeviceTokenRequest: Codable {
    let device_token: String
    let device_type: String
}

struct LocationRequest: Codable {
    let latitude: Double
    let longitude: Double
    let address: String?
}

struct LocationResponse: Codable {
    let success: Bool
    let message: String
    let location: LocationData?
}

struct LocationData: Codable {
    let latitude: Double
    let longitude: Double
    let address: String?
}

