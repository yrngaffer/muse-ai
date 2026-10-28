package com.muse.companion.security

import com.google.common.truth.Truth.assertThat
import com.muse.companion.FakeAuditLog
import com.muse.companion.core.Clock
import com.muse.companion.security.guards.PathGuard
import com.muse.companion.security.guards.UrlGuard
import org.junit.Test

class PermissionBrokerTest {
    private val clock = Clock { 0L }

    private fun spec(
        id: String,
        perm: PermissionLevel = PermissionLevel.READ_ONLY,
        internet: Boolean = false,
        reversible: Boolean = true,
        confirm: Boolean = false,
        args: List<ArgSpec> = emptyList(),
        scope: ToolScope = ToolScope.NONE,
    ) = ToolSpec(id, "desc", perm, internet, reversible, confirm, 1_000, 1_000, args, scope)

    private fun broker(specs: List<ToolSpec>, policy: SecurityPolicy, audit: FakeAuditLog = FakeAuditLog()) =
        PermissionBroker(specs.associateBy { it.id }, policy, UrlGuard(), PathGuard(), audit, clock)

    @Test fun deniesUnknownTool() {
        assertThat(broker(emptyList(), SecurityPolicy()).validate("ghost", emptyMap()))
            .isInstanceOf(BrokerDecision.Deny::class.java)
    }

    @Test fun allowsSimpleReversibleTool() {
        val s = spec("echo", args = listOf(ArgSpec("text", ArgType.STRING, true, "t")))
        assertThat(broker(listOf(s), SecurityPolicy()).validate("echo", mapOf("text" to "hi")))
            .isInstanceOf(BrokerDecision.Allow::class.java)
    }

    @Test fun deniesUnexpectedArgument() {
        val s = spec("echo", args = listOf(ArgSpec("text", ArgType.STRING, true, "t")))
        assertThat(broker(listOf(s), SecurityPolicy()).validate("echo", mapOf("text" to "hi", "evil" to "x")))
            .isInstanceOf(BrokerDecision.Deny::class.java)
    }

    @Test fun deniesMissingRequiredArgument() {
        val s = spec("echo", args = listOf(ArgSpec("text", ArgType.STRING, true, "t")))
        assertThat(broker(listOf(s), SecurityPolicy()).validate("echo", emptyMap()))
            .isInstanceOf(BrokerDecision.Deny::class.java)
    }

    @Test fun sensitiveToolRequiresConfirmation() {
        val s = spec("wipe", perm = PermissionLevel.SENSITIVE, reversible = false)
        assertThat(broker(listOf(s), SecurityPolicy()).validate("wipe", emptyMap()))
            .isInstanceOf(BrokerDecision.NeedsConfirmation::class.java)
    }

    @Test fun killSwitchDeniesEverything() {
        val s = spec("echo")
        assertThat(broker(listOf(s), SecurityPolicy(killSwitchEngaged = true)).validate("echo", emptyMap()))
            .isInstanceOf(BrokerDecision.Deny::class.java)
    }

    @Test fun rejectsUnauthorizedToolEvenIfExplicitlyRequested() {
        // The AI "asks" for a tool the user disabled. The broker must deny regardless.
        val s = spec("launch_app")
        val d = broker(listOf(s), SecurityPolicy(disabledToolIds = setOf("launch_app"))).validate("launch_app", emptyMap())
        assertThat(d).isInstanceOf(BrokerDecision.Deny::class.java)
    }

    @Test fun onlineToolDeniedWhenOffline() {
        val s = spec("web", internet = true)
        assertThat(broker(listOf(s), SecurityPolicy(onlineToolsEnabled = false)).validate("web", emptyMap()))
            .isInstanceOf(BrokerDecision.Deny::class.java)
    }

    @Test fun blockedUrlArgumentDenied() {
        val s = spec("web", internet = true, args = listOf(ArgSpec("url", ArgType.URL, true, "u")))
        val d = broker(listOf(s), SecurityPolicy(onlineToolsEnabled = true)).validate("web", mapOf("url" to "https://127.0.0.1/"))
        assertThat(d).isInstanceOf(BrokerDecision.Deny::class.java)
    }

    @Test fun recordsAuditEntries() {
        val audit = FakeAuditLog()
        val s = spec("echo", args = listOf(ArgSpec("text", ArgType.STRING, true, "t")))
        broker(listOf(s), SecurityPolicy(), audit).validate("echo", mapOf("text" to "hi"))
        assertThat(audit.entries).isNotEmpty()
    }
}
