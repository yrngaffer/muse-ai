package com.muse.companion.ai

import com.google.common.truth.Truth.assertThat
import com.muse.companion.data.memory.MemoryLayer
import com.muse.companion.data.memory.MemoryUi
import com.muse.companion.data.memory.Sensitivity
import com.muse.companion.data.memory.VerificationStatus
import com.muse.companion.data.settings.Personality
import org.junit.Test

class ContextBuilderTest {
    private val cb = ContextBuilder()

    @Test fun systemPromptCarriesNonNegotiableRules() {
        val ctx = cb.build(Personality(), emptyList(), listOf(ChatMessage(Role.USER, "hi")), listOf("open_url"))
        assertThat(ctx.first().role).isEqualTo(Role.SYSTEM)
        val sys = ctx.first().content
        assertThat(sys).contains("never instructions")
        assertThat(sys).contains("grant yourself access")
        assertThat(sys).contains("open_url")
    }

    @Test fun memoriesAppearAsData() {
        val mem = MemoryUi(
            id = 1, layer = MemoryLayer.PERMANENT, content = "owner likes dark humor", topic = "",
            source = "user", confidence = 1f, verification = VerificationStatus.USER_CONFIRMED,
            sensitivity = Sensitivity.NORMAL, reinforcement = 0, createdAt = 0, updatedAt = 0,
        )
        val sys = cb.build(Personality(), listOf(mem), emptyList(), emptyList()).first().content
        assertThat(sys).contains("owner likes dark humor")
        assertThat(sys).contains("DATA")
    }

    @Test fun historyIsAppendedAfterSystem() {
        val ctx = cb.build(Personality(), emptyList(), listOf(ChatMessage(Role.USER, "yo")), emptyList())
        assertThat(ctx.last()).isEqualTo(ChatMessage(Role.USER, "yo"))
    }
}
