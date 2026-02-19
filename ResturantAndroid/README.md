# Restaurant Ordering Android App

A production-ready Android Restaurant Ordering App built with Kotlin, following MVVM architecture and Material Design 3 principles.

## Features

### ✅ Completed

1. **Authentication**
   - Customer registration with email and password
   - Customer login/logout
   - Session persistence using SharedPreferences
   - JWT token-based authentication

2. **Backend Integration**
   - REST API integration with Retrofit
   - Customer authentication endpoints
   - Restaurant listing and details
   - Product/menu fetching
   - Order creation and history

3. **Architecture**
   - MVVM (Model-View-ViewModel) pattern
   - Repository pattern for data management
   - Coroutines for async operations
   - LiveData for reactive UI updates

4. **Data Models**
   - Customer, Restaurant, Product, Order, CartItem models
   - Proper data classes with Kotlin

5. **UI Components**
   - Login Activity
   - Register Activity
   - Main Activity with bottom navigation
   - Restaurant List Fragment with search
   - Restaurant card adapter

### 🚧 To Be Completed

1. **Restaurant Details Screen**
   - Display restaurant information
   - Show menu items grouped by category
   - Add to cart functionality

2. **Cart & Checkout**
   - Cart screen with item management
   - Quantity increase/decrease
   - Checkout screen with order details
   - Place order functionality

3. **Order Management**
   - Order confirmation screen
   - Order history screen
   - Order status tracking

4. **Additional Features**
   - Image loading with Glide (configured but needs implementation)
   - Error handling improvements
   - Loading states
   - Empty states
   - Pull-to-refresh

## Project Structure

```
app/src/main/java/com/mnsf/resturantandroid/
├── data/
│   └── model/          # Data models (Customer, Restaurant, Product, Order, CartItem)
├── network/            # Retrofit API service and client
├── repository/         # Repository classes for data access
├── ui/
│   ├── auth/          # Login and Register activities
│   ├── home/          # Restaurant list fragment
│   └── restaurant/     # Restaurant details (to be implemented)
├── util/               # Utility classes (SessionManager)
└── viewmodel/          # ViewModels for each screen
```

## Setup Instructions

### 1. Backend Configuration

Update the base URL in `RetrofitClient.kt`:
- For Android Emulator: `http://10.0.2.2:3000/api/`
- For Physical Device: `http://YOUR_COMPUTER_IP:3000/api/`

### 2. Build and Run

```bash
# Open in Android Studio
# Sync Gradle files
# Run the app
```

### 3. Backend Requirements

Ensure the backend is running at `http://localhost:3000` with:
- Customer authentication endpoints (`/api/auth/register`, `/api/auth/login`)
- Restaurant endpoints (`/api/websites`)
- Product endpoints (`/api/products/website/:websiteId`)
- Order endpoints (`/api/orders`, `/api/customers/:customerId/orders`)

## Dependencies

- **Retrofit 2.9.0** - REST API client
- **OkHttp 4.12.0** - HTTP client
- **Coroutines 1.7.3** - Async operations
- **Material Design 1.12.0** - UI components
- **Glide 4.16.0** - Image loading
- **Navigation Component** - Navigation between screens
- **Lifecycle Components** - ViewModel and LiveData

## API Endpoints Used

- `POST /api/auth/register` - Register new customer
- `POST /api/auth/login` - Login customer
- `GET /api/auth/me` - Get current customer
- `GET /api/websites` - Get all restaurants
- `GET /api/websites/:id` - Get restaurant details
- `GET /api/products/website/:websiteId` - Get restaurant menu
- `POST /api/orders` - Create order
- `GET /api/customers/:customerId/orders` - Get customer orders

## Next Steps

1. Complete RestaurantDetailsActivity with menu display
2. Implement Cart functionality
3. Create Checkout screen
4. Add Order History screen
5. Improve error handling and user feedback
6. Add loading and empty states
7. Implement image loading with Glide
8. Add pull-to-refresh functionality
9. Improve UI/UX with animations and transitions

## Notes

- The app uses Material Design 3 for a modern look
- All network calls are handled with Coroutines
- Session management is done via SharedPreferences
- The app follows clean architecture principles
- Ready for extension with admin dashboard, payments, etc.

