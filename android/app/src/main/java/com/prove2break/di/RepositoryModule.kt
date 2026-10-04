package com.prove2break.di

import com.prove2break.data.auth.AuthTokenStore
import com.prove2break.data.auth.TokenStore
import com.prove2break.data.repository.AnalyticsRepository
import com.prove2break.data.repository.AnalyticsRepositoryImpl
import com.prove2break.data.repository.AuthRepository
import com.prove2break.data.repository.AuthRepositoryImpl
import com.prove2break.data.repository.BlockedAppRepository
import com.prove2break.data.repository.BlockedAppRepositoryImpl
import com.prove2break.data.repository.BreakRepository
import com.prove2break.data.repository.BreakRepositoryImpl
import com.prove2break.data.repository.EmergencyUnlockRepository
import com.prove2break.data.repository.EmergencyUnlockRepositoryImpl
import com.prove2break.data.repository.FocusSessionRepository
import com.prove2break.data.repository.FocusSessionRepositoryImpl
import com.prove2break.data.repository.LocalAnalyticsRepository
import com.prove2break.data.repository.LocalAnalyticsRepositoryImpl
import com.prove2break.data.repository.MaterialRepository
import com.prove2break.data.repository.MaterialRepositoryImpl
import com.prove2break.data.repository.QuizRepository
import com.prove2break.data.repository.QuizRepositoryImpl
import com.prove2break.data.repository.SubjectRepository
import com.prove2break.data.repository.SubjectRepositoryImpl
import com.prove2break.data.repository.TopicRepository
import com.prove2break.data.repository.TopicRepositoryImpl
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
    abstract fun bindFocusSessionRepository(
        impl: FocusSessionRepositoryImpl,
    ): FocusSessionRepository

    @Binds
    @Singleton
    abstract fun bindBlockedAppRepository(
        impl: BlockedAppRepositoryImpl,
    ): BlockedAppRepository

    @Binds
    @Singleton
    abstract fun bindEmergencyUnlockRepository(
        impl: EmergencyUnlockRepositoryImpl,
    ): EmergencyUnlockRepository

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(impl: SubjectRepositoryImpl): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindMaterialRepository(impl: MaterialRepositoryImpl): MaterialRepository

    @Binds
    @Singleton
    abstract fun bindTopicRepository(impl: TopicRepositoryImpl): TopicRepository

    @Binds
    @Singleton
    abstract fun bindBreakRepository(impl: BreakRepositoryImpl): BreakRepository

    @Binds
    @Singleton
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: AuthTokenStore): TokenStore

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindLocalAnalyticsRepository(impl: LocalAnalyticsRepositoryImpl): LocalAnalyticsRepository
}
