# Network Security Setup

## Problem
iOS blocks HTTP connections by default. Since we're using `http://localhost:3000` for development, we need to configure the app to allow HTTP connections.

## Solution: Add Network Security Settings in Xcode

### Method 1: Using Xcode UI (Recommended)

1. Open `RestaurantIOS.xcodeproj` in Xcode
2. Select the **project** (blue icon) in the navigator
3. Select the **RestaurantIOS** target
4. Click on the **"Info"** tab
5. Under **"Custom iOS Target Properties"**, click the **"+"** button
6. Type: `App Transport Security Settings` (or just start typing and select it)
7. Expand the new entry
8. Click the **"+"** button inside it
9. Add: `Allow Arbitrary Loads` = `YES` (type `YES` or select from dropdown)

### Method 2: Using Raw Keys

If Method 1 doesn't work, you can add the raw keys:

1. In the "Info" tab, click "+" to add a new key
2. Key: `NSAppTransportSecurity` (type: Dictionary)
3. Expand it and add: `NSAllowsArbitraryLoads` = `YES` (type: Boolean)

### Verification

After adding the setting, you should see in the Info tab:
```
App Transport Security Settings
  └─ Allow Arbitrary Loads: YES
```

Or:
```
NSAppTransportSecurity
  └─ NSAllowsArbitraryLoads: YES
```

## Important Notes

- ⚠️ **For Production**: Remove this setting and use HTTPS only
- This setting allows ALL HTTP connections (not secure for production)
- Only use this for local development

## Alternative: Use HTTPS

If you set up HTTPS for your backend, you won't need this setting. But for local development with `localhost`, HTTP is simpler.

