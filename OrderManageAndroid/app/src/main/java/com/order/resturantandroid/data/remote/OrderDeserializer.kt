package com.order.resturantandroid.data.remote

import com.google.gson.*
import com.order.resturantandroid.data.model.OrderItem
import java.lang.reflect.Type

class OrderItemsDeserializer : JsonDeserializer<List<OrderItem>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<OrderItem> {
        if (json == null || json.isJsonNull) {
            return emptyList()
        }
        
        return try {
            when {
                json.isJsonArray -> {
                    json.asJsonArray.mapNotNull { element ->
                        try {
                            val obj = element.asJsonObject
                            OrderItem(
                                id = obj.get("id")?.asInt ?: 0,
                                productId = obj.get("product_id")?.asInt ?: 0,
                                productName = obj.get("product_name")?.asString ?: "",
                                productPrice = obj.get("product_price")?.asString ?: "0.00",
                                quantity = obj.get("quantity")?.asInt ?: 0,
                                subtotal = obj.get("subtotal")?.asString ?: "0.00"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                    // Try to parse JSON string
                    val jsonString = json.asString
                    if (jsonString.isBlank()) {
                        emptyList()
                    } else {
                        val parser = JsonParser()
                        val parsed = parser.parse(jsonString)
                        if (parsed.isJsonArray) {
                            parsed.asJsonArray.mapNotNull { element ->
                                try {
                                    val obj = element.asJsonObject
                                    OrderItem(
                                        id = obj.get("id")?.asInt ?: 0,
                                        productId = obj.get("product_id")?.asInt ?: 0,
                                        productName = obj.get("product_name")?.asString ?: "",
                                        productPrice = obj.get("product_price")?.asString ?: "0.00",
                                        quantity = obj.get("quantity")?.asInt ?: 0,
                                        subtotal = obj.get("subtotal")?.asString ?: "0.00"
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        } else {
                            emptyList()
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

