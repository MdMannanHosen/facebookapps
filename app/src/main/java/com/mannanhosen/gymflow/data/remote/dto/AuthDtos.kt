package com.mannanhosen.gymflow.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase GoTrue auth responses. */

@Serializable
data class AuthUserDto(
    val id: String = "",
    val email: String = "",
    @SerialName("user_metadata") val userMetadata: UserMetadataDto? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserMetadataDto(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("phone") val phone: String? = null
)

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val user: AuthUserDto? = null
)

@Serializable
data class AuthErrorDto(
    @SerialName("error_description") val errorDescription: String? = null,
    @SerialName("msg") val msg: String? = null,
    val message: String? = null,
    val error: String? = null
) {
    fun displayMessage(fallback: String): String =
        errorDescription ?: msg ?: message ?: error ?: fallback
}