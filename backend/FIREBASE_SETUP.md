# Firebase Admin SDK Setup Guide

## Problem
The backend logs show:
```
Firebase service account not configured. Push notifications will be disabled.
Push notifications sent to 0 driver(s) for delivery order...
```

This means the backend cannot send push notifications to drivers because Firebase Admin SDK is not configured.

## Solution: Get Firebase Service Account Key

### Step 1: Go to Firebase Console
1. Visit: https://console.firebase.google.com/
2. Select your project: `tashkeela-8cab1`

### Step 2: Generate Service Account Key
1. Click the **gear icon** (⚙️) next to "Project Overview"
2. Select **"Project settings"**
3. Go to the **"Service accounts"** tab
4. Click **"Generate new private key"**
5. Click **"Generate key"** in the confirmation dialog
6. A JSON file will be downloaded (e.g., `tashkeela-8cab1-firebase-adminsdk-xxxxx.json`)

### Step 3: Set Environment Variable

You have two options:

#### Option A: Set as Environment Variable (Recommended for Production)

1. Open the downloaded JSON file
2. Copy the entire JSON content
3. Set it as an environment variable:

**On macOS/Linux:**
```bash
export FIREBASE_SERVICE_ACCOUNT='{"type":"service_account","project_id":"tashkeela-8cab1",...}'
```

**Or add to your `.env` file:**
```bash
# In backend/.env file
FIREBASE_SERVICE_ACCOUNT='{"type":"service_account","project_id":"tashkeela-8cab1","private_key_id":"...","private_key":"-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n","client_email":"firebase-adminsdk-xxxxx@tashkeela-8cab1.iam.gserviceaccount.com","client_id":"...","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":"..."}'
```

**Important:** The JSON must be on a single line or properly escaped.

#### Option B: Use JSON File Path (Alternative)

If you prefer to use a file path instead, you can modify `pushNotificationService.js` to read from a file:

```javascript
const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || './firebase-service-account.json';
const serviceAccount = require(serviceAccountPath);
```

### Step 4: Restart Backend Server

After setting the environment variable:
```bash
cd backend
npm start
```

You should see:
```
Firebase Admin SDK initialized successfully
```

Instead of:
```
Firebase service account not configured. Push notifications will be disabled.
```

## Testing

1. Create a delivery order
2. Mark it as "confirmed"
3. Check backend logs for:
   ```
   Push notifications sent to 1 driver(s) for delivery order ORD-XXX
   ```
4. Check emulator - you should receive a notification

## Security Notes

⚠️ **IMPORTANT:**
- Never commit the service account JSON file to Git
- Add `firebase-service-account.json` to `.gitignore`
- Use environment variables in production
- The service account has admin access to your Firebase project

## Troubleshooting

### Error: "Error parsing Firebase service account"
- Make sure the JSON is valid
- If using environment variable, ensure it's properly escaped
- Check that all quotes are escaped correctly

### Error: "Firebase not initialized"
- Verify `FIREBASE_SERVICE_ACCOUNT` environment variable is set
- Restart the backend server after setting the variable
- Check that the JSON content is correct

### Still not working?
1. Check backend logs for Firebase initialization message
2. Verify the service account JSON is valid
3. Make sure the project ID matches (`tashkeela-8cab1`)

