//
//  APIConfig.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct APIConfig {
    // Base URL configuration
    // For iOS Simulator: use "http://localhost:3000/api"
    // For Physical Device: use "http://YOUR_IP_ADDRESS:3000/api" (e.g., "http://192.168.1.100:3000/api")
    // For Production: use your production URL
    
    static var baseURL: String {
        #if DEBUG
        // Change this to your local IP address when testing on a physical device
        // You can find your IP by running: ifconfig | grep "inet " | grep -v 127.0.0.1
        return "http://localhost:3000/api"
        #else
        return "https://your-production-url.com/api"
        #endif
    }
}

