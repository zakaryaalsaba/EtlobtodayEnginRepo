package com.mnsf.resturantandroid.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.mnsf.resturantandroid.data.model.Product
import java.lang.reflect.Type

/**
 * Custom deserializer for Product to handle boolean fields that come as 0/1 from MySQL
 */
class ProductDeserializer : JsonDeserializer<Product> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Product {
        val jsonObject = json?.asJsonObject ?: throw IllegalStateException("Product JSON is null")
        
        // Helper function to safely parse boolean (handles 0/1, true/false, null)
        fun parseBoolean(element: JsonElement?): Boolean? {
            if (element == null || element.isJsonNull) return null
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
                        else -> null
                    }
                }
                else -> null
            }
        }
        
        return Product(
            id = jsonObject.get("id")?.asInt ?: throw IllegalStateException("Product id is missing"),
            website_id = jsonObject.get("website_id")?.asInt ?: throw IllegalStateException("Product website_id is missing"),
            name = jsonObject.get("name")?.asString ?: throw IllegalStateException("Product name is missing"),
            name_ar = jsonObject.get("name_ar")?.takeIf { !it.isJsonNull }?.asString,
            description = jsonObject.get("description")?.takeIf { !it.isJsonNull }?.asString,
            description_ar = jsonObject.get("description_ar")?.takeIf { !it.isJsonNull }?.asString,
            price = jsonObject.get("price")?.asDouble ?: throw IllegalStateException("Product price is missing"),
            category = jsonObject.get("category")?.takeIf { !it.isJsonNull }?.asString,
            category_ar = jsonObject.get("category_ar")?.takeIf { !it.isJsonNull }?.asString,
            image_url = jsonObject.get("image_url")?.takeIf { !it.isJsonNull }?.asString,
            is_available = parseBoolean(jsonObject.get("is_available")) ?: true,
            addon_required = parseBoolean(jsonObject.get("addon_required")),
            addon_required_min = jsonObject.get("addon_required_min")?.takeIf { !it.isJsonNull }?.asInt,
            created_at = jsonObject.get("created_at")?.takeIf { !it.isJsonNull }?.asString
        )
    }
}
