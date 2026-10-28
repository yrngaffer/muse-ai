package com.muse.companion.agent

import com.muse.companion.ai.ChatMessage
import com.muse.companion.ai.ContextBuilder
import com.muse.companion.ai.InferenceEngine
import com.muse.companion.ai.Role
import com.muse.companion.core.Clock
import com.muse.companion.data.memory.MemoryUi
import com.muse.companion.data.settings.Personality
import com.muse.companion.security.BrokerDecision
import com.muse.companion.security.PermissionBroker
import com.muse.companion.security.audit.AuditEntry
import com.muse.companion.security.audit.AuditLog
import com.muse.companion.security.guards.PromptInjectionSanitizer
import com.muse.companion.tools.ToolCallParser
import com.muse.companion.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * The agent loop, deliberately BOUNDED: one generation pass, then any tool calls the model
 * proposed are each checked by the broker, then (if tools ran) one follow-up pass that sees the
 * results as fenced DATA. No autonomous looping — the model cannot keep running tasks on its own.
 */
class Orchestrator(
    private val engine: InferenceEngine,
    private val broker: PermissionBroker,
    private val tools: ToolRegistry,
    private val contextBuilder: ContextBuilder,
    private val audit: AuditLog,
    private val clock: Clock = Clock.SYSTEM,
) {
    fun respond(
        history: List<ChatMessage>,
        personality: Personality,
        memories: List<MemoryUi>,
    ): Flow<AgentEvent> = flow {
        val ctx = contextBuilder.build(personality, memories, history, tools.ids())
        val sb = StringBuilder()
        try {
            engine.generate(ctx).collect { chunk ->
                sb.append(chunk)
                emit(AgentEvent.Token(chunk))
            }
        } catch (e: Exception) {
            emit(AgentEvent.Error(e.message ?: "generation failed"))
            emit(AgentEvent.Done)
            return@flow
        }

        val full = sb.toString()
        val executed = mutableListOf<ChatMessage>()
        for (call in ToolCallParser.parse(full)) {
            when (val d = broker.validate(call.toolId, call.args)) {
                is BrokerDecision.Deny -> emit(AgentEvent.Denied(call.toolId, d.reason))
                is BrokerDecision.NeedsConfirmation ->
                    emit(AgentEvent.ToolNeedsConfirmation(call.toolId, d.args, d.reason))
                is BrokerDecision.Allow -> {
                    val tool = tools.get(call.toolId)
                    if (tool == null) {
                        emit(AgentEvent.Denied(call.toolId, "tool missing"))
                        continue
                    }
                    val res = tool.execute(d.args)
                    audit.record(
                        AuditEntry(clock.nowMillis(), call.toolId, if (res.success) "EXECUTED" else "FAILED", res.output.take(120)),
                    )
                    emit(AgentEvent.ToolResultEvent(res.toolId, res.success, res.output))
                    executed.add(ChatMessage(Role.TOOL, PromptInjectionSanitizer.fence("tool:${res.toolId}", res.output)))
                }
            }
        }

        if (executed.isNotEmpty()) {
            val followHistory = history +
                ChatMessage(Role.ASSISTANT, ToolCallParser.stripToolBlocks(full)) +
                executed
            val follow = contextBuilder.build(personality, memories, followHistory, tools.ids())
            try {
                engine.generate(follow).collect { emit(AgentEvent.Token(it)) }
            } catch (e: Exception) {
                emit(AgentEvent.Error(e.message ?: "follow-up failed"))
            }
        }
        emit(AgentEvent.Done)
    }.flowOn(Dispatchers.IO)

    /** Runs a tool the user explicitly confirmed. Args were already broker-validated. */
    suspend fun executeConfirmed(toolId: String, args: Map<String, Any>): AgentEvent {
        val tool = tools.get(toolId) ?: return AgentEvent.Denied(toolId, "tool missing")
        val res = tool.execute(args)
        audit.record(
            AuditEntry(clock.nowMillis(), toolId, if (res.success) "EXECUTED_CONFIRMED" else "FAILED", res.output.take(120)),
        )
        return AgentEvent.ToolResultEvent(res.toolId, res.success, res.output)
    }
}
