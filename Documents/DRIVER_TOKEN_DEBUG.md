# Driver Device Token Debugging Guide

## Issue: device_token and device_type not appearing in database

### Possible Causes

1. **Database columns don't exist** (most likely)
   - The `drivers` table was created before these columns were added
   - Solution: Restart backend server (migration will run automatically)

2. **Firebase not configured**
   - Missing `google-services.json` in DriverAndroid/app/
   - Solution: Add the Driver app to Firebase project and download google-services.json

3. **FCM token retrieval failing**
   - Firebase not initialized properly
   - Solution: Check Android logcat for errors

4. **API call failing silently**
   - Network error or authentication issue
   - Solution: Check backend logs and Android logcat

## Debugging Steps

### Step 1: Verify Database Columns Exist

Run this SQL query to check:
```sql
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'restaurant_websites' 
AND TABLE_NAME = 'drivers'
AND COLUMN_NAME IN ('device_token', 'device_type');
```

**If columns don't exist:**
- Restart the backend server
- The migration in `backend/db/init.js` will automatically add them
- Check backend logs for: "Added device_token column to drivers table"

### Step 2: Check Android Logcat

Filter by tag: `FCMTokenManager`

**Expected logs:**
```
D/FCMTokenManager: Attempting to get FCM token...
D/FCMTokenManager: FCM Token retrieved: <token>
D/FCMTokenManager: Registering FCM token: <token>
D/FCMTokenManager: FCM token registered successfully
```

**If you see errors:**
- "Firebase not configured" → Add google-services.json
- "No auth token" → Login first
- "Error getting FCM token" → Check Firebase setup

### Step 3: Check Backend Logs

When driver logs in, you should see:
```
Driver <id> updating device token. Columns exist: device_token=true, device_type=true
Device token updated for driver <id>
```

**If you see:**
- "Device token columns not found" → Restart backend (migration will run)
- "Device token feature not available" → Check database migration

### Step 4: Verify Firebase Setup

1. Check if `google-services.json` exists:
   ```
   DriverAndroid/app/google-services.json
   ```

2. Verify package name matches:
   - `google-services.json` should contain: `com.driver.resturantandroid`
   - `build.gradle.kts` should have: `applicationId = "com.driver.resturantandroid"`

3. Rebuild the app after adding google-services.json

### Step 5: Manual Test

1. **Login to Driver app**
2. **Check Android logcat** for FCMTokenManager logs
3. **Check backend logs** for device token update
4. **Query database:**
   ```sql
   SELECT id, name, email, device_token, device_type 
   FROM drivers 
   WHERE email = 'your_driver_email@example.com';
   ```

## Quick Fix

If columns don't exist, restart the backend server. The migration will run automatically:

```bash
cd backend
npm start
```

Look for these log messages:
- "Added device_token column to drivers table"
- "Added device_type column to drivers table"

## Testing Token Registration

After login, check:

1. **Android Logcat:**
   ```
   adb logcat | grep FCMTokenManager
   ```

2. **Backend Logs:**
   ```
   Driver <id> updating device token...
   Device token updated for driver <id>
   ```

3. **Database:**
   ```sql
   SELECT device_token, device_type FROM drivers WHERE id = <driver_id>;
   ```

## Common Issues

### Issue: "No auth token, cannot register FCM token"
**Solution:** Login must complete before token registration. This is expected if called too early.

### Issue: "Firebase not configured"
**Solution:** Add `google-services.json` to `DriverAndroid/app/` directory

### Issue: Columns don't exist
**Solution:** Restart backend server - migration will add them automatically

### Issue: Token retrieved but not saved
**Solution:** Check backend logs for API errors. Verify authentication token is valid.

