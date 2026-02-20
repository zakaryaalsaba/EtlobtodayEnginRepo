//
//  RegisterView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct RegisterView: View {
    @StateObject private var viewModel = AuthViewModel()
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var phone = ""
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Form
                    VStack(spacing: 16) {
                        TextField(LocalizedStrings.name, text: $name)
                            .textFieldStyle(.roundedBorder)
                        
                        TextField(LocalizedStrings.email, text: $email)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                            .autocorrectionDisabled()
                        
                        TextField("\(LocalizedStrings.phone) (\(LocalizedStrings.optional))", text: $phone)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.phonePad)
                        
                        SecureField(LocalizedStrings.password, text: $password)
                            .textFieldStyle(.roundedBorder)
                        
                        SecureField("Confirm \(LocalizedStrings.password)", text: $confirmPassword)
                            .textFieldStyle(.roundedBorder)
                        
                        if let errorMessage = viewModel.errorMessage {
                            Text(errorMessage)
                                .foregroundColor(.red)
                                .font(.caption)
                        }
                        
                        Button(action: {
                            Task {
                                await viewModel.register(
                                    name: name,
                                    email: email,
                                    password: password,
                                    phone: phone.isEmpty ? nil : phone
                                )
                            }
                        }) {
                            if viewModel.isLoading {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                            } else {
                                Text(LocalizedStrings.register)
                                    .frame(maxWidth: .infinity)
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(viewModel.isLoading || !isFormValid)
                    }
                    .padding(.horizontal, 32)
                    .padding(.top, 40)
                }
            }
            .navigationTitle(LocalizedStrings.register)
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(isPresented: $viewModel.isAuthenticated) {
                MainTabView()
            }
        }
    }
    
    private var isFormValid: Bool {
        !name.isEmpty &&
        !email.isEmpty &&
        !password.isEmpty &&
        password == confirmPassword &&
        password.count >= 6
    }
}

#Preview {
    RegisterView()
}

