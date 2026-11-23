package com.mannanhosen.gymflow.data.remote.mapper

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Small, safe JSON accessors shared by repository implementations. */

internal fun JsonElement.asArrayOrEmpty(): JsonArray =
    this as? JsonArray ?: JsonArray(emptyList())

internal fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.content

internal fun JsonObject.boolean(key: String, default: Boolean = false): Boolean {
    val primitive = this[key] as? JsonPrimitive ?: return default
    return primitive.content.toBooleanStrictOrNull() ?: default
}

internal fun JsonObject.double(key: String, default: Double = 0.0): Double {
    val primitive = this[key] as? JsonPrimitive ?: return default
    return primitive.content.toDoubleOrNull() ?: default
}