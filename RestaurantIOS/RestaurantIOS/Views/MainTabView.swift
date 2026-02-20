//
//  MainTabView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct MainTabView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @StateObject private var languageManager = LanguageManager.shared
    
    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label(LocalizedStrings.home, systemImage: "house.fill")
                }
            
            OrderHistoryView()
                .tabItem {
                    Label(LocalizedStrings.orders, systemImage: "tray")
                }
            
            ProfileView()
                .tabItem {
                    Label(LocalizedStrings.profile, systemImage: "person")
                }
        }
    }
}

struct ProfileView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @StateObject private var languageManager = LanguageManager.shared
    @StateObject private var viewModel = ProfileViewModel()
    @State private var showLogin = false
    @State private var showSettings = false
    @State private var isEditing = false
    @State private var editedName = ""
    @State private var editedEmail = ""
    @State private var editedPhone = ""
    @State private var editedAddress = ""
    @State private var selectedImage: UIImage?
    @State private var showImagePicker = false
    @State private var profileImageURL: String?
    
    var body: some View {
        NavigationStack {
            Group {
                if sessionManager.isLoggedIn {
                    loggedInContent
                } else {
                    notLoggedInContent
                }
            }
            .navigationTitle(LocalizedStrings.profile)
            .toolbar {
                if sessionManager.isLoggedIn && !isEditing {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(action: {
                            Task {
                                await viewModel.loadProfile()
                            }
                        }) {
                            Image(systemName: "arrow.clockwise")
                        }
                        .disabled(viewModel.isLoading)
                    }
                }
            }
            .sheet(isPresented: $showLogin) {
                LoginView()
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
            .sheet(isPresented: $showImagePicker) {
                ImagePicker(selectedImage: $selectedImage)
            }
            .task {
                if sessionManager.isLoggedIn {
                    await viewModel.loadProfile()
                    // Load profile picture URL from customer data
                    if let customer = sessionManager.currentCustomer {
                        profileImageURL = customer.profile_picture_url
                    }
                }
            }
            .onChange(of: sessionManager.currentCustomer) { newValue in
                profileImageURL = newValue?.profile_picture_url
            }
        }
        .environment(\.layoutDirection, languageManager.isRTL ? .rightToLeft : .leftToRight)
    }
    
    private func startEditing() {
        editedName = sessionManager.getCustomerName() ?? ""
        editedEmail = sessionManager.getCustomerEmail() ?? ""
        editedPhone = sessionManager.getCustomerPhone() ?? ""
        editedAddress = sessionManager.getCustomerAddress() ?? ""
        isEditing = true
    }
    
    private func cancelEditing() {
        isEditing = false
        viewModel.errorMessage = nil
        viewModel.successMessage = nil
    }
    
    private func saveProfile() {
        Task {
            // Upload profile picture if selected
            var pictureURL = profileImageURL
            if let selectedImage = selectedImage {
                if let uploadedURL = await viewModel.uploadProfilePicture(selectedImage) {
                    pictureURL = uploadedURL
                }
            }
            
            let success = await viewModel.updateProfile(
                name: editedName.isEmpty ? nil : editedName,
                email: editedEmail.isEmpty ? nil : editedEmail,
                phone: editedPhone.isEmpty ? nil : editedPhone,
                address: editedAddress.isEmpty ? nil : editedAddress,
                profilePictureURL: pictureURL
            )
            if success {
                isEditing = false
                selectedImage = nil
                await viewModel.loadProfile()
            }
        }
    }
    
    // MARK: - Content Views
    private var loggedInContent: some View {
        ScrollView {
            VStack(spacing: 24) {
                profileHeaderSection
                profileInformationCard
                settingsButton
            }
        }
        .refreshable {
            await viewModel.loadProfile()
        }
        .alert("Error", isPresented: .constant(viewModel.errorMessage != nil)) {
            Button("OK") {
                viewModel.errorMessage = nil
            }
        } message: {
            if let error = viewModel.errorMessage {
                Text(error)
            }
        }
        .alert("Success", isPresented: .constant(viewModel.successMessage != nil)) {
            Button("OK") {
                viewModel.successMessage = nil
            }
        } message: {
            if let message = viewModel.successMessage {
                Text(message)
            }
        }
    }
    
    private var notLoggedInContent: some View {
        VStack(spacing: 24) {
            Spacer()
            
            Image(systemName: "person.circle")
                .font(.system(size: 80))
                .foregroundColor(.gray.opacity(0.5))
            
            Text(LocalizedStrings.pleaseLoginToViewProfile)
                .font(.headline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button(LocalizedStrings.login) {
                showLogin = true
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            
            Spacer()
        }
    }
    
    private var profileHeaderSection: some View {
        VStack(spacing: 16) {
            profilePictureView
            
            if !isEditing {
                profileNameSection
            }
        }
        .padding(.top, 20)
    }
    
    private var profileNameSection: some View {
        VStack(spacing: 4) {
            Text(sessionManager.getCustomerName() ?? LocalizedStrings.user)
                .font(.title2)
                .fontWeight(.bold)
            
            if let email = sessionManager.getCustomerEmail() {
                Text(email)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            if let phone = sessionManager.getCustomerPhone() {
                Text(phone)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
    }
    
    private var profileInformationCard: some View {
        VStack(alignment: .leading, spacing: 20) {
            if isEditing {
                editingForm
            } else {
                displayForm
            }
        }
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, x: 0, y: 2)
        .padding(.horizontal)
    }
    
    private var editingForm: some View {
        VStack(spacing: 16) {
            editableFields
            saveCancelButtons
        }
    }
    
    private var editableFields: some View {
        VStack(spacing: 16) {
            profileTextField(title: "Name", text: $editedName, keyboardType: .default)
            profileTextField(title: "Email", text: $editedEmail, keyboardType: .emailAddress)
            profileTextField(title: "Phone", text: $editedPhone, keyboardType: .phonePad)
            profileTextField(title: "Address", text: $editedAddress, keyboardType: .default, isMultiline: true)
        }
        .padding()
    }
    
    private func profileTextField(title: String, text: Binding<String>, keyboardType: UIKeyboardType = .default, isMultiline: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.secondary)
            
            if isMultiline {
                TextField("Enter your \(title.lowercased())", text: text, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(3...6)
            } else {
                TextField("Enter your \(title.lowercased())", text: text)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(keyboardType)
                    .autocapitalization(keyboardType == .emailAddress ? .none : .words)
                    .autocorrectionDisabled(keyboardType == .emailAddress)
            }
        }
    }
    
    private var saveCancelButtons: some View {
        HStack(spacing: 12) {
            Button(action: cancelEditing) {
                Text("Cancel")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(.systemGray5))
                    .foregroundColor(.primary)
                    .cornerRadius(12)
            }
            
            Button(action: saveProfile) {
                if viewModel.isUpdating {
                    ProgressView()
                        .tint(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                } else {
                    Text("Save")
                        .frame(maxWidth: .infinity)
                        .padding()
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(.blue)
            .disabled(viewModel.isUpdating)
        }
        .padding(.horizontal)
        .padding(.bottom)
    }
    
    private var displayForm: some View {
        VStack(spacing: 0) {
            VStack(spacing: 16) {
                ProfileInfoRow(
                    icon: "person.fill",
                    title: "Name",
                    value: sessionManager.getCustomerName() ?? "Not set"
                )
                
                Divider()
                
                ProfileInfoRow(
                    icon: "envelope.fill",
                    title: "Email",
                    value: sessionManager.getCustomerEmail() ?? "Not set"
                )
                
                if let phone = sessionManager.getCustomerPhone(), !phone.isEmpty {
                    Divider()
                    ProfileInfoRow(
                        icon: "phone.fill",
                        title: "Phone",
                        value: phone
                    )
                }
                
                if let address = sessionManager.getCustomerAddress(), !address.isEmpty {
                    Divider()
                    ProfileInfoRow(
                        icon: "mappin.circle.fill",
                        title: "Address",
                        value: address
                    )
                }
            }
            .padding()
            
            Button(action: startEditing) {
                HStack {
                    Image(systemName: "pencil")
                    Text("Edit Profile")
                }
                .frame(maxWidth: .infinity)
                .padding()
            }
            .buttonStyle(.borderedProminent)
            .tint(.blue)
            .padding(.horizontal)
            .padding(.bottom)
        }
    }
    
    private var settingsButton: some View {
        Button(action: {
            showSettings = true
        }) {
            HStack {
                Image(systemName: "gearshape.fill")
                Text("Settings")
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
        }
        .padding(.horizontal)
        .padding(.bottom, 20)
    }
    
    // MARK: - Profile Picture View
    private var profilePictureView: some View {
        ZStack(alignment: .bottomTrailing) {
            profileImageContent
            editPictureButton
        }
        .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
    }
    
    private var profileImageContent: some View {
        Group {
            if let profileImageURL = profileImageURL, let url = URL(string: profileImageURL) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    defaultProfileCircle
                }
                .frame(width: 120, height: 120)
                .clipShape(Circle())
            } else if let selectedImage = selectedImage {
                Image(uiImage: selectedImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 120, height: 120)
                    .clipShape(Circle())
            } else {
                defaultProfileCircle
            }
        }
    }
    
    private var defaultProfileCircle: some View {
        Circle()
            .fill(
                LinearGradient(
                    colors: [.blue, .purple],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(width: 120, height: 120)
            .overlay(
                Image(systemName: "person.fill")
                    .font(.system(size: 50))
                    .foregroundColor(.white)
            )
    }
    
    private var editPictureButton: some View {
        Button(action: {
            showImagePicker = true
        }) {
            Image(systemName: "camera.fill")
                .font(.system(size: 16))
                .foregroundColor(.white)
                .padding(10)
                .background(Color.blue)
                .clipShape(Circle())
        }
        .offset(x: 5, y: 5)
    }
}

// Profile Info Row Component
struct ProfileInfoRow: View {
    let icon: String
    let title: String
    let value: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.blue)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(value)
                    .font(.body)
                    .foregroundColor(.primary)
            }
            
            Spacer()
        }
    }
}

#Preview {
    MainTabView()
}

