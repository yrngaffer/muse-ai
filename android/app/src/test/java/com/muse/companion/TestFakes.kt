package com.muse.companion

import com.muse.companion.ai.ChatMessage
import com.muse.companion.ai.InferenceEngine
import com.muse.companion.security.ToolSpec
import com.muse.companion.security.audit.AuditEntry
import com.muse.companion.security.audit.AuditLog
import com.muse.companion.tools.Tool
import com.muse.companion.tools.ToolResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Collects audit entries in memory for assertions. */
class FakeAuditLog : AuditLog {
    val entries = mutableListOf<AuditEntry>()
    override fun record(entry: AuditEntry) {
        entries.add(entry)
    }
}

/** Emits a scripted response per generate() call (turn 1, follow-up turn 2, …). */
class FakeEngine(private val script: List<String>) : InferenceEngine {
    var calls = 0
    override val id = "fake"
    override val displayName = "Fake"
    override val runsOnDevice = true
    override val requiresNetwork = false
    override suspend fun isReady() = true
    override fun generate(messages: List<ChatMessage>, maxTokens: Int): Flow<String> = flow {
        val out = script.getOrElse(calls) { "" }
        calls++
        if (out.isNotEmpty()) emit(out)
    }
}

/** Records whether it was executed and with what args. */
class FakeTool(override val spec: ToolSpec) : Tool {
    var executed = false
    var lastArgs: Map<String, Any> = emptyMap()
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        executed = true
        lastArgs = args
        return ToolResult(spec.id, true, "ran")
    }
}
