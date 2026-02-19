# Firebase Realtime Database - Driver App Order Listening

## Overview
The DriverAndroid app now reads orders in real-time from Firebase Realtime Database in the Home activity. All write operations continue to go through MySQL/REST API.

## Implementation

### 1. Firebase Realtime Database Dependency
Added to `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-database-ktx")
```

### 2. FirebaseOrderService
- **Location**: `app/src/main/java/com/driver/resturantandroid/service/FirebaseOrderService.kt`
- **Purpose**: Read-only service for listening to orders from Firebase
- **Firebase Path**: `orders/{website_id}/{order_number}`
- **Database URL**: The app uses the **same** Realtime Database URL as the backend (`FIREBASE_DATABASE_URL`). Default: `https://tashkeela-8cab1-default-rtdb.europe-west1.firebasedatabase.app`. Without this, the app would connect to the default (US) instance and never receive data. Override via `FIREBASE_DATABASE_URL` in `gradle.properties` or `local.properties` if your backend uses a different URL.
- **Filtering**: Only listens to delivery orders with status: `pending`, `confirmed`, `preparing`, `ready`
  - **Note**: Includes `pending` status so newly created orders appear immediately

### 3. HomeViewModel
- **Location**: `app/src/main/java/com/driver/resturantandroid/ui/home/HomeViewModel.kt`
- **Changes**: 
  - Uses `FirebaseOrderService` to listen to orders in real-time
  - Exposes `availableOrders` LiveData that updates automatically when Firebase data changes
  - Automatically starts listening when ViewModel is created

### 4. HomeFragment
- **Location**: `app/src/main/java/com/driver/resturantandroid/ui/home/HomeFragment.kt`
- **Changes**:
  - Observes `homeViewModel.availableOrders` to get real-time order updates
  - Updates "Today's Orders" count based on Firebase orders
  - Updates "Today's Earnings" based on completed orders

### 5. AvailableOrdersFragment (Updated)
- **Location**: `app/src/main/java/com/driver/resturantandroid/ui/orders/AvailableOrdersFragment.kt`
- **Changes**:
  - Now uses Firebase real-time listener instead of REST API polling
  - Calls `ordersViewModel.startListeningToFirebaseOrders(token)` to get real-time updates
  - Orders appear immediately when created (including `pending` status)

### 6. OrdersViewModel (Updated)
- **Location**: `app/src/main/java/com/driver/resturantandroid/viewmodel/OrdersViewModel.kt`
- **Changes**:
  - Added `startListeningToFirebaseOrders(token)` method to listen to Firebase
  - Falls back to REST API if Firebase fails
  - `loadAvailableOrders()` now redirects to Firebase listener

## ⚠️ IMPORTANT: Firebase Rules Update Required

The current Firebase Realtime Database rules deny all read access. **You must update the rules** to allow the Driver app to read orders.

### Current Rules (from `backend/FIREBASE_REALTIME_DATABASE_RULES.md`):
```json
{
  "rules": {
    ".read": "false",
    ".write": "auth != null && auth.uid == 'ERr61aQKyOSMqjbkl8SFy5EpBxD2'"
  }
}
```

### Updated Rules (to allow Driver app to read):
Go to Firebase Console → Realtime Database → Rules and update to:

```json
{
  "rules": {
    "orders": {
      ".read": true,
      ".write": "auth != null && auth.uid == 'ERr61aQKyOSMqjbkl8SFy5EpBxD2'"
    },
    ".read": false,
    ".write": false
  }
}
```

If you use time-based rules, use **valid JSON only** (no `//` comments). Example:
```json
{
  "rules": {
    ".read": "now < 1772571600000",
    ".write": "now < 1772571600000"
  }
}
```

This allows:
- ✅ **Read access** to `orders/` path for all clients (Driver app can read)
- ✅ **Write access** only for backend UID `ERr61aQKyOSMqjbkl8SFy5EpBxD2` (backend can write)
- ❌ **No read/write** access to other paths

## Firebase Database URL

The Firebase Realtime Database URL should be configured in:
1. Firebase Console → Project Settings → General → Realtime Database URL
2. Or set `FIREBASE_DATABASE_URL` in backend `.env` (for backend writes)

The Android SDK will automatically use the database URL from `google-services.json` or Firebase project settings.

## How It Works

1. **Backend writes orders to Firebase**: When an order is created/updated, backend saves it to `orders/{website_id}/{order_number}` (see `backend/services/firebaseOrderSync.js`)

2. **Driver app listens in real-time**: `FirebaseOrderService.listenToOrders()` sets up a `ValueEventListener` on `orders/` path

3. **Automatic updates**: When Firebase data changes, the listener fires and updates `HomeViewModel.availableOrders`

4. **UI updates**: `HomeFragment` observes the LiveData and updates stats (today's orders count, earnings)

5. **Write operations**: All order acceptance/rejection/status updates go through REST API → MySQL (not Firebase)

## Diagnostic: REST test

If the Firebase SDK listener never fires, the app runs a **REST test**: it does a plain HTTP GET to `{FIREBASE_DATABASE_URL}/orders.json`. Check logcat for `🌐 [REST TEST]`:

- **HTTP 200** and body with data → Rules and network are OK; the issue is likely the Firebase Android SDK (e.g. connection with custom URL).
- **HTTP 401 or 403** → Rules are denying read; update rules so `.read` is allowed for the path you read.
- **"Unable to resolve host"** → **DNS/network**: The device or emulator cannot resolve the Firebase hostname. See **Device cannot resolve Firebase host** below.
- **Timeout / connection error** → Network or URL problem (e.g. device can’t reach the Realtime Database host).

### Device cannot resolve Firebase host

If you see `Unable to resolve host "...firebasedatabase.app": No address associated with hostname`, the app’s device/emulator has no working DNS to reach Firebase. The backend (on your Mac) can reach Firebase; the Android runtime cannot.

**Fix:**

1. **Physical device** – Run the Driver app on a **phone/tablet** with **WiFi or mobile data**. Often real-time Firebase works there.
2. **Emulator** – Give the emulator working DNS, then cold boot:
   - `adb shell setprop net.dns1 8.8.8.8`
   - `adb shell setprop net.dns2 8.8.4.4`
   - Restart the app (or cold boot the AVD). Or create a new AVD with a recent system image and ensure it has internet (e.g. open Chrome in emulator and load google.com).
3. **Network** – Ensure the device/emulator has internet and is not behind a firewall that blocks `*.firebasedatabase.app`.

Until the device can resolve and reach Firebase, the app still loads available orders via the **REST API** (pull on open/refresh); only real-time updates will be missing.

## Testing

1. Ensure Firebase rules are updated (see above)
2. Place a delivery order from the customer app
3. Backend should write it to Firebase
4. Driver app Home activity should automatically show the new order count
5. Check logs: `HomeViewModel` and `FirebaseOrderService` log order updates; `🌐 [REST TEST]` for REST diagnostic

## Order Statuses

Orders are filtered to show only delivery orders with these statuses:
- `pending` - Newly created orders (appear immediately)
- `confirmed` - Orders confirmed by restaurant
- `preparing` - Orders being prepared
- `ready` - Orders ready for pickup

**Note**: Orders with status `picked_up`, `completed`, or `cancelled` are excluded from available orders.

## Notes

- **Read-only**: Driver app only reads from Firebase, never writes
- **Filtering**: Only delivery orders with status `pending`, `confirmed`, `preparing`, `ready` are shown
- **Real-time**: Updates happen automatically without polling
- **Performance**: Firebase listeners are efficient and only sync changed data
- **Backend Sync**: Backend currently only writes orders to Firebase when they are created. Order updates (acceptance, status changes) are not synced to Firebase yet. This means:
  - New orders appear immediately ✅
  - When a driver accepts an order, Firebase won't update automatically (backend needs to sync)
  - The REST API still handles order acceptance correctly (prevents double-acceptance)
