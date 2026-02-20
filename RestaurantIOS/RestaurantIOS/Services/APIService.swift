//
//  APIService.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

enum APIError: Error {
    case invalidURL
    case invalidResponse
    case httpError(Int)
    case decodingError(Error)
    case networkError(Error)
    case unauthorized
    case unknown
}

class APIService {
    static let shared = APIService()
    
    // Base URL - configured via APIConfig
    private let baseURL = APIConfig.baseURL
    
    private let session: URLSession
    
    private init() {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30
        configuration.timeoutIntervalForResource = 30
        session = URLSession(configuration: configuration)
    }
    
    // MARK: - Generic Request Method
    
    private func request<T: Decodable>(
        endpoint: String,
        method: String = "GET",
        body: Data? = nil,
        headers: [String: String]? = nil
    ) async throws -> T {
        let fullURL = "\(baseURL)/\(endpoint)"
        print("🔗 Full URL: \(fullURL)")
        guard let url = URL(string: fullURL) else {
            print("❌ Invalid URL: \(fullURL)")
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let headers = headers {
            for (key, value) in headers {
                request.setValue(value, forHTTPHeaderField: key)
            }
        }
        
        if let body = body {
            request.httpBody = body
        }
        
        do {
            print("🌐 API Request: \(method) \(baseURL)/\(endpoint)")
            let (data, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                print("❌ Invalid response type")
                throw APIError.invalidResponse
            }
            
            print("📡 Response Status: \(httpResponse.statusCode)")
            
            if httpResponse.statusCode == 401 {
                print("❌ Unauthorized")
                throw APIError.unauthorized
            }
            
            guard (200...299).contains(httpResponse.statusCode) else {
                let errorBody = String(data: data, encoding: .utf8) ?? "No error body"
                print("❌ HTTP Error \(httpResponse.statusCode): \(errorBody)")
                throw APIError.httpError(httpResponse.statusCode)
            }
            
            // Log response data for debugging
            if let responseString = String(data: data, encoding: .utf8) {
                print("✅ Response: \(responseString)")
            } else {
                print("⚠️ Response data is empty or not valid UTF-8")
            }
            
            // Check if data is empty
            guard !data.isEmpty else {
                print("❌ Response data is empty")
                throw APIError.decodingError(NSError(domain: "APIService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Response data is empty"]))
            }
            
            let decoder = JSONDecoder()
            // Don't use keyDecodingStrategy when we have custom CodingKeys
            // decoder.keyDecodingStrategy = .convertFromSnakeCase
            
            do {
                // For debugging: print the raw JSON structure
                if let jsonObject = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    print("📋 JSON Structure: \(jsonObject.keys.joined(separator: ", "))")
                    if let websites = jsonObject["websites"] as? [[String: Any]], let firstWebsite = websites.first {
                        print("📋 First restaurant keys: \(firstWebsite.keys.joined(separator: ", "))")
                    }
                }
                
                return try decoder.decode(T.self, from: data)
            } catch let decodingError as DecodingError {
                print("❌ Decoding error: \(decodingError)")
                // Print detailed decoding error
                switch decodingError {
                case .dataCorrupted(let context):
                    print("❌ Data corrupted: \(context.debugDescription)")
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("❌ Response body: \(responseString)")
                    }
                case .keyNotFound(let key, let context):
                    print("❌ Key '\(key.stringValue)' not found")
                    print("❌ Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: "."))")
                    print("❌ Debug: \(context.debugDescription)")
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("❌ Response body: \(responseString.prefix(500))")
                    }
                case .typeMismatch(let type, let context):
                    print("❌ Type mismatch - expected: \(type)")
                    print("❌ Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: "."))")
                    print("❌ Debug: \(context.debugDescription)")
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("❌ Response body: \(responseString.prefix(500))")
                    }
                case .valueNotFound(let type, let context):
                    print("❌ Value not found - expected type: \(type)")
                    print("❌ Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: "."))")
                    print("❌ Debug: \(context.debugDescription)")
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("❌ Response body: \(responseString.prefix(500))")
                    }
                @unknown default:
                    print("❌ Unknown decoding error: \(decodingError)")
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("❌ Response body: \(responseString)")
                    }
                }
                throw APIError.decodingError(decodingError)
            }
        } catch let error as DecodingError {
            throw APIError.decodingError(error)
        } catch let error as APIError {
            throw error
        } catch {
            print("❌ Network error: \(error.localizedDescription)")
            throw APIError.networkError(error)
        }
    }
    
    // MARK: - Authentication
    
    func register(_ request: RegisterRequest) async throws -> AuthResponse {
        let body = try JSONEncoder().encode(request)
        return try await self.request(
            endpoint: "auth/register",
            method: "POST",
            body: body
        )
    }
    
    func login(_ request: LoginRequest) async throws -> AuthResponse {
        let body = try JSONEncoder().encode(request)
        return try await self.request(
            endpoint: "auth/login",
            method: "POST",
            body: body
        )
    }
    
    func loginWithPhone(_ request: PhoneLoginRequest) async throws -> AuthResponse {
        let body = try JSONEncoder().encode(request)
        return try await self.request(
            endpoint: "auth/login/phone",
            method: "POST",
            body: body
        )
    }
    
    func getCurrentCustomer(token: String) async throws -> CustomerResponse {
        return try await self.request(
            endpoint: "auth/me",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    // MARK: - Restaurants
    
    func getRestaurants() async throws -> RestaurantsResponse {
        return try await self.request(endpoint: "websites")
    }
    
    func getRestaurant(id: Int) async throws -> WebsiteResponse {
        return try await self.request(endpoint: "websites/\(id)")
    }
    
    /// All active offers from all restaurants (valid today). For home "Offers" section.
    func getOffersList() async throws -> OffersListResponse {
        return try await self.request(endpoint: "websites/offers/list")
    }
    
    /// Active offers for a single restaurant.
    func getOffersByWebsiteId(websiteId: Int) async throws -> OffersResponse {
        return try await self.request(endpoint: "websites/\(websiteId)/offers")
    }
    
    // MARK: - Products
    
    func getProducts(websiteId: Int) async throws -> ProductsResponse {
        return try await self.request(endpoint: "products/website/\(websiteId)")
    }
    
    /// Get addons for a product (for meal details with customization).
    func getProductAddons(productId: Int) async throws -> ProductAddonsResponse {
        return try await self.request(endpoint: "products/\(productId)/addons")
    }
    
    // MARK: - Settings
    
    func getSettings() async throws -> SettingsResponse {
        return try await self.request(endpoint: "settings")
    }
    
    // MARK: - Regions, Areas, Zones (for delivery company address flow)
    
    func getRestaurantRegions(websiteId: Int) async throws -> RegionsResponse {
        return try await self.request(endpoint: "websites/\(websiteId)/regions")
    }
    
    func getRestaurantAreas(websiteId: Int, regionId: Int) async throws -> AreasResponse {
        return try await self.request(endpoint: "websites/\(websiteId)/areas?region_id=\(regionId)")
    }
    
    func getRestaurantZones(websiteId: Int, areaId: Int) async throws -> DeliveryZonesResponse {
        return try await self.request(endpoint: "websites/\(websiteId)/zones?area_id=\(areaId)")
    }
    
    // MARK: - Addresses
    
    func getAddresses(customerId: Int, token: String) async throws -> AddressesResponse {
        return try await self.request(
            endpoint: "customers/\(customerId)/addresses",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    func createAddress(customerId: Int, request: CreateAddressRequest, token: String) async throws -> AddressResponse {
        let body = try JSONEncoder().encode(request)
        return try await self.request(
            endpoint: "customers/\(customerId)/addresses",
            method: "POST",
            body: body,
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    // MARK: - Orders
    
    /// Create order. Pass nil token for guest checkout.
    func createOrder(_ request: CreateOrderRequest, token: String?) async throws -> OrderResponse {
        let body = try JSONEncoder().encode(request)
        var headers: [String: String] = [:]
        if let token = token, !token.isEmpty {
            headers["Authorization"] = "Bearer \(token)"
        }
        return try await self.request(
            endpoint: "orders",
            method: "POST",
            body: body,
            headers: headers.isEmpty ? nil : headers
        )
    }
    
    /// Get order by number for tracking. Token optional (guest can track by order number).
    func getOrderByNumber(_ orderNumber: String, token: String?) async throws -> OrderResponse {
        var headers: [String: String]? = nil
        if let token = token, !token.isEmpty {
            headers = ["Authorization": "Bearer \(token)"]
        }
        return try await self.request(
            endpoint: "orders/\(orderNumber)",
            headers: headers
        )
    }
    
    func getCustomerOrders(customerId: Int, token: String) async throws -> OrdersResponse {
        return try await self.request(
            endpoint: "customers/\(customerId)/orders",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    // MARK: - Notifications
    
    func getCustomerNotifications(customerId: Int, token: String) async throws -> NotificationsResponse {
        return try await self.request(
            endpoint: "customers/\(customerId)/notifications",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    func markNotificationAsRead(customerId: Int, notificationId: Int, token: String) async throws -> ApiResponse {
        return try await self.request(
            endpoint: "customers/\(customerId)/notifications/\(notificationId)/read",
            method: "PUT",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    func markAllNotificationsAsRead(customerId: Int, token: String) async throws -> ApiResponse {
        return try await self.request(
            endpoint: "customers/\(customerId)/notifications/read-all",
            method: "PUT",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    // MARK: - Device Token and Location
    
    func updateDeviceToken(customerId: Int, deviceToken: String, token: String) async throws -> ApiResponse {
        let request = DeviceTokenRequest(device_token: deviceToken, device_type: "ios")
        let body = try JSONEncoder().encode(request)
        return try await self.request(
            endpoint: "customers/\(customerId)/device-token",
            method: "PUT",
            body: body,
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    func updateLocation(customerId: Int, latitude: Double, longitude: Double, address: String?, token: String) async throws -> LocationResponse {
        let request = LocationRequest(latitude: latitude, longitude: longitude, address: address)
        let body = try JSONEncoder().encode(request)
        return try await self.request(
            endpoint: "customers/\(customerId)/location",
            method: "PUT",
            body: body,
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    func getCustomerLocation(customerId: Int, token: String) async throws -> LocationResponse {
        return try await self.request(
            endpoint: "customers/\(customerId)/location",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    // MARK: - Customer Profile
    
    func getCustomerProfile(customerId: Int, token: String) async throws -> CustomerProfileResponse {
        return try await self.request(
            endpoint: "customers/\(customerId)",
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
    
    func updateCustomerProfile(customerId: Int, request: UpdateCustomerProfileRequest, token: String) async throws -> CustomerProfileResponse {
        let body = try JSONEncoder().encode(request)
        return try await self.request(
            endpoint: "customers/\(customerId)",
            method: "PUT",
            body: body,
            headers: ["Authorization": "Bearer \(token)"]
        )
    }
}

