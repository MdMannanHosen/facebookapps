package com.mannanhosen.gymflow.core.network

import com.mannanhosen.gymflow.core.session.SessionManager
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.IoDispatcher
import com.mannanhosen.gymflow.core.util.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, explicit Supabase REST client built on OkHttp + kotlinx.serialization.
 *
 * Responsibilities:
 * - Attach the anon key and (when signed in) the user's bearer token.
 * - Execute suspending HTTP calls off the main thread.
 * - Map failures into typed [AppError]s without leaking sensitive payloads.
 *
 * RLS on the server enforces per-role and per-member data access.
 */
@Singleton
class SupabaseClient @Inject constructor(
    private val http: OkHttpClient,
    private val sessionManager: SessionManager,
    @IoDispatcher private val io: CoroutineDispatcher
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Performs a GET request.
     *
     * @param accessToken optional bearer token override. Pass the freshly issued
     *   token when the persisted session is not yet available (e.g. immediately
     *   after sign-in, when the profile must be read to resolve the role).
     */
    suspend fun get(
        path: String,
        query: Map<String, String> = emptyMap(),
        prefer: String? = null,
        accessToken: String? = null
    ): NetworkResult<String> =
        execute(prefer, accessToken) {
            Request.Builder()
                .url(buildUrl(path, query))
                .get()
                .build()
        }

    suspend fun post(
        path: String,
        body: JsonObject? = null,
        query: Map<String, String> = emptyMap(),
        prefer: String? = null,
        accessToken: String? = null
    ): NetworkResult<String> =
        execute(prefer, accessToken) {
            Request.Builder()
                .url(buildUrl(path, query))
                .post(body.toRequestBody(jsonMediaType))
                .build()
        }

    suspend fun patch(
        path: String,
        body: JsonObject,
        query: Map<String, String> = emptyMap(),
        prefer: String? = null,
        accessToken: String? = null
    ): NetworkResult<String> =
        execute(prefer, accessToken) {
            Request.Builder()
                .url(buildUrl(path, query))
                .patch(body.toRequestBody(jsonMediaType))
                .build()
        }

    suspend fun delete(
        path: String,
        query: Map<String, String> = emptyMap(),
        prefer: String? = null,
        accessToken: String? = null
    ): NetworkResult<String> =
        execute(prefer, accessToken) {
            Request.Builder()
                .url(buildUrl(path, query))
                .delete()
                .build()
        }

    private fun JsonObject?.toRequestBody(mediaType: okhttp3.MediaType) =
        (this?.toString() ?: "{}").toRequestBody(mediaType)

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val base = SupabaseConfig.URL.trimEnd('/')
        val normalized = if (path.startsWith("/")) path else "/$path"
        val builder = StringBuilder(base).append(normalized)
        if (query.isNotEmpty()) {
            builder.append('?')
            query.entries.forEachIndexed { index, (key, value) ->
                if (index > 0) builder.append('&')
                builder.append(key).append('=').append(java.net.URLEncoder.encode(value, "UTF-8"))
            }
        }
        return builder.toString()
    }

    private suspend fun execute(
        prefer: String?,
        accessToken: String?,
        requestBuilder: () -> Request
    ): NetworkResult<String> =
        withContext(io) {
            try {
                // Explicit override wins (used right after sign-in, before the
                // session is persisted); otherwise use the stored session token.
                val token = accessToken?.takeIf { it.isNotBlank() }
                    ?: sessionManager.sessionFlow.first()?.accessToken
                val request = requestBuilder()
                    .newBuilder()
                    .header("apikey", SupabaseConfig.ANON_KEY)
                    .header("Accept", "application/json")
                    .apply {
                        if (!prefer.isNullOrBlank()) header("Prefer", prefer)
                        if (!token.isNullOrBlank()) {
                            header("Authorization", "Bearer $token")
                        }
                    }
                    .build()

                http.newCall(request).execute().use { response ->
                    parseResponse(response)
                }
            } catch (e: SocketTimeoutException) {
                NetworkResult.Failure(AppError.Timeout("Request timed out", e))
            } catch (e: IOException) {
                NetworkResult.Failure(AppError.Network("Network error", e))
            } catch (e: Exception) {
                NetworkResult.Failure(AppError.Unknown("Unexpected error", e))
            }
        }

    private fun parseResponse(response: Response): NetworkResult<String> {
        val body = response.body?.string().orEmpty()
        return when (response.code) {
            in 200..299 -> NetworkResult.Success(body)
            400, 422 -> NetworkResult.Failure(AppError.Validation(extractMessage(body, "Invalid request")))
            401 -> NetworkResult.Failure(AppError.Unauthorized(extractMessage(body, "Unauthorized")))
            403 -> NetworkResult.Failure(AppError.Forbidden(extractMessage(body, "Forbidden")))
            404 -> NetworkResult.Failure(AppError.NotFound(extractMessage(body, "Not found")))
            in 500..599 -> NetworkResult.Failure(AppError.Server(extractMessage(body, "Server error")))
            else -> NetworkResult.Failure(AppError.Unknown("HTTP ${response.code}"))
        }
    }

    /** Extract a human message from a Supabase/PostgREST error envelope. */
    private fun extractMessage(body: String, fallback: String): String {
        if (body.isBlank()) return fallback
        return runCatching {
            val json = SupabaseConfig.json.parseToJsonElement(body) as? JsonObject ?: return fallback
            (json["message"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: (json["error_description"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: fallback
        }.getOrDefault(fallback)
    }
}

/** OkHttp client factory shared by the Supabase client. */
object HttpClientProvider {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}