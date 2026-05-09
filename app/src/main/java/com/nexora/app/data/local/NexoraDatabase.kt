package com.nexora.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// Temporary empty entity to satisfy Room. Will replace with actual entities in Phase 2.
@Database(
    entities = [DummyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract val nexoraDao: NexoraDao

    companion object {
        const val DATABASE_NAME = "nexora_db"
    }
}

@androidx.room.Entity(tableName = "dummy_table")
data class DummyEntity(
    @androidx.room.PrimaryKey val id: Int = 0
)
