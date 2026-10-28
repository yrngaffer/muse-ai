package com.muse.companion.security

import com.muse.companion.core.Clock
import com.muse.companion.security.audit.AuditEntry
import com.muse.companion.security.audit.AuditLog
import com.muse.companion.security.guards.PathGuard
import com.muse.companion.security.guards.UrlGuard

/**
 * The zero-trust gate. EVERY proposed tool call passes through [validate].
 *
 * The broker trusts only the registered [ToolSpec] table, the user's [SecurityPolicy], and the
 * guards — never the model's assertion that a call is safe. External/tool content cannot register
 * tools, widen a scope, or grant a permission; those come only from code + user settings. This is
 * the single most important security boundary in Muse, and it is enforced here in code (and tests),
 * not by prompting.
 */
class PermissionBroker(
    private val specs: Map<String, ToolSpec>,
    private val policy: SecurityPolicy,
    private val urlGuard: UrlGuard,
    private val pathGuard: PathGuard,
    private val audit: AuditLog,
    private val clock: Clock = Clock.SYSTEM,
) {
    fun validate(toolId: String, rawArgs: Map<String, String>): BrokerDecision {
        if (policy.killSwitchEngaged) {
            return deny(toolId, "kill switch engaged — all automation is stopped")
        }

        val spec = specs[toolId] ?: return deny(toolId, "unknown tool '$toolId'")

        if (spec.requiresInternet && !policy.onlineToolsEnabled) {
            return deny(toolId, "online tools are disabled")
        }
        if (!policy.isToolEnabled(toolId)) {
            return deny(toolId, "tool '$toolId' is disabled in settings")
        }

        // Reject any argument not declared by the spec (strict schema).
        val known = spec.args.associateBy { it.name }
        for (k in rawArgs.keys) {
            if (k !in known) return deny(toolId, "unexpected argument '$k'")
        }

        // Validate and type each declared argument.
        val typed = HashMap<String, Any>()
        for (arg in spec.args) {
            val raw = rawArgs[arg.name]
            if (raw.isNullOrBlank()) {
                if (arg.required) return deny(toolId, "missing required argument '${arg.name}'")
                else continue
            }
            if (raw.length > arg.maxLength) {
                return deny(toolId, "argument '${arg.name}' exceeds ${arg.maxLength} chars")
            }
            val value: Any = when (arg.type) {
                ArgType.STRING -> raw
                ArgType.INT -> raw.toIntOrNull() ?: return deny(toolId, "'${arg.name}' is not an integer")
                ArgType.BOOL -> raw.toBooleanStrictOrNull() ?: return deny(toolId, "'${arg.name}' is not a boolean")
                ArgType.URL -> when (val r = urlGuard.check(raw, (spec.scope as? ToolScope.Hosts)?.allowedSuffixes)) {
                    is UrlGuard.Result.Ok -> r.url.toString()
                    is UrlGuard.Result.Blocked -> return deny(toolId, "url blocked: ${r.reason}")
                }
                ArgType.PATH -> when (val r = pathGuard.check(raw, (spec.scope as? ToolScope.Paths)?.allowedRoots ?: emptyList())) {
                    is PathGuard.Result.Ok -> r.canonicalPath
                    is PathGuard.Result.Blocked -> return deny(toolId, "path blocked: ${r.reason}")
                }
            }
            typed[arg.name] = value
        }

        val mustConfirm = spec.requiresConfirmation ||
            spec.permission == PermissionLevel.SENSITIVE ||
            (!spec.reversible && !policy.autoAllowIrreversible)

        return if (mustConfirm) {
            audit.record(AuditEntry(clock.nowMillis(), toolId, "NEEDS_CONFIRMATION", "args=${typed.keys}"))
            BrokerDecision.NeedsConfirmation(typed, confirmReason(spec))
        } else {
            audit.record(AuditEntry(clock.nowMillis(), toolId, "ALLOW", "args=${typed.keys}"))
            BrokerDecision.Allow(typed)
        }
    }

    private fun confirmReason(spec: ToolSpec): String = when {
        spec.permission == PermissionLevel.SENSITIVE -> "This is a sensitive or destructive action."
        !spec.reversible -> "This action may not be reversible."
        else -> "Confirmation required."
    }

    private fun deny(toolId: String, reason: String): BrokerDecision.Deny {
        audit.record(AuditEntry(clock.nowMillis(), toolId, "DENY", reason))
        return BrokerDecision.Deny(reason)
    }
}
