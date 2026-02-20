//
//  RestaurantViewModel.swift
//  RestaurantViewModel.swift
//
//  Created on 1/21/26.
//

import Foundation

@MainActor
class RestaurantViewModel: ObservableObject {
    @Published var restaurants: [Restaurant] = []
    @Published var offers: [Offer] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var searchQuery: String = ""
    @Published private(set) var orderType: String = "delivery"
    
    private let apiService = APIService.shared
    private let sessionManager = SessionManager.shared
    
    init() {
        orderType = sessionManager.getOrderType()
    }
    
    /// Restaurants filtered by current order type and search (name, description, address).
    var filteredRestaurants: [Restaurant] {
        var list = restaurants
        if orderType == "delivery" {
            list = list.filter { $0.order_type_delivery_enabled ?? true }
        } else {
            list = list.filter { $0.order_type_pickup_enabled ?? true }
        }
        let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if q.isEmpty { return list }
        return list.filter { r in
            (r.restaurant_name.lowercased().contains(q)) ||
            (r.description?.lowercased().contains(q) ?? false) ||
            (r.address?.lowercased().contains(q) ?? false)
        }
    }
    
    /// Restaurants with free delivery (delivery_fee == 0).
    var freeDeliveryRestaurants: [Restaurant] {
        if orderType == "delivery" {
            return restaurants.filter { ($0.delivery_fee ?? 0) == 0 && ($0.order_type_delivery_enabled ?? true) }
        }
        return restaurants.filter { ($0.delivery_fee ?? 0) == 0 }
    }
    
    /// Restaurants filtered by order type only (no search). Used for Popular section.
    private var orderTypeFilteredRestaurants: [Restaurant] {
        if orderType == "delivery" {
            return restaurants.filter { $0.order_type_delivery_enabled ?? true }
        }
        return restaurants.filter { $0.order_type_pickup_enabled ?? true }
    }
    
    /// First 6 restaurants (for Popular section).
    var popularRestaurants: [Restaurant] {
        Array(orderTypeFilteredRestaurants.prefix(6))
    }
    
    func setOrderType(_ type: String) {
        orderType = type
        sessionManager.saveOrderType(type)
    }
    
    func fetchRestaurants() async {
        isLoading = true
        errorMessage = nil
        
        do {
            print("🔄 Fetching restaurants...")
            let response = try await apiService.getRestaurants()
            print("✅ Received \(response.websites.count) restaurants")
            restaurants = response.websites.filter { $0.is_published }
            print("✅ Filtered to \(restaurants.count) published restaurants")
        } catch {
            let errorMsg = handleError(error)
            print("❌ Error fetching restaurants: \(errorMsg)")
            errorMessage = errorMsg
        }
        
        isLoading = false
    }
    
    func loadOffers() async {
        do {
            let response = try await apiService.getOffersList()
            offers = response.offers
        } catch {
            // Non-blocking: home still works without offers
            offers = []
        }
    }
    
    /// Fetch a single restaurant by id (e.g. for checkout when cart has items from one store).
    func fetchRestaurant(id: Int) async {
        do {
            let response = try await apiService.getRestaurant(id: id)
            if !restaurants.contains(where: { $0.id == response.website.id }) {
                restaurants.append(response.website)
            } else if let idx = restaurants.firstIndex(where: { $0.id == response.website.id }) {
                restaurants[idx] = response.website
            }
        } catch {
            let _ = handleError(error)
        }
    }
    
    private func handleError(_ error: Error) -> String {
        if let apiError = error as? APIError {
            switch apiError {
            case .invalidURL:
                return "Invalid API URL. Please check APIConfig.swift"
            case .networkError(let underlyingError):
                let nsError = underlyingError as NSError
                if nsError.domain == NSURLErrorDomain {
                    switch nsError.code {
                    case NSURLErrorNotConnectedToInternet:
                        return "No internet connection. Please check your network."
                    case NSURLErrorCannotFindHost, NSURLErrorCannotConnectToHost:
                        return "Cannot connect to server. Is the backend running? Check APIConfig.swift for the correct URL."
                    case NSURLErrorTimedOut:
                        return "Connection timeout. Please try again."
                    default:
                        return "Network error: \(nsError.localizedDescription)"
                    }
                }
                return "Network error: \(underlyingError.localizedDescription)"
            case .httpError(let code):
                return "Server error (HTTP \(code)). Please check the backend."
            case .decodingError(let decodingError):
                // Extract detailed error information
                if let decodingErr = decodingError as? DecodingError {
                    switch decodingErr {
                    case .keyNotFound(let key, let context):
                        return "Missing field '\(key.stringValue)' at path: \(context.codingPath.map { $0.stringValue }.joined(separator: "."))"
                    case .typeMismatch(let type, let context):
                        return "Type mismatch for '\(context.codingPath.map { $0.stringValue }.joined(separator: "."))': expected \(type), got something else"
                    case .valueNotFound(let type, let context):
                        return "Value not found for '\(context.codingPath.map { $0.stringValue }.joined(separator: "."))': expected \(type)"
                    case .dataCorrupted(let context):
                        return "Data corrupted: \(context.debugDescription)"
                    @unknown default:
                        return "Decoding error: \(decodingError.localizedDescription)"
                    }
                }
                return "Failed to parse response: \(decodingError.localizedDescription)"
            case .unauthorized:
                return "Unauthorized. Please login again."
            default:
                return "Failed to load restaurants: \(apiError)"
            }
        }
        return "Error: \(error.localizedDescription)"
    }
}

