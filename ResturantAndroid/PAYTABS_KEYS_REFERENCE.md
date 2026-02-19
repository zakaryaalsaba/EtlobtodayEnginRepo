# PayTabs keys for this app (reference)

Use the **Test SDK Key** row (Type: **Mobile**), not the Test API Key (Standard).

From your dashboard (Profile ID 169360):

| Key         | Value (Test SDK Key – Mobile) |
|------------|-------------------------------|
| Profile ID | `169360`                      |
| Server Key| `SKJ9DJTWB2-JM26GTZHB6-2BMG9GZZKN` |
| Client Key| `C6K2B9-67GM6N-MGT79H-M66QM2` |

## local.properties

In **project root** `local.properties` (e.g. `RestaurantEngin/local.properties` — the app’s build.gradle.kts reads from `rootProject.file("local.properties")`), set:

```properties
PAYTABS_PROFILE_ID=169360
PAYTABS_SERVER_KEY=SKJ9DJTWB2-JM26GTZHB6-2BMG9GZZKN
PAYTABS_CLIENT_KEY=C6K2B9-67GM6N-MGT79H-M66QM2
```

- Do **not** use the **Test API Key** (Standard) row for the SDK; that’s for server/API integration.
- Use the **Test SDK Key** (Mobile) row so the app uses the correct Server Key and Client Key.

After changing, sync Gradle and rebuild. Your log should show `serverKeyPrefix=SKJ9DJT***` (SDK key). If you still get a network error, test on a **real device** or **mobile data**; PayTabs sandbox often blocks emulator IPs.
