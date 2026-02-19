# Restaurant Order Management Android App

A modern Android application for restaurants to manage incoming customer orders from the food delivery platform.

## Features

- ✅ **Secure Authentication** - Username/password login with session persistence
- ✅ **Real-time Order Dashboard** - Live updates of incoming orders
- ✅ **Order Management** - Accept, reject, and update order status
- ✅ **Order History** - View completed and cancelled orders
- ✅ **Push Notifications** - Firebase Cloud Messaging for new orders
- ✅ **Bluetooth Printing** - Print kitchen receipts to thermal printers
- ✅ **Bilingual Support** - Arabic and English localization
- ✅ **Modern UI** - Material Design 3 with dark mode support

## Architecture

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: Material Design 3
- **Networking**: Retrofit + Coroutines
- **Real-time**: Polling (5-second intervals)
- **Notifications**: Firebase Cloud Messaging
- **Localization**: Arabic/English with RTL support

## Prerequisites

- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK 24+ (Android 7.0+)
- Backend server running at `http://localhost:3000` (or configure API_BASE_URL)

## Setup Instructions

### 1. Clone and Open Project

```bash
cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/OrderManageAndroid
```

Open the project in Android Studio.

### 2. Configure API Base URL

The default API URL is `http://10.0.2.2:3000/api/` (for Android Emulator).

For physical devices, update `app/build.gradle.kts` or add to `local.properties`:

```properties
API_BASE_URL=http://YOUR_IP_ADDRESS:3000/api/
```

### 3. Firebase Cloud Messaging Setup

**IMPORTANT**: The app includes a placeholder `google-services.json` file. You MUST replace it with your actual Firebase configuration.

1. **Create Firebase Project**:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use existing
   - Add Android app with package name: `com.order.resturantandroid`

2. **Download google-services.json**:
   - Download `google-services.json` from Firebase Console
   - **Replace** the placeholder file in `app/google-services.json` with your actual file

3. **Enable Cloud Messaging**:
   - In Firebase Console → Project Settings → Cloud Messaging
   - Enable Cloud Messaging API
   - Copy Server Key (needed for backend)

4. **Backend Configuration**:
   - Add Firebase Server Key to backend environment variables
   - Backend will send notifications to restaurant devices

**Note**: The app will build with the placeholder file, but notifications won't work until you replace it with your real Firebase configuration.

### 4. Build and Run

1. Sync Gradle files
2. Build the project
3. Run on emulator or physical device

## Usage

### Login

- Use restaurant admin email and password
- Session is persisted automatically
- Logout available from menu

### Dashboard

- Shows all active orders (pending, confirmed, preparing, ready)
- Swipe down to refresh
- Tap order to view details
- Auto-refreshes every 5 seconds

### Order Actions

- **Accept Order**: Changes status from "pending" to "confirmed"
- **Reject Order**: Cancels the order with reason
- **Update Status**: Change status (confirmed → preparing → ready → completed)
- **Print Order**: Print kitchen receipt to Bluetooth printer

### Order Details

- View complete order information
- See customer details and address
- View all order items with quantities
- Update status or print receipt

## Bluetooth Printer Setup

### 1. Pair Printer

1. Go to Android Settings → Bluetooth
2. Pair your thermal printer
3. Note the printer's MAC address

### 2. Configure in App

1. Open Settings (from menu)
2. Select "Printer Settings"
3. Choose paired Bluetooth device
4. Test print

### 3. Supported Printers

- ESC/POS compatible thermal printers
- 58mm or 80mm width
- Bluetooth connectivity

## Localization

The app supports Arabic and English:

- **Change Language**: Settings → Language
- **RTL Support**: Automatically enabled for Arabic
- **Persistent**: Language preference is saved

## Project Structure

```
app/src/main/java/com/order/resturantandroid/
├── data/
│   ├── model/          # Data models (Order, OrderItem, etc.)
│   ├── repository/    # Repository layer
│   └── remote/        # API service and Retrofit client
├── ui/
│   ├── auth/          # Login screen
│   ├── dashboard/     # Orders dashboard
│   └── orders/        # Order details screen
├── service/           # Firebase messaging, printing, real-time
└── util/              # SessionManager, LocaleHelper
```

## API Endpoints Used

- `POST /api/admin/login` - Restaurant login
- `GET /api/orders/website/:websiteId` - Get orders
- `GET /api/orders/:orderId` - Get order details
- `PUT /api/orders/:id/status` - Update order status
- `GET /api/admin/me` - Get admin profile

## Troubleshooting

### Notifications Not Working

1. Check Firebase setup (google-services.json)
2. Verify backend has Firebase Server Key
3. Check device has Google Play Services
4. Enable notifications in Android Settings

### Bluetooth Printing Not Working

1. Ensure printer is paired
2. Check Bluetooth permissions
3. Verify printer supports ESC/POS
4. Test with printer's test print function

### API Connection Issues

1. Check API_BASE_URL configuration
2. Verify backend is running
3. Check network connectivity
4. For emulator: use `10.0.2.2` instead of `localhost`

## Development Notes

- Real-time updates use polling (5-second intervals)
- Can be upgraded to WebSocket for better performance
- Printing uses native Android Bluetooth API
- All strings are localized (English/Arabic)

## License

Proprietary - Restaurant Order Management System

