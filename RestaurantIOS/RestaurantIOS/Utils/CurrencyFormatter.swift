//
//  CurrencyFormatter.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

class CurrencyFormatter {
    static func format(_ amount: Double, currencyCode: String? = nil, symbolPosition: String? = nil) -> String {
        let code = currencyCode ?? "USD"
        let position = symbolPosition ?? "before"
        
        // Default currency symbols
        let symbol: String
        switch code.uppercased() {
        case "USD":
            symbol = "$"
        case "JOD":
            symbol = "د.ا"
        default:
            symbol = code
        }
        
        let formattedAmount = String(format: "%.2f", amount)
        
        if position == "after" {
            return "\(formattedAmount) \(symbol)"
        } else {
            return "\(symbol)\(formattedAmount)"
        }
    }
}

