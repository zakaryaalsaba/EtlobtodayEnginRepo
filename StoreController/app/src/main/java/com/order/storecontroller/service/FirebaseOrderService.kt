package com.order.storecontroller.service

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.order.storecontroller.BuildConfig
import com.order.storecontroller.data.model.Order
import com.order.storecontroller.data.model.OrderItem
import com.order.storecontroller.data.model.RestaurantInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Read-only service for Firebase Realtime Database.
 * Structure: orders/{website_id}/{order_number}
 * Call center: shows all orders (all types, all statuses). Optional filter by website IDs.
 */
class FirebaseOrderService {

    private val database by lazy {
        val url = BuildConfig.FIREBASE_DATABASE_URL
        Log.d(TAG, "Using Firebase Database URL: $url")
        FirebaseDatabase.getInstance(url).reference
    }

    companion object {
        private const val TAG = "FirebaseOrderService"
        private const val ORDERS_PATH = "orders"
    }

    /**
     * Listen to orders in real-time. For call center: no delivery-only or request_status filter.
     * @param websiteIds If non-empty, only orders from these website_ids are included. Empty = all websites.
     */
    fun listenToOrders(websiteIds: Set<Int> = emptySet()): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Starting listener on $ORDERS_PATH, websiteFilter=${websiteIds.takeIf { it.isNotEmpty() } ?: "all"}")

        val ordersMap = mutableMapOf<String, Order>()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (!snapshot.exists()) {
                        ordersMap.clear()
                        trySend(emptyList())
                        return
                    }
                    ordersMap.clear()
                    snapshot.children.forEach websiteLoop@ { websiteSnapshot ->
                        val websiteId = websiteSnapshot.key?.toIntOrNull() ?: return@websiteLoop
                        if (websiteIds.isNotEmpty() && websiteId !in websiteIds) return@websiteLoop
                        websiteSnapshot.children.forEach { orderSnapshot ->
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val orderData = orderSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                                if (orderData == null) return@forEach
                                val order = parseOrderFromFirebase(orderData, websiteId) ?: return@forEach
                                ordersMap["${order.website_id}_${order.order_number}"] = order
                            } catch (e: Exception) {
                                Log.e(TAG, "Parse order error: ${e.message}")
                            }
                        }
                    }
                    val list = ordersMap.values.toList().sortedByDescending { it.created_at }
                    trySend(list)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing snapshot: ${e.message}", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        database.child(ORDERS_PATH).addValueEventListener(listener)
        awaitClose {
            database.child(ORDERS_PATH).removeEventListener(listener)
        }
    }

    private fun parseAmount(value: Any?): String {
        if (value == null) return "0"
        return when (value) {
            is Number -> value.toString()
            is String -> value.trim().ifEmpty { "0" }
            else -> "0"
        }
    }

    private fun parseDouble(value: Any?): Double? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull()
            else -> null
        }
    }

    private fun parseRestaurant(data: Map<*, *>?): RestaurantInfo? {
        if (data == null) return null
        return try {
            val name = data["name"] as? String
            val phone = data["phone"] as? String
            val address = data["address"] as? String
            val logoUrl = data["logo_url"] as? String
            val lat = parseDouble(data["latitude"])
            val lng = parseDouble(data["longitude"])
            if (name == null && phone == null && address == null && lat == null && lng == null) null
            else RestaurantInfo(
                name = name,
                phone = phone,
                logo_url = logoUrl,
                address = address,
                latitude = lat,
                longitude = lng
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseOrderFromFirebase(data: Map<*, *>, websiteId: Int): Order? {
        return try {
            val orderNumber = (data["order_number"] as? String) ?: ""
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
            val restaurant = parseRestaurant(data["restaurant"] as? Map<*, *>)

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
            Log.e(TAG, "Parse order error: ${e.message}", e)
            null
        }
    }
}
