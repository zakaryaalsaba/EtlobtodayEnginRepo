//
//  RestaurantIOSApp.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI
import FirebaseCore
import Foundation

@main
struct RestaurantIOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var sessionManager = SessionManager.shared
    @StateObject private var languageManager = LanguageManager.shared
    
    init() {
        // Initialize Firebase
        // Check if GoogleService-Info.plist exists
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") else {
            fatalError("GoogleService-Info.plist not found. Please add it to your Xcode project.")
        }
        
        guard let plist = NSDictionary(contentsOfFile: path) else {
            fatalError("GoogleService-Info.plist is invalid or corrupted.")
        }
        
        print("✅ GoogleService-Info.plist found at: \(path)")
        print("✅ Firebase project ID: \(plist["PROJECT_ID"] ?? "unknown")")
        
        // Configure Firebase with app delegate swizzling enabled (default)
        // Configure Firebase
        // Note: App delegate swizzling is disabled by default in SwiftUI apps
        // We handle notifications manually in AppDelegate
        FirebaseApp.configure()
        print("✅ Firebase initialized successfully")
        print("ℹ️ App delegate swizzling is disabled - using manual notification handling")
    }
    
    var body: some Scene {
        WindowGroup {
            if sessionManager.isLoggedIn {
                MainTabView()
                    .environment(\.layoutDirection, languageManager.isRTL ? .rightToLeft : .leftToRight)
            } else {
                LoginView()
                    .environment(\.layoutDirection, languageManager.isRTL ? .rightToLeft : .leftToRight)
            }
        }
    }
}
