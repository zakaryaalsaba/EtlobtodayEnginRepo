//
//  AuthViewModel.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

@MainActor
class AuthViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isAuthenticated = false
    
    private let apiService = APIService.shared
    private let sessionManager = SessionManager.shared
    
    init() {
        isAuthenticated = sessionManager.isLoggedIn
    }
    
    func login(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let request = LoginRequest(email: email, password: password)
            let response = try await apiService.login(request)
            
            sessionManager.saveAuthToken(response.token)
            sessionManager.saveCustomerInfo(
                id: response.customer.id,
                name: response.customer.name,
                email: response.customer.email,
                phone: response.customer.phone,
                address: response.customer.address,
                profilePictureURL: response.customer.profile_picture_url
            )
            
            isAuthenticated = true
        } catch {
            errorMessage = handleError(error)
        }
        
        isLoading = false
    }
    
    func register(name: String, email: String, password: String, phone: String? = nil) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let request = RegisterRequest(name: name, email: email, password: password, phone: phone, address: nil)
            let response = try await apiService.register(request)
            
            sessionManager.saveAuthToken(response.token)
            sessionManager.saveCustomerInfo(
                id: response.customer.id,
                name: response.customer.name,
                email: response.customer.email,
                phone: response.customer.phone,
                address: response.customer.address,
                profilePictureURL: response.customer.profile_picture_url
            )
            
            isAuthenticated = true
        } catch {
            errorMessage = handleError(error)
        }
        
        isLoading = false
    }
    
    func loginWithPhone(
        phone: String,
        firebaseToken: String?,
        latitude: Double?,
        longitude: Double?,
        address: String?
    ) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let request = PhoneLoginRequest(
                phone: phone,
                firebase_token: firebaseToken,
                latitude: latitude,
                longitude: longitude,
                address: address
            )
            let response = try await apiService.loginWithPhone(request)
            
            sessionManager.saveAuthToken(response.token)
            sessionManager.saveCustomerInfo(
                id: response.customer.id,
                name: response.customer.name,
                email: response.customer.email,
                phone: response.customer.phone,
                address: response.customer.address,
                profilePictureURL: response.customer.profile_picture_url
            )
            
            isAuthenticated = true
        } catch {
            errorMessage = handleError(error)
        }
        
        isLoading = false
    }
    
    func logout() {
        sessionManager.logout()
        isAuthenticated = false
    }
    
    private func handleError(_ error: Error) -> String {
        if let apiError = error as? APIError {
            switch apiError {
            case .unauthorized:
                return "Invalid email or password"
            case .httpError(let code):
                return "Server error (code: \(code))"
            case .decodingError:
                return "Failed to parse server response"
            case .networkError:
                return "Network error. Please check your connection"
            default:
                return "An error occurred"
            }
        }
        return error.localizedDescription
    }
}

