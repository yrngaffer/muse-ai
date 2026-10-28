package com.muse.companion.data.audit

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.muse.companion.security.audit.AuditEntry
import com.muse.companion.security.audit.AuditLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "audit")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val toolId: String,
    val decision: String,
    val detail: String,
)

@Dao
interface AuditDao {
    @Insert suspend fun insert(e: AuditEntity): Long

    @Query("SELECT * FROM audit ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AuditEntity>>

    @Query("SELECT COUNT(*) FROM audit")
    suspend fun count(): Int
}

/**
 * Append-only [AuditLog] backed by Room. [record] is fire-and-forget on a background scope so
 * callers (the synchronous broker) are never blocked. Only insert + read are exposed — there is
 * deliberately no update or delete, so the trail cannot be rewritten from within the app.
 */
class RoomAuditLog(
    private val dao: AuditDao,
    private val scope: CoroutineScope,
) : AuditLog {
    override fun record(entry: AuditEntry) {
        scope.launch(Dispatchers.IO) {
            dao.insert(
                AuditEntity(
                    timeMillis = entry.timeMillis,
                    toolId = entry.toolId,
                    decision = entry.decision,
                    detail = entry.detail,
                ),
            )
        }
    }
}
