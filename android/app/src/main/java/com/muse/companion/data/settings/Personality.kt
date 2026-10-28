package com.muse.companion.data.settings

import kotlinx.serialization.Serializable

/**
 * Configurable assistant personality. Defaults carry over the established web-Muse persona
 * (witty, direct, addresses the owner as "Red") — all of it user-editable in Settings.
 */
@Serializable
data class Personality(
    val assistantName: String = "Muse",
    val ownerNickname: String = "Red",
    val tone: String = "witty, direct, a little sarcastic, never mean; loyal but honest",
    val verbosity: Verbosity = Verbosity.BALANCED,
    val humor: Int = 70,       // 0..100
    val formality: Int = 20,   // 0..100
    val emojiUse: Boolean = false,
) {
    enum class Verbosity { TERSE, BALANCED, DETAILED }
}
