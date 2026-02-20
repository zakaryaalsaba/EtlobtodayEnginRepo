//
//  OrderConfirmationView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct OrderConfirmationView: View {
    let order: Order
    @Environment(\.dismiss) private var dismiss
    var onClose: (() -> Void)?
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            // Success Icon
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.green)
            
            Text(LocalizedStrings.orderConfirmed)
                .font(.title)
                .fontWeight(.bold)
            
            Text(LocalizedStrings.orderPlacedSuccessfully)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            // Order Details
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(LocalizedStrings.orderNumber)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text(order.order_number)
                        .fontWeight(.semibold)
                }
                
                HStack {
                    Text(LocalizedStrings.total)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("$\(String(format: "%.2f", order.total_amount))")
                        .fontWeight(.semibold)
                }
                
                HStack {
                    Text(LocalizedStrings.status)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text(order.status.capitalized)
                        .fontWeight(.semibold)
                        .foregroundColor(.blue)
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
            .padding(.horizontal)
            
            Spacer()
            
            // Close Button
            Button(action: {
                // Clear the cart first
                CartManager.shared.clearCart()
                
                // Call the onClose callback if provided
                onClose?()
                
                // Dismiss this view
                dismiss()
            }) {
                Text(LocalizedStrings.close)
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal)
            .padding(.bottom)
        }
        .navigationBarBackButtonHidden(true)
    }
}

#Preview {
    NavigationStack {
        OrderConfirmationView(order: Order(
            id: 1,
            website_id: 1,
            customer_id: 1,
            order_number: "ORD-123",
            customer_name: "John Doe",
            customer_email: "john@example.com",
            customer_phone: "1234567890",
            customer_address: nil,
            order_type: "pickup",
            status: "pending",
            total_amount: 25.99,
            payment_method: "cash",
            payment_status: "pending",
            notes: nil,
            created_at: nil,
            items: nil
        ), onClose: nil)
    }
}

