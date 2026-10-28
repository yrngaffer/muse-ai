package com.muse.companion.data.memory

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert suspend fun insert(m: MemoryEntity): Long
    @Update suspend fun update(m: MemoryEntity)
    @Delete suspend fun delete(m: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clear()

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE layer = :layer ORDER BY updatedAt DESC")
    suspend fun byLayer(layer: MemoryLayer): List<MemoryEntity>

    @Query(
        "SELECT * FROM memories WHERE encrypted = 0 AND (content LIKE '%' || :q || '%' OR topic LIKE '%' || :q || '%') " +
            "ORDER BY reinforcement DESC, updatedAt DESC LIMIT :limit",
    )
    suspend fun search(q: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY reinforcement DESC, updatedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Query("UPDATE memories SET reinforcement = reinforcement + 1, lastAccessedAt = :t WHERE id = :id")
    suspend fun reinforce(id: Long, t: Long)
}
