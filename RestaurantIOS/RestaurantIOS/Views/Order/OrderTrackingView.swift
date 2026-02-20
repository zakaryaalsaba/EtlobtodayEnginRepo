//
//  OrderTrackingView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct OrderTrackingView: View {
    let orderNumber: String
    @StateObject private var viewModel = OrderViewModel()
    @State private var timer: Timer?
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if let order = viewModel.currentOrder {
                    // Order Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Order #\(order.order_number)")
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        Text("\(LocalizedStrings.status): \(order.status.capitalized)")
                            .font(.headline)
                            .foregroundColor(statusColor(for: order.status))
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    // Order Items
                    if let items = order.items, !items.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(LocalizedStrings.items)
                                .font(.headline)
                                .padding(.horizontal)
                            
                            ForEach(items) { item in
                                HStack {
                                    VStack(alignment: .leading) {
                                        Text(item.product_name)
                                            .font(.subheadline)
                                            .fontWeight(.medium)
                                        Text("$\(String(format: "%.2f", item.product_price))")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                    Spacer()
                                    Text("x\(item.quantity)")
                                        .foregroundColor(.secondary)
                                    Text("$\(String(format: "%.2f", item.subtotal))")
                                        .fontWeight(.semibold)
                                }
                                .padding()
                                .background(Color(.systemGray6))
                                .cornerRadius(8)
                            }
                            .padding(.horizontal)
                        }
                    }
                    
                    // Order Summary
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Order Summary")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        VStack(spacing: 8) {
                            HStack {
                                Text(LocalizedStrings.subtotal)
                                Spacer()
                                Text("$\(String(format: "%.2f", order.total_amount))")
                            }
                            
                            Divider()
                            
                            HStack {
                                Text(LocalizedStrings.total)
                                    .fontWeight(.bold)
                                Spacer()
                                Text("$\(String(format: "%.2f", order.total_amount))")
                                    .fontWeight(.bold)
                            }
                        }
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                        .padding(.horizontal)
                    }
                    
                    // Customer Info
                    VStack(alignment: .leading, spacing: 8) {
                        Text(LocalizedStrings.customerInformation)
                            .font(.headline)
                            .padding(.horizontal)
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(LocalizedStrings.name): \(order.customer_name)")
                            Text("\(LocalizedStrings.phone): \(order.customer_phone)")
                            if let email = order.customer_email {
                                Text("\(LocalizedStrings.email): \(email)")
                            }
                            if let address = order.customer_address {
                                Text("\(LocalizedStrings.address): \(address)")
                            }
                        }
                        .font(.subheadline)
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                        .padding(.horizontal)
                    }
                } else if viewModel.isLoading {
                    ProgressView(LocalizedStrings.loading)
                        .frame(maxWidth: .infinity)
                        .padding()
                } else if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .padding()
                }
            }
            .padding(.vertical)
        }
        .navigationTitle(LocalizedStrings.trackOrder)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            Task {
                await viewModel.fetchOrder(orderNumber: orderNumber)
            }
            // Poll for updates every 5 seconds
            timer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { _ in
                Task {
                    await viewModel.fetchOrder(orderNumber: orderNumber)
                }
            }
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func statusColor(for status: String) -> Color {
        switch status.lowercased() {
        case "pending", "confirmed":
            return .orange
        case "preparing", "ready":
            return .blue
        case "on_the_way":
            return .purple
        case "delivered":
            return .green
        case "cancelled":
            return .red
        default:
            return .gray
        }
    }
}

#Preview {
    NavigationStack {
        OrderTrackingView(orderNumber: "ORD-123")
    }
}

