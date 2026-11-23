package com.mannanhosen.gymflow.data.remote

import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Shared JSON helpers for repository implementations. */

@Suppress("UNCHECKED_CAST")
internal fun <T> NetworkResult<String>.parseJson(mapper: (JsonElement) -> T?): NetworkResult<T> {
    return when (this) {
        is NetworkResult.Success -> {
            runCatching {
                val element = SupabaseConfig.json.parseToJsonElement(data.ifBlank { "null" })
                mapper(element) ?: error("Empty or unexpected response")
            }.fold(
                onSuccess = { NetworkResult.Success(it) },
                onFailure = { NetworkResult.Failure(AppError.Unknown("Failed to parse response", it)) }
            )
        }
        is NetworkResult.Failure -> NetworkResult.Failure(error)
    }
}

/** Interpret a PostgREST array response and return its first element. */
internal fun <T> NetworkResult<String>.parseFirst(mapper: (JsonElement) -> T?): NetworkResult<T> =
    parseJson { element ->
        val array = element as? JsonArray
        when {
            array == null -> mapper(element)
            array.isEmpty() -> null
            else -> mapper(array.first())
        }
    }

internal fun JsonElement.asJsonArray(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())

internal fun JsonObject.require(key: String): JsonElement =
    this[key] ?: error("Missing key: $key")