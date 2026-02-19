/**
 * Firebase Order Sync Service
 * Saves orders to Firebase Realtime Database when an order is placed.
 * Saves driver requests (orders_delivery) when a restaurant requests a driver for a zone.
 * Writes as the backend UID (FIREBASE_BACKEND_UID) so only that identity can insert.
 * Data is stored under: orders/{website_id}/{order_number}, orders_delivery/{website_id}/{id}
 */

import admin from 'firebase-admin';
import { initializeFirebase } from './pushNotificationService.js';

const BACKEND_UID = process.env.FIREBASE_BACKEND_UID || 'ERr61aQKyOSMqjbkl8SFy5EpBxD2';

let cachedIdToken = null;
let tokenExpiryMs = 0;
const TOKEN_BUFFER_MS = 5 * 60 * 1000; // refresh 5 min before expiry

/**
 * Get Firebase Realtime Database base URL (no trailing slash).
 */
function getDatabaseUrl() {
  if (process.env.FIREBASE_DATABASE_URL) {
    return process.env.FIREBASE_DATABASE_URL.replace(/\/$/, '');
  }
  const app = admin.app();
  const url = app.options?.databaseURL;
  if (url) return url.replace(/\/$/, '');
  return null;
}

/**
 * Exchange a custom token for an ID token (for REST API auth).
 */
async function getIdTokenForBackendUid() {
  const apiKey = process.env.FIREBASE_WEB_API_KEY;
  if (!apiKey) {
    throw new Error('FIREBASE_WEB_API_KEY is required for backend order sync. Get it from Firebase Console > Project settings > General > Web API Key.');
  }
  const customToken = await admin.auth().createCustomToken(BACKEND_UID);
  const res = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${apiKey}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: customToken, returnSecureToken: true })
    }
  );
  if (!res.ok) {
    const err = await res.text();
    throw new Error(`signInWithCustomToken failed: ${res.status} ${err}`);
  }
  const data = await res.json();
  return { idToken: data.idToken, expiresIn: parseInt(data.expiresIn || '3600', 10) * 1000 };
}

/**
 * Get a valid ID token for the backend UID (cached until near expiry).
 */
async function getValidIdToken() {
  const now = Date.now();
  if (cachedIdToken && now < tokenExpiryMs - TOKEN_BUFFER_MS) {
    return cachedIdToken;
  }
  const { idToken, expiresIn } = await getIdTokenForBackendUid();
  cachedIdToken = idToken;
  tokenExpiryMs = now + expiresIn;
  return idToken;
}

/**
 * Converts an order object to a format suitable for Firebase Realtime Database
 */
function orderToFirebaseData(order) {
  const firebaseData = {
    id: order.id,
    website_id: order.website_id,
    customer_id: order.customer_id || null,
    order_number: order.order_number,
    customer_name: order.customer_name,
    customer_phone: order.customer_phone,
    order_type: order.order_type || 'pickup',
    status: order.status,
    total_amount: order.total_amount,
    payment_method: order.payment_method || 'cash',
    payment_status: order.payment_status || 'pending',
    created_at: order.created_at || new Date().toISOString(),
    updated_at: order.updated_at || new Date().toISOString(),
    request_status: 'pending'  // Drivers see order only when pending; becomes 'Accepted' when a driver accepts
  };

  if (order.customer_email) firebaseData.customer_email = order.customer_email;
  if (order.customer_address) firebaseData.customer_address = order.customer_address;
  if (order.notes) firebaseData.notes = order.notes;
  if (order.tip) firebaseData.tip = order.tip;
  if (order.delivery_instructions) firebaseData.delivery_instructions = order.delivery_instructions;
  if (order.service_fee) firebaseData.service_fee = order.service_fee;
  if (order.payment_intent_id) firebaseData.payment_intent_id = order.payment_intent_id;
  if (order.delivery_latitude) firebaseData.delivery_latitude = order.delivery_latitude;
  if (order.delivery_longitude) firebaseData.delivery_longitude = order.delivery_longitude;
  if (order.total_original_amount) firebaseData.total_original_amount = order.total_original_amount;
  if (order.tax) firebaseData.tax = order.tax;
  if (order.delivery_fees) firebaseData.delivery_fees = order.delivery_fees;
  if (order.restaurant && typeof order.restaurant === 'object') {
    firebaseData.restaurant = {
      name: order.restaurant.name ?? null,
      phone: order.restaurant.phone ?? null,
      address: order.restaurant.address ?? null,
      latitude: order.restaurant.latitude ?? null,
      longitude: order.restaurant.longitude ?? null
    };
  }

  if (order.items && Array.isArray(order.items) && order.items.length > 0) {
    firebaseData.items = order.items.map(item => ({
      id: item.id || null,
      product_id: item.product_id,
      product_name: item.product_name,
      product_price: item.product_price,
      quantity: item.quantity,
      subtotal: item.subtotal
    }));
  }

  const cleanData = {};
  for (const [key, value] of Object.entries(firebaseData)) {
    if (value !== null && value !== undefined) cleanData[key] = value;
  }
  return cleanData;
}

/**
 * Converts an orders_delivery record to a format suitable for Firebase Realtime Database
 */
function orderDeliveryToFirebaseData(record) {
  const firebaseData = {
    id: record.id,
    website_id: record.website_id,
    zone_id: record.zone_id,
    zone_name: record.zone_name || null,
    status: record.status || 'pending',
    created_at: record.created_at || new Date().toISOString(),
    updated_at: record.updated_at || new Date().toISOString()
  };
  const cleanData = {};
  for (const [key, value] of Object.entries(firebaseData)) {
    if (value !== null && value !== undefined) cleanData[key] = value;
  }
  return cleanData;
}

/**
 * Saves a driver request (orders_delivery) to Firebase Realtime Database using REST API as the backend UID.
 * Path: orders_delivery/{website_id}/{id} so delivery company/drivers can listen for new requests.
 */
export async function saveOrderDeliveryToFirebase(record) {
  try {
    initializeFirebase();

    if (!admin.apps || admin.apps.length === 0) {
      console.warn('[FirebaseOrderSync] Firebase Admin not initialized. Skipping orders_delivery Firebase sync.');
      return;
    }

    const databaseUrl = getDatabaseUrl();
    if (!databaseUrl) {
      console.warn('[FirebaseOrderSync] Firebase Database URL not set. Skipping orders_delivery Firebase sync.');
      return;
    }

    const idToken = await getValidIdToken();
    const firebaseData = orderDeliveryToFirebaseData(record);
    const path = `orders_delivery/${record.website_id}/${record.id}`;
    const url = `${databaseUrl}/${path}.json?auth=${encodeURIComponent(idToken)}`;

    const res = await fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(firebaseData)
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Firebase REST PUT failed: ${res.status} ${text}`);
    }

    console.log(`[FirebaseOrderSync] ✅ Driver request (orders_delivery) saved to Firebase: ${path}`);
  } catch (error) {
    cachedIdToken = null;
    tokenExpiryMs = 0;
    console.error('[FirebaseOrderSync] ❌ Failed to save orders_delivery to Firebase:', record?.id, error);
  }
}

/**
 * Saves an order to Firebase Realtime Database using REST API as the backend UID.
 * Only the backend UID (ERr61aQKyOSMqjbkl8SFy5EpBxD2) is allowed to write per rules.
 */
export async function saveOrderToFirebase(order) {
  try {
    initializeFirebase();

    if (!admin.apps || admin.apps.length === 0) {
      console.warn('[FirebaseOrderSync] Firebase Admin not initialized. Skipping Firebase sync.');
      return;
    }

    const databaseUrl = getDatabaseUrl();
    if (!databaseUrl) {
      console.warn('[FirebaseOrderSync] Firebase Database URL not set. Skipping Firebase sync.');
      return;
    }

    const idToken = await getValidIdToken();
    const firebaseData = orderToFirebaseData(order);
    const path = `orders/${order.website_id}/${order.order_number}`;
    const url = `${databaseUrl}/${path}.json?auth=${encodeURIComponent(idToken)}`;

    const res = await fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(firebaseData)
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Firebase REST PUT failed: ${res.status} ${text}`);
    }

    console.log(`[FirebaseOrderSync] ✅ Order saved to Firebase: ${path}`);
  } catch (error) {
    cachedIdToken = null;
    tokenExpiryMs = 0;
    console.error(`[FirebaseOrderSync] ❌ Failed to save order to Firebase: ${order.order_number}`, error);
  }
}

/**
 * Tries to set request_status to 'Accepted' in Firebase only if it is currently 'pending' (or null).
 * Uses a transaction so only one driver can succeed.
 * @param {number} websiteId
 * @param {string} orderNumber
 * @returns {Promise<{ accepted: boolean }>} { accepted: true } if this driver won; { accepted: false } if already accepted by another
 */
export async function tryAcceptOrderInFirebase(websiteId, orderNumber) {
  try {
    initializeFirebase();
    if (!admin.apps || admin.apps.length === 0) {
      console.warn('[FirebaseOrderSync] Firebase Admin not initialized.');
      return { accepted: false };
    }
    const db = admin.database();
    const ref = db.ref('orders').child(String(websiteId)).child(orderNumber).child('request_status');
    const result = await ref.transaction((current) => {
      if (current === null || current === 'pending') return 'Accepted';
      return undefined; // abort: leave value unchanged
    });
    const accepted = result.committed === true && result.snapshot.val() === 'Accepted';
    if (accepted) {
      console.log(`[FirebaseOrderSync] ✅ request_status set to Accepted for ${websiteId}/${orderNumber}`);
    } else {
      console.log(`[FirebaseOrderSync] ⚠️ Order ${websiteId}/${orderNumber} already accepted by another driver (request_status was ${result.snapshot.val()})`);
    }
    return { accepted };
  } catch (error) {
    console.error('[FirebaseOrderSync] ❌ tryAcceptOrderInFirebase failed:', error);
    return { accepted: false };
  }
}

/**
 * Removes an order from Firebase Realtime Database only.
 * Does not touch MySQL. Call this when order status becomes 'completed' or 'cancelled'
 * so drivers no longer see it in available/active lists.
 * @param {number} websiteId
 * @param {string} orderNumber
 */
export async function removeOrderFromFirebase(websiteId, orderNumber) {
  try {
    initializeFirebase();
    if (!admin.apps || admin.apps.length === 0) {
      console.warn('[FirebaseOrderSync] Firebase Admin not initialized. Skipping Firebase remove.');
      return;
    }
    const db = admin.database();
    const ref = db.ref('orders').child(String(websiteId)).child(String(orderNumber));
    await ref.remove();
    console.log(`[FirebaseOrderSync] ✅ Order removed from Firebase: orders/${websiteId}/${orderNumber}`);
  } catch (error) {
    console.error(`[FirebaseOrderSync] ❌ Failed to remove order from Firebase: ${websiteId}/${orderNumber}`, error);
  }
}
