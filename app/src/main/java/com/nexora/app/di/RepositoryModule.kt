package com.nexora.app.di

import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.data.repository.DiscoveryRepositoryImpl
import android.content.Context
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
}
