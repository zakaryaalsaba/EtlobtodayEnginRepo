# Quick Firebase Setup - Fix Push Notifications

## The Problem
Your backend logs show:
```
Firebase service account not configured. Push notifications will be disabled.
Push notifications sent to 0 driver(s) for delivery order...
```

## Quick Fix (5 minutes)

### Step 1: Get Firebase Service Account Key

1. Go to: https://console.firebase.google.com/
2. Select project: **tashkeela-8cab1**
3. Click **⚙️ Settings** → **Project settings**
4. Go to **"Service accounts"** tab
5. Click **"Generate new private key"**
6. Click **"Generate key"** - a JSON file downloads

### Step 2: Create `.env` File in Backend Directory

Create a file: `/Users/zakaryaalsaba/Desktop/RestaurantEngin/backend/.env`

Add this line (replace with your actual JSON content):
```bash
FIREBASE_SERVICE_ACCOUNT={"type":"service_account","project_id":"tashkeela-8cab1","private_key_id":"YOUR_KEY_ID","private_key":"-----BEGIN PRIVATE KEY-----\nYOUR_PRIVATE_KEY\n-----END PRIVATE KEY-----\n","client_email":"firebase-adminsdk-xxxxx@tashkeela-8cab1.iam.gserviceaccount.com","client_id":"YOUR_CLIENT_ID","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-xxxxx%40tashkeela-8cab1.iam.gserviceaccount.com"}
```

**Important:** 
- Copy the ENTIRE JSON from the downloaded file
- Put it all on ONE line
- Make sure all quotes are properly escaped
- Or use single quotes around the entire JSON string

### Step 3: Restart Backend

```bash
cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/backend
npm start
```

You should see:
```
Firebase Admin SDK initialized successfully
```

### Step 4: Test

1. Create a delivery order
2. Mark it as "confirmed"
3. Check logs - should see: `Push notifications sent to 1 driver(s)`
4. Check emulator - notification should appear!

## Alternative: Use File Instead of Environment Variable

If the JSON is too complex for environment variable, you can modify the code to read from a file. Let me know if you want this option.

