//
//  MealDetailsView.swift
//  RestaurantIOS
//
//  Full-screen meal details for products with add-ons (matches Android MealDetailsActivity).
//

import SwiftUI

struct MealDetailsView: View {
    let product: Product
    let restaurantId: Int
    let restaurantName: String
    let currencyCode: String?
    let symbolPosition: String?
    let displayPrice: Double
    let originalPrice: Double?
    let onDismiss: () -> Void
    
    @StateObject private var cartManager = CartManager.shared
    @State private var addonsData: ProductAddonsResponse?
    @State private var loadingAddons = true
    @State private var quantity = 1
    @State private var selectedRequired: Set<Int> = []
    @State private var selectedOptional: Set<Int> = []
    @State private var showClearCartAlert = false
    
    private var requiredAddons: [ProductAddon] {
        (addonsData?.addons ?? []).filter(\.is_required).sorted { $0.display_order < $1.display_order }
    }
    
    private var optionalAddons: [ProductAddon] {
        (addonsData?.addons ?? []).filter { !$0.is_required }.sorted { $0.display_order < $1.display_order }
    }
    
    private var addonRequiredMin: Int {
        addonsData?.addon_required_min ?? 1
    }
    
    private var canAddToCart: Bool {
        guard addonsData?.addon_required == true else { return true }
        let minN = addonRequiredMin
        if minN == -1 { return selectedRequired.count == requiredAddons.count }
        return selectedRequired.count >= minN
    }
    
    private var totalPrice: Double {
        let base = displayPrice * Double(quantity)
        let addons = selectedRequiredIds.compactMap { id in requiredAddons.first(where: { $0.id == id }) }
            + selectedOptionalIds.compactMap { id in optionalAddons.first(where: { $0.id == id }) }
        let addonTotal = addons.reduce(0.0) { $0 + $1.price } * Double(quantity)
        return base + addonTotal
    }
    
    private var selectedRequiredIds: [Int] { Array(selectedRequired) }
    private var selectedOptionalIds: [Int] { Array(selectedOptional) }
    
    private var selectedAddons: [ProductAddon] {
        let req = requiredAddons.filter { selectedRequired.contains($0.id) }
        let opt = optionalAddons.filter { selectedOptional.contains($0.id) }
        return req + opt
    }
    
    var body: some View {
        NavigationStack {
            Group {
                if loadingAddons {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 24) {
                            productHeader
                            if !requiredAddons.isEmpty {
                                requiredAddonsSection
                            }
                            if !optionalAddons.isEmpty {
                                optionalAddonsSection
                            }
                        }
                        .padding()
                        .padding(.bottom, 120)
                    }
                }
            }
            .navigationTitle(product.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStrings.close) { onDismiss() }
                }
            }
            .task {
                await loadAddons()
            }
            .safeAreaInset(edge: .bottom) {
                if !loadingAddons {
                    bottomBar
                }
            }
            .alert(LocalizedStrings.clearCart, isPresented: $showClearCartAlert) {
                Button(LocalizedStrings.clearCartAndAdd) {
                    cartManager.clearCartAndAdd(product, restaurantId: restaurantId, unitPriceOverride: displayPrice, selectedAddons: selectedAddons)
                    if quantity > 1 {
                        cartManager.updateQuantity(productId: product.id, addonIds: selectedAddons.map(\.id), quantity: quantity)
                    }
                    onDismiss()
                }
                Button(LocalizedStrings.cancel, role: .cancel) { }
            } message: {
                Text(LocalizedStrings.cartContainsDifferentRestaurant)
            }
        }
    }
    
    private var productHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            AsyncImage(url: URL(string: URLHelper.fixImageURL(product.image_url) ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle().fill(Color.gray.opacity(0.2))
            }
            .frame(height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            Text(product.name)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            Text(product.description ?? LocalizedStrings.noDescriptionAvailable)
                .font(.subheadline)
                .foregroundColor(.secondary)
            HStack {
                if let orig = originalPrice, orig > displayPrice {
                    Text(CurrencyFormatter.format(orig, currencyCode: currencyCode, symbolPosition: symbolPosition))
                        .strikethrough()
                        .foregroundColor(.secondary)
                }
                Text(CurrencyFormatter.format(displayPrice, currencyCode: currencyCode, symbolPosition: symbolPosition))
                    .font(.headline)
                    .foregroundColor(.accentColor)
            }
        }
    }
    
    private var requiredAddonsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center) {
                Text(LocalizedStrings.chooseFrom)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                Spacer()
                Text(LocalizedStrings.requiredChip)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.primary)
                    .cornerRadius(12)
            }
            Text(LocalizedStrings.chooseN(addonRequiredMin == -1 ? requiredAddons.count : addonRequiredMin))
                .font(.subheadline)
                .foregroundColor(.primary)
            ForEach(requiredAddons) { addon in
                AddonRow(
                    addon: addon,
                    currencyCode: currencyCode,
                    symbolPosition: symbolPosition,
                    isSelected: selectedRequired.contains(addon.id),
                    isMultiple: addonRequiredMin != 1
                ) {
                    if addonRequiredMin == 1 {
                        selectedRequired = [addon.id]
                    } else {
                        if selectedRequired.contains(addon.id) {
                            selectedRequired.remove(addon.id)
                        } else {
                            selectedRequired.insert(addon.id)
                        }
                    }
                }
            }
        }
    }
    
    private var optionalAddonsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(LocalizedStrings.orderedTogether)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            Text(LocalizedStrings.orderedTogetherSubtitle)
                .font(.subheadline)
                .foregroundColor(.secondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(optionalAddons) { addon in
                        AddonRow(
                            addon: addon,
                            currencyCode: currencyCode,
                            symbolPosition: symbolPosition,
                            isSelected: selectedOptional.contains(addon.id),
                            isMultiple: true
                        ) {
                            if selectedOptional.contains(addon.id) {
                                selectedOptional.remove(addon.id)
                            } else {
                                selectedOptional.insert(addon.id)
                            }
                        }
                        .frame(minWidth: 160)
                    }
                }
                .padding(.vertical, 4)
            }
        }
    }
    
    private var bottomBar: some View {
        VStack(spacing: 0) {
            if addonsData?.addon_required == true && !canAddToCart {
                Text(LocalizedStrings.selectRequiredToAddItem)
                    .font(.subheadline)
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 8)
            }
            HStack(alignment: .center, spacing: 16) {
                HStack(spacing: 0) {
                    Button(action: {
                        if quantity > 1 { quantity -= 1 }
                    }) {
                        Image(systemName: "minus")
                            .font(.body.weight(.semibold))
                            .frame(width: 40, height: 40)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Text("\(quantity)")
                        .font(.headline)
                        .frame(minWidth: 44)
                    Button(action: {
                        if quantity < 99 { quantity += 1 }
                    }) {
                        Image(systemName: "plus")
                            .font(.body.weight(.semibold))
                            .frame(width: 40, height: 40)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                .padding(6)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(22)
                Button(action: addToCart) {
                    HStack {
                        Text(product.is_available ? LocalizedStrings.addItem : LocalizedStrings.unavailable)
                            .font(.subheadline)
                            .fontWeight(.bold)
                        Spacer()
                        if product.is_available {
                            Text(CurrencyFormatter.format(totalPrice, currencyCode: currencyCode, symbolPosition: symbolPosition))
                                .font(.subheadline)
                                .fontWeight(.bold)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                    .frame(maxWidth: .infinity)
                    .background(canAddToCart && product.is_available ? Color.accentColor : Color(.systemGray4))
                    .foregroundColor(.white)
                    .cornerRadius(26)
                }
                .buttonStyle(.plain)
                .disabled(!canAddToCart || !product.is_available)
            }
            .padding(.horizontal)
        }
        .padding(.top, 12)
        .padding(.bottom, 20)
        .background(Color(.systemBackground))
    }
    
    private func loadAddons() async {
        loadingAddons = true
        defer { loadingAddons = false }
        do {
            addonsData = try await APIService.shared.getProductAddons(productId: product.id)
            if addonRequiredMin == 1, let first = requiredAddons.first {
                selectedRequired = [first.id]
            }
        } catch {
            addonsData = ProductAddonsResponse(addons: [], addon_required: false, addon_required_min: nil)
        }
    }
    
    private func addToCart() {
        guard product.is_available, canAddToCart else { return }
        if let current = cartManager.currentRestaurantId, current != restaurantId {
            showClearCartAlert = true
            return
        }
        cartManager.addItem(product, restaurantId: restaurantId, unitPriceOverride: displayPrice, selectedAddons: selectedAddons)
        if quantity > 1 {
            cartManager.updateQuantity(productId: product.id, addonIds: selectedAddons.map(\.id), quantity: quantity)
        }
        onDismiss()
    }
}

// MARK: - Addon row (selectable)
struct AddonRow: View {
    let addon: ProductAddon
    let currencyCode: String?
    let symbolPosition: String?
    let isSelected: Bool
    let isMultiple: Bool
    let onToggle: () -> Void
    
    var body: some View {
        Button(action: onToggle) {
            HStack {
                Image(systemName: isMultiple ? (isSelected ? "checkmark.square.fill" : "square") : (isSelected ? "circle.inset.filled" : "circle"))
                    .foregroundColor(isSelected ? .accentColor : .secondary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(addon.name)
                        .font(.subheadline)
                        .foregroundColor(.primary)
                    if addon.price > 0 {
                        Text(CurrencyFormatter.format(addon.price, currencyCode: currencyCode, symbolPosition: symbolPosition))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                Spacer()
            }
            .padding(12)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
        }
        .buttonStyle(.plain)
    }
}

