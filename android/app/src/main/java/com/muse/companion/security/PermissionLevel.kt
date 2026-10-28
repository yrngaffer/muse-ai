package com.muse.companion.security

/** Tool risk tiers (see docs/08-security-architecture.md §C). */
enum class PermissionLevel {
    /** Level 1 — read-only, no side effects. */
    READ_ONLY,

    /** Level 2 — reversible side effects (open app, draft, organize a workspace). */
    REVERSIBLE,

    /** Level 3 — sensitive/destructive (delete, send, share private data, financial). Always confirmed. */
    SENSITIVE,
}
