# Firebase Setup Instructions for RestaurantIOS

## ❌ Current Error
```
No such module 'FirebaseAuth'
```

This means Firebase SDK needs to be added to your Xcode project.

## ✅ Solution: Add Firebase via Swift Package Manager

### Step 1: Open Xcode Project
1. Open `RestaurantIOS.xcodeproj` in Xcode

### Step 2: Add Firebase Package
1. In Xcode, go to **File → Add Package Dependencies...**
2. In the search bar, enter: `https://github.com/firebase/firebase-ios-sdk`
3. Click **Add Package**
4. Wait for Xcode to fetch the package

### Step 3: Select Required Products
When the package is fetched, you'll see a list of products. Select:
- ✅ **FirebaseAuth**
- ✅ **FirebaseCore**

Then click **Add Package**

### Step 4: Verify Package Added
1. In Xcode Navigator, you should see:
   - `Package Dependencies` section
   - `firebase-ios-sdk` listed

### Step 5: Clean and Build
1. **Product → Clean Build Folder** (Shift + Cmd + K)
2. **Product → Build** (Cmd + B)

## Alternative: If Swift Package Manager Doesn't Work

### Option A: CocoaPods
1. Install CocoaPods if not installed:
   ```bash
   sudo gem install cocoapods
   ```

2. Navigate to project directory:
   ```bash
   cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/RestaurantIOS
   ```

3. Initialize Podfile:
   ```bash
   pod init
   ```

4. Edit `Podfile` and add:
   ```ruby
   platform :ios, '15.0'
   
   target 'RestaurantIOS' do
     use_frameworks!
     
     pod 'Firebase/Auth'
     pod 'Firebase/Core'
   end
   ```

5. Install pods:
   ```bash
   pod install
   ```

6. **Important**: Open `RestaurantIOS.xcworkspace` (not .xcodeproj) from now on

### Option B: Manual Framework (Not Recommended)
Only use if SPM and CocoaPods both fail. This is complex and not recommended.

## ✅ Verification

After adding Firebase, verify:
1. Build succeeds (Cmd + B)
2. No "No such module" errors
3. `FirebaseApp.configure()` runs without errors

## 📝 Notes

- **GoogleService-Info.plist** should already be in the correct location:
  - `RestaurantIOS/RestaurantIOS/GoogleService-Info.plist`
- Make sure it's added to the target in Xcode
- Firebase will be initialized in `RestaurantIOSApp.swift` on app launch

## 🔍 Troubleshooting

### "Package not found"
- Check internet connection
- Try: **File → Packages → Reset Package Caches**
- Then try adding package again

### "Module not found" after adding
- Clean build folder (Shift + Cmd + K)
- Restart Xcode
- Rebuild project

### Build errors persist
- Check that `GoogleService-Info.plist` is in the project
- Verify it's added to the target's "Copy Bundle Resources"
- Check that Firebase packages are added to the correct target

