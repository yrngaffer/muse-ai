package com.muse.companion.data.memory

import com.muse.companion.core.Clock
import com.muse.companion.security.crypto.SecretBox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The only path to memory. SENSITIVE content is encrypted at rest with the Keystore-backed
 * [SecretBox] and transparently decrypted on read. Deleting removes the row entirely — and,
 * because search is a plain DB query (no separate vector index in M1), that also removes it
 * from the "search index".
 */
class MemoryRepository(
    private val dao: MemoryDao,
    private val secretBox: SecretBox,
    private val clock: Clock = Clock.SYSTEM,
) {
    val all: Flow<List<MemoryUi>> = dao.observeAll().map { list -> list.map { it.toUi() } }

    suspend fun remember(
        content: String,
        layer: MemoryLayer,
        topic: String = "",
        source: String = "user",
        verification: VerificationStatus = VerificationStatus.ASSUMED,
        sensitivity: Sensitivity = Sensitivity.NORMAL,
        confidence: Float = 1.0f,
    ): Long {
        val now = clock.nowMillis()
        val encrypt = sensitivity == Sensitivity.SENSITIVE
        val stored = if (encrypt) secretBox.sealString(content) else content
        return dao.insert(
            MemoryEntity(
                layer = layer,
                content = stored,
                encrypted = encrypt,
                topic = topic,
                source = source,
                confidence = confidence,
                verification = verification,
                sensitivity = sensitivity,
                createdAt = now,
                updatedAt = now,
                lastAccessedAt = now,
            ),
        )
    }

    suspend fun search(query: String, limit: Int = 8): List<MemoryUi> =
        dao.search(query, limit).map { it.toUi() }

    suspend fun recent(limit: Int = 12): List<MemoryUi> = dao.recent(limit).map { it.toUi() }

    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun clearAll() = dao.clear()
    suspend fun count(): Int = dao.count()
    suspend fun reinforce(id: Long) = dao.reinforce(id, clock.nowMillis())

    private fun MemoryEntity.toUi(): MemoryUi {
        val text = if (encrypted) {
            runCatching { secretBox.openString(content) }.getOrElse { "<decryption failed>" }
        } else {
            content
        }
        return MemoryUi(
            id = id, layer = layer, content = text, topic = topic, source = source,
            confidence = confidence, verification = verification, sensitivity = sensitivity,
            reinforcement = reinforcement, createdAt = createdAt, updatedAt = updatedAt,
        )
    }
}

/** Decrypted, UI-facing view of a memory. */
data class MemoryUi(
    val id: Long,
    val layer: MemoryLayer,
    val content: String,
    val topic: String,
    val source: String,
    val confidence: Float,
    val verification: VerificationStatus,
    val sensitivity: Sensitivity,
    val reinforcement: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
