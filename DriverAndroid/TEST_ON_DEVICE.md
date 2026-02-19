# Test the Driver app on a real Android device

On a **physical device**, `10.0.2.2` (emulator’s host) does **not** work. The phone must reach your backend using your computer’s **LAN IP** on the same Wi‑Fi.

## Quick fix: set your computer’s IP

1. **Find your computer’s IP** (same Wi‑Fi the phone will use):
   - **Mac:** `ifconfig | grep "inet "` or System Settings → Network → Wi‑Fi → Details. Use something like `192.168.1.107`.
   - **Windows:** `ipconfig` → IPv4 Address of your Wi‑Fi adapter.

2. **Add to `DriverAndroid/local.properties`** (create the line if it doesn’t exist):
   ```properties
   API_BASE_URL=http://YOUR_IP:3000/api/
   ```
   Example:
   ```properties
   API_BASE_URL=http://192.168.1.107:3000/api/
   ```

3. **Run the backend** on that machine (e.g. `npm start` in the backend folder), then **rebuild and run** the Driver app on the device.

The app reads `API_BASE_URL` from `local.properties` first (same as ResturantAndroid). If you don’t set it, the app uses **DeviceHelper**: emulator → `10.0.2.2`, physical device → `192.168.1.107`. If your LAN IP is different, either set `API_BASE_URL` in `local.properties` or change `DEFAULT_PHYSICAL_DEVICE_IP` in `util/DeviceHelper.kt`.

## If you still see "Failed to connect"

- Phone and computer must be on the **same Wi‑Fi**.
- Backend must be listening on `0.0.0.0` or your LAN IP (not only `localhost`).
- Firewall must allow incoming TCP on port 3000 from the LAN.
