//
//  ProfileViewModel.swift
//  RestaurantIOS
//
//  Created on 1/24/26.
//

import Foundation
import UIKit

@MainActor
class ProfileViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isUpdating = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    
    private let apiService = APIService.shared
    private let sessionManager = SessionManager.shared
    
    func loadProfile() async {
        guard let token = sessionManager.getAuthToken() else {
            errorMessage = "Not authenticated"
            return
        }
        
        let customerId = sessionManager.getCustomerId()
        guard customerId > 0 else {
            errorMessage = "Invalid customer ID"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            let response = try await apiService.getCustomerProfile(
                customerId: customerId,
                token: token
            )
            
            // Update session manager with latest customer info
            sessionManager.saveCustomerInfo(
                id: response.customer.id,
                name: response.customer.name,
                email: response.customer.email,
                phone: response.customer.phone,
                address: response.customer.address,
                profilePictureURL: response.customer.profile_picture_url
            )
        } catch {
            errorMessage = handleError(error)
        }
        
        isLoading = false
    }
    
    func uploadProfilePicture(_ image: UIImage) async -> String? {
        guard let token = sessionManager.getAuthToken() else {
            errorMessage = "Not authenticated"
            return nil
        }
        
        let customerId = sessionManager.getCustomerId()
        guard customerId > 0 else {
            errorMessage = "Invalid customer ID"
            return nil
        }
        
        // Convert image to base64 or upload to server
        // For now, we'll need to implement image upload endpoint
        // This is a placeholder - you'll need to implement the actual upload
        guard let imageData = image.jpegData(compressionQuality: 0.8) else {
            errorMessage = "Failed to process image"
            return nil
        }
        
        // TODO: Implement actual image upload to your backend
        // This should upload to your server and return the URL
        // For now, returning nil means we'll need to add the upload endpoint
        
        return nil
    }
    
    func updateProfile(name: String?, email: String?, phone: String?, address: String?, profilePictureURL: String? = nil) async -> Bool {
        guard let token = sessionManager.getAuthToken() else {
            errorMessage = "Not authenticated"
            return false
        }
        
        let customerId = sessionManager.getCustomerId()
        guard customerId > 0 else {
            errorMessage = "Invalid customer ID"
            return false
        }
        
        isUpdating = true
        errorMessage = nil
        successMessage = nil
        
        do {
            let request = UpdateCustomerProfileRequest(
                name: name,
                email: email,
                phone: phone,
                address: address,
                profile_picture_url: profilePictureURL
            )
            
            let response = try await apiService.updateCustomerProfile(
                customerId: customerId,
                request: request,
                token: token
            )
            
            // Update session manager with updated customer info
            sessionManager.saveCustomerInfo(
                id: response.customer.id,
                name: response.customer.name,
                email: response.customer.email,
                phone: response.customer.phone,
                address: response.customer.address,
                profilePictureURL: response.customer.profile_picture_url
            )
            
            successMessage = response.message ?? "Profile updated successfully"
            return true
        } catch {
            errorMessage = handleError(error)
            return false
        }
        
        isUpdating = false
    }
    
    private func handleError(_ error: Error) -> String {
        if let apiError = error as? APIError {
            switch apiError {
            case .unauthorized:
                return "Session expired. Please login again."
            case .httpError(let code):
                return "Server error (Code: \(code))"
            case .networkError:
                return "Network error. Please check your connection."
            default:
                return "An error occurred. Please try again."
            }
        }
        return error.localizedDescription
    }
}

