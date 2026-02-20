//
//  LoginView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel = AuthViewModel()
    @State private var email = ""
    @State private var password = ""
    @State private var showRegister = false
    @State private var showPhoneAuth = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()
                
                // Logo/Title
                VStack(spacing: 8) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 60))
                        .foregroundColor(.blue)
                    
                    Text("Restaurant App")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                }
                .padding(.bottom, 40)
                
                // Form
                VStack(spacing: 16) {
                    TextField(LocalizedStrings.email, text: $email)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .autocorrectionDisabled()
                    
                    SecureField(LocalizedStrings.password, text: $password)
                        .textFieldStyle(.roundedBorder)
                    
                    if let errorMessage = viewModel.errorMessage {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                    
                    Button(action: {
                        Task {
                            await viewModel.login(email: email, password: password)
                        }
                    }) {
                        if viewModel.isLoading {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        } else {
                            Text(LocalizedStrings.login)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(viewModel.isLoading || email.isEmpty || password.isEmpty)
                    
                    // Divider
                    HStack {
                        Rectangle()
                            .frame(height: 1)
                            .foregroundColor(.gray.opacity(0.3))
                        Text(LocalizedStrings.or)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 8)
                        Rectangle()
                            .frame(height: 1)
                            .foregroundColor(.gray.opacity(0.3))
                    }
                    .padding(.vertical, 8)
                    
                    // Phone Auth Button
                    Button(action: {
                        showPhoneAuth = true
                    }) {
                        Text(LocalizedStrings.signInWithPhone)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .disabled(viewModel.isLoading)
                }
                .padding(.horizontal, 32)
                
                // Register Link
                HStack {
                    Text("Don't have an account?")
                    Button(LocalizedStrings.register) {
                        showRegister = true
                    }
                }
                .font(.caption)
                
                Spacer()
            }
            .navigationDestination(isPresented: $showRegister) {
                RegisterView()
            }
            .navigationDestination(isPresented: $showPhoneAuth) {
                PhoneAuthView()
            }
            .navigationDestination(isPresented: $viewModel.isAuthenticated) {
                MainTabView()
            }
        }
    }
}

#Preview {
    LoginView()
}

