# Firebase Realtime Database – Security Rules & Backend UID

## Backend as single writer (UID)

Only one identity is allowed to write to the Realtime Database: the **backend UID**  
`ERr61aQKyOSMqjbkl8SFy5EpBxD2`.

- The backend obtains an ID token for this UID (custom token → sign in) and writes via the REST API with `?auth=<idToken>`.
- Rules allow **write** only when `auth.uid == 'ERr61aQKyOSMqjbkl8SFy5EpBxD2'`.
- **Read** is denied for everyone (clients and this UID); only the backend service can write.

## Rules to use in Firebase Console

**Build → Realtime Database → Rules:**

```json
{
  "rules": {
    ".read": "false",
    ".write": "auth != null && auth.uid == 'ERr61aQKyOSMqjbkl8SFy5EpBxD2'"
  }
}
```

- Only the backend (writing as UID `ERr61aQKyOSMqjbkl8SFy5EpBxD2`) can insert/update.
- No client can read or write.

## Backend configuration

The backend must authenticate as that UID when calling the Realtime Database REST API.

1. **FIREBASE_WEB_API_KEY** (required for order sync)
   - Firebase Console → Project settings → General → **Web API Key**.
   - In `backend/.env`:
     ```bash
     FIREBASE_WEB_API_KEY=your_web_api_key_here
     ```

2. **FIREBASE_BACKEND_UID** (optional)
   - Default: `ERr61aQKyOSMqjbkl8SFy5EpBxD2`.
   - Override only if you use a different dedicated backend user:
     ```bash
     FIREBASE_BACKEND_UID=ERr61aQKyOSMqjbkl8SFy5EpBxD2
     ```

3. **FIREBASE_DATABASE_URL** (optional if default US DB)
   - Your Realtime Database URL (e.g. from Realtime Database → project URL).
   - Example: `https://your-project-id-default-rtdb.firebaseio.com`

## Summary

| Who / What              | Allowed        | How                                      |
|-------------------------|----------------|------------------------------------------|
| Backend (this UID)      | Write only     | REST API with ID token for backend UID   |
| Clients / other users   | No read/write  | Rules deny `.read` and `.write` for them |

Only the backend, acting as UID `ERr61aQKyOSMqjbkl8SFy5EpBxD2`, can insert records in Firebase.
