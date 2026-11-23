package com.mannanhosen.gymflow.core.di

import com.mannanhosen.gymflow.core.network.HttpClientProvider
import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.session.SessionManager
import com.mannanhosen.gymflow.data.remote.SupabaseAuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = HttpClientProvider.create()

    @Provides
    @Singleton
    fun provideSupabaseAuthApi(
        client: SupabaseClient,
        sessionManager: SessionManager
    ): SupabaseAuthApi = SupabaseAuthApi(client, sessionManager)
}