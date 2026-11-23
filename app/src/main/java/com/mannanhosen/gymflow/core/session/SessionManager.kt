package com.mannanhosen.gymflow.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "gymflow_session")

/**
 * Persists the authenticated session using DataStore preferences.
 * No passwords are stored. Only the Supabase JWT and profile basics.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val FULL_NAME = stringPreferencesKey("full_name")
        val ROLE = stringPreferencesKey("role")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = stringPreferencesKey("expires_at")
    }

    val sessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID] ?: return@map null
        val accessToken = prefs[Keys.ACCESS_TOKEN] ?: return@map null
        UserSession(
            userId = userId,
            email = prefs[Keys.EMAIL].orEmpty(),
            fullName = prefs[Keys.FULL_NAME].orEmpty(),
            role = prefs[Keys.ROLE].orEmpty(),
            accessToken = accessToken,
            refreshToken = prefs[Keys.REFRESH_TOKEN].orEmpty(),
            expiresAtEpochSeconds = prefs[Keys.EXPIRES_AT]?.toLongOrNull() ?: 0L
        )
    }

    suspend fun current(): UserSession? = sessionFlow.first()

    suspend fun accessToken(): String? = sessionFlow.first()?.accessToken

    suspend fun save(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.EMAIL] = session.email
            prefs[Keys.FULL_NAME] = session.fullName
            prefs[Keys.ROLE] = session.role
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
            prefs[Keys.EXPIRES_AT] = session.expiresAtEpochSeconds.toString()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}