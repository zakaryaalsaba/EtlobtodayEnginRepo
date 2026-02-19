# Driver Push Notifications Implementation

## Overview
Push notifications have been implemented for the Driver app to notify drivers when new delivery orders are available.

## Implementation Details

### Backend Changes

#### 1. Device Token Endpoint
**File:** `backend/routes/drivers.js`
- **Endpoint:** `PUT /api/drivers/device-token`
- **Purpose:** Allows drivers to register/update their FCM device token
- **Request Body:**
  ```json
  {
    "device_token": "fcm_token_here",
    "device_type": "android"
  }
  ```
- **Authentication:** Requires driver authentication token

#### 2. Order Creation Notification
**File:** `backend/routes/orders.js`
- When a new delivery order is created, the system:
  1. Gets all online drivers with device tokens
  2. Sends push notifications to all online drivers
  3. Cleans up invalid device tokens automatically
- **Notification Content:**
  - Title: "New Delivery Order Available"
  - Body: Order number, total amount, and delivery address
  - Data: Order ID, order number, website ID, total amount

### Android App Changes

#### 1. Firebase Messaging Service
**File:** `DriverAndroid/app/src/main/java/com/driver/resturantandroid/service/DriverFirebaseMessagingService.kt`
- Handles incoming FCM messages
- Creates notifications with proper channel setup
- Handles notification clicks to navigate to Available Orders
- Automatically registers token with backend when token is refreshed

#### 2. FCM Token Manager
**File:** `DriverAndroid/app/src/main/java/com/driver/resturantandroid/util/FCMTokenManager.kt`
- Utility class to manage FCM token registration
- Gets FCM token from Firebase
- Sends token to backend API
- Handles token refresh

#### 3. Login Activity Update
**File:** `DriverAndroid/app/src/main/java/com/driver/resturantandroid/ui/auth/LoginActivity.kt`
- Automatically registers FCM token after successful login
- Ensures token is sent to backend when driver logs in

#### 4. Main Activity Update
**File:** `DriverAndroid/app/src/main/java/com/driver/resturantandroid/MainActivity.kt`
- Registers FCM token on app start (if logged in)
- Handles notification navigation (opens Available Orders when notification is clicked)
- Implements `onNewIntent()` to handle notification clicks when app is already running

#### 5. Available Orders Fragment Update
**File:** `DriverAndroid/app/src/main/java/com/driver/resturantandroid/ui/orders/AvailableOrdersFragment.kt`
- Added `onResume()` to refresh orders when fragment becomes visible
- Ensures orders are refreshed when user returns from notification

#### 6. API Service Update
**File:** `DriverAndroid/app/src/main/java/com/driver/resturantandroid/network/ApiService.kt`
- Added `updateDeviceToken()` endpoint

#### 7. Android Manifest
**File:** `DriverAndroid/app/src/main/AndroidManifest.xml`
- Registered `DriverFirebaseMessagingService` as a service
- Service handles FCM messages in the background

#### 8. Strings Resource
**File:** `DriverAndroid/app/src/main/res/values/strings.xml`
- Added notification channel ID string resource

## How It Works

### Flow Diagram

```
1. Driver Logs In
   ↓
2. FCM Token Retrieved
   ↓
3. Token Sent to Backend (stored in drivers table)
   ↓
4. Driver Goes Online
   ↓
5. Customer Places Delivery Order
   ↓
6. Backend Finds All Online Drivers
   ↓
7. Push Notification Sent to All Online Drivers
   ↓
8. Driver Receives Notification
   ↓
9. Driver Taps Notification
   ↓
10. App Opens to Available Orders Tab
   ↓
11. Orders Automatically Refresh
```

## Notification Behavior

### When Notifications Are Sent
- Only for **delivery orders** (`order_type = 'delivery'`)
- Only to **online drivers** (`is_online = 1`)
- Only to drivers with **valid device tokens**

### Notification Content
- **Title:** "New Delivery Order Available"
- **Body:** Order details (order number, total, address)
- **Data Payload:**
  - `type`: "new_delivery_order"
  - `order_id`: Order ID
  - `order_number`: Order number
  - `website_id`: Restaurant ID
  - `total_amount`: Order total

### Notification Actions
- **Tap Notification:** Opens app and navigates to Available Orders tab
- **Orders Auto-Refresh:** Available Orders automatically refreshes when fragment becomes visible

## Setup Requirements

### Backend
1. **Firebase Service Account:** Set `FIREBASE_SERVICE_ACCOUNT` environment variable
2. **Database:** Ensure `drivers` table has `device_token` and `device_type` columns (already in schema)

### Android App
1. **Firebase Project:** Create Firebase project and add `google-services.json` to `app/` directory
2. **Permissions:** `POST_NOTIFICATIONS` permission is already in manifest
3. **Notification Channel:** Automatically created on first notification

## Testing

### Test Push Notifications
1. Driver logs in (token automatically registered)
2. Driver goes online
3. Create a delivery order from customer app/website
4. Driver should receive push notification
5. Tap notification → app opens to Available Orders
6. Order should appear in the list

### Verify Token Registration
- Check backend logs for "FCM token registered successfully"
- Check `drivers` table: `device_token` should be populated
- Check Android logcat for FCM token logs

## Troubleshooting

### Notifications Not Received
1. **Check Firebase Setup:**
   - Verify `google-services.json` is in `app/` directory
   - Verify Firebase project is configured correctly

2. **Check Device Token:**
   - Verify token is stored in database: `SELECT device_token FROM drivers WHERE id = ?`
   - Check Android logcat for token registration logs

3. **Check Driver Status:**
   - Driver must be online (`is_online = 1`)
   - Only delivery orders trigger notifications

4. **Check Backend Logs:**
   - Look for "Push notifications sent to X driver(s)"
   - Check for Firebase initialization errors

### Token Not Registering
1. **Check Authentication:**
   - Driver must be logged in
   - Token must be valid

2. **Check Network:**
   - Verify backend is accessible
   - Check API endpoint is correct

3. **Check Logs:**
   - Android logcat: Look for "FCMTokenManager" logs
   - Backend logs: Check for device token update errors

## Future Enhancements

1. **Notification Actions:** Add "Accept" and "Reject" buttons in notification
2. **Sound Customization:** Allow custom notification sounds
3. **Notification Grouping:** Group multiple orders in one notification
4. **Priority Orders:** Different notification for high-value orders
5. **Location-Based:** Only notify drivers near the restaurant

