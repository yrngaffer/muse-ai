package com.muse.companion.agent

/** Streamed events from one assistant turn. */
sealed interface AgentEvent {
    data class Token(val text: String) : AgentEvent
    data class ToolNeedsConfirmation(val toolId: String, val args: Map<String, Any>, val reason: String) : AgentEvent
    data class ToolResultEvent(val toolId: String, val success: Boolean, val output: String) : AgentEvent
    data class Denied(val toolId: String, val reason: String) : AgentEvent
    data class Error(val message: String) : AgentEvent
    data object Done : AgentEvent
}
