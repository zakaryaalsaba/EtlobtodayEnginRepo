# Firebase Phone Authentication Setup Guide

## Error: "This operation is not allowed. This may be because the given sign-in provider is disabled"

This error occurs when Phone Authentication is not enabled in your Firebase project. Follow these steps to enable it:

## Step 1: Enable Phone Authentication in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Authentication** in the left sidebar
4. Click on the **Sign-in method** tab
5. Find **Phone** in the list of providers
6. Click on **Phone** to open its settings
7. Toggle **Enable** to ON
8. Click **Save**

## Step 2: Configure Phone Authentication (Optional but Recommended)

### For Development/Testing:
- You can add test phone numbers that will bypass SMS verification
- Click on the **Phone** provider settings
- Scroll to **Phone numbers for testing**
- Click **Add phone number**
- Add test numbers in format: `+1234567890`
- Add corresponding test codes (e.g., `123456`)

### For Production:
- Ensure your app's SHA-1 and SHA-256 certificates are added to Firebase
- Go to **Project Settings** > **Your apps** > Select your Android app
- Add SHA-1 and SHA-256 certificates from your keystore
- This is required for reCAPTCHA verification

## Step 3: Verify Configuration

After enabling Phone Authentication:
1. The error should no longer occur
2. You should be able to receive SMS verification codes
3. Test with a real phone number or use test numbers you configured

## Step 4: Get SHA-1 and SHA-256 Certificates

### For Debug Build:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### For Release Build:
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-alias
```

Copy the SHA-1 and SHA-256 fingerprints and add them to Firebase Console under your Android app settings.

## Troubleshooting

### Still Getting Errors?
1. **Check Firebase Console**: Ensure Phone Authentication is enabled and saved
2. **Wait a few minutes**: Changes may take a few minutes to propagate
3. **Check App Package Name**: Ensure it matches exactly in Firebase Console
4. **Check google-services.json**: Ensure it's the latest version from Firebase Console
5. **Rebuild the app**: Clean and rebuild after making Firebase changes

### Common Issues:
- **"Invalid phone number"**: Ensure phone number includes country code (e.g., +962...)
- **"Quota exceeded"**: You've hit Firebase's free tier SMS limit
- **"reCAPTCHA verification failed"**: Add SHA certificates to Firebase Console

## Additional Resources

- [Firebase Phone Auth Documentation](https://firebase.google.com/docs/auth/android/phone-auth)
- [Firebase Console](https://console.firebase.google.com/)
- [Firebase Pricing](https://firebase.google.com/pricing) - Check SMS quota limits

