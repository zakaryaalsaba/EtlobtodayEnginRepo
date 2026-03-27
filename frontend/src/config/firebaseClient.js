import { initializeApp, getApps, getApp } from 'firebase/app';
import { getDatabase } from 'firebase/database';

/**
 * Web client config (safe to expose in the browser). Set at build time via Vite env:
 * VITE_FIREBASE_API_KEY, VITE_FIREBASE_AUTH_DOMAIN, VITE_FIREBASE_DATABASE_URL,
 * VITE_FIREBASE_PROJECT_ID, VITE_FIREBASE_APP_ID (optional: STORAGE_BUCKET, MESSAGING_SENDER_ID).
 *
 * Firebase Realtime Database rules must allow read for guests on the paths you expose, e.g.:
 * orders/{websiteId}/{orderNumber} — read: true (tracking), write: false for clients.
 */
function pickDefined(obj) {
  return Object.fromEntries(
    Object.entries(obj).filter(([, v]) => v != null && String(v).trim() !== '')
  );
}

/** Built from Vite env; matches backend Firebase project (see backend/.env.example). */
const firebaseConfig = pickDefined({
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
});

export function isFirebaseRealtimeConfigured() {
  return !!(firebaseConfig.databaseURL && firebaseConfig.apiKey && firebaseConfig.projectId);
}

let cachedDb = null;

export function getFirebaseRealtimeDb() {
  if (!isFirebaseRealtimeConfigured()) return null;
  if (cachedDb) return cachedDb;
  const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
  cachedDb = getDatabase(app);
  return cachedDb;
}
