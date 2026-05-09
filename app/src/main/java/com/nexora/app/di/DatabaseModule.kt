package com.nexora.app.di

import android.content.Context
import androidx.room.Room
import com.nexora.app.data.local.NexoraDao
import com.nexora.app.data.local.NexoraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNexoraDatabase(
        @ApplicationContext context: Context
    ): NexoraDatabase {
        return Room.databaseBuilder(
            context,
            NexoraDatabase::class.java,
            NexoraDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideNexoraDao(database: NexoraDatabase): NexoraDao {
        return database.nexoraDao
    }
}
