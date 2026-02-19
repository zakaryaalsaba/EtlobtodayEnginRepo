/**
 * Push Notification Service
 * Handles sending push notifications via Firebase Cloud Messaging (FCM)
 */

import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

let firebaseInitialized = false;

/**
 * Initialize Firebase Admin SDK
 */
export function initializeFirebase() {
  console.log(`[PUSH NOTIFICATION SERVICE] initializeFirebase() called`);
  console.log(`[PUSH NOTIFICATION SERVICE] Current firebaseInitialized: ${firebaseInitialized}`);
  
  if (firebaseInitialized) {
    console.log(`[PUSH NOTIFICATION SERVICE] Firebase already initialized, skipping`);
    return;
  }

  try {
    console.log(`[PUSH NOTIFICATION SERVICE] Starting Firebase initialization...`);
    // Check if Firebase credentials are provided
    // Option 1: Environment variable (FIREBASE_SERVICE_ACCOUNT)
    // Option 2: File path (FIREBASE_SERVICE_ACCOUNT_PATH)
    // Option 3: Default file location (firebase-service-account.json in backend directory)
    let serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT;
    let credentials;
    
    console.log(`[PUSH NOTIFICATION SERVICE] Checking for FIREBASE_SERVICE_ACCOUNT env var: ${serviceAccount ? 'Found' : 'Not found'}`);
    
    // If no env var, try file path
    if (!serviceAccount) {
      const filePath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || 
                      path.join(__dirname, '..', 'firebase-service-account.json');
      
      console.log(`[PUSH NOTIFICATION SERVICE] Checking for service account file at: ${filePath}`);
      try {
        if (fs.existsSync(filePath)) {
          const fileContent = fs.readFileSync(filePath, 'utf8');
          serviceAccount = fileContent;
          console.log(`[PUSH NOTIFICATION SERVICE] ✅ Firebase service account loaded from: ${filePath}`);
        } else {
          console.log(`[PUSH NOTIFICATION SERVICE] ❌ Service account file not found at: ${filePath}`);
        }
      } catch (fileError) {
        console.error(`[PUSH NOTIFICATION SERVICE] ❌ Error reading Firebase service account file:`, fileError.message);
        // Don't return yet, check if env var is set
      }
    }
    
    if (!serviceAccount) {
      console.warn(`[PUSH NOTIFICATION SERVICE] ❌ Firebase service account not configured. Push notifications will be disabled.`);
      console.warn(`[PUSH NOTIFICATION SERVICE] Set FIREBASE_SERVICE_ACCOUNT environment variable or FIREBASE_SERVICE_ACCOUNT_PATH to a JSON file.`);
      console.warn(`[PUSH NOTIFICATION SERVICE] Or place firebase-service-account.json in the backend directory.`);
      return;
    }

    // Parse service account JSON if it's a string
    console.log(`[PUSH NOTIFICATION SERVICE] Parsing service account JSON...`);
    try {
      credentials = typeof serviceAccount === 'string' 
        ? JSON.parse(serviceAccount) 
        : serviceAccount;
      console.log(`[PUSH NOTIFICATION SERVICE] ✅ Service account JSON parsed successfully`);
      console.log(`[PUSH NOTIFICATION SERVICE] Project ID: ${credentials.project_id || 'N/A'}`);
    } catch (e) {
      console.error(`[PUSH NOTIFICATION SERVICE] ❌ Error parsing Firebase service account:`, e.message);
      console.error(`[PUSH NOTIFICATION SERVICE] Make sure the JSON is valid. If using environment variable, ensure it's properly escaped.`);
      return;
    }

    // Initialize Firebase Admin (include databaseURL for Realtime Database)
    const databaseURL = process.env.FIREBASE_DATABASE_URL ||
      `https://${credentials.project_id}-default-rtdb.firebaseio.com`;
    console.log(`[PUSH NOTIFICATION SERVICE] Initializing Firebase Admin SDK...`);
    admin.initializeApp({
      credential: admin.credential.cert(credentials),
      databaseURL // Required for Firebase Realtime Database (e.g. order sync)
    });

    firebaseInitialized = true;
    console.log(`[PUSH NOTIFICATION SERVICE] ✅ Firebase Admin SDK initialized successfully`);
  } catch (error) {
    console.error(`[PUSH NOTIFICATION SERVICE] ❌ Error initializing Firebase Admin SDK:`, error.message);
    console.error(`[PUSH NOTIFICATION SERVICE] Error stack:`, error.stack);
    firebaseInitialized = false;
  }
}

/**
 * Send push notification to a device
 * @param {string} deviceToken - FCM device token
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {Object} data - Additional data payload
 * @returns {Promise<boolean>} Success status
 */
export async function sendPushNotification(deviceToken, title, body, data = {}) {
  if (!firebaseInitialized) {
    initializeFirebase();
  }

  if (!firebaseInitialized) {
    console.warn('Firebase not initialized. Push notification not sent.');
    return false;
  }

  if (!deviceToken) {
    console.warn('No device token provided. Push notification not sent.');
    return false;
  }

  try {
    const message = {
      notification: {
        title: title,
        body: body
      },
      data: {
        ...data,
        // Convert all data values to strings (FCM requirement)
        ...Object.fromEntries(
          Object.entries(data).map(([key, value]) => [key, String(value)])
        )
      },
      token: deviceToken,
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          channelId: 'driver_order_notifications'
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1
          }
        }
      }
    };

    const response = await admin.messaging().send(message);
    console.log('Push notification sent successfully:', response);
    return true;
  } catch (error) {
    console.error('Error sending push notification:', error);
    
    // Handle invalid token errors
    if (error.code === 'messaging/invalid-registration-token' || 
        error.code === 'messaging/registration-token-not-registered') {
      console.warn('Invalid or unregistered device token. Token should be removed from database.');
      // Return false so caller can handle token cleanup
    }
    
    return false;
  }
}

/**
 * Send push notification to multiple devices
 * @param {Array<string>} deviceTokens - Array of FCM device tokens
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {Object} data - Additional data payload
 * @returns {Promise<Object>} Results with success and failure counts
 */
export async function sendPushNotificationToMultiple(deviceTokens, title, body, data = {}) {
  console.log(`[PUSH NOTIFICATION SERVICE] sendPushNotificationToMultiple called`);
  console.log(`[PUSH NOTIFICATION SERVICE] - deviceTokens count: ${deviceTokens?.length || 0}`);
  console.log(`[PUSH NOTIFICATION SERVICE] - title: ${title}`);
  console.log(`[PUSH NOTIFICATION SERVICE] - body: ${body}`);
  console.log(`[PUSH NOTIFICATION SERVICE] - data:`, JSON.stringify(data));
  
  console.log(`[PUSH NOTIFICATION SERVICE] Step 1: Checking Firebase initialization...`);
  if (!firebaseInitialized) {
    console.log(`[PUSH NOTIFICATION SERVICE] Firebase not initialized, initializing now...`);
    initializeFirebase();
  }

  console.log(`[PUSH NOTIFICATION SERVICE] Step 2: Firebase initialized: ${firebaseInitialized}`);
  if (!firebaseInitialized || !deviceTokens || deviceTokens.length === 0) {
    console.log(`[PUSH NOTIFICATION SERVICE] ❌ Early return - firebaseInitialized: ${firebaseInitialized}, deviceTokens: ${deviceTokens?.length || 0}`);
    return { success: 0, failure: 0, invalidTokens: [] };
  }

  try {
    console.log(`[PUSH NOTIFICATION SERVICE] Step 3: Building FCM message...`);
    const message = {
      notification: {
        title: title,
        body: body
      },
      data: {
        ...Object.fromEntries(
          Object.entries(data).map(([key, value]) => [key, String(value)])
        )
      },
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          channelId: 'driver_order_notifications'
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1
          }
        }
      },
      tokens: deviceTokens.filter(token => token) // Remove null/undefined tokens
    };
    
    console.log(`[PUSH NOTIFICATION SERVICE] Step 3: ✅ Message built with ${message.tokens.length} token(s)`);
    console.log(`[PUSH NOTIFICATION SERVICE] Full FCM message being sent:`);
    console.log(`[PUSH NOTIFICATION SERVICE] Message JSON:`, JSON.stringify(message, null, 2));
    console.log(`[PUSH NOTIFICATION SERVICE] Tokens being sent (${message.tokens.length}):`);
    message.tokens.forEach((token, index) => {
      console.log(`[PUSH NOTIFICATION SERVICE]   Token ${index + 1}: ${token}`);
    });

    console.log(`[PUSH NOTIFICATION SERVICE] Step 4: Calling admin.messaging().sendEachForMulticast()...`);
    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(`[PUSH NOTIFICATION SERVICE] Step 4: ✅ FCM response received`);
    console.log(`[PUSH NOTIFICATION SERVICE] FCM Response:`, {
      successCount: response.successCount,
      failureCount: response.failureCount,
      responses: response.responses?.map((r, idx) => ({
        index: idx,
        success: r.success,
        error: r.error ? {
          code: r.error.code,
          message: r.error.message
        } : null
      }))
    });
    
    const invalidTokens = [];
    if (response.failureCount > 0) {
      console.log(`[PUSH NOTIFICATION SERVICE] Step 5: Processing ${response.failureCount} failure(s)...`);
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          console.log(`[PUSH NOTIFICATION SERVICE] Failure ${idx}:`, {
            errorCode: resp.error?.code,
            errorMessage: resp.error?.message,
            token: deviceTokens[idx]?.substring(0, 20) + '...'
          });
          if (resp.error?.code === 'messaging/invalid-registration-token' ||
              resp.error?.code === 'messaging/registration-token-not-registered') {
            invalidTokens.push(deviceTokens[idx]);
            console.log(`[PUSH NOTIFICATION SERVICE] Token ${idx} marked as invalid`);
          }
        }
      });
    } else {
      console.log(`[PUSH NOTIFICATION SERVICE] Step 5: No failures to process`);
    }

    const result = {
      success: response.successCount,
      failure: response.failureCount,
      invalidTokens: invalidTokens
    };
    
    console.log(`[PUSH NOTIFICATION SERVICE] Step 6: ✅ Returning result:`, JSON.stringify(result));
    return result;
  } catch (error) {
    console.error(`[PUSH NOTIFICATION SERVICE] ❌ ERROR in sendPushNotificationToMultiple:`);
    console.error(`[PUSH NOTIFICATION SERVICE] Error message:`, error.message);
    console.error(`[PUSH NOTIFICATION SERVICE] Error code:`, error.code);
    console.error(`[PUSH NOTIFICATION SERVICE] Error stack:`, error.stack);
    console.error(`[PUSH NOTIFICATION SERVICE] Full error:`, error);
    return { success: 0, failure: deviceTokens.length, invalidTokens: [] };
  }
}

/**
 * Send order status update push notification
 * @param {string} deviceToken - FCM device token
 * @param {Object} order - Order object
 * @param {string} status - New order status
 * @returns {Promise<boolean>} Success status
 */
export async function sendOrderStatusPushNotification(deviceToken, order, status) {
  const statusMessages = {
    'pending': 'Your order has been received and is being processed',
    'confirmed': 'Your order has been confirmed',
    'preparing': 'Your order is being prepared',
    'ready': 'Your order is ready for pickup',
    'completed': 'Your order has been completed',
    'cancelled': 'Your order has been cancelled'
  };

  const title = `Order ${order.order_number} - ${status.charAt(0).toUpperCase() + status.slice(1)}`;
  const body = statusMessages[status] || `Your order status has been updated to ${status}`;

  const data = {
    type: 'order_update',
    order_id: String(order.id),
    order_number: order.order_number,
    status: status
  };

  return await sendPushNotification(deviceToken, title, body, data);
}

