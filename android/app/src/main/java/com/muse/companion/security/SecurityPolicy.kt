package com.muse.companion.security

/** Immutable snapshot of the user's security configuration, read by the broker on every call. */
data class SecurityPolicy(
    val onlineToolsEnabled: Boolean = false,
    val autoAllowIrreversible: Boolean = false,
    val disabledToolIds: Set<String> = emptySet(),
    val killSwitchEngaged: Boolean = false,
) {
    fun isToolEnabled(id: String): Boolean = !killSwitchEngaged && id !in disabledToolIds
}
