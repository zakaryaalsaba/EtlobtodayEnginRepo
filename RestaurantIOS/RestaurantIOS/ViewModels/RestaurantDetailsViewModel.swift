//
//  RestaurantDetailsViewModel.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

@MainActor
class RestaurantDetailsViewModel: ObservableObject {
    @Published var restaurant: Restaurant?
    @Published var products: [Product] = []
    @Published var restaurantOffers: [Offer] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let apiService = APIService.shared
    
    func fetchRestaurant(id: Int) async {
        isLoading = true
        errorMessage = nil
        do {
            let response = try await apiService.getRestaurant(id: id)
            restaurant = response.website
        } catch {
            errorMessage = handleError(error)
        }
        isLoading = false
    }
    
    func fetchProducts(websiteId: Int) async {
        isLoading = true
        errorMessage = nil
        do {
            let response = try await apiService.getProducts(websiteId: websiteId)
            products = response.products.filter { $0.is_available }
        } catch {
            errorMessage = handleError(error)
        }
        isLoading = false
    }
    
    func fetchRestaurantOffers(websiteId: Int) async {
        do {
            let response = try await apiService.getOffersByWebsiteId(websiteId: websiteId)
            restaurantOffers = response.offers
        } catch {
            restaurantOffers = []
        }
    }
    
    /// Best percent-off for this product (0–100) or nil. Uses offer_scope (all_items vs selected_items).
    func getBestPercentOff(productId: Int) -> Double? {
        let percentOffers = restaurantOffers.filter { offer in
            let type = (offer.offer_type ?? "").trimmingCharacters(in: .whitespaces).lowercased()
            let val = offer.value ?? 0
            return type == "percent_off" && val > 0
        }
        let applicable = percentOffers.filter { offer in
            let scope = (offer.offer_scope ?? "").trimmingCharacters(in: .whitespaces).lowercased()
            if scope == "selected_items" {
                return offer.getSelectedProductIds().contains(productId)
            }
            return true
        }
        return applicable.compactMap(\.value).max()
    }
    
    /// (displayPrice, originalPriceForStrikethrough?). When discounted, original is non-nil.
    func getDisplayPrice(for product: Product) -> (display: Double, original: Double?) {
        guard let percent = getBestPercentOff(productId: product.id), percent > 0 else {
            return (product.price, nil)
        }
        let discounted = (product.price * (1.0 - percent / 100.0) * 100.0).rounded() / 100.0
        return (discounted, product.price)
    }
    
    var groupedProducts: [String: [Product]] {
        Dictionary(grouping: products) { $0.category?.isEmpty == false ? $0.category! : "Menu" }
    }
    
    var categories: [String] {
        Array(groupedProducts.keys).sorted()
    }
    
    /// First 4 products for Trending section (2x2 grid).
    var trendingProducts: [Product] {
        Array(products.prefix(4))
    }
    
    private func handleError(_ error: Error) -> String {
        if let apiError = error as? APIError {
            switch apiError {
            case .networkError(_):
                return "Network error. Please check your connection"
            case .httpError(let code):
                return "Server error (code: \(code))"
            default:
                return "Failed to load data"
            }
        }
        return error.localizedDescription
    }
}

