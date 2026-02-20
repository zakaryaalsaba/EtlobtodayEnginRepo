//
//  SettingsView.swift
//  RestaurantIOS
//
//  Created on 1/25/26.
//

import SwiftUI

struct SettingsView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @StateObject private var languageManager = LanguageManager.shared
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Language Selection Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text(LocalizedStrings.language)
                            .font(.headline)
                            .padding(.horizontal)
                        
                        VStack(spacing: 0) {
                            ForEach(AppLanguage.allCases, id: \.self) { language in
                                Button(action: {
                                    languageManager.setLanguage(language)
                                }) {
                                    HStack {
                                        Text(language.displayName)
                                            .foregroundColor(.primary)
                                        Spacer()
                                        if languageManager.currentLanguage == language {
                                            Image(systemName: "checkmark")
                                                .foregroundColor(.blue)
                                                .fontWeight(.semibold)
                                        }
                                    }
                                    .padding()
                                    .background(languageManager.currentLanguage == language ? Color.blue.opacity(0.1) : Color.clear)
                                }
                                .buttonStyle(.plain)
                                
                                if language != AppLanguage.allCases.last {
                                    Divider()
                                        .padding(.leading)
                                }
                            }
                        }
                        .background(Color(.systemGray6))
                        .cornerRadius(12)
                        .padding(.horizontal)
                    }
                    .padding(.vertical, 8)
                    
                    // Logout Button
                    Button(action: {
                        sessionManager.logout()
                    }) {
                        HStack {
                            Image(systemName: "arrow.right.square")
                            Text(LocalizedStrings.logout)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(.white)
                        .background(
                            LinearGradient(
                                colors: [.red, .red.opacity(0.8)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(12)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 20)
                }
                .padding(.top)
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(LocalizedStrings.close) {
                        dismiss()
                    }
                }
            }
        }
        .environment(\.layoutDirection, languageManager.isRTL ? .rightToLeft : .leftToRight)
    }
}

#Preview {
    SettingsView()
}

