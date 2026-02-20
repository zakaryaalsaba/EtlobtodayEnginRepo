//
//  Offer.swift
//  RestaurantIOS
//

import Foundation

struct Offer: Codable, Identifiable {
    let id: Int
    let website_id: Int
    let offer_type: String
    let title: String
    let description: String?
    let value: Double?
    let min_order_value: Double?
    let valid_from: String?
    let valid_until: String?
    let display_order: Int?
    let restaurant_name: String?
    let logo_url: String?
    let first_product_image_url: String?
    let offer_scope: String?
    let selected_product_ids: [Int]?
    
    enum CodingKeys: String, CodingKey {
        case id, website_id, offer_type, title, description, value, min_order_value
        case valid_from, valid_until, display_order, restaurant_name, logo_url, first_product_image_url
        case offer_scope, selected_product_ids
    }
    
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        website_id = try c.decode(Int.self, forKey: .website_id)
        offer_type = try c.decode(String.self, forKey: .offer_type)
        title = try c.decode(String.self, forKey: .title)
        description = try c.decodeIfPresent(String.self, forKey: .description)
        value = try c.decodeIfPresent(Double.self, forKey: .value)
        min_order_value = try c.decodeIfPresent(Double.self, forKey: .min_order_value)
        valid_from = try c.decodeIfPresent(String.self, forKey: .valid_from)
        valid_until = try c.decodeIfPresent(String.self, forKey: .valid_until)
        display_order = try c.decodeIfPresent(Int.self, forKey: .display_order)
        restaurant_name = try c.decodeIfPresent(String.self, forKey: .restaurant_name)
        logo_url = try c.decodeIfPresent(String.self, forKey: .logo_url)
        first_product_image_url = try c.decodeIfPresent(String.self, forKey: .first_product_image_url)
        offer_scope = try c.decodeIfPresent(String.self, forKey: .offer_scope)
        if let arr = try? c.decode([Int].self, forKey: .selected_product_ids) {
            selected_product_ids = arr
        } else if let str = try? c.decode(String.self, forKey: .selected_product_ids),
                  let data = str.data(using: .utf8),
                  let arr = try? JSONDecoder().decode([Int].self, from: data) {
            selected_product_ids = arr
        } else {
            selected_product_ids = nil
        }
    }
    
    func getSelectedProductIds() -> [Int] { selected_product_ids ?? [] }
}

struct OffersListResponse: Codable {
    let offers: [Offer]
}

struct OffersResponse: Codable {
    let offers: [Offer]
}
