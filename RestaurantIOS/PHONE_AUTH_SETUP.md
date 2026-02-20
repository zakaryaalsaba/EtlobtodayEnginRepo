# Phone Authentication Setup Guide for RestaurantIOS

## ✅ Implementation Complete

Phone authentication with Firebase has been implemented for the iOS app. Here's what was added:

## 📁 Files Created/Modified

### New Files:
1. **`Services/LocationHelper.swift`** - Location services helper
2. **`Views/Auth/PhoneAuthView.swift`** - Phone authentication UI

### Modified Files:
1. **`RestaurantIOSApp.swift`** - Added Firebase initialization
2. **`ViewModels/AuthViewModel.swift`** - Added `loginWithPhone()` method
3. **`Services/APIService.swift`** - Added `loginWithPhone()` endpoint
4. **`Models/Customer.swift`** - Added `PhoneLoginRequest` struct
5. **`Views/Auth/LoginView.swift`** - Added phone auth button
6. **`Utils/LocalizedStrings.swift`** - Added phone auth strings

## 🔧 Required Setup Steps

### 1. Add Firebase SDK to Xcode Project

**Option A: Swift Package Manager (Recommended)**
1. Open `RestaurantIOS.xcodeproj` in Xcode
2. Go to **File → Add Package Dependencies**
3. Add: `https://github.com/firebase/firebase-ios-sdk`
4. Select these packages:
   - `FirebaseAuth`
   - `FirebaseCore`

**Option B: CocoaPods**
If using CocoaPods, add to `Podfile`:
```ruby
pod 'Firebase/Auth'
pod 'Firebase/Core'
```

### 2. Verify GoogleService-Info.plist Location

The file should be at:
```
RestaurantIOS/RestaurantIOS/GoogleService-Info.plist
```

**In Xcode:**
1. Right-click the file → **Add Files to "RestaurantIOS"**
2. Ensure it's added to the target
3. Verify it's in the Copy Bundle Resources build phase

### 3. Add Location Permissions to Info.plist

Add these keys to your `Info.plist`:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need your location to provide accurate delivery addresses and improve your experience.</string>

<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>We need your location to provide accurate delivery addresses and improve your experience.</string>
```

**In Xcode:**
1. Select your project in the navigator
2. Select the **RestaurantIOS** target
3. Go to **Info** tab
4. Add the keys above with appropriate descriptions

### 4. Enable Phone Authentication in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Authentication → Sign-in method**
4. Enable **Phone** provider
5. Save

### 5. Configure iOS App in Firebase

1. In Firebase Console → **Project Settings → Your apps**
2. Add iOS app (if not already added):
   - Bundle ID: Check your Xcode project settings
   - App nickname: "RestaurantIOS"
3. Download `GoogleService-Info.plist` and replace the existing one
4. Add SHA-1 and SHA-256 certificates (for production)

## 🧪 Testing

### Test Phone Numbers (Firebase Console)
1. Go to **Authentication → Sign-in method → Phone**
2. Scroll to **Phone numbers for testing**
3. Add test numbers (e.g., `+962793837799`) with test codes (e.g., `123456`)

### Test Flow:
1. Open the app
2. Tap "Sign in with Phone" on login screen
3. Enter phone number (e.g., `793837799`)
4. Receive verification code (or use test code)
5. Enter code
6. App should authenticate and navigate to main screen

## 📱 Features Implemented

✅ Phone number input with +962 prefix (Jordan country code)  
✅ OTP verification screen  
✅ Firebase phone authentication integration  
✅ Location permission request (early in flow)  
✅ Location and address capture before login  
✅ Backend authentication with phone, Firebase token, location, and address  
✅ Error handling for all Firebase errors  
✅ Localized strings (English and Arabic)  
✅ RTL/LTR support  

## 🔍 Troubleshooting

### "Firebase not configured" error
- Ensure `GoogleService-Info.plist` is in the project and added to target
- Verify Firebase SDK is properly installed
- Check that `FirebaseApp.configure()` is called in `RestaurantIOSApp.swift`

### "Phone authentication not enabled" error
- Enable Phone Authentication in Firebase Console
- Wait a few minutes for changes to propagate

### Location permission not requested
- Check `Info.plist` has location permission keys
- Verify `LocationHelper.requestLocationPermission()` is called

### "Invalid phone number" error
- Ensure phone number format: `+9627XXXXXXXX` (no spaces, dashes)
- For testing, use test phone numbers from Firebase Console

## 📝 Notes

- The backend endpoint `/api/auth/login/phone` is already implemented and ready
- Location is captured **before** calling the backend, so it's saved immediately during login/registration
- Address is obtained via reverse geocoding from coordinates
- All phone auth strings are localized for English and Arabic

## 🚀 Next Steps

1. Add Firebase SDK to Xcode project
2. Add location permissions to Info.plist
3. Enable Phone Authentication in Firebase Console
4. Test the complete flow
5. Add address display in MainTabView (if needed)

