package com.mannanhosen.gymflow.domain.repository

import com.mannanhosen.gymflow.core.session.UserSession
import com.mannanhosen.gymflow.core.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Current persisted session (null when signed out). */
    fun session(): Flow<UserSession?>

    suspend fun signIn(email: String, password: String): NetworkResult<UserSession>

    suspend fun signUp(fullName: String, email: String, password: String): NetworkResult<UserSession>

    suspend fun signOut()

    /** Rotate the access token using the stored refresh token. */
    suspend fun refreshSession(): NetworkResult<UserSession>
}