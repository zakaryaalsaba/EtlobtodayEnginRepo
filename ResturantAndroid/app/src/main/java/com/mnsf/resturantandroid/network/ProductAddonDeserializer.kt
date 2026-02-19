package com.mnsf.resturantandroid.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.mnsf.resturantandroid.data.model.ProductAddon
import com.mnsf.resturantandroid.data.model.ProductAddonsResponse
import java.lang.reflect.Type

/**
 * Custom deserializer for ProductAddon to handle boolean fields that come as 0/1 from MySQL
 */
class ProductAddonDeserializer : JsonDeserializer<ProductAddon> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ProductAddon {
        val jsonObject = json?.asJsonObject ?: throw IllegalStateException("ProductAddon JSON is null")
        
        // Helper function to safely parse boolean (handles 0/1, true/false, null)
        fun parseBoolean(element: JsonElement?): Boolean {
            if (element == null || element.isJsonNull) return false
            return when {
                element.isJsonPrimitive -> {
                    val primitive = element.asJsonPrimitive
                    when {
                        primitive.isBoolean -> primitive.asBoolean
                        primitive.isNumber -> primitive.asInt != 0
                        primitive.isString -> {
                            val str = primitive.asString
                            str.equals("true", ignoreCase = true) || str == "1"
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }
        
        return ProductAddon(
            id = jsonObject.get("id")?.asInt ?: throw IllegalStateException("ProductAddon id is missing"),
            product_id = jsonObject.get("product_id")?.asInt ?: throw IllegalStateException("ProductAddon product_id is missing"),
            name = jsonObject.get("name")?.asString ?: throw IllegalStateException("ProductAddon name is missing"),
            name_ar = jsonObject.get("name_ar")?.takeIf { !it.isJsonNull }?.asString,
            description = jsonObject.get("description")?.takeIf { !it.isJsonNull }?.asString,
            description_ar = jsonObject.get("description_ar")?.takeIf { !it.isJsonNull }?.asString,
            image_url = jsonObject.get("image_url")?.takeIf { !it.isJsonNull }?.asString,
            price = jsonObject.get("price")?.asDouble ?: 0.0,
            is_required = parseBoolean(jsonObject.get("is_required")),
            display_order = jsonObject.get("display_order")?.takeIf { !it.isJsonNull }?.asInt ?: 0
        )
    }
}

/**
 * Custom deserializer for ProductAddonsResponse to handle boolean fields that come as 0/1 from MySQL
 */
class ProductAddonsResponseDeserializer : JsonDeserializer<ProductAddonsResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ProductAddonsResponse {
        val jsonObject = json?.asJsonObject ?: throw IllegalStateException("ProductAddonsResponse JSON is null")
        
        // Helper function to safely parse boolean (handles 0/1, true/false)
        fun parseBoolean(element: JsonElement?): Boolean {
            if (element == null || element.isJsonNull) return false
            return when {
                element.isJsonPrimitive -> {
                    val primitive = element.asJsonPrimitive
                    when {
                        primitive.isBoolean -> primitive.asBoolean
                        primitive.isNumber -> primitive.asInt != 0
                        primitive.isString -> {
                            val str = primitive.asString
                            str.equals("true", ignoreCase = true) || str == "1"
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }
        
        // Deserialize addons list using context
        val addonsElement = jsonObject.get("addons")
        val addons: List<ProductAddon> = if (addonsElement != null && addonsElement.isJsonArray && context != null) {
            addonsElement.asJsonArray.map { element ->
                context.deserialize(element, ProductAddon::class.java) as ProductAddon
            }
        } else {
            emptyList()
        }
        
        return ProductAddonsResponse(
            addons = addons,
            addon_required = parseBoolean(jsonObject.get("addon_required")),
            addon_required_min = jsonObject.get("addon_required_min")?.takeIf { !it.isJsonNull }?.asInt
        )
    }
}
