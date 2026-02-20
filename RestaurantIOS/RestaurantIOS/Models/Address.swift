//
//  Address.swift
//  RestaurantIOS
//

import Foundation

struct Address: Codable, Identifiable {
    let id: Int
    let customer_id: Int
    let area: String?
    let region_id: Int?
    let region_name: String?
    let area_id: Int?
    let area_name: String?
    let zone_id: Int?
    let zone_name: String?
    let zone_price: Double?
    let latitude: Double?
    let longitude: Double?
    let address_type: String?
    let building_name: String?
    let apartment_number: String?
    let floor: String?
    let street: String?
    let phone_country_code: String?
    let phone_number: String?
    let additional_directions: String?
    let address_label: String?
    let is_default: Bool
    let created_at: String?
    let updated_at: String?
    
    enum CodingKeys: String, CodingKey {
        case id, customer_id, area, region_id, region_name, area_id, area_name
        case zone_id, zone_name, zone_price, latitude, longitude, address_type
        case building_name, apartment_number, floor, street, phone_country_code
        case phone_number, additional_directions, address_label, is_default
        case created_at, updated_at
    }
    
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        customer_id = try c.decode(Int.self, forKey: .customer_id)
        area = try c.decodeIfPresent(String.self, forKey: .area)
        region_id = try c.decodeIfPresent(Int.self, forKey: .region_id)
        region_name = try c.decodeIfPresent(String.self, forKey: .region_name)
        area_id = try c.decodeIfPresent(Int.self, forKey: .area_id)
        area_name = try c.decodeIfPresent(String.self, forKey: .area_name)
        zone_id = try c.decodeIfPresent(Int.self, forKey: .zone_id)
        zone_name = try c.decodeIfPresent(String.self, forKey: .zone_name)
        zone_price = try c.decodeIfPresent(Double.self, forKey: .zone_price)
        latitude = try c.decodeIfPresent(Double.self, forKey: .latitude)
        longitude = try c.decodeIfPresent(Double.self, forKey: .longitude)
        address_type = try c.decodeIfPresent(String.self, forKey: .address_type)
        building_name = try c.decodeIfPresent(String.self, forKey: .building_name)
        apartment_number = try c.decodeIfPresent(String.self, forKey: .apartment_number)
        floor = try c.decodeIfPresent(String.self, forKey: .floor)
        street = try c.decodeIfPresent(String.self, forKey: .street)
        phone_country_code = try c.decodeIfPresent(String.self, forKey: .phone_country_code)
        phone_number = try c.decodeIfPresent(String.self, forKey: .phone_number)
        additional_directions = try c.decodeIfPresent(String.self, forKey: .additional_directions)
        address_label = try c.decodeIfPresent(String.self, forKey: .address_label)
        if let b = try? c.decode(Bool.self, forKey: .is_default) {
            is_default = b
        } else if let i = try? c.decode(Int.self, forKey: .is_default) {
            is_default = i != 0
        } else {
            is_default = false
        }
        created_at = try c.decodeIfPresent(String.self, forKey: .created_at)
        updated_at = try c.decodeIfPresent(String.self, forKey: .updated_at)
    }
}

struct AddressesResponse: Codable {
    let addresses: [Address]
}

struct AddressResponse: Codable {
    let address: Address
}

struct CreateAddressRequest: Codable {
    let area: String?
    let region_id: Int?
    let region_name: String?
    let area_id: Int?
    let area_name: String?
    let zone_id: Int?
    let zone_name: String?
    let zone_price: Double?
    let latitude: Double?
    let longitude: Double?
    let address_type: String?
    let building_name: String?
    let apartment_number: String?
    let floor: String?
    let street: String?
    let phone_country_code: String?
    let phone_number: String?
    let additional_directions: String?
    let address_label: String?
    let is_default: Bool?
}
