package com.driver.resturantandroid.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.driver.resturantandroid.data.model.Order
import com.driver.resturantandroid.data.model.OrderItem
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Service for reading orders from Firebase Realtime Database (read-only).
 * All write operations should go through MySQL/REST API.
 * 
 * Firebase structure: orders/{website_id}/{order_number}
 * 
 * IMPORTANT: Firebase Realtime Database rules must allow read access.
 * Update Firebase Console → Realtime Database → Rules to:
 * {
 *   "rules": {
 *     "orders": {
 *       ".read": true,
 *       ".write": "auth != null && auth.uid == 'ERr61aQKyOSMqjbkl8SFy5EpBxD2'"
 *     }
 *   }
 * }
 */
class FirebaseOrderService {
    private val database: DatabaseReference = try {
        // Use same Realtime Database URL as backend (europe-west1). Without this, the app
        // would use the default US instance and never receive data the backend writes.
        val databaseUrl = com.driver.resturantandroid.BuildConfig.FIREBASE_DATABASE_URL
        Log.d(TAG, "🔧 Using Firebase Database URL: $databaseUrl")
        val firebaseDatabase = FirebaseDatabase.getInstance(databaseUrl)
        // Enable debug logging to diagnose connection/callback issues (see logcat tag "FirebaseDatabase")
        try {
            firebaseDatabase.setLogLevel(Logger.Level.DEBUG)
            Log.d(TAG, "🔧 Firebase Realtime Database debug logging enabled")
        } catch (e: Exception) {
            Log.w(TAG, "🔧 Could not set Firebase log level: ${e.message}")
        }
        firebaseDatabase.reference
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get Firebase Database: ${e.message}", e)
        throw e
    }
    
    companion object {
        private const val TAG = "FirebaseOrderService"
        private const val ORDERS_PATH = "orders"
    }
    
    init {
        // Log Firebase database configuration
        Log.d(TAG, "🔧 Firebase Database initialized")
        Log.d(TAG, "🔧 Firebase Database Path: ${database.path}")
        
        // Verify Firebase is initialized
        try {
            val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
            Log.d(TAG, "🔧 Firebase App Name: ${firebaseApp.name}")
            Log.d(TAG, "🔧 Firebase App Options: ${firebaseApp.options.projectId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase App not initialized: ${e.message}")
        }
    }
    
    /**
     * Listen to all orders in real-time. Emits only orders that:
     * - Have order_type == "delivery" (pickup/dine-in orders never appear in the driver app)
     * - Have request_status != "Accepted" (not yet taken by another driver)
     * - Have status in pending, confirmed, preparing, ready
     */
    fun listenToOrders(): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "🔥 Starting Firebase listener on path: $ORDERS_PATH")
        val ordersMap = mutableMapOf<String, Order>()
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    Log.d(TAG, "📥 ✅ Firebase onDataChange TRIGGERED!")
                    Log.d(TAG, "📥 Snapshot exists: ${snapshot.exists()}, hasChildren: ${snapshot.hasChildren()}")
                    Log.d(TAG, "📥 Snapshot key: ${snapshot.key}, path: ${snapshot.ref.path}")
                    
                    if (!snapshot.exists()) {
                        Log.w(TAG, "⚠️ Firebase snapshot is empty - no orders found")
                        ordersMap.clear()
                        trySend(emptyList())
                        return
                    }
                    
                    ordersMap.clear()
                    var totalOrdersFound = 0
                    var deliveryOrdersFound = 0
                    var filteredOrdersCount = 0
                    
                    // Iterate through all website_ids
                    snapshot.children.forEach { websiteSnapshot ->
                        val websiteId = websiteSnapshot.key?.toIntOrNull()
                        if (websiteId == null) {
                            Log.w(TAG, "⚠️ Invalid website_id: ${websiteSnapshot.key}")
                            return@forEach
                        }
                        
                        Log.d(TAG, "📦 Processing website_id: $websiteId, orders count: ${websiteSnapshot.childrenCount}")
                        
                        // Iterate through all orders for this website
                        websiteSnapshot.children.forEach { orderSnapshot ->
                            totalOrdersFound++
                            try {
                                // Firebase requires GenericTypeIndicator for generic types like Map (type erasure)
                                val orderData = orderSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                                if (orderData == null) {
                                    Log.w(TAG, "⚠️ Order data is null for key: ${orderSnapshot.key}")
                                    return@forEach
                                }
                                
                                val order = parseOrderFromFirebase(orderData, websiteId)
                                if (order != null) {
                                    val requestStatus = (orderData["request_status"] as? String)?.trim()?.lowercase()
                                    // Only show orders that are still available: request_status is null or "pending"
                                    // When a driver accepts, backend sets request_status to "Accepted" in Firebase
                                    if (requestStatus == "accepted") {
                                        Log.d(TAG, "⏭️ Skipped order ${order.order_number} - already accepted by another driver (request_status=Accepted)")
                                        return@forEach
                                    }
                                    // Driver app shows ONLY delivery orders; pickup/dine-in must not appear
                                    val orderTypeNormalized = order.order_type.trim().lowercase()
                                    if (orderTypeNormalized != "delivery") {
                                        Log.d(TAG, "⏭️ Skipped order ${order.order_number} - order_type is '${order.order_type}' (only delivery orders appear in driver app)")
                                        return@forEach
                                    }
                                    Log.d(TAG, "✅ Parsed order: ${order.order_number}, type: ${order.order_type}, status: ${order.status}, request_status: ${requestStatus ?: "pending"}")
                                    deliveryOrdersFound++
                                    // Filter: delivery orders that are available for drivers
                                    // Status: pending, confirmed, preparing, ready (matches backend API logic)
                                    if (order.status in listOf("pending", "confirmed", "preparing", "ready")) {
                                        filteredOrdersCount++
                                        ordersMap[order.order_number] = order
                                        Log.d(TAG, "✅ Added order to available list: ${order.order_number} (status: ${order.status})")
                                    } else {
                                        Log.d(TAG, "⏭️ Skipped order ${order.order_number} - status '${order.status}' not in allowed list")
                                    }
                                } else {
                                    Log.w(TAG, "⚠️ Failed to parse order: ${orderSnapshot.key}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error parsing order ${orderSnapshot.key}: ${e.message}", e)
                            }
                        }
                    }
                    
                    val ordersList = ordersMap.values.toList().sortedByDescending { 
                        it.created_at 
                    }
                    
                    Log.d(TAG, "📊 Summary: Total orders: $totalOrdersFound, Delivery orders: $deliveryOrdersFound, Filtered (available): $filteredOrdersCount")
                    Log.d(TAG, "📤 Emitting ${ordersList.size} available orders to Flow")
                    
                    if (ordersList.isNotEmpty()) {
                        ordersList.forEach { order ->
                            Log.d(TAG, "  → Order: ${order.order_number}, Status: ${order.status}, Type: ${order.order_type}")
                        }
                    }
                    
                    trySend(ordersList)
                    Log.d(TAG, "✅ Successfully sent ${ordersList.size} orders to Flow")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing Firebase snapshot: ${e.message}", e)
                    trySend(emptyList())
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Firebase listener cancelled!")
                Log.e(TAG, "❌ Error code: ${error.code}, Message: ${error.message}")
                Log.e(TAG, "❌ Details: ${error.details}")
                close(error.toException())
            }
        }
        
        val databaseRef = database.child(ORDERS_PATH)
        Log.d(TAG, "🔗 Attaching listener to: ${databaseRef.path}")
        Log.d(TAG, "🔗 Database reference key: ${databaseRef.key}")
        
        // 1) Test read at ROOT to see if we get ANY callback (connectivity vs rules)
        Log.d(TAG, "🧪 [ROOT] Starting test read at path '/' to check connectivity...")
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "🧪 [ROOT] ✅ Root read SUCCESS - Firebase connection works! exists=${snapshot.exists()}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "🧪 [ROOT] ❌ Root read FAILED: code=${error.code} message=${error.message}")
            }
        })
        
        // 2) Test read at /orders to check permissions on that path
        Log.d(TAG, "🧪 Starting test read at '/orders' to check Firebase connection and permissions...")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "🧪 ✅ Test read SUCCESSFUL!")
                Log.d(TAG, "🧪 Snapshot exists: ${snapshot.exists()}")
                Log.d(TAG, "🧪 Snapshot has children: ${snapshot.hasChildren()}")
                if (snapshot.exists()) {
                    Log.d(TAG, "🧪 Snapshot value: ${snapshot.value}")
                    Log.d(TAG, "🧪 Children count: ${snapshot.childrenCount}")
                } else {
                    Log.w(TAG, "🧪 ⚠️ Path '/orders' exists but is empty - no orders in Firebase")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "🧪 ❌ Test read FAILED!")
                Log.e(TAG, "🧪 Error code: ${error.code}")
                Log.e(TAG, "🧪 Error message: ${error.message}")
                Log.e(TAG, "🧪 Error details: ${error.details}")
                when (error.code) {
                    DatabaseError.PERMISSION_DENIED -> {
                        Log.e(TAG, "🚫 PERMISSION DENIED - Firebase rules are blocking read access!")
                        Log.e(TAG, "🚫 Current rules may not allow reading '/orders' path")
                        Log.e(TAG, "🚫 Update Firebase Console → Realtime Database → Rules to:")
                        Log.e(TAG, "🚫 { \"rules\": { \"orders\": { \".read\": true } } }")
                    }
                    DatabaseError.NETWORK_ERROR -> {
                        Log.e(TAG, "🌐 NETWORK ERROR - Check internet connection")
                    }
                    DatabaseError.UNAVAILABLE -> {
                        Log.e(TAG, "🔌 UNAVAILABLE - Firebase service is unavailable")
                    }
                    else -> {
                        Log.e(TAG, "❌ Other Firebase error: ${error.code} - ${error.message}")
                    }
                }
            }
        })
        
        // Now attach the persistent listener
        databaseRef.addValueEventListener(listener)
        Log.d(TAG, "✅ Firebase listener attached successfully")
        Log.d(TAG, "⏳ Waiting for Firebase onDataChange callback... (check logcat for tag 'FirebaseDatabase' for connection details)")
        
        // If no callback after 8s, log hint (connectivity/rules often the cause per Firebase docs)
        val mainHandler = Handler(Looper.getMainLooper())
        val delayedHint = Runnable {
            Log.w(TAG, "⚠️ No Firebase callback received after 8s. Check: 1) Realtime Database Rules allow .read for 'orders' (Firebase Console → Rules), 2) Network, 3) Data exists at /orders (Firebase Console → Data). Enable tag 'FirebaseDatabase' in logcat for connection logs.")
        }
        mainHandler.postDelayed(delayedHint, 8000L)
        
        // REST test: can we read from Firebase at all? (no auth; rules must allow .read)
        val baseUrl = com.driver.resturantandroid.BuildConfig.FIREBASE_DATABASE_URL
        Thread {
            try {
                val url = "$baseUrl/orders.json"
                Log.d(TAG, "🌐 [REST TEST] GET $url")
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                Log.d(TAG, "🌐 [REST TEST] HTTP ${response.code} | body length=${body.length} | preview=${body.take(300)}")
                if (!response.isSuccessful) Log.e(TAG, "🌐 [REST TEST] If 401/403: Rules deny read. If 200: SDK may be failing to connect.")
            } catch (e: Exception) {
                Log.e(TAG, "🌐 [REST TEST] Failed: ${e.message}", e)
                if (e.message?.contains("Unable to resolve host") == true) {
                    Log.e(TAG, "🌐 [REST TEST] ⚠️ DNS/network: This device cannot resolve the Firebase host. Try: 1) Use a physical device on WiFi/mobile data, 2) Emulator: set DNS to 8.8.8.8 (e.g. adb shell setprop net.dns1 8.8.8.8), 3) Ensure device has internet access.")
                }
            }
        }.start()
        
        awaitClose {
            mainHandler.removeCallbacks(delayedHint)
            Log.d(TAG, "🛑 Removing Firebase listener")
            database.child(ORDERS_PATH).removeEventListener(listener)
            Log.d(TAG, "✅ Firebase listener removed")
        }
    }
    
    /**
     * Parse amount from Firebase (can be stored as Number or String).
     */
    private fun parseAmount(value: Any?): String {
        if (value == null) return "0"
        return when (value) {
            is Number -> value.toString()
            is String -> value.trim().ifEmpty { "0" }
            else -> "0"
        }
    }

    /**
     * Parse double from Firebase (Number or String).
     */
    private fun parseDouble(value: Any?): Double? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull()
            else -> null
        }
    }

    /**
     * Parse restaurant from Firebase nested map (if backend includes it).
     */
    private fun parseRestaurant(data: Map<*, *>?): com.driver.resturantandroid.data.model.RestaurantInfo? {
        if (data == null) return null
        return try {
            val name = data["name"] as? String
            val phone = data["phone"] as? String
            val address = data["address"] as? String
            val logoUrl = data["logo_url"] as? String
            val lat = parseDouble(data["latitude"])
            val lng = parseDouble(data["longitude"])
            if (name == null && phone == null && address == null && lat == null && lng == null) null
            else com.driver.resturantandroid.data.model.RestaurantInfo(
                name = name,
                phone = phone,
                logo_url = logoUrl,
                address = address,
                latitude = lat,
                longitude = lng
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse restaurant: ${e.message}")
            null
        }
    }

    /**
     * Parse Firebase data map to Order object.
     */
    private fun parseOrderFromFirebase(data: Map<*, *>, websiteId: Int): Order? {
        return try {
            val orderNumber = (data["order_number"] as? String) ?: ""
            Log.v(TAG, "🔍 Parsing order: $orderNumber")
            
            val itemsList = (data["items"] as? List<*>)?.mapNotNull { itemMap ->
                val item = itemMap as? Map<*, *> ?: return@mapNotNull null
                OrderItem(
                    id = (item["id"] as? Number)?.toInt() ?: 0,
                    product_id = (item["product_id"] as? Number)?.toInt() ?: 0,
                    product_name = (item["product_name"] as? String) ?: "",
                    product_price = parseAmount(item["product_price"]),
                    quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                    subtotal = parseAmount(item["subtotal"])
                )
            } ?: emptyList()
            
            val orderType = (data["order_type"] as? String) ?: "pickup"
            val status = (data["status"] as? String) ?: "pending"
            val restaurantMap = data["restaurant"] as? Map<*, *>
            val restaurant = parseRestaurant(restaurantMap)
            
            Log.v(TAG, "  → Order type: $orderType, Status: $status, Items: ${itemsList.size}")
            
            Order(
                id = (data["id"] as? Number)?.toInt() ?: 0,
                website_id = websiteId,
                customer_id = (data["customer_id"] as? Number)?.toInt(),
                order_number = orderNumber,
                customer_name = (data["customer_name"] as? String) ?: "",
                customer_email = data["customer_email"] as? String,
                customer_phone = (data["customer_phone"] as? String) ?: "",
                customer_address = data["customer_address"] as? String,
                order_type = orderType,
                status = status,
                total_amount = parseAmount(data["total_amount"]),
                payment_status = (data["payment_status"] as? String) ?: "pending",
                payment_method = data["payment_method"] as? String,
                delivery_latitude = parseDouble(data["delivery_latitude"]),
                delivery_longitude = parseDouble(data["delivery_longitude"]),
                items = itemsList,
                created_at = (data["created_at"] as? String) ?: "",
                updated_at = (data["updated_at"] as? String) ?: "",
                restaurant = restaurant,
                currency_code = null,
                currency_symbol_position = null,
                delivery_fees = parseAmount(data["delivery_fees"]).takeIf { it != "0" },
                tip = parseAmount(data["tip"]).takeIf { it != "0" }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing order: ${e.message}", e)
            Log.e(TAG, "❌ Order data keys: ${data.keys}")
            null
        }
    }
}
