# Firebase Setup Guide for Multiple Android Apps

## Overview
You have two Android apps that need Firebase Cloud Messaging (FCM):
1. **Restaurant App** (`com.mnsf.resturantandroid`) - Customer ordering app
2. **Driver App** (`com.driver.resturantandroid`) - Driver delivery app

## Recommended Approach: Same Firebase Project

**Why?** Both apps are part of the same system and share the same backend. Using one Firebase project:
- ✅ Easier to manage
- ✅ Shared backend configuration
- ✅ Single FCM service account
- ✅ Unified analytics (if needed)
- ✅ Lower cost

## Setup Steps

### Option 1: Add Driver App to Existing Firebase Project (Recommended)

1. **Go to Firebase Console**
   - Visit: https://console.firebase.google.com/
   - Select your existing project: `tashkeela-8cab1`

2. **Add Android App**
   - Click "Add app" → Select Android icon
   - **Package name:** `com.driver.resturantandroid`
   - **App nickname:** "Driver App" (optional)
   - **Debug signing certificate SHA-1:** (optional for now)
   - Click "Register app"

3. **Download google-services.json**
   - Download the new `google-services.json` file
   - This file will contain BOTH apps in the `client` array

4. **Place in Driver App**
   - Copy the downloaded `google-services.json` to:
     ```
     DriverAndroid/app/google-services.json
     ```

5. **Verify Configuration**
   - The `google-services.json` should have TWO entries in the `client` array:
     - One for `com.mnsf.resturantandroid` (Restaurant app)
     - One for `com.driver.resturantandroid` (Driver app)

### Option 2: Separate Firebase Projects (Not Recommended)

If you prefer separate projects:
1. Create a new Firebase project for Driver app
2. Download separate `google-services.json` for each app
3. You'll need separate FCM service accounts
4. Backend will need to handle both service accounts

## Current google-services.json Structure

Your current file has:
```json
{
  "client": [
    {
      "package_name": "com.mnsf.resturantandroid"  // Restaurant app
    }
  ]
}
```

After adding Driver app, it will have:
```json
{
  "client": [
    {
      "package_name": "com.mnsf.resturantandroid"  // Restaurant app
    },
    {
      "package_name": "com.driver.resturantandroid"  // Driver app
    }
  ]
}
```

## Important Notes

1. **Each App Needs Its Own google-services.json**
   - Even though they're in the same Firebase project
   - Each app's `google-services.json` contains ALL apps in the project
   - Firebase automatically uses the correct configuration based on package name

2. **Backend Configuration**
   - You can use the SAME Firebase service account for both apps
   - The `FIREBASE_SERVICE_ACCOUNT` environment variable works for both
   - FCM tokens are app-specific (different package names = different tokens)

3. **Package Name Must Match**
   - The `package_name` in `google-services.json` MUST match `applicationId` in `build.gradle.kts`
   - Restaurant app: `com.mnsf.resturantandroid`
   - Driver app: `com.driver.resturantandroid`

## Verification

After setup, verify:

1. **Restaurant App:**
   - `ResturantAndroid/app/google-services.json` exists
   - Contains `com.mnsf.resturantandroid` in client array

2. **Driver App:**
   - `DriverAndroid/app/google-services.json` exists
   - Contains `com.driver.resturantandroid` in client array
   - Can also contain Restaurant app entry (both apps can have both entries)

3. **Build Both Apps:**
   - Both should build without Firebase errors
   - Both should be able to receive FCM tokens

## Troubleshooting

### Error: "No matching client found for package name"
- **Cause:** Package name in `google-services.json` doesn't match `applicationId`
- **Fix:** Ensure `applicationId` in `build.gradle.kts` matches `package_name` in `google-services.json`

### Error: "google-services.json not found"
- **Cause:** File not in correct location
- **Fix:** Place `google-services.json` in `app/` directory (not root)

### Both apps can't receive notifications
- **Cause:** Using wrong `google-services.json` or package mismatch
- **Fix:** Each app needs its own `google-services.json` with correct package name

## Summary

✅ **Use the same Firebase project** for both apps
✅ **Add Driver app** to existing Firebase project
✅ **Download new google-services.json** (contains both apps)
✅ **Place in DriverAndroid/app/** directory
✅ **Keep existing one** in ResturantAndroid/app/ (or update both with the new file)

The new `google-services.json` will work for BOTH apps because it contains entries for both package names. Firebase automatically selects the correct configuration based on the app's package name.

