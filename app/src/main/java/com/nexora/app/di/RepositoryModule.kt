package com.nexora.app.di

import android.content.Context
import com.nexora.app.data.remote.AuthApi
import com.nexora.app.data.remote.PoolApi
import com.nexora.app.data.repository.AuthRepositoryImpl
import com.nexora.app.data.repository.DiscoveryRepositoryImpl
import com.nexora.app.data.repository.PoolRepositoryImpl
import com.nexora.app.domain.repository.AuthRepository
import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.domain.repository.PoolRepository
import com.nexora.app.core.network.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDiscoveryRepository(@ApplicationContext context: Context): DiscoveryRepository {
        return DiscoveryRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepositoryImpl(authApi, tokenManager)
    }

    @Provides
    @Singleton
    fun providePoolRepository(
        poolApi: PoolApi
    ): PoolRepository {
        return PoolRepositoryImpl(poolApi)
    }
}
