package com.muse.companion.security.audit

/** One row in the append-only audit trail. */
data class AuditEntry(
    val timeMillis: Long,
    val toolId: String,
    /** ALLOW | NEEDS_CONFIRMATION | DENY | EXECUTED | EXECUTED_CONFIRMED | FAILED | CANCELLED */
    val decision: String,
    val detail: String,
)

/** Append-only audit trail. Implementations must never mutate or delete prior entries. */
interface AuditLog {
    fun record(entry: AuditEntry)
}
