//
//  LanguageManager.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation
import SwiftUI

enum AppLanguage: String, CaseIterable {
    case english = "en"
    case arabic = "ar"
    
    var displayName: String {
        switch self {
        case .english:
            return "English"
        case .arabic:
            return "العربية"
        }
    }
    
    var locale: Locale {
        switch self {
        case .english:
            return Locale(identifier: "en")
        case .arabic:
            return Locale(identifier: "ar")
        }
    }
}

class LanguageManager: ObservableObject {
    static let shared = LanguageManager()
    
    private let userDefaults = UserDefaults.standard
    private let languageKey = "app_language"
    
    @Published var currentLanguage: AppLanguage = .english
    
    private init() {
        loadLanguage()
    }
    
    private func loadLanguage() {
        if let savedLanguage = userDefaults.string(forKey: languageKey),
           let language = AppLanguage(rawValue: savedLanguage) {
            currentLanguage = language
        } else {
            // Default to system language or English
            let systemLanguage = Locale.current.languageCode ?? "en"
            currentLanguage = AppLanguage(rawValue: systemLanguage) ?? .english
        }
        applyLanguage()
    }
    
    func setLanguage(_ language: AppLanguage) {
        currentLanguage = language
        userDefaults.set(language.rawValue, forKey: languageKey)
        applyLanguage()
    }
    
    private func applyLanguage() {
        // Set app language
        UserDefaults.standard.set([currentLanguage.rawValue], forKey: "AppleLanguages")
        UserDefaults.standard.synchronize()
    }
    
    var isRTL: Bool {
        currentLanguage == .arabic
    }
}

