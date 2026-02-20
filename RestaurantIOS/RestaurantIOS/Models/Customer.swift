//
//  Customer.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct Customer: Codable, Equatable {
    let id: Int
    let name: String
    let email: String?
    let phone: String?
    let address: String?
    let profile_picture_url: String?
    let created_at: String?
    let updated_at: String?
}

struct AuthResponse: Codable {
    let customer: Customer
    let token: String
    let message: String
}

struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct RegisterRequest: Codable {
    let name: String
    let email: String
    let password: String
    let phone: String?
    let address: String?
}

struct PhoneLoginRequest: Codable {
    let phone: String
    let firebase_token: String?
    let latitude: Double?
    let longitude: Double?
    let address: String?
}

struct CustomerResponse: Codable {
    let customer: Customer
}

struct CustomerProfileResponse: Codable {
    let success: Bool?
    let customer: Customer
    let message: String?
}

struct UpdateCustomerProfileRequest: Codable {
    let name: String?
    let email: String?
    let phone: String?
    let address: String?
    let profile_picture_url: String?
}

