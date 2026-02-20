//
//  HomeView.swift
//  RestaurantIOS
//

import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = RestaurantViewModel()
    
    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading && viewModel.restaurants.isEmpty {
                    ProgressView(LocalizedStrings.loadingRestaurants)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let errorMessage = viewModel.errorMessage, viewModel.restaurants.isEmpty {
                    errorView(message: errorMessage)
                } else {
                    mainContent
                }
            }
            .navigationTitle(LocalizedStrings.home)
            .searchable(text: $viewModel.searchQuery, prompt: LocalizedStrings.searchRestaurants)
            .refreshable {
                await viewModel.fetchRestaurants()
                await viewModel.loadOffers()
            }
            .task {
                if viewModel.restaurants.isEmpty {
                    await viewModel.fetchRestaurants()
                }
                await viewModel.loadOffers()
            }
        }
    }
    
    private var mainContent: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    orderTypeToggle
                    
                    if viewModel.orderType == "delivery" && !viewModel.freeDeliveryRestaurants.isEmpty {
                        horizontalSection(
                            title: LocalizedStrings.freeDelivery,
                            viewAllAction: { clearSearchAndScrollToAll(proxy: proxy) }
                        ) {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(viewModel.freeDeliveryRestaurants) { restaurant in
                                        NavigationLink(destination: RestaurantDetailsView(restaurantId: restaurant.id)) {
                                            RestaurantCardView(restaurant: restaurant)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .padding(.horizontal)
                            }
                            .frame(height: 140)
                        }
                    }
                    
                    if !viewModel.offers.isEmpty {
                        horizontalSection(
                            title: LocalizedStrings.offers,
                            viewAllAction: { clearSearchAndScrollToAll(proxy: proxy) }
                        ) {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(viewModel.offers) { offer in
                                        NavigationLink(destination: RestaurantDetailsView(restaurantId: offer.website_id)) {
                                            OfferCardView(offer: offer)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .padding(.horizontal)
                            }
                            .frame(height: 140)
                        }
                    }
                    
                    if !viewModel.popularRestaurants.isEmpty {
                        horizontalSection(
                            title: LocalizedStrings.popularRestaurants,
                            viewAllAction: { clearSearchAndScrollToAll(proxy: proxy) }
                        ) {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(viewModel.popularRestaurants) { restaurant in
                                        NavigationLink(destination: RestaurantDetailsView(restaurantId: restaurant.id)) {
                                            RestaurantCardView(restaurant: restaurant)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .padding(.horizontal)
                            }
                            .frame(height: 140)
                        }
                    }
                    
                    allRestaurantsSection(proxy: proxy)
                }
                .padding(.bottom, 24)
            }
        }
    }
    
    private var orderTypeToggle: some View {
        HStack(spacing: 0) {
            Button {
                viewModel.setOrderType("delivery")
            } label: {
                Text(LocalizedStrings.delivery)
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(viewModel.orderType == "delivery" ? Color.accentColor : Color(.systemGray5))
                    .foregroundColor(viewModel.orderType == "delivery" ? .white : .primary)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
            
            Button {
                viewModel.setOrderType("pickup")
            } label: {
                Text(LocalizedStrings.pickup)
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(viewModel.orderType == "pickup" ? Color.accentColor : Color(.systemGray5))
                    .foregroundColor(viewModel.orderType == "pickup" ? .white : .primary)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal)
        .padding(.top, 8)
    }
    
    private func horizontalSection<Content: View>(
        title: String,
        viewAllAction: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.headline)
                Spacer()
                Button(action: viewAllAction) {
                    Text(LocalizedStrings.viewAll)
                        .font(.subheadline)
                        .foregroundColor(.accentColor)
                }
            }
            .padding(.horizontal)
            content()
        }
    }
    
    private func allRestaurantsSection(proxy: ScrollViewProxy) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(LocalizedStrings.allRestaurants)
                .font(.headline)
                .padding(.horizontal)
                .id("allRestaurants")
            
            if viewModel.filteredRestaurants.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 50))
                        .foregroundColor(.gray)
                    Text(LocalizedStrings.noRestaurantsFound)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
            } else {
                LazyVStack(spacing: 0) {
                    ForEach(viewModel.filteredRestaurants) { restaurant in
                        NavigationLink(destination: RestaurantDetailsView(restaurantId: restaurant.id)) {
                            RestaurantRowView(restaurant: restaurant)
                        }
                        .buttonStyle(.plain)
                        Divider()
                            .padding(.leading, 76)
                    }
                }
                .background(Color(.systemBackground))
                .padding(.horizontal)
            }
        }
    }
    
    private func clearSearchAndScrollToAll(proxy: ScrollViewProxy) {
        viewModel.searchQuery = ""
        withAnimation {
            proxy.scrollTo("allRestaurants", anchor: .top)
        }
    }
    
    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .foregroundColor(.red)
                .multilineTextAlignment(.center)
                .padding()
            Button(LocalizedStrings.retry) {
                Task {
                    await viewModel.fetchRestaurants()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Restaurant Card (horizontal sections)
struct RestaurantCardView: View {
    let restaurant: Restaurant
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            AsyncImage(url: URL(string: URLHelper.fixImageURL(restaurant.logo_url) ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Image(systemName: "fork.knife")
                    .foregroundColor(.gray)
            }
            .frame(width: 100, height: 80)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            
            Text(restaurant.restaurant_name)
                .font(.caption)
                .fontWeight(.medium)
                .lineLimit(2)
                .frame(width: 100, alignment: .leading)
        }
        .frame(width: 100)
    }
}

// MARK: - Offer Card (horizontal section)
struct OfferCardView: View {
    let offer: Offer
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: URL(string: URLHelper.fixImageURL(offer.first_product_image_url ?? offer.logo_url) ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle()
                    .fill(Color(.systemGray5))
                    .overlay(Image(systemName: "tag").foregroundColor(.gray))
            }
            .frame(width: 160, height: 130)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            
            VStack(alignment: .leading, spacing: 2) {
                Text(offer.title)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .lineLimit(2)
                    .shadow(color: .black.opacity(0.5), radius: 2)
                if let name = offer.restaurant_name {
                    Text(name)
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.9))
                        .lineLimit(1)
                        .shadow(color: .black.opacity(0.5), radius: 2)
                }
            }
            .padding(8)
        }
        .frame(width: 160, height: 130)
    }
}

#Preview {
    HomeView()
}
