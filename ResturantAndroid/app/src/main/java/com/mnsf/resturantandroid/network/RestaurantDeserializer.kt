package com.mnsf.resturantandroid.network

import com.google.gson.*
import com.mnsf.resturantandroid.data.model.Restaurant
import java.lang.reflect.Type

/**
 * Custom deserializer for Restaurant to handle payment_methods field
 * which can be either a JSON object or a JSON string
 */
class RestaurantDeserializer : JsonDeserializer<Restaurant> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Restaurant {
        if (json == null || !json.isJsonObject) {
            throw JsonParseException("Expected JSON object")
        }

        val jsonObject = json.asJsonObject
        
        // Handle payment_methods field - convert object to string if needed
        val paymentMethodsElement = jsonObject.get("payment_methods")
        val paymentMethodsString: String? = when {
            paymentMethodsElement == null || paymentMethodsElement.isJsonNull -> null
            paymentMethodsElement.isJsonObject -> {
                // Convert JSON object to string
                paymentMethodsElement.asJsonObject.toString()
            }
            paymentMethodsElement.isJsonPrimitive && paymentMethodsElement.asJsonPrimitive.isString -> {
                // Already a string
                paymentMethodsElement.asString
            }
            else -> null
        }
        
        // Replace the payment_methods field with the string version
        if (paymentMethodsString != null) {
            jsonObject.addProperty("payment_methods", paymentMethodsString)
        } else {
            jsonObject.remove("payment_methods")
        }
        
        // Handle tax_enabled - convert from number (0/1) to boolean if needed
        val taxEnabledElement = jsonObject.get("tax_enabled")
        if (taxEnabledElement != null && taxEnabledElement.isJsonPrimitive) {
            val taxEnabledPrimitive = taxEnabledElement.asJsonPrimitive
            if (taxEnabledPrimitive.isNumber) {
                // Convert number (0/1) to boolean
                val taxEnabledValue = taxEnabledPrimitive.asInt != 0
                jsonObject.addProperty("tax_enabled", taxEnabledValue)
            }
        }
        
        // Handle order type fields - convert from number (0/1) to boolean if needed
        val orderTypeFields = listOf(
            "order_type_dine_in_enabled",
            "order_type_pickup_enabled",
            "order_type_delivery_enabled"
        )
        
        orderTypeFields.forEach { fieldName ->
            val fieldElement = jsonObject.get(fieldName)
            if (fieldElement != null && fieldElement.isJsonPrimitive) {
                val fieldPrimitive = fieldElement.asJsonPrimitive
                if (fieldPrimitive.isNumber) {
                    // Convert number (0/1) to boolean
                    val fieldValue = fieldPrimitive.asInt != 0
                    jsonObject.addProperty(fieldName, fieldValue)
                }
            }
        }
        
        // Use default deserialization for the rest
        val gson = GsonBuilder()
            .registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())
            .registerTypeAdapter(Boolean::class.javaPrimitiveType, BooleanTypeAdapter())
            .create()
        
        return gson.fromJson(jsonObject, Restaurant::class.java)
    }
}

