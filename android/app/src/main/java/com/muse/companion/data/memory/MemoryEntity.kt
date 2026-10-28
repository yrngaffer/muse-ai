package com.muse.companion.data.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val layer: MemoryLayer,
    /** Plaintext, or ciphertext when [encrypted] is true (see MemoryRepository). */
    val content: String,
    val encrypted: Boolean = false,
    val topic: String = "",
    val source: String = "",
    val confidence: Float = 1.0f,
    val verification: VerificationStatus = VerificationStatus.ASSUMED,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val reinforcement: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long,
)
