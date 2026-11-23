package com.mannanhosen.gymflow.core.di

import com.mannanhosen.gymflow.data.repository.AuthRepositoryImpl
import com.mannanhosen.gymflow.data.repository.DashboardRepositoryImpl
import com.mannanhosen.gymflow.data.repository.MemberRepositoryImpl
import com.mannanhosen.gymflow.data.repository.MembershipRepositoryImpl
import com.mannanhosen.gymflow.data.repository.NotificationRepositoryImpl
import com.mannanhosen.gymflow.data.repository.PaymentRepositoryImpl
import com.mannanhosen.gymflow.domain.repository.AuthRepository
import com.mannanhosen.gymflow.domain.repository.DashboardRepository
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import com.mannanhosen.gymflow.domain.repository.NotificationRepository
import com.mannanhosen.gymflow.domain.repository.PaymentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMemberRepository(impl: MemberRepositoryImpl): MemberRepository

    @Binds
    @Singleton
    abstract fun bindMembershipRepository(impl: MembershipRepositoryImpl): MembershipRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}