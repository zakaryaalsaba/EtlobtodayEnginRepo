//
//  RestaurantListView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI

struct RestaurantListView: View {
    @StateObject private var viewModel = RestaurantViewModel()
    @State private var searchText = ""
    
    var filteredRestaurants: [Restaurant] {
        if searchText.isEmpty {
            return viewModel.restaurants
        }
        return viewModel.restaurants.filter {
            $0.restaurant_name.localizedCaseInsensitiveContains(searchText)
        }
    }
    
    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView(LocalizedStrings.loadingRestaurants)
                } else if let errorMessage = viewModel.errorMessage {
                    VStack(spacing: 16) {
                        Text(errorMessage)
                            .foregroundColor(.red)
                        Button("Retry") {
                            Task {
                                await viewModel.fetchRestaurants()
                            }
                        }
                    }
                } else if filteredRestaurants.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "fork.knife")
                            .font(.system(size: 50))
                            .foregroundColor(.gray)
                        Text(LocalizedStrings.noRestaurantsFound)
                            .foregroundColor(.gray)
                    }
                } else {
                    List(filteredRestaurants) { restaurant in
                        NavigationLink(destination: RestaurantDetailsView(restaurantId: restaurant.id)) {
                            RestaurantRowView(restaurant: restaurant)
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle(LocalizedStrings.restaurants)
            .searchable(text: $searchText, prompt: LocalizedStrings.searchRestaurants)
            .refreshable {
                await viewModel.fetchRestaurants()
            }
            .task {
                if viewModel.restaurants.isEmpty {
                    await viewModel.fetchRestaurants()
                }
            }
        }
    }
}

struct RestaurantRowView: View {
    let restaurant: Restaurant
    
    var body: some View {
        HStack(spacing: 16) {
            // Logo
            AsyncImage(url: URL(string: URLHelper.fixImageURL(restaurant.logo_url) ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Image(systemName: "fork.knife")
                    .foregroundColor(.gray)
            }
            .frame(width: 60, height: 60)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(restaurant.restaurant_name)
                    .font(.headline)
                
                if let description = restaurant.description {
                    Text(description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }
            
            Spacer()
        }
        .padding(.vertical, 8)
    }
}

#Preview {
    RestaurantListView()
}

