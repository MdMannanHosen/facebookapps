package com.mannanhosen.gymflow.data.repository

import com.mannanhosen.gymflow.core.session.SessionManager
import com.mannanhosen.gymflow.core.session.UserSession
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.SupabaseAuthApi
import com.mannanhosen.gymflow.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override fun session(): Flow<UserSession?> = sessionManager.sessionFlow

    override suspend fun signIn(email: String, password: String): NetworkResult<UserSession> {
        return when (val result = authApi.signIn(email, password)) {
            is NetworkResult.Failure -> result
            is NetworkResult.Success -> readPersistedSession()
        }
    }

    override suspend fun signUp(
        fullName: String,
        email: String,
        password: String
    ): NetworkResult<UserSession> {
        return when (val result = authApi.signUp(fullName, email, password)) {
            is NetworkResult.Failure -> result
            is NetworkResult.Success -> readPersistedSession()
        }
    }

    override suspend fun signOut() = authApi.signOut()

    override suspend fun refreshSession(): NetworkResult<UserSession> {
        val refreshToken = sessionManager.current()?.refreshToken
            ?: return NetworkResult.failure(AppError.Unauthorized("No session"))
        return when (val result = authApi.refresh(refreshToken)) {
            is NetworkResult.Failure -> result
            is NetworkResult.Success -> readPersistedSession()
        }
    }

    private suspend fun readPersistedSession(): NetworkResult<UserSession> {
        val session = sessionManager.current()
            ?: return NetworkResult.failure(AppError.Unknown("Session not persisted"))
        return NetworkResult.success(session)
    }
}