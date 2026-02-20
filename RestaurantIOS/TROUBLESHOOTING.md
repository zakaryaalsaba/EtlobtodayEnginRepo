# Troubleshooting Guide

## "Failed to load restaurants" Error

### Common Causes:

1. **Wrong API URL (Most Common)**
   - **iOS Simulator**: Use `http://localhost:3000/api`
   - **Physical Device**: Use `http://YOUR_IP_ADDRESS:3000/api`
     - Find your IP: `ifconfig | grep "inet " | grep -v 127.0.0.1`
     - Example: `http://192.168.1.100:3000/api`
   - Update in: `RestaurantIOS/Utils/APIConfig.swift`

2. **Backend Not Running**
   - Make sure the backend is running:
     ```bash
     cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/backend
     npm start
     ```
   - Check if it's accessible: Open `http://localhost:3000/api/websites` in a browser

3. **Network Security (iOS)**
   - iOS blocks HTTP (non-HTTPS) connections by default
   - For development, you need to allow HTTP in Info.plist
   - Add this to `Info.plist`:
     ```xml
     <key>NSAppTransportSecurity</key>
     <dict>
         <key>NSAllowsArbitraryLoads</key>
         <true/>
     </dict>
     ```

4. **CORS Issues**
   - The backend should have CORS enabled (it does in server.js)
   - If still having issues, check backend logs

### Debugging Steps:

1. **Check Console Logs**
   - Open Xcode Console (View → Debug Area → Activate Console)
   - Look for logs starting with 🌐, 📡, ✅, or ❌
   - These will show the exact error

2. **Test Backend Directly**
   - Open browser: `http://localhost:3000/api/websites`
   - Should return JSON with restaurants

3. **Check Network Connection**
   - If on physical device, ensure device and computer are on same WiFi network
   - Try pinging the server IP from device

4. **Verify API URL**
   - Check `APIConfig.swift` has the correct URL
   - For physical device, must use IP address, not localhost

### Quick Fix Checklist:

- [ ] Backend is running (`npm start` in backend folder)
- [ ] API URL in `APIConfig.swift` is correct
- [ ] If on physical device, using IP address (not localhost)
- [ ] Device and computer on same network
- [ ] Info.plist has NSAppTransportSecurity configured
- [ ] Check Xcode console for detailed error messages

