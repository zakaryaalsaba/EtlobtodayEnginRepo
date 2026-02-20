//
//  Region.swift
//  RestaurantIOS
//

import Foundation

struct Region: Codable, Identifiable {
    let id: Int
    let name: String?
    let name_ar: String?
    let city_id: Int?
}

struct RegionsResponse: Codable {
    let regions: [Region]
}

struct Area: Codable, Identifiable {
    let id: Int
    let region_id: Int
    let name: String?
    let name_ar: String?
}

struct AreasResponse: Codable {
    let areas: [Area]
}

struct DeliveryZone: Codable, Identifiable {
    let id: Int
    let area_id: Int
    let delivery_company_id: Int
    let name: String?
    let name_ar: String?
    let price: Double?
    let is_active: Bool?
}

struct DeliveryZonesResponse: Codable {
    let zones: [DeliveryZone]
}
