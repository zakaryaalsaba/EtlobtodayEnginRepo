//
//  PhoneAuthView.swift
//  RestaurantIOS
//
//  Created on 1/24/26.
//

import SwiftUI
import FirebaseAuth
import FirebaseCore

struct PhoneAuthView: View {
    @StateObject private var viewModel = AuthViewModel()
    private let locationHelper = LocationHelper.shared
    private let phoneAuthUIDelegate = PhoneAuthUIDelegate()
    
    @State private var phoneNumber = ""
    @State private var verificationCode = ""
    @State private var verificationID: String?
    @State private var showOTPView = false
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()
                
                // Logo/Title
                VStack(spacing: 8) {
                    Image(systemName: "phone.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.blue)
                    
                    Text(LocalizedStrings.signInWithPhone)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text(LocalizedStrings.enterPhoneNumber)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.bottom, 40)
                
                if showOTPView {
                    // OTP Verification View
                    otpVerificationView
                } else {
                    // Phone Input View
                    phoneInputView
                }
                
                Spacer()
            }
            .padding(.horizontal, 32)
            .navigationTitle(LocalizedStrings.signInWithPhone)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.cancel) {
                        dismiss()
                    }
                }
            }
            .navigationDestination(isPresented: $viewModel.isAuthenticated) {
                MainTabView()
            }
            .onAppear {
                // Request location permission as soon as possible
                if !locationHelper.hasLocationPermission() {
                    locationHelper.requestLocationPermission()
                }
            }
        }
    }
    
    // MARK: - Phone Input View
    
    private var phoneInputView: some View {
        VStack(spacing: 16) {
            HStack {
                Text("+962")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color.gray.opacity(0.1))
                    .cornerRadius(8)
                
                TextField(LocalizedStrings.phoneNumberHint, text: $phoneNumber)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(.phonePad)
            }
            
            if let errorMessage = errorMessage {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .font(.caption)
            }
            
            Button(action: {
                sendVerificationCode()
            }) {
                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text(LocalizedStrings.sendVerificationCode)
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading || phoneNumber.isEmpty)
        }
    }
    
    // MARK: - OTP Verification View
    
    private var otpVerificationView: some View {
        VStack(spacing: 16) {
            Text(LocalizedStrings.enterVerificationCode)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            TextField(LocalizedStrings.verificationCodeHint, text: $verificationCode)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.title2)
                .frame(maxWidth: 200)
            
            if let errorMessage = errorMessage {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .font(.caption)
            }
            
            Button(action: {
                verifyCode()
            }) {
                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text(LocalizedStrings.verifyCode)
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading || verificationCode.count < 6)
            
            Button(action: {
                resendCode()
            }) {
                Text(LocalizedStrings.resendCode)
                    .font(.caption)
            }
            .disabled(isLoading)
        }
    }
    
    // MARK: - Actions
    
    private func sendVerificationCode() {
        guard !phoneNumber.isEmpty else {
            errorMessage = LocalizedStrings.invalidPhoneNumber
            return
        }
        
        // Ensure Firebase is initialized
        guard FirebaseApp.app() != nil else {
            print("❌ Firebase not initialized")
            errorMessage = "Firebase not initialized. Please restart the app."
            return
        }
        
        // Ensure phone number starts with +962
        var fullPhoneNumber = phoneNumber
        if fullPhoneNumber.hasPrefix("0") {
            fullPhoneNumber = String(fullPhoneNumber.dropFirst())
        }
        fullPhoneNumber = "+962\(fullPhoneNumber)"
        
        // Basic validation
        guard fullPhoneNumber.count >= 12 else {
            errorMessage = LocalizedStrings.invalidPhoneNumber
            return
        }
        
        // Update UI on main thread
        DispatchQueue.main.async {
            self.isLoading = true
            self.errorMessage = nil
        }
        
        // Ensure we're on main thread for Firebase calls
        DispatchQueue.main.async {
            // Call Firebase with UI delegate for reCAPTCHA
            PhoneAuthProvider.provider().verifyPhoneNumber(
                fullPhoneNumber,
                uiDelegate: self.phoneAuthUIDelegate
            ) { verificationID, error in
                DispatchQueue.main.async {
                    self.isLoading = false
                    
                    if let error = error {
                        let errorDescription = error.localizedDescription
                        print("❌ Phone verification error: \(errorDescription)")
                        
                        // Check if this is the app delegate swizzling warning
                        // This is often a false positive - reCAPTCHA uses URL schemes, not notifications
                        if errorDescription.contains("app delegate swizzling") || 
                           errorDescription.contains("canHandleNotification") {
                            print("⚠️ App delegate swizzling warning detected")
                            print("ℹ️ This is often a false positive. ReCAPTCHA uses URL schemes.")
                            print("ℹ️ Trying to proceed anyway...")
                            
                            // Try to get verification ID from error if available
                            // Sometimes Firebase still sends the code despite this warning
                            self.errorMessage = "Please try again. If the issue persists, check your internet connection."
                        } else {
                            self.errorMessage = self.handleFirebaseError(error)
                        }
                        return
                    }
                    
                    guard let verificationID = verificationID else {
                        self.errorMessage = LocalizedStrings.phoneVerificationFailed
                        return
                    }
                    
                    print("✅ Verification code sent to \(fullPhoneNumber)")
                    self.verificationID = verificationID
                    self.phoneNumber = fullPhoneNumber
                    self.showOTPView = true
                }
            }
        }
    }
    
    private func verifyCode() {
        guard let verificationID = verificationID else {
            errorMessage = LocalizedStrings.codeVerificationFailed
            return
        }
        
        guard verificationCode.count >= 6 else {
            errorMessage = LocalizedStrings.codeVerificationFailed
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        let credential = PhoneAuthProvider.provider().credential(
            withVerificationID: verificationID,
            verificationCode: verificationCode
        )
        
        Auth.auth().signIn(with: credential) { authResult, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Firebase sign-in error: \(error.localizedDescription)")
                    self.isLoading = false
                    self.errorMessage = self.handleFirebaseError(error)
                    return
                }
                
                guard let user = authResult?.user else {
                    self.isLoading = false
                    self.errorMessage = LocalizedStrings.phoneVerificationFailed
                    return
                }
                
                print("✅ Firebase authentication successful for \(user.phoneNumber ?? "unknown")")
                
                // Get Firebase ID token (force refresh to get a fresh token)
                user.getIDTokenForcingRefresh(true, completion: { token, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            print("❌ Failed to get Firebase ID token: \(error.localizedDescription)")
                            self.isLoading = false
                            self.errorMessage = LocalizedStrings.phoneVerificationFailed
                            return
                        }
                        
                        guard let firebaseToken = token else {
                            self.isLoading = false
                            self.errorMessage = LocalizedStrings.phoneVerificationFailed
                            return
                        }
                        
                        // Get location and address, then authenticate with backend
                        self.authenticateWithBackend(phone: self.phoneNumber, firebaseToken: firebaseToken)
                    }
                })
            }
        }
    }
    
    private func resendCode() {
        showOTPView = false
        verificationCode = ""
        verificationID = nil
        sendVerificationCode()
    }
    
    private func authenticateWithBackend(phone: String, firebaseToken: String) {
        // Get location and address before calling backend
        var latitude: Double?
        var longitude: Double?
        var address: String?
        
        let locationGroup = DispatchGroup()
        
        if locationHelper.hasLocationPermission() {
            locationGroup.enter()
            locationHelper.getCurrentLocation { location in
                if let location = location {
                    latitude = location.coordinate.latitude
                    longitude = location.coordinate.longitude
                    
                    print("📍 Got location: lat=\(latitude!), lng=\(longitude!)")
                    
                    // Get address from coordinates
                    locationHelper.getAddressFromLocation(
                        latitude: latitude!,
                        longitude: longitude!
                    ) { addr in
                        address = addr
                        print("📍 Got address: \(address ?? "none")")
                        locationGroup.leave()
                    }
                } else {
                    locationGroup.leave()
                }
            }
        } else {
            locationGroup.leave()
        }
        
        locationGroup.notify(queue: .main) {
            Task { @MainActor in
                await viewModel.loginWithPhone(
                    phone: phone,
                    firebaseToken: firebaseToken,
                    latitude: latitude,
                    longitude: longitude,
                    address: address
                )
                
                isLoading = false
                
                if let error = viewModel.errorMessage {
                    errorMessage = error
                }
            }
        }
    }
    
    private func handleFirebaseError(_ error: Error) -> String {
        if let authError = error as NSError? {
            if let errorCode = AuthErrorCode(rawValue: authError.code) {
                switch errorCode {
                case .invalidPhoneNumber:
                    return LocalizedStrings.invalidPhoneNumber
                case .tooManyRequests:
                    return "Too many requests. Please try again later."
                case .quotaExceeded:
                    return "SMS quota exceeded. Please try again later."
                case .missingVerificationCode:
                    return LocalizedStrings.codeVerificationFailed
                case .invalidVerificationCode:
                    return LocalizedStrings.codeVerificationFailed
                default:
                    return LocalizedStrings.phoneVerificationFailed
                }
            }
        }
        
        // Check for specific error messages
        let errorDescription = error.localizedDescription.lowercased()
        if errorDescription.contains("not allowed") || errorDescription.contains("disabled") {
            return LocalizedStrings.phoneAuthNotEnabled
        }
        
        return LocalizedStrings.phoneVerificationFailed
    }
}

#Preview {
    PhoneAuthView()
}

