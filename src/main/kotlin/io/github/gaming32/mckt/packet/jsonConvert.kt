package io.github.gaming32.mckt.packet

import com.google.gson.JsonParser
import com.google.gson.JsonArray as JsonArrayGson
import com.google.gson.JsonElement as JsonElementGson
import com.google.gson.JsonNull as JsonNullGson
import com.google.gson.JsonPrimitive as JsonPrimitiveGson
import com.google.gson.JsonObject as JsonObjectGson
import kotlinx.serialization.json.JsonArray as JsonArrayKotlinx
import kotlinx.serialization.json.JsonElement as JsonElementKotlinx
import kotlinx.serialization.json.JsonNull as JsonNullKotlinx
import kotlinx.serialization.json.JsonPrimitive as JsonPrimitiveKotlinx
import kotlinx.serialization.json.JsonObject as JsonObjectKotlinx

fun JsonElementGson.toKotlinx(): JsonElementKotlinx = when (this) {
    is JsonNullGson -> JsonNullKotlinx
    is JsonPrimitiveGson -> toKotlinx()
    is JsonArrayGson -> toKotlinx()
    is JsonObjectGson -> toKotlinx()
    else -> throw AssertionError()
}

fun JsonNullGson.toKotlinx() = JsonNullKotlinx

fun JsonPrimitiveGson.toKotlinx() = when {
    isBoolean -> JsonPrimitiveKotlinx(asBoolean)
    isNumber -> JsonPrimitiveKotlinx(asNumber)
    else -> JsonPrimitiveKotlinx(asString)
}

fun JsonArrayGson.toKotlinx() = JsonArrayKotlinx(map { it.toKotlinx() })

fun JsonObjectGson.toKotlinx() = JsonObjectKotlinx(entrySet().associate { it.key to it.value.toKotlinx() })

fun JsonElementKotlinx.toGson(): JsonElementGson = when (this) {
    JsonNullKotlinx -> JsonNullGson.INSTANCE
    is JsonPrimitiveKotlinx -> toGson()
    is JsonArrayKotlinx -> toGson()
    is JsonObjectKotlinx -> toGson()
    else -> throw AssertionError()
}

private val JSON_PARSER = JsonParser()

fun JsonNullKotlinx.toGson() = JsonNullGson.INSTANCE!!

fun JsonPrimitiveKotlinx.toGson() = if (isString) {
    JsonPrimitiveGson(content)
} else {
    JSON_PARSER.parse(content)!!
}

fun JsonArrayKotlinx.toGson() = JsonArrayGson().also { array -> forEach { array.add(it.toGson()) } }

fun JsonObjectKotlinx.toGson() = JsonObjectGson().also { obj -> entries.forEach { obj.add(it.key, it.value.toGson()) } }
