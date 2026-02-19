# Firebase Setup for Order Manage Android

Push notifications require a valid Firebase project. The app ships with a **placeholder** `google-services.json`; you must replace it with your real config.

## Fix "Please set a valid API key" / "Firebase not configured"

1. **Create or use a Firebase project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or select an existing one

2. **Add an Android app**
   - In the project, click "Add app" → Android
   - **Package name:** `com.order.resturantandroid` (must match exactly)
   - Register the app (you can skip optional steps for now)

3. **Download `google-services.json`**
   - Firebase will offer to download `google-services.json`
   - Download it and **replace** the file at:
     ```
     OrderManageAndroid/app/google-services.json
     ```
   - The file must contain real values (not "placeholder-project" or "PLACEHOLDER_KEY_REPLACE_WITH_REAL_FIREBASE_CONFIG")

4. **Enable Cloud Messaging**
   - In Firebase Console → Project Settings (gear) → Cloud Messaging
   - Enable Cloud Messaging if needed
   - The **Server key** (or use Firebase Admin SDK) is needed for the **backend** to send notifications to devices

5. **Enable Cloud Messaging API** (fixes `SERVICE_NOT_AVAILABLE`)
   - Open [Google Cloud Console](https://console.cloud.google.com/)
   - Select the same project as Firebase (e.g. `tashkeela-8cab1`)
   - Go to **APIs & Services** → **Library**
   - Search for **Firebase Cloud Messaging API** (or **Cloud Messaging API**)
   - Open it and click **Enable**
   - Wait a minute, then try "Enable Notifications" again in the app

6. **Use a device or emulator with Google Play**
   - On an **emulator**: use a system image that includes **Google Play** (e.g. "Pixel 5 API 33" with Play Store). Images without Play often return `SERVICE_NOT_AVAILABLE`.
   - On a **physical device**: ensure Google Play Services is up to date.

7. **Rebuild the app**
   - Sync Gradle and rebuild
   - Enable Notifications again from Profile → Order Notifications

## If you see SERVICE_NOT_AVAILABLE

- Enable **Firebase Cloud Messaging API** in Google Cloud Console (step 5 above).
- Use an emulator image **with Google Play** or a real device.
- The app will retry once after 3 seconds; if it still fails, try again later or check network/firewall (ports 5228, 5229, 5230, 443 and mtalk.google.com).

## Backend (optional, for sending notifications)

To send push notifications from the server, the backend needs Firebase Admin credentials:

- Place `firebase-service-account.json` in the backend folder, or
- Set env var `FIREBASE_SERVICE_ACCOUNT` or `FIREBASE_SERVICE_ACCOUNT_PATH`

See backend docs (e.g. `backend/FIREBASE_SETUP.md`) for details.
