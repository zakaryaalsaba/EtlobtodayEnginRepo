package com.mnsf.resturantandroid.network

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Custom TypeAdapter to handle boolean values that come as 0/1 (numbers) from MySQL
 * Converts 0/1 to false/true
 */
class BooleanTypeAdapter : TypeAdapter<Boolean>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
    }

    override fun read(`in`: JsonReader): Boolean? {
        val peek = `in`.peek()
        when (peek) {
            JsonToken.BOOLEAN -> return `in`.nextBoolean()
            JsonToken.NULL -> {
                `in`.nextNull()
                return null
            }
            JsonToken.NUMBER -> {
                // Convert 0/1 to false/true
                val value = `in`.nextInt()
                return value != 0
            }
            JsonToken.STRING -> {
                // Handle string "true"/"false" or "0"/"1"
                val stringValue = `in`.nextString()
                return when {
                    stringValue.equals("true", ignoreCase = true) -> true
                    stringValue.equals("false", ignoreCase = true) -> false
                    stringValue == "1" -> true
                    stringValue == "0" -> false
                    else -> false
                }
            }
            else -> {
                `in`.skipValue()
                return false
            }
        }
    }
}

