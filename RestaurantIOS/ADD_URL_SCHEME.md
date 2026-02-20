# 🔗 Add URL Scheme for Firebase Phone Authentication

## ❌ Error
```
Fatal error: Unexpectedly found nil while implicitly unwrapping an Optional value
Please register custom URL scheme in the app's Info.plist file.
```

## 🔍 Root Cause
Firebase Phone Authentication requires a custom URL scheme to handle reCAPTCHA verification. This must be added to your app's Info.plist.

## ✅ Solution: Add URL Scheme in Xcode

### **STEP 1: Open Xcode Project**
1. Open `RestaurantIOS.xcodeproj` in Xcode

### **STEP 2: Select Target**
1. In the **left sidebar**, click the **blue icon** at the top: **"RestaurantIOS"** (the project)
2. In the **main editor**, under **TARGETS**, select **"RestaurantIOS"** (the app target)

### **STEP 3: Open Info Tab**
1. Click the **"Info"** tab at the top
2. You should see a section called **"URL Types"** or **"Custom iOS Target Properties"**

### **STEP 4: Add URL Type**
1. Click the **"+"** button to add a new URL Type
2. Expand the new URL Type entry
3. Set the following:
   - **Identifier**: `com.tashkeela.restaurant.RestaurantIOS` (or any unique identifier)
   - **URL Schemes**: Click the **"+"** button and add:
     ```
     com.tashkeela.restaurant.RestaurantIOS
     ```
   - **Role**: `Editor` (default)

### **STEP 5: Alternative Method (If Info Tab Doesn't Show URL Types)**
If you don't see URL Types in the Info tab:

1. Right-click in the **left sidebar** on the `RestaurantIOS` folder (blue icon)
2. Select **"New File..."**
3. Choose **"Property List"**
4. Name it: `Info.plist`
5. Make sure it's added to the **RestaurantIOS** target
6. Add the following keys:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.tashkeela.restaurant.RestaurantIOS</string>
        </array>
    </dict>
</array>
```

### **STEP 6: Verify Bundle Identifier**
1. Go to **"General"** tab
2. Check that **Bundle Identifier** is: `com.tashkeela.restaurant.RestaurantIOS`
3. This must match the URL scheme you just added

### **STEP 7: Clean and Rebuild**
1. Press **Shift + Cmd + K** (Clean Build Folder)
2. Press **Cmd + B** (Build)
3. Run the app again

## 🎯 Visual Guide

```
Xcode Info Tab:
┌─────────────────────────────────────┐
│ URL Types                           │
│   ┌─────────────────────────────┐  │
│   │ Identifier:                  │  │
│   │   com.tashkeela.restaurant...│  │
│   │                              │  │
│   │ URL Schemes:                 │  │
│   │   ┌───────────────────────┐  │  │
│   │   │ + com.tashkeela...    │  │  │
│   │   └───────────────────────┘  │  │
│   │                              │  │
│   │ Role: Editor                 │  │
│   └─────────────────────────────┘  │
└─────────────────────────────────────┘
```

## 📝 What This Does

The URL scheme allows Firebase to:
- Open your app after reCAPTCHA verification
- Handle the phone authentication callback
- Complete the verification flow

The URL scheme should match your **Bundle Identifier** exactly.

## ⚠️ Important Notes

1. **Bundle ID must match**: The URL scheme should be the same as your Bundle Identifier
2. **Case sensitive**: Make sure the case matches exactly
3. **No spaces**: URL schemes cannot contain spaces
4. **One URL scheme**: You only need one URL scheme entry

## 🆘 Still Not Working?

If you still get the error:

1. **Check Bundle Identifier**:
   - General tab → Bundle Identifier
   - Should be: `com.tashkeela.restaurant.RestaurantIOS`

2. **Verify URL Scheme**:
   - Info tab → URL Types → URL Schemes
   - Should contain: `com.tashkeela.restaurant.RestaurantIOS`

3. **Clean Build**:
   - Delete Derived Data
   - Clean Build Folder (Shift + Cmd + K)
   - Rebuild (Cmd + B)

4. **Check Info.plist file** (if using separate file):
   - Make sure it's added to the target
   - Check Target Membership in File Inspector

