//
//  RestaurantDetailsView.swift
//  RestaurantIOS
//
//  Matches Android RestaurantDetailsActivity: hero, info card, tabs, trending, categories, bottom bar.
//

import SwiftUI

struct RestaurantDetailsView: View {
    let restaurantId: Int
    @StateObject private var viewModel = RestaurantDetailsViewModel()
    @StateObject private var cartManager = CartManager.shared
    @State private var showCart = false
    @State private var showMealSheet: Product? = nil
    @State private var showMealDetails: Product? = nil
    @State private var clearCartAlert: ClearCartAlert? = nil
    @Environment(\.dismiss) private var dismiss
    
    private let favPrefsKey = "restaurant_favorites"
    
    var body: some View {
        Group {
            if let restaurant = viewModel.restaurant {
                mainContent(restaurant: restaurant)
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Text(error).foregroundColor(.red).multilineTextAlignment(.center)
                        .padding()
                    Button(LocalizedStrings.retry) {
                        Task {
                            await viewModel.fetchRestaurant(id: restaurantId)
                            await viewModel.fetchProducts(websiteId: restaurantId)
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ProgressView(LocalizedStrings.loadingRestaurants)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                HStack(spacing: 8) {
                    Button(action: shareRestaurant) {
                        Image(systemName: "square.and.arrow.up")
                    }
                    Button(action: toggleFavorite) {
                        Image(systemName: isFavorite ? "heart.fill" : "heart")
                            .foregroundColor(isFavorite ? .red : .primary)
                    }
                }
            }
        }
        .sheet(isPresented: $showCart) {
            CartView(onOrderComplete: { dismiss() })
        }
        .sheet(item: $showMealSheet) { product in
            if let restaurant = viewModel.restaurant {
                MealSimpleSheet(
                    product: product,
                    restaurantId: restaurantId,
                    restaurantName: restaurant.restaurant_name,
                    currencyCode: restaurant.currency_code,
                    symbolPosition: restaurant.currency_symbol_position,
                    displayPrice: viewModel.getDisplayPrice(for: product).display,
                    originalPrice: viewModel.getDisplayPrice(for: product).original,
                    currentQuantity: cartManager.quantity(for: product.id),
                    onAdd: { qty in
                        addProductToCart(product, quantity: qty, displayPrice: viewModel.getDisplayPrice(for: product).display)
                    },
                    onDismiss: { showMealSheet = nil }
                )
            }
        }
        .fullScreenCover(item: $showMealDetails) { product in
            if let restaurant = viewModel.restaurant {
                MealDetailsView(
                    product: product,
                    restaurantId: restaurantId,
                    restaurantName: restaurant.restaurant_name,
                    currencyCode: restaurant.currency_code,
                    symbolPosition: restaurant.currency_symbol_position,
                    displayPrice: viewModel.getDisplayPrice(for: product).display,
                    originalPrice: viewModel.getDisplayPrice(for: product).original,
                    onDismiss: { showMealDetails = nil }
                )
            }
        }
        .alert(item: $clearCartAlert) { alert in
            Alert(
                title: Text(LocalizedStrings.clearCart),
                message: Text(LocalizedStrings.cartContainsDifferentRestaurant),
                primaryButton: .destructive(Text(LocalizedStrings.clearCartAndAdd)) {
                    cartManager.clearCartAndAdd(alert.product, restaurantId: restaurantId, unitPriceOverride: alert.unitPrice)
                    if alert.quantity > 1 {
                        cartManager.updateQuantity(productId: alert.product.id, quantity: alert.quantity)
                    }
                    showMealSheet = nil
                },
                secondaryButton: .cancel()
            )
        }
        .task {
            await viewModel.fetchRestaurant(id: restaurantId)
            await viewModel.fetchProducts(websiteId: restaurantId)
            await viewModel.fetchRestaurantOffers(websiteId: restaurantId)
        }
    }
    
    private var isFavorite: Bool {
        UserDefaults.standard.bool(forKey: "\(favPrefsKey)_\(restaurantId)")
    }
    
    private func toggleFavorite() {
        let key = "\(favPrefsKey)_\(restaurantId)"
        let new = !UserDefaults.standard.bool(forKey: key)
        UserDefaults.standard.set(new, forKey: key)
    }
    
    private func shareRestaurant() {
        let name = viewModel.restaurant?.restaurant_name ?? ""
        let text = name.isEmpty ? LocalizedStrings.checkOutThisRestaurant : "\(name) - \(LocalizedStrings.checkOutThisRestaurant)"
        #if os(iOS)
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = windowScene.windows.first?.rootViewController {
            root.present(av, animated: true)
        }
        #endif
    }
    
    private func mainContent(restaurant: Restaurant) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 0) {
                    heroSection(restaurant: restaurant)
                    infoCard(restaurant: restaurant)
                    categoryTabs(proxy: proxy)
                    menuContent(restaurant: restaurant)
                }
                .padding(.bottom, 100)
            }
        }
        .safeAreaInset(edge: .bottom) {
            bottomCartBar(restaurant: restaurant)
        }
    }
    
    private func heroSection(restaurant: Restaurant) -> some View {
        ZStack(alignment: .topTrailing) {
            AsyncImage(url: URL(string: URLHelper.fixImageURL(restaurant.logo_url) ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle().fill(Color.gray.opacity(0.3))
            }
            .frame(height: 280)
            .clipped()
            LinearGradient(colors: [.clear, .black.opacity(0.4)], startPoint: .top, endPoint: .bottom)
                .frame(height: 280)
        }
    }
    
    private func infoCard(restaurant: Restaurant) -> some View {
        HStack(alignment: .top, spacing: 16) {
            AsyncImage(url: URL(string: URLHelper.fixImageURL(restaurant.logo_url) ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Image(systemName: "fork.knife").foregroundColor(.gray)
            }
            .frame(width: 72, height: 72)
            .clipShape(Circle())
            VStack(alignment: .leading, spacing: 6) {
                Text(restaurant.restaurant_name)
                    .font(.title2)
                    .fontWeight(.bold)
                if let address = restaurant.address, !address.isEmpty {
                    Label(address, systemImage: "location")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                deliveryTimeLabel(restaurant: restaurant)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(20)
        .background(Color(.systemBackground))
        .cornerRadius(28)
        .shadow(color: .black.opacity(0.08), radius: 12, y: 4)
        .padding(.horizontal, 20)
        .offset(y: -16)
    }
    
    private func deliveryTimeLabel(restaurant: Restaurant) -> some View {
        Group {
            if let min = restaurant.delivery_time_min, let max = restaurant.delivery_time_max {
                Text("\(min)–\(max) mins")
            } else {
                Text("— mins")
            }
        }
    }
    
    private func categoryTabs(proxy: ScrollViewProxy) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                Button(LocalizedStrings.trending) {
                    withAnimation { proxy.scrollTo("trending", anchor: .top) }
                }
                .buttonStyle(.borderedProminent)
                .tint(.accentColor)
                ForEach(viewModel.categories, id: \.self) { cat in
                    Button(cat) {
                        withAnimation { proxy.scrollTo(cat, anchor: .top) }
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
        .background(Color(.systemBackground))
    }
    
    @ViewBuilder
    private func menuContent(restaurant: Restaurant) -> some View {
        if viewModel.products.isEmpty {
            VStack(spacing: 16) {
                Image(systemName: "fork.knife")
                    .font(.system(size: 50))
                    .foregroundColor(.secondary)
                Text(LocalizedStrings.noMenuItemsAvailable)
                    .font(.headline)
                    .foregroundColor(.primary)
                Text(LocalizedStrings.checkBackLaterForMenu)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(48)
        } else {
            LazyVStack(alignment: .leading, spacing: 0) {
                trendingSection(restaurant: restaurant)
                ForEach(viewModel.categories, id: \.self) { category in
                    categorySection(category: category, restaurant: restaurant)
                }
            }
        }
    }
    
    private func trendingSection(restaurant: Restaurant) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(LocalizedStrings.trending)
                .font(.headline)
                .padding(.horizontal)
                .id("trending")
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(viewModel.trendingProducts) { product in
                    let hasAddons = product.addon_required == true
                    TrendingProductCard(
                        product: product,
                        restaurantId: restaurantId,
                        displayPrice: viewModel.getDisplayPrice(for: product).display,
                        originalPrice: viewModel.getDisplayPrice(for: product).original,
                        currencyCode: restaurant.currency_code,
                        symbolPosition: restaurant.currency_symbol_position,
                        hasAddons: hasAddons,
                        onTap: {
                            if hasAddons { showMealDetails = product }
                            else { showMealSheet = product }
                        },
                        onAddDirect: { addProductToCartDirect(product, displayPrice: viewModel.getDisplayPrice(for: product).display) }
                    )
                }
            }
            .padding(.horizontal)
        }
        .padding(.bottom, 24)
    }
    
    private func categorySection(category: String, restaurant: Restaurant) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(category)
                .font(.headline)
                .padding(.horizontal)
                .id(category)
            ForEach(viewModel.groupedProducts[category] ?? []) { product in
                let hasAddons = product.addon_required == true
                ProductRowView(
                    product: product,
                    restaurantId: restaurantId,
                    displayPrice: viewModel.getDisplayPrice(for: product).display,
                    originalPrice: viewModel.getDisplayPrice(for: product).original,
                    currencyCode: restaurant.currency_code,
                    symbolPosition: restaurant.currency_symbol_position,
                    hasAddons: hasAddons,
                    quantity: cartManager.quantity(for: product.id),
                    onTap: {
                        if hasAddons { showMealDetails = product }
                        else { showMealSheet = product }
                    },
                    onAddDirect: { addProductToCartDirect(product, displayPrice: viewModel.getDisplayPrice(for: product).display) },
                    onQuantityChange: { cartManager.updateQuantity(productId: product.id, quantity: $0) }
                )
                Divider().padding(.leading, 96)
            }
        }
        .padding(.bottom, 24)
    }
    
    private func bottomCartBar(restaurant: Restaurant) -> some View {
        Button(action: { showCart = true }) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(cartManager.isEmpty ? Color(.systemGray5) : Color.white.opacity(0.3))
                        .frame(width: 28, height: 28)
                    Text("\(cartManager.items.reduce(0) { $0 + $1.quantity })")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(cartManager.isEmpty ? .primary : .white)
                }
                Image(systemName: "cart")
                    .foregroundColor(cartManager.isEmpty ? .primary : .white)
                Text(LocalizedStrings.viewCart)
                    .font(.subheadline)
                    .foregroundColor(cartManager.isEmpty ? .primary : .white)
                Spacer()
                Text(CurrencyFormatter.format(cartManager.total, currencyCode: restaurant.currency_code, symbolPosition: restaurant.currency_symbol_position))
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(cartManager.isEmpty ? .primary : .white)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(cartManager.isEmpty ? Color(.systemGray6) : Color.accentColor)
            .cornerRadius(24)
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
        .padding(.bottom, 8)
    }
    
    private func addProductToCartDirect(_ product: Product, displayPrice: Double) {
        guard product.is_available else { return }
        if let current = cartManager.currentRestaurantId, current != restaurantId {
            clearCartAlert = ClearCartAlert(product: product, unitPrice: displayPrice, quantity: 1)
            return
        }
        cartManager.addItem(product, restaurantId: restaurantId, unitPriceOverride: displayPrice)
    }
    
    private func addProductToCart(_ product: Product, quantity: Int, displayPrice: Double) {
        guard product.is_available, quantity > 0 else { return }
        if let current = cartManager.currentRestaurantId, current != restaurantId {
            clearCartAlert = ClearCartAlert(product: product, unitPrice: displayPrice, quantity: quantity)
            return
        }
        let rounded = (displayPrice * 100).rounded() / 100
        if cartManager.quantity(for: product.id) > 0 {
            cartManager.updateQuantity(productId: product.id, quantity: quantity)
        } else {
            cartManager.addItem(product, restaurantId: restaurantId, unitPriceOverride: rounded)
            if quantity > 1 {
                cartManager.updateQuantity(productId: product.id, quantity: quantity)
            }
        }
        showMealSheet = nil
    }
}

// MARK: - Clear cart alert payload
struct ClearCartAlert: Identifiable {
    let id = UUID()
    let product: Product
    let unitPrice: Double
    let quantity: Int
}

// MARK: - Trending product card (2-column grid). Chevron for addon items, plus for simple.
struct TrendingProductCard: View {
    let product: Product
    let restaurantId: Int
    let displayPrice: Double
    let originalPrice: Double?
    let currencyCode: String?
    let symbolPosition: String?
    let hasAddons: Bool
    let onTap: () -> Void
    let onAddDirect: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                AsyncImage(url: URL(string: URLHelper.fixImageURL(product.image_url) ?? "")) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle().fill(Color.gray.opacity(0.2))
                }
                .frame(height: 100)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                Text(product.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.leading)
                HStack {
                    if let orig = originalPrice, orig > displayPrice {
                        Text(CurrencyFormatter.format(orig, currencyCode: currencyCode, symbolPosition: symbolPosition))
                            .font(.caption)
                            .strikethrough()
                            .foregroundColor(.secondary)
                    }
                    Text(CurrencyFormatter.format(displayPrice, currencyCode: currencyCode, symbolPosition: symbolPosition))
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.accentColor)
                    Spacer()
                    if hasAddons {
                        Image(systemName: "chevron.right")
                            .font(.title2)
                            .foregroundColor(.accentColor)
                    } else {
                        Button(action: { onAddDirect() }) {
                            Image(systemName: "plus.circle.fill")
                                .font(.title2)
                                .foregroundColor(.accentColor)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(12)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Product row (list style). Chevron for addon items (opens meal details), plus/stepper for simple.
struct ProductRowView: View {
    let product: Product
    let restaurantId: Int
    let displayPrice: Double
    let originalPrice: Double?
    let currencyCode: String?
    let symbolPosition: String?
    let hasAddons: Bool
    let quantity: Int
    let onTap: () -> Void
    let onAddDirect: () -> Void
    let onQuantityChange: (Int) -> Void
    
    @State private var showAddedMessage = false
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 16) {
                AsyncImage(url: URL(string: URLHelper.fixImageURL(product.image_url) ?? "")) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle().fill(Color.gray.opacity(0.2)).overlay(Image(systemName: "photo").foregroundColor(.gray))
                }
                .frame(width: 80, height: 80)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 4) {
                    Text(product.name).font(.headline)
                    if let desc = product.description {
                        Text(desc).font(.caption).foregroundColor(.secondary).lineLimit(2)
                    }
                    HStack {
                        if let orig = originalPrice, orig > displayPrice {
                            Text(CurrencyFormatter.format(orig, currencyCode: currencyCode, symbolPosition: symbolPosition))
                                .font(.caption).strikethrough().foregroundColor(.secondary)
                        }
                        Text(CurrencyFormatter.format(displayPrice, currencyCode: currencyCode, symbolPosition: symbolPosition))
                            .font(.subheadline).fontWeight(.semibold).foregroundColor(.accentColor)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if hasAddons {
                    Image(systemName: "chevron.right")
                        .font(.body)
                        .foregroundColor(.secondary)
                } else if quantity > 0 {
                    HStack(spacing: 8) {
                        Button("-") { onQuantityChange(quantity - 1) }
                            .buttonStyle(.bordered)
                        Text("\(quantity)").font(.subheadline).frame(minWidth: 24)
                        Button("+") { onQuantityChange(quantity + 1) }
                            .buttonStyle(.bordered)
                    }
                } else {
                    Button(action: {
                        onAddDirect()
                        withAnimation { showAddedMessage = true }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { showAddedMessage = false }
                    }) {
                        Image(systemName: "plus.circle.fill").font(.title2).foregroundColor(.accentColor)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
        .overlay(alignment: .center) {
            if showAddedMessage {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                    Text(LocalizedStrings.addedToCart).font(.subheadline).fontWeight(.semibold)
                }
                .padding(.horizontal, 16).padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 25).fill(Color(.systemBackground)).shadow(color: .black.opacity(0.15), radius: 8, y: 4))
                .transition(.scale.combined(with: .opacity))
                .zIndex(1)
            }
        }
    }
}

// MARK: - Meal simple bottom sheet (quantity + Add item)
struct MealSimpleSheet: View {
    let product: Product
    let restaurantId: Int
    let restaurantName: String
    let currencyCode: String?
    let symbolPosition: String?
    let displayPrice: Double
    let originalPrice: Double?
    let currentQuantity: Int
    let onAdd: (Int) -> Void
    let onDismiss: () -> Void
    
    @State private var quantity: Int
    
    init(product: Product, restaurantId: Int, restaurantName: String, currencyCode: String?, symbolPosition: String?, displayPrice: Double, originalPrice: Double?, currentQuantity: Int, onAdd: @escaping (Int) -> Void, onDismiss: @escaping () -> Void) {
        self.product = product
        self.restaurantId = restaurantId
        self.restaurantName = restaurantName
        self.currencyCode = currencyCode
        self.symbolPosition = symbolPosition
        self.displayPrice = displayPrice
        self.originalPrice = originalPrice
        self.currentQuantity = currentQuantity
        self.onAdd = onAdd
        self.onDismiss = onDismiss
        _quantity = State(initialValue: max(1, currentQuantity))
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                AsyncImage(url: URL(string: URLHelper.fixImageURL(product.image_url) ?? "")) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle().fill(Color.gray.opacity(0.2))
                }
                .frame(height: 180)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                VStack(alignment: .leading, spacing: 8) {
                    Text(product.name).font(.title2).fontWeight(.bold)
                    Text(product.description ?? LocalizedStrings.noDescriptionAvailable)
                        .font(.subheadline).foregroundColor(.secondary)
                    HStack {
                        if let orig = originalPrice, orig > displayPrice {
                            Text(CurrencyFormatter.format(orig, currencyCode: currencyCode, symbolPosition: symbolPosition))
                                .strikethrough().foregroundColor(.secondary)
                        }
                        Text(CurrencyFormatter.format(displayPrice, currencyCode: currencyCode, symbolPosition: symbolPosition))
                            .font(.headline).foregroundColor(.accentColor)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                HStack {
                    Text(LocalizedStrings.addItem)
                    Spacer()
                    HStack(spacing: 16) {
                        Button("-") {
                            if quantity > 1 { quantity -= 1 }
                        }
                        .buttonStyle(.bordered)
                        Text("\(quantity)").font(.headline).frame(minWidth: 28)
                        Button("+") {
                            if quantity < 99 { quantity += 1 }
                        }
                        .buttonStyle(.bordered)
                    }
                }
                Spacer()
                Button(action: {
                    onAdd(quantity)
                }) {
                    Text("\(LocalizedStrings.addItem) · \(CurrencyFormatter.format(displayPrice * Double(quantity), currencyCode: currencyCode, symbolPosition: symbolPosition))")
                        .frame(maxWidth: .infinity)
                        .padding()
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
            .navigationTitle(product.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStrings.close) { onDismiss() }
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        RestaurantDetailsView(restaurantId: 1)
    }
}
