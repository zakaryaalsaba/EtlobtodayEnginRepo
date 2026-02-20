# RestaurantIOS

iOS app for restaurant ordering, cloned from the Android Restaurant app.

## Features

- **Authentication**: Login and Register with token-based authentication
- **Restaurant Browsing**: View list of restaurants and restaurant details
- **Menu Browsing**: Browse menu items by category
- **Shopping Cart**: Add/remove items, update quantities
- **Order Management**: Place orders, track order status, view order history
- **Real-time Updates**: Order status polling for live updates

## Architecture

- **UI Framework**: SwiftUI
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: URLSession (native iOS networking)
- **Data Persistence**: UserDefaults for session management

## Project Structure

```
RestaurantIOS/
├── Models/              # Data models
│   ├── Restaurant.swift
│   ├── Product.swift
│   ├── Order.swift
│   ├── Customer.swift
│   ├── CartItem.swift
│   └── Notification.swift
├── Views/               # SwiftUI views
│   ├── Auth/
│   │   ├── LoginView.swift
│   │   └── RegisterView.swift
│   ├── Restaurant/
│   │   ├── RestaurantListView.swift
│   │   └── RestaurantDetailsView.swift
│   ├── Cart/
│   │   └── CartView.swift
│   ├── Order/
│   │   ├── CheckoutView.swift
│   │   ├── OrderConfirmationView.swift
│   │   ├── OrderHistoryView.swift
│   │   └── OrderTrackingView.swift
│   └── MainTabView.swift
├── ViewModels/          # View models
│   ├── AuthViewModel.swift
│   ├── RestaurantViewModel.swift
│   ├── RestaurantDetailsViewModel.swift
│   └── OrderViewModel.swift
├── Services/            # Business logic and API
│   ├── APIService.swift
│   ├── SessionManager.swift
│   └── CartManager.swift
└── Utils/               # Utilities
    ├── APIConfig.swift
    ├── CurrencyFormatter.swift
    └── URLHelper.swift
```

## Configuration

### API Base URL

Update the base URL in `Utils/APIConfig.swift`:

- **iOS Simulator**: `http://localhost:3000/api`
- **Physical Device**: `http://YOUR_IP_ADDRESS:3000/api`
  - Find your IP: `ifconfig | grep "inet " | grep -v 127.0.0.1`
  - Example: `http://192.168.1.100:3000/api`
- **Production**: Update with your production URL

### Backend Requirements

The app connects to the backend at `/Users/zakaryaalsaba/Desktop/RestaurantEngin/backend`.

Make sure the backend is running:
```bash
cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/backend
npm start
```

## Setup

1. Open `RestaurantIOS.xcodeproj` in Xcode
2. **Configure Network Security** (Required for HTTP connections):
   - Select the project in the navigator
   - Select the `RestaurantIOS` target
   - Go to the "Info" tab
   - Under "Custom iOS Target Properties", click the "+" button
   - Add key: `App Transport Security Settings` (or `NSAppTransportSecurity`)
   - Expand it and add: `Allow Arbitrary Loads` = `YES`
   - Alternatively, you can add it as a dictionary with key `NSAppTransportSecurity` and nested key `NSAllowsArbitraryLoads` = `YES`
3. Update `APIConfig.swift` with your backend URL:
   - **iOS Simulator**: `http://localhost:3000/api`
   - **Physical Device**: `http://YOUR_IP:3000/api` (find IP with `ifconfig`)
4. Build and run on simulator or device

### Important: Network Security

iOS blocks HTTP (non-HTTPS) connections by default. The network security setting above allows HTTP connections for local development. For production, you should use HTTPS and remove this setting.

## Features Matching Android App

✅ Authentication (Login/Register)
✅ Restaurant List with Search
✅ Restaurant Details
✅ Menu Items by Category
✅ Shopping Cart
✅ Checkout with Order Types (Pickup/Dine-in/Delivery)
✅ Payment Methods Support
✅ Tax Calculation
✅ Delivery Fees
✅ Order Confirmation
✅ Order History (Active/Archive)
✅ Order Tracking with Real-time Updates
✅ Profile View
✅ Session Persistence

## Notes

- The app uses native SwiftUI components
- All API calls use URLSession (no third-party networking libraries)
- Session is persisted using UserDefaults
- Cart is managed in-memory during app session
- Order tracking polls the API every 5 seconds for updates

