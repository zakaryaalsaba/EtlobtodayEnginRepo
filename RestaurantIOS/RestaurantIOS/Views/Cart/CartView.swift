//
//  CartView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct CartView: View {
    @StateObject private var cartManager = CartManager.shared
    @StateObject private var restaurantViewModel = RestaurantViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showCheckout = false
    @State private var productToEdit: Product?
    var onOrderComplete: (() -> Void)?
    
    private var restaurant: Restaurant? {
        guard let id = cartManager.restaurantId else { return nil }
        return restaurantViewModel.restaurants.first { $0.id == id }
    }
    
    private var subtotal: Double { cartManager.total }
    
    private var tax: Double {
        guard let r = restaurant, r.tax_enabled == true, let rate = r.tax_rate else { return 0 }
        return subtotal * (rate / 100)
    }
    
    private var totalWithTax: Double { subtotal + tax }
    
    private func formatPrice(_ amount: Double) -> String {
        CurrencyFormatter.format(amount, currencyCode: restaurant?.currency_code, symbolPosition: restaurant?.currency_symbol_position)
    }
    
    var body: some View {
        NavigationStack {
            Group {
                if cartManager.isEmpty {
                    emptyState
                } else {
                    VStack(spacing: 0) {
                        stampBanner
                        ScrollView {
                            LazyVStack(spacing: 0) {
                                ForEach(Array(cartManager.items.enumerated()), id: \.element.id) { _, item in
                                    CartItemRowView(
                                        item: item,
                                        currencyCode: restaurant?.currency_code,
                                        symbolPosition: restaurant?.currency_symbol_position,
                                        onEdit: { productToEdit = item.product }
                                    )
                                }
                                paymentSummaryCard
                            }
                        }
                        bottomBar
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(LocalizedStrings.close) { dismiss() }
                }
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 2) {
                        Text(LocalizedStrings.cart)
                            .font(.headline)
                        if let r = restaurant, !cartManager.isEmpty {
                            Text(r.restaurant_name)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .task {
                if let id = cartManager.restaurantId {
                    await restaurantViewModel.fetchRestaurant(id: id)
                }
            }
            .fullScreenCover(item: $productToEdit) { product in
                MealDetailsView(
                    product: product,
                    restaurantId: cartManager.restaurantId ?? 0,
                    restaurantName: restaurant?.restaurant_name ?? "",
                    currencyCode: restaurant?.currency_code,
                    symbolPosition: restaurant?.currency_symbol_position,
                    displayPrice: product.price,
                    originalPrice: nil,
                    onDismiss: { productToEdit = nil }
                )
            }
            .navigationDestination(isPresented: $showCheckout) {
                CheckoutView(onOrderComplete: {
                    dismiss()
                    onOrderComplete?()
                })
            }
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "cart")
                .font(.system(size: 60))
                .foregroundColor(.gray)
            Text(LocalizedStrings.cartEmptyTitle)
                .font(.headline)
                .foregroundColor(.primary)
            Text(LocalizedStrings.cartEmptyMessage)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    @ViewBuilder
    private var stampBanner: some View {
        if !cartManager.isEmpty {
            HStack(spacing: 8) {
                Image(systemName: "star.circle.fill")
                    .foregroundColor(.orange)
                Text(LocalizedStrings.placeOrderEarnStamp)
                    .font(.subheadline)
                    .foregroundColor(.primary)
                Spacer()
                Image(systemName: "info.circle")
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color.orange.opacity(0.12))
        }
    }
    
    private var paymentSummaryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(LocalizedStrings.paymentSummary)
                .font(.headline)
            HStack {
                Text(LocalizedStrings.subtotal)
                Spacer()
                Text(formatPrice(subtotal))
            }
            .font(.subheadline)
            if let r = restaurant, r.tax_enabled == true, r.tax_rate != nil {
                HStack {
                    Text(LocalizedStrings.salesTax)
                    Spacer()
                    Text(formatPrice(tax))
                }
                .font(.subheadline)
            }
            Divider()
            HStack {
                Text(LocalizedStrings.totalAmount)
                    .fontWeight(.semibold)
                Spacer()
                Text(formatPrice(totalWithTax))
                    .fontWeight(.semibold)
            }
            Text("\(cartManager.itemCount) \(LocalizedStrings.items)")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .padding(.horizontal)
        .padding(.vertical, 12)
    }
    
    private var bottomBar: some View {
        HStack(spacing: 12) {
            Button(action: { dismiss() }) {
                Text(LocalizedStrings.addItems)
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.bordered)
            Button(action: { showCheckout = true }) {
                Text(LocalizedStrings.proceedToCheckout)
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
        .background(Color(.systemBackground))
    }
}

struct CartItemRowView: View {
    let item: CartItem
    let currencyCode: String?
    let symbolPosition: String?
    let onEdit: () -> Void
    
    @StateObject private var cartManager = CartManager.shared
    
    private var addonIds: [Int] { item.selectedAddons.map(\.id) }
    
    private func formatPrice(_ amount: Double) -> String {
        CurrencyFormatter.format(amount, currencyCode: currencyCode, symbolPosition: symbolPosition)
    }
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.product.name)
                    .font(.headline)
                if !item.selectedAddons.isEmpty {
                    Text(item.selectedAddons.map(\.name).joined(separator: ", "))
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                Button(action: onEdit) {
                    Text(LocalizedStrings.edit)
                        .font(.subheadline)
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 8)
            ZStack(alignment: .bottomTrailing) {
                AsyncImage(url: URL(string: URLHelper.fixImageURL(item.product.image_url) ?? "")) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle()
                        .fill(Color.gray.opacity(0.3))
                        .overlay { Image(systemName: "photo").foregroundColor(.gray) }
                }
                .frame(width: 100, height: 100)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                HStack(spacing: 8) {
                    Button(action: {
                        let newQty = item.quantity - 1
                        cartManager.updateQuantity(productId: item.product.id, addonIds: addonIds, quantity: newQty)
                    }) {
                        Image(systemName: "minus.circle.fill")
                            .font(.title3)
                            .foregroundColor(.blue)
                    }
                    .buttonStyle(.plain)
                    Text("\(item.quantity)")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .frame(minWidth: 24)
                    Button(action: {
                        cartManager.updateQuantity(productId: item.product.id, addonIds: addonIds, quantity: item.quantity + 1)
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                            .foregroundColor(.blue)
                    }
                    .buttonStyle(.plain)
                }
                .padding(6)
                .background(.ultraThinMaterial)
                .clipShape(Capsule())
                .padding(4)
            }
            Text(formatPrice(item.subtotal))
                .font(.subheadline)
                .fontWeight(.semibold)
                .frame(width: 70, alignment: .trailing)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
    }
}

#Preview {
    CartView()
}
