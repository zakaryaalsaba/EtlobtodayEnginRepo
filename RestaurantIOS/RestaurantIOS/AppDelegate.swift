//
//  AppDelegate.swift
//  RestaurantIOS
//
//  Created on 1/24/26.
//

import UIKit
import FirebaseAuth
import FirebaseCore

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Firebase is already configured in RestaurantIOSApp.init()
        
        // Register for remote notifications (required for Firebase Phone Auth)
        // Note: This may fail in simulator, but that's okay for phone auth
        application.registerForRemoteNotifications()
        
        print("✅ AppDelegate initialized and registered for remote notifications")
        return true
    }
    
    // Handle remote notifications for Firebase Phone Auth
    // This method MUST be implemented for Firebase Phone Auth to work without swizzling
    func application(_ application: UIApplication,
                     didReceiveRemoteNotification notification: [AnyHashable : Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        print("📱 Received remote notification")
        
        // CRITICAL: Check if Firebase Auth can handle this notification FIRST
        if Auth.auth().canHandleNotification(notification) {
            print("✅ Firebase Auth handled the notification")
            completionHandler(.noData)
            return
        }
        
        // Handle other notifications here if needed
        print("ℹ️ Notification not handled by Firebase Auth")
        completionHandler(.noData)
    }
    
    // Handle URL schemes for Firebase Phone Auth reCAPTCHA
    // This is CRITICAL for reCAPTCHA verification flow
    func application(_ app: UIApplication,
                     open url: URL,
                     options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        print("🔗 Received URL: \(url.absoluteString)")
        
        // CRITICAL: Check if Firebase Auth can handle this URL FIRST
        if Auth.auth().canHandle(url) {
            print("✅ Firebase Auth handled the URL")
            return true
        }
        
        // Handle other URLs here if needed
        print("ℹ️ URL not handled by Firebase Auth")
        return false
    }
    
    // Register for remote notifications - called when registration succeeds
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("✅ Successfully registered for remote notifications")
        // Firebase handles this automatically if swizzling is enabled
        // Since we're not using swizzling, we don't need to do anything here
    }
    
    // Register for remote notifications - called when registration fails
    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("⚠️ Failed to register for remote notifications: \(error.localizedDescription)")
        print("ℹ️ This is normal in simulator. Phone auth will still work via reCAPTCHA.")
    }
}

