package com.mannanhosen.gymflow.core.network

import com.mannanhosen.gymflow.BuildConfig
import kotlinx.serialization.json.Json

/**
 * Centralized Supabase configuration and JSON codec.
 *
 * Only the public anon key is shipped in the app. Row Level Security is the
 * authoritative authorization boundary; service-role keys never ship to clients.
 */
object SupabaseConfig {
    val URL: String = BuildConfig.SUPABASE_URL
    val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = false
        prettyPrint = false
    }
}