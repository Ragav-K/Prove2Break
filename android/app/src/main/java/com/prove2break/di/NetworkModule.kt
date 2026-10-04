package com.prove2break.di

import com.prove2break.data.auth.AuthAuthenticator
import com.prove2break.data.auth.AuthInterceptor
import com.prove2break.data.remote.Prove2BreakApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks the "plain" OkHttpClient/Retrofit/ApiService: no [AuthInterceptor] and no
 * [AuthAuthenticator]. [AuthAuthenticator] depends on the [PlainClient]-qualified
 * [Prove2BreakApiService] to call `/api/auth/refresh` without recursing back into itself.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainClient

/**
 * Provides Retrofit/OkHttp per ANDROID-ARCHITECTURE.md. The main client attaches
 * [AuthInterceptor] (adds `Authorization: Bearer <token>`) and [AuthAuthenticator]
 * (refreshes on 401 and retries); base URL comes from ENVIRONMENT.md build config.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.prove2break.example.com/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    @Provides
    @Singleton
    @PlainClient
    fun providePlainOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(logging).build()

    @Provides
    @Singleton
    @PlainClient
    fun providePlainRetrofit(@PlainClient okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @PlainClient
    fun providePlainApiService(@PlainClient retrofit: Retrofit): Prove2BreakApiService =
        retrofit.create(Prove2BreakApiService::class.java)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authAuthenticator: AuthAuthenticator,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authAuthenticator)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): Prove2BreakApiService {
        return retrofit.create(Prove2BreakApiService::class.java)
    }
}
