package com.mannanhosen.gymflow.core.session

/**
 * The authenticated user's session state, persisted locally after login.
 * The access token is short-lived and used as the Supabase bearer token.
 */
data class UserSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val role: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() / 1000 >= expiresAtEpochSeconds
}