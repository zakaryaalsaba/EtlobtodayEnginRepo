//
//  SessionManager.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    private let userDefaults = UserDefaults.standard
    private let tokenKey = "auth_token"
    private let customerIdKey = "customer_id"
    private let customerNameKey = "customer_name"
    private let customerEmailKey = "customer_email"
    private let customerPhoneKey = "customer_phone"
    private let customerAddressKey = "customer_address"
    private let orderTypeKey = "order_type"
    
    @Published var isLoggedIn: Bool = false
    @Published var currentCustomer: Customer?
    
    private init() {
        checkLoginStatus()
    }
    
    func checkLoginStatus() {
        isLoggedIn = getAuthToken() != nil && getCustomerId() != -1
    }
    
    func saveAuthToken(_ token: String) {
        userDefaults.set(token, forKey: tokenKey)
        checkLoginStatus()
    }
    
    func getAuthToken() -> String? {
        return userDefaults.string(forKey: tokenKey)
    }
    
    func saveCustomerInfo(id: Int, name: String, email: String?, phone: String? = nil, address: String? = nil, profilePictureURL: String? = nil) {
        userDefaults.set(id, forKey: customerIdKey)
        userDefaults.set(name, forKey: customerNameKey)
        if let email = email {
            userDefaults.set(email, forKey: customerEmailKey)
        } else {
            userDefaults.removeObject(forKey: customerEmailKey)
        }
        if let phone = phone {
            userDefaults.set(phone, forKey: customerPhoneKey)
        }
        if let address = address {
            userDefaults.set(address, forKey: customerAddressKey)
        }
        
        currentCustomer = Customer(
            id: id,
            name: name,
            email: email,
            phone: phone,
            address: address,
            profile_picture_url: profilePictureURL,
            created_at: nil,
            updated_at: nil
        )
        
        checkLoginStatus()
    }
    
    func getCustomerId() -> Int {
        return userDefaults.integer(forKey: customerIdKey)
    }
    
    func getCustomerName() -> String? {
        return userDefaults.string(forKey: customerNameKey)
    }
    
    func getCustomerEmail() -> String? {
        return userDefaults.string(forKey: customerEmailKey)
    }
    
    func getCustomerPhone() -> String? {
        return userDefaults.string(forKey: customerPhoneKey)
    }
    
    func getCustomerAddress() -> String? {
        return userDefaults.string(forKey: customerAddressKey)
    }
    
    func saveOrderType(_ type: String) {
        userDefaults.set(type, forKey: orderTypeKey)
    }
    
    func getOrderType() -> String {
        return userDefaults.string(forKey: orderTypeKey) ?? "delivery"
    }
    
    func logout() {
        userDefaults.removeObject(forKey: tokenKey)
        userDefaults.removeObject(forKey: customerIdKey)
        userDefaults.removeObject(forKey: customerNameKey)
        userDefaults.removeObject(forKey: customerEmailKey)
        userDefaults.removeObject(forKey: customerPhoneKey)
        userDefaults.removeObject(forKey: customerAddressKey)
        userDefaults.removeObject(forKey: orderTypeKey)
        currentCustomer = nil
        isLoggedIn = false
    }
}

