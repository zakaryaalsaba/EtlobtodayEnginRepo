//
//  OrderHistoryView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct OrderHistoryView: View {
    @StateObject private var viewModel = OrderViewModel()
    @State private var selectedTab = 0
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Tab Selector
                Picker("", selection: $selectedTab) {
                    Text(LocalizedStrings.active).tag(0)
                    Text(LocalizedStrings.archive).tag(1)
                }
                .pickerStyle(.segmented)
                .padding()
                
                // Orders List
                Group {
                    if viewModel.isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if let errorMessage = viewModel.errorMessage {
                        VStack(spacing: 16) {
                            Text(errorMessage)
                                .foregroundColor(.red)
                            Button(LocalizedStrings.retry) {
                                Task {
                                    await viewModel.fetchCustomerOrders()
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        let orders = selectedTab == 0 ? viewModel.activeOrders : viewModel.archivedOrders
                        
                        if orders.isEmpty {
                            VStack(spacing: 16) {
                                Image(systemName: "tray")
                                    .font(.system(size: 50))
                                    .foregroundColor(.gray)
                                Text(selectedTab == 0 ? LocalizedStrings.noOrdersFound : LocalizedStrings.noOrdersFound)
                                    .foregroundColor(.gray)
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                        } else {
                            List(orders) { order in
                                NavigationLink(destination: OrderTrackingView(orderNumber: order.order_number)) {
                                    OrderRowView(order: order)
                                }
                            }
                            .listStyle(.plain)
                        }
                    }
                }
            }
            .navigationTitle(LocalizedStrings.orders)
            .refreshable {
                await viewModel.fetchCustomerOrders()
            }
            .task {
                await viewModel.fetchCustomerOrders()
            }
        }
    }
}

struct OrderRowView: View {
    let order: Order
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(order.order_number)
                    .font(.headline)
                Spacer()
                Text(order.status.capitalized)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(statusColor(for: order.status).opacity(0.2))
                    .foregroundColor(statusColor(for: order.status))
                    .cornerRadius(8)
            }
            
            if let items = order.items, !items.isEmpty {
                Text("\(items.count) item\(items.count > 1 ? "s" : "")")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            HStack {
                Text(LocalizedStrings.total)
                    .foregroundColor(.secondary)
                Spacer()
                Text("$\(String(format: "%.2f", order.total_amount))")
                    .fontWeight(.semibold)
            }
            
            if let createdAt = order.created_at {
                Text(formatDate(createdAt))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
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
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .medium
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
}

#Preview {
    OrderHistoryView()
}

