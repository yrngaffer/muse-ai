package com.muse.companion.agent

import com.google.common.truth.Truth.assertThat
import com.muse.companion.FakeAuditLog
import com.muse.companion.FakeEngine
import com.muse.companion.FakeTool
import com.muse.companion.ai.ChatMessage
import com.muse.companion.ai.ContextBuilder
import com.muse.companion.ai.Role
import com.muse.companion.core.Clock
import com.muse.companion.data.settings.Personality
import com.muse.companion.security.PermissionBroker
import com.muse.companion.security.PermissionLevel
import com.muse.companion.security.SecurityPolicy
import com.muse.companion.security.ToolScope
import com.muse.companion.security.ToolSpec
import com.muse.companion.security.guards.PathGuard
import com.muse.companion.security.guards.UrlGuard
import com.muse.companion.tools.ToolRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OrchestratorTest {
    private val clock = Clock { 0L }

    private fun spec(id: String, perm: PermissionLevel = PermissionLevel.REVERSIBLE) =
        ToolSpec(id, "d", perm, false, true, false, 1_000, 1_000, emptyList(), ToolScope.NONE)

    @Test fun deniesDisabledToolEvenWhenModelRequestsIt() = runTest {
        val tool = FakeTool(spec("danger"))
        val registry = ToolRegistry(listOf(tool))
        val audit = FakeAuditLog()
        val broker = PermissionBroker(
            registry.specs, SecurityPolicy(disabledToolIds = setOf("danger")),
            UrlGuard(), PathGuard(), audit, clock,
        )
        val engine = FakeEngine(listOf("sure.\n[TOOL]\ntool: danger\n[/TOOL]"))
        val orch = Orchestrator(engine, broker, registry, ContextBuilder(), audit, clock)

        val events = orch.respond(listOf(ChatMessage(Role.USER, "do danger")), Personality(), emptyList()).toList()

        assertThat(events.any { it is AgentEvent.Denied }).isTrue()
        assertThat(tool.executed).isFalse()
    }

    @Test fun executesAllowedToolAndFollowsUp() = runTest {
        val tool = FakeTool(spec("open_url"))
        val registry = ToolRegistry(listOf(tool))
        val audit = FakeAuditLog()
        val broker = PermissionBroker(registry.specs, SecurityPolicy(), UrlGuard(), PathGuard(), audit, clock)
        val engine = FakeEngine(listOf("ok\n[TOOL]\ntool: open_url\n[/TOOL]", "done"))
        val orch = Orchestrator(engine, broker, registry, ContextBuilder(), audit, clock)

        val events = orch.respond(listOf(ChatMessage(Role.USER, "open")), Personality(), emptyList()).toList()

        assertThat(tool.executed).isTrue()
        assertThat(events.any { it is AgentEvent.ToolResultEvent }).isTrue()
    }
}
