# Test the app on a real Android device

## 1. Enable Developer options on the phone

On a real device, **Developer options** are hidden until you enable them:

1. Open **Settings** on the phone.
2. Go to **About phone** (or **About device**).
3. Find **Build number**.
4. Tap **Build number** **7 times**. You’ll see a message like “You are now a developer!”.
5. Go back to **Settings** — you should now see **Developer options** (sometimes under **System** or **Additional settings**).

---

## 2. Enable USB debugging

1. Open **Settings → Developer options**.
2. Turn **Developer options** **ON** (if there’s a switch).
3. Find **USB debugging** and turn it **ON**.
4. (Optional) Turn **OFF** **“Don’t keep activities”** if you’re testing PayTabs — this can stop the app from restarting when the payment screen opens.

---

## 3. Connect the phone to your computer

1. Connect the phone with a **USB cable**.
2. On the phone, when asked **“Allow USB debugging?”**, tap **Allow** (and optionally **Always allow from this computer**).
3. On Mac/Windows, if the device doesn’t appear in Android Studio, you may need to install/update [USB drivers](https://developer.android.com/studio/run/oem-usb) (Windows) or accept the USB debugging prompt again.

---

## 4. Point the app to your backend (same Wi‑Fi)

On a real device, `10.0.2.2` (emulator’s localhost) does **not** work. The device must use your computer’s **IP address** on the same Wi‑Fi.

1. On your **computer**, find your IP address:
   - **Mac:** System Settings → Network → Wi‑Fi → Details, or run `ifconfig | grep "inet "` and use the one like `192.168.x.x`.
   - **Windows:** `ipconfig` and use the **IPv4 Address** of your Wi‑Fi adapter (e.g. `192.168.1.x`).
2. Make sure your **backend** (Node/Express) is running on that machine, e.g. `http://localhost:3000`.
3. In the project’s **`gradle.properties`** (in `ResturantAndroid/`), add:
   ```properties
   API_BASE_URL=http://YOUR_IP:3000/api/
   ```
   Example: if your IP is `192.168.1.100`:
   ```properties
   API_BASE_URL=http://192.168.1.100:3000/api/
   ```
4. **Rebuild** the app (Build → Rebuild Project or run again) so it picks up the new URL.
5. The app reads `API_BASE_URL` from **`local.properties`** first (not only gradle.properties). Use the same format there: `API_BASE_URL=http://YOUR_IP:3000/api/`.

---

## 4b. If you see "Failed to connect"

When the app can’t reach the server, it logs details so you can fix it:

1. **Check Logcat** (Android Studio → Logcat, or run in terminal):
   ```bash
   adb logcat -s ApiConnection AuthRepository
   ```
   You’ll see:
   - **ApiConnection**: The exact `API base URL` the app is using.
   - **AuthRepository** / **ApiConnection**: The exception (e.g. `Connection refused`, `Unknown host`, timeout).

2. **Verify:**
   - **IP**: `API_BASE_URL` in `local.properties` (or gradle.properties) must be your computer’s **current** LAN IP (e.g. `http://192.168.1.100:3000/api/`). Find it with `ifconfig` (Mac) or `ipconfig` (Windows).
   - **Server**: Backend is running on that machine (e.g. `http://localhost:3000`). Server must listen on **all interfaces** (e.g. `0.0.0.0:3000`), not only `127.0.0.1`.
   - **Same Wi‑Fi**: Phone and computer are on the **same** Wi‑Fi network.
   - **Firewall**: Computer firewall allows **incoming** connections on port **3000**.

3. After changing `local.properties`, **rebuild** the app so the new URL is baked into the APK.

---

## 5. Run the app from Android Studio

1. In Android Studio, open the **ResturantAndroid** project.
2. At the top, in the device dropdown, select your **physical device** (e.g. “Pixel 6” or your phone model) instead of an emulator.
3. Click the **Run** (green play) button, or use **Run → Run 'app'**.
4. The app will build and install on the phone and then launch.

---

## 6. (Optional) Install a debug APK without Android Studio

If you want to install the APK on the device without running from Android Studio:

1. Build the APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. The APK path will be shown (e.g. `app/build/outputs/apk/debug/app-debug.apk`). Copy it to the phone (USB, cloud, etc.) or install via USB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Remember: for the app to work, the phone must be on the same Wi‑Fi as your computer and `API_BASE_URL` in `local.properties` must use your computer’s IP (then rebuild before creating the APK).

---

## Quick checklist

- [ ] Developer options enabled (tap Build number 7 times).
- [ ] USB debugging ON.
- [ ] Phone connected via USB and “Allow USB debugging” accepted.
- [ ] `gradle.properties` has `API_BASE_URL=http://YOUR_IP:3000/api/` (your computer’s IP).
- [ ] Backend is running on your computer.
- [ ] Phone and computer on the same Wi‑Fi.
- [ ] In Android Studio, physical device selected and Run pressed.
