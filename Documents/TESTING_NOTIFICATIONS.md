# Manual Testing Guide for Push Notifications

## Prerequisites

1. **Firebase Setup Complete**
   - ✅ `google-services.json` is in `ResturantAndroid/app/` directory
   - ✅ Firebase project created
   - ✅ Android app registered in Firebase Console

2. **Backend Setup**
   - Install Firebase Admin SDK: `npm install firebase-admin`
   - Get Firebase Service Account Key from Firebase Console
   - Set `FIREBASE_SERVICE_ACCOUNT` environment variable

## Step-by-Step Testing Instructions

### Step 1: Install Firebase Admin SDK (if not already installed)

```bash
cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/backend
npm install firebase-admin
```

### Step 2: Get Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click the gear icon ⚙️ → **Project Settings**
4. Go to **Service Accounts** tab
5. Click **Generate New Private Key**
6. Download the JSON file
7. Copy the entire JSON content
8. Set it as an environment variable in your `.env` file:

```bash
# In backend/.env
FIREBASE_SERVICE_ACCOUNT='{"type":"service_account","project_id":"your-project-id",...}'
```

**OR** you can set it directly when running the server:

```bash
FIREBASE_SERVICE_ACCOUNT='{"type":"service_account",...}' npm start
```

### Step 3: Start the Backend Server

```bash
cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/backend
npm start
```

### Step 4: Build and Run the Android App

1. Open Android Studio
2. Build and run the app on a physical device or emulator
3. **Important**: Use a physical device for best results (emulators may have issues with FCM)

### Step 5: Register/Login in the App

1. Open the Android app
2. Register a new account or login with an existing account
3. The app will automatically:
   - Get the FCM device token
   - Send it to the backend
   - Store it in the database

### Step 6: Get Customer ID and Verify Token is Stored

**Option A: Check Database Directly**
```sql
SELECT id, name, email, device_token, last_token_updated 
FROM customers 
WHERE device_token IS NOT NULL 
ORDER BY last_token_updated DESC;
```

**Option B: Use the Test API Endpoint**
```bash
# Get list of customers with device tokens
curl http://localhost:3000/api/test-notification/customers
```

This will show you:
- Customer IDs
- Names
- Whether they have a device token
- Token preview (first 20 characters)

### Step 7: Send a Test Notification

**Using cURL:**
```bash
curl -X POST http://localhost:3000/api/test-notification \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "title": "Test Notification",
    "body": "This is a test push notification!"
  }'
```

**Using Postman or any HTTP client:**
- **URL**: `POST http://localhost:3000/api/test-notification`
- **Headers**: `Content-Type: application/json`
- **Body**:
```json
{
  "customerId": 1,
  "title": "Test Notification",
  "body": "This is a test push notification!"
}
```

Replace `customerId` with the actual customer ID from Step 6.

### Step 8: Verify Notification Received

1. Check the Android device/emulator
2. You should see a notification appear in the notification tray
3. The notification should show:
   - Title: "Test Notification"
   - Body: "This is a test push notification!"

### Step 9: Test Order Status Notification

1. Place an order through the app
2. Update the order status in the admin dashboard
3. The customer should receive a push notification automatically

## Troubleshooting

### Issue: "Firebase service account not configured"
**Solution**: Make sure `FIREBASE_SERVICE_ACCOUNT` environment variable is set correctly in your `.env` file or when starting the server.

### Issue: "Customer does not have a device token"
**Solution**: 
- Make sure the app is running and user is logged in
- Check Android Studio Logcat for FCM token logs
- Verify the app has internet connection
- Check that `google-services.json` is in the correct location

### Issue: "Firebase not initialized"
**Solution**:
- Verify Firebase Admin SDK is installed: `npm list firebase-admin`
- Check that the service account JSON is valid
- Check backend logs for Firebase initialization errors

### Issue: Notification not appearing
**Solution**:
- Make sure the app is in the foreground or background (not force-stopped)
- Check notification permissions on the device
- Verify the device token is correct in the database
- Check backend logs for FCM sending errors
- Try sending from Firebase Console directly to verify the token works

### Check Device Token in Logcat

In Android Studio, filter Logcat by "DeviceTokenHelper" or "FCMService" to see:
- When the token is obtained
- When it's sent to the backend
- Any errors

## Testing with Firebase Console (Alternative Method)

1. Go to Firebase Console → Cloud Messaging
2. Click "Send test message"
3. Enter the device token (from database or Logcat)
4. Enter title and message
5. Click "Test"

This helps verify if the issue is with the backend or the device token itself.

## Expected Behavior

✅ **Success**: Notification appears on device, backend returns `{"success": true}`
❌ **Failure**: Check backend logs and verify Firebase configuration

