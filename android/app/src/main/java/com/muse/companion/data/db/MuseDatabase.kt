package com.muse.companion.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.muse.companion.data.audit.AuditDao
import com.muse.companion.data.audit.AuditEntity
import com.muse.companion.data.memory.MemoryDao
import com.muse.companion.data.memory.MemoryEntity

@Database(
    entities = [MemoryEntity::class, AuditEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MuseDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun auditDao(): AuditDao
}
