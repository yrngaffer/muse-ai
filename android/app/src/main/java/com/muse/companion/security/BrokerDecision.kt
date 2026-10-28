package com.muse.companion.security

/** The broker's verdict for one proposed tool call. */
sealed interface BrokerDecision {
    /** Allowed to execute immediately with these validated, typed args. */
    data class Allow(val args: Map<String, Any>) : BrokerDecision

    /** Allowed but requires explicit user confirmation first. */
    data class NeedsConfirmation(val args: Map<String, Any>, val reason: String) : BrokerDecision

    /** Rejected — the call never reaches a tool. */
    data class Deny(val reason: String) : BrokerDecision
}
