//
//  URLHelper.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

class URLHelper {
    static func fixImageURL(_ urlString: String?) -> String? {
        guard let urlString = urlString else { return nil }
        
        // If already a full URL, return as is
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return urlString
        }
        
        // Use the same base URL as API config
        let baseURL = APIConfig.baseURL.replacingOccurrences(of: "/api", with: "")
        
        // Remove leading slash if present
        let cleanPath = urlString.hasPrefix("/") ? String(urlString.dropFirst()) : urlString
        
        return "\(baseURL)/\(cleanPath)"
    }
}

