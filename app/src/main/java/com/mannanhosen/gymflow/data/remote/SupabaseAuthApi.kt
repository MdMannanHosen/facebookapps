package com.mannanhosen.gymflow.data.remote

import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.core.session.SessionManager
import com.mannanhosen.gymflow.core.session.UserSession
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.dto.AuthResponseDto
import com.mannanhosen.gymflow.data.remote.dto.ProfileDto
import com.mannanhosen.gymflow.data.remote.mapper.asArrayOrEmpty
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Authenticates against Supabase (GoTrue) and maintains the local session.
 * Credentials never leave the device except in the auth request itself.
 */
class SupabaseAuthApi(
    private val client: SupabaseClient,
    private val sessionManager: SessionManager
) {

    suspend fun signIn(email: String, password: String): NetworkResult<String> {
        val response = client.post(
            path = "/auth/v1/token",
            query = mapOf("grant_type" to "password"),
            body = buildJsonObject {
                put("email", email)
                put("password", password)
            }
        )
        return when (response) {
            is NetworkResult.Failure -> response
            is NetworkResult.Success -> decodeSession(response.data)
        }
    }

    suspend fun signUp(fullName: String, email: String, password: String): NetworkResult<String> {
        val response = client.post(
            path = "/auth/v1/signup",
            body = buildJsonObject {
                put("email", email)
                put("password", password)
                put("data", buildJsonObject { put("full_name", fullName) })
            }
        )
        return when (response) {
            is NetworkResult.Failure -> response
            is NetworkResult.Success -> decodeSession(response.data)
        }
    }

    suspend fun refresh(refreshToken: String): NetworkResult<String> {
        val response = client.post(
            path = "/auth/v1/token",
            query = mapOf("grant_type" to "refresh_token"),
            body = buildJsonObject { put("refresh_token", refreshToken) }
        )
        return when (response) {
            is NetworkResult.Failure -> response
            is NetworkResult.Success -> decodeSession(response.data)
        }
    }

    suspend fun signOut() {
        runCatching { client.post(path = "/auth/v1/logout") }
        sessionManager.clear()
    }

    private suspend fun decodeSession(body: String): NetworkResult<String> {
        val auth = runCatching {
            SupabaseConfig.json.decodeFromString(AuthResponseDto.serializer(), body)
        }.getOrElse {
            return NetworkResult.failure(AppError.Unknown("Invalid auth response", it))
        }

        val accessToken = auth.accessToken
            ?: return NetworkResult.failure(AppError.Unauthorized("Invalid credentials"))
        val userId = auth.user?.id.orEmpty()
        if (userId.isBlank()) {
            return NetworkResult.failure(AppError.Unknown("Missing user id"))
        }

        val email = auth.user?.email ?: sessionManager.current()?.email.orEmpty()
        val fullNameFromMeta = auth.user?.userMetadata?.fullName

        // Load the profile to obtain the authoritative role and name. The freshly
        // issued token is passed explicitly because the session is not persisted
        // yet; without it RLS would reject the read and the role would default to
        // MEMBER even for an owner.
        val profile = loadProfile(userId, accessToken)
        val role = profile?.role?.takeIf { it.isNotBlank() } ?: "MEMBER"
        val fullName = profile?.fullName?.takeIf { it.isNotBlank() }
            ?: fullNameFromMeta?.takeIf { it.isNotBlank() }
            ?: "Member"

        val expiresIn = auth.expiresIn ?: 3600L
        val expiresAt = (System.currentTimeMillis() / 1000) + expiresIn

        val session = UserSession(
            userId = userId,
            email = email,
            fullName = fullName,
            role = role,
            accessToken = accessToken,
            refreshToken = auth.refreshToken.orEmpty(),
            expiresAtEpochSeconds = expiresAt
        )
        sessionManager.save(session)
        return NetworkResult.success(body)
    }

    private suspend fun loadProfile(userId: String, accessToken: String): ProfileDto? {
        val result = client.get(
            path = "/profiles",
            query = mapOf("id" to "eq.$userId", "select" to "*", "limit" to "1"),
            accessToken = accessToken
        )
        val body = result.getOrNull() ?: return null
        return runCatching {
            val array = SupabaseConfig.json.parseToJsonElement(body).asArrayOrEmpty()
            val first = array.firstOrNull() as? JsonObject ?: return null
            SupabaseConfig.json.decodeFromJsonElement(ProfileDto.serializer(), first)
        }.getOrNull()
    }
}