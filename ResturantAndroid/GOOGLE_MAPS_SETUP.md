# Google Maps Setup for Confirm Location & New Address

The app uses **Google Maps SDK for Android** for the checkout flow: **Confirm location** (map + “Enter complete address”) and **New address** (address form with map snippet). To get the map working you need an API key and the right APIs enabled in Google Cloud.

---

## 1. Create or use a Google Cloud project

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project or select an existing one.
3. Make sure **billing** is enabled (Maps APIs require a billing account, but there is a free tier).

---

## 2. Enable the required APIs

1. In Cloud Console go to **APIs & Services** → **Library**.
2. Enable:
   - **Maps SDK for Android**  
     Used to show the map on **Confirm location** and **New address**.
   - **Geocoding API** (optional but recommended)  
     Used for reverse geocoding (coordinates → address) and pre-filling area/street on the **New address** screen.  
     If you don’t enable it, the app still works but area/street may stay empty until you add another source (e.g. Places).

---

## 3. Create an API key

1. Go to **APIs & Services** → **Credentials**.
2. Click **Create credentials** → **API key**.
3. Copy the new API key.
4. (Recommended) Restrict the key:
   - **Application restrictions** → **Android apps**.
   - Add your app’s package name: `com.mnsf.resturantandroid`.
   - Add your **SHA-1** fingerprint(s) (debug and release).  
     Get debug SHA-1:  
     `./gradlew signingReport` (or **Gradle** → **app** → **Tasks** → **android** → **signingReport**).
   - **API restrictions** → “Restrict key” and select **Maps SDK for Android** (and **Geocoding API** if you use it).

---

## 4. Add the API key to the app

1. In the **ResturantAndroid** project folder (the same folder that contains `build.gradle.kts`, `app/`, and `settings.gradle.kts`), open or create **`local.properties`**.
2. Add (replace with your real key):

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_ANDROID_API_KEY_HERE
```

3. **Do not** commit `local.properties` to version control (it should be in `.gitignore`). Each developer or CI should have their own key in `local.properties`.

---

## 5. Build and run

1. Sync the project (e.g. “Sync Project with Gradle Files”).
2. Build and run the **ResturantAndroid** app.
3. Add items to the cart and tap **Checkout**:
   - You should see the **Confirm location** map (and location permission if not yet granted).
   - After **Enter complete address**, you should see the **New address** screen with the small map and form.

If the map is blank, check:

- `MAPS_API_KEY` is set in **root** `local.properties`.
- **Maps SDK for Android** is enabled for the project that owns the key.
- For restricted keys: package name and SHA-1 are correct.

---

## Summary

| What you need | Where |
|---------------|--------|
| Google Cloud project with billing | [console.cloud.google.com](https://console.cloud.google.com/) |
| **Maps SDK for Android** enabled | APIs & Services → Library |
| **Geocoding API** enabled (optional) | Same place |
| API key created and (optionally) restricted | APIs & Services → Credentials |
| `MAPS_API_KEY=...` in `local.properties` | ResturantAndroid folder (same level as `app/`) |

The app already has:

- **Play Services Maps** dependency (`play-services-maps`).
- **Location** permission and **Play Services Location** for current location.
- Manifest meta-data reading `MAPS_API_KEY` from Gradle.
- **Confirm location** and **New address** screens using the map and (if available) Geocoding for address pre-fill.
