# Notification Setup Guide for Android Emulator

## Issue: Notifications Not Displaying in Emulator

### Common Causes

1. **Notification Permission Not Granted** (Android 13+)
   - Runtime permission required for Android 13 (API 33+)
   - Emulator may have notifications disabled by default

2. **Emulator Settings**
   - Notifications may be disabled in emulator settings
   - Do Not Disturb mode may be enabled

3. **Notification Channel Not Created Early Enough**
   - Channel should be created on app startup, not just when notification arrives

4. **Google Play Services Not Available**
   - Some emulators don't have Google Play Services
   - FCM requires Google Play Services

## Solutions

### Solution 1: Request Notification Permission (Android 13+)

The app needs to request `POST_NOTIFICATIONS` permission at runtime for Android 13+.

**Check Android Version:**
- Android 13 (API 33) and above: Runtime permission required
- Android 12 and below: Permission granted automatically

### Solution 2: Emulator Settings

1. **Enable Notifications in Emulator:**
   - Open Settings → Apps → [Your App Name]
   - Tap "Notifications"
   - Enable "Show notifications"
   - Enable "Order Notifications" channel

2. **Check Do Not Disturb:**
   - Settings → Sound → Do Not Disturb
   - Make sure it's OFF

3. **Check Notification Access:**
   - Settings → Apps → Special app access → Notification access
   - Ensure your app has access

### Solution 3: Use Emulator with Google Play Services

**Recommended Emulators:**
- ✅ **Pixel 5 with Google Play** (Recommended)
- ✅ **Pixel 6 with Google Play**
- ❌ **Pixel 5 without Google Play** (Won't work for FCM)

**To Create Emulator with Google Play:**
1. Android Studio → Tools → Device Manager
2. Create Device → Select "Pixel 5" or similar
3. **IMPORTANT:** Select a system image with "Google Play" icon
4. Finish setup

### Solution 4: Test Notification Manually

You can test if notifications work by sending a test notification from Firebase Console:

1. Go to Firebase Console → Cloud Messaging
2. Click "Send test message"
3. Enter your FCM token (from logcat: `FCM Token retrieved: <token>`)
4. Send notification
5. Check if it appears in emulator

### Solution 5: Check Logcat for Errors

Filter logcat by:
```
adb logcat | grep -E "DriverFCMService|FCMTokenManager|Notification"
```

**Expected logs:**
```
D/DriverFCMService: From: <sender-id>
D/DriverFCMService: Message Notification Body: <message>
D/DriverFCMService: Notification sent
```

**If you see errors:**
- "Permission denied" → Request notification permission
- "Channel not found" → Create notification channel on app start
- "Google Play Services not available" → Use emulator with Google Play

## Quick Fix Checklist

- [ ] App requests notification permission (Android 13+)
- [ ] Notification channel created on app startup
- [ ] Emulator has Google Play Services
- [ ] Notifications enabled in emulator settings
- [ ] Do Not Disturb is OFF
- [ ] FCM token is registered in database
- [ ] Backend is sending notifications correctly

## Testing Steps

1. **Check Permission:**
   ```bash
   adb shell dumpsys package com.driver.resturantandroid | grep permission
   ```

2. **Check Notification Channel:**
   ```bash
   adb shell dumpsys notification | grep -A 10 "Order Notifications"
   ```

3. **Send Test Notification:**
   - Use Firebase Console
   - Or use backend API to create a delivery order

4. **Check Logcat:**
   ```bash
   adb logcat | grep DriverFCMService
   ```

## Physical Device vs Emulator

**Physical Device:**
- ✅ Usually works out of the box
- ✅ Google Play Services always available
- ✅ Better for testing real-world scenarios

**Emulator:**
- ⚠️ Must have Google Play Services
- ⚠️ May need manual permission grant
- ⚠️ Settings may need adjustment

## Next Steps

1. Add runtime permission request to MainActivity
2. Create notification channel on app startup
3. Test with emulator that has Google Play Services
4. Verify backend is sending notifications correctly

