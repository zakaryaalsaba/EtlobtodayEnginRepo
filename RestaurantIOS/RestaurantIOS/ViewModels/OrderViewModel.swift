//
//  OrderViewModel.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

@MainActor
class OrderViewModel: ObservableObject {
    @Published var orders: [Order] = []
    @Published var currentOrder: Order?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let apiService = APIService.shared
    private let sessionManager = SessionManager.shared
    
    func placeOrder(
        websiteId: Int,
        customerId: Int?,
        customerName: String,
        customerEmail: String?,
        customerPhone: String,
        customerAddress: String?,
        orderType: String,
        paymentMethod: String,
        deliveryLatitude: Double?,
        deliveryLongitude: Double?,
        deliveryInstructions: String?,
        tip: Double? = nil,
        items: [OrderItemRequest],
        totalAmount: Double,
        notes: String? = nil
    ) async -> Bool {
        isLoading = true
        errorMessage = nil
        
        do {
            let token = sessionManager.getAuthToken()
            
            let request = CreateOrderRequest(
                website_id: websiteId,
                customer_id: customerId,
                customer_name: customerName,
                customer_email: customerEmail,
                customer_phone: customerPhone,
                customer_address: customerAddress,
                order_type: orderType,
                payment_method: paymentMethod,
                delivery_latitude: deliveryLatitude,
                delivery_longitude: deliveryLongitude,
                delivery_instructions: deliveryInstructions,
                tip: tip,
                items: items,
                notes: notes,
                total_amount: totalAmount
            )
            
            let response = try await apiService.createOrder(request, token: token)
            currentOrder = response.order
            isLoading = false
            return true
        } catch {
            errorMessage = handleError(error)
            isLoading = false
            return false
        }
    }
    
    func fetchOrder(orderNumber: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let token = sessionManager.getAuthToken()
            let response = try await apiService.getOrderByNumber(orderNumber, token: token)
            currentOrder = response.order
        } catch {
            errorMessage = handleError(error)
        }
        
        isLoading = false
    }
    
    func fetchCustomerOrders() async {
        isLoading = true
        errorMessage = nil
        
        do {
            guard let token = sessionManager.getAuthToken() else {
                errorMessage = "Not authenticated"
                isLoading = false
                return
            }
            
            let customerId = sessionManager.getCustomerId()
            guard customerId != -1 else {
                errorMessage = "Invalid customer ID"
                isLoading = false
                return
            }
            
            let response = try await apiService.getCustomerOrders(customerId: customerId, token: token)
            orders = response.orders
        } catch {
            errorMessage = handleError(error)
        }
        
        isLoading = false
    }
    
    var activeOrders: [Order] {
        orders.filter { ["pending", "confirmed", "preparing", "ready", "on_the_way"].contains($0.status.lowercased()) }
    }
    
    var archivedOrders: [Order] {
        orders.filter { ["delivered", "cancelled"].contains($0.status.lowercased()) }
    }
    
    private func handleError(_ error: Error) -> String {
        if let apiError = error as? APIError {
            switch apiError {
            case .unauthorized:
                return "Not authenticated"
            case .networkError(_):
                return "Network error. Please check your connection"
            case .httpError(let code):
                return "Server error (code: \(code))"
            default:
                return "Failed to process order"
            }
        }
        return error.localizedDescription
    }
}

