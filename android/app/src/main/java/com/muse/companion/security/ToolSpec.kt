package com.muse.companion.security

/** Declared type of a tool argument. Drives validation in the PermissionBroker. */
enum class ArgType { STRING, INT, BOOL, URL, PATH }

/** One declared argument of a tool. */
data class ArgSpec(
    val name: String,
    val type: ArgType,
    val required: Boolean,
    val description: String,
    val maxLength: Int = 4_000,
)

/**
 * The immutable contract the broker validates every call against. Defined in code per tool —
 * the AI never sees or edits it, and no external/tool content can create or widen one.
 */
data class ToolSpec(
    val id: String,
    val description: String,
    val permission: PermissionLevel,
    val requiresInternet: Boolean,
    val reversible: Boolean,
    val requiresConfirmation: Boolean,
    val timeoutMs: Long,
    val maxOutputBytes: Int,
    val args: List<ArgSpec>,
    val scope: ToolScope = ToolScope.NONE,
)

/** Resource scope a tool is confined to. Interpreted by the guards. */
sealed interface ToolScope {
    data object NONE : ToolScope
    data class Hosts(val allowedSuffixes: List<String>) : ToolScope
    data class Paths(val allowedRoots: List<String>) : ToolScope
}
