package com.muse.companion.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Zero-model offline engine. Lets Muse run and be tested with no model installed. It is
 * deterministic, fully on-device, and never touches the network. It does NOT pretend to be an
 * LLM — it gives short, persona-flavored acknowledgements and nudges you to install a local model
 * (Model Manager) or enable the opt-in remote engine for real generation. Streams word-by-word so
 * the streaming + cancellation path is exercised exactly like a real engine.
 */
class OfflineRuleEngine : InferenceEngine {
    override val id = "offline"
    override val displayName = "Offline (no model)"
    override val runsOnDevice = true
    override val requiresNetwork = false
    override suspend fun isReady() = true

    override fun generate(messages: List<ChatMessage>, maxTokens: Int): Flow<String> = flow {
        val user = messages.lastOrNull { it.role == Role.USER }?.content?.trim().orEmpty()
        for (word in compose(user).split(" ")) {
            emit("$word ")
            delay(12)
        }
    }

    private fun compose(user: String): String {
        val u = user.lowercase()
        return when {
            user.isEmpty() ->
                "I'm here. No language model is loaded yet, so I'm in offline fallback mode — open Model Manager to install one."
            u.startsWith("hi") || u.startsWith("hey") || u.contains("hello") ->
                "Hey. I'm running offline with no model behind me, so this is the lightweight fallback. Install a local model and I'll actually think."
            u.contains("who are you") || u.contains("your name") ->
                "I'm Muse. Right now I'm in offline fallback mode — no model loaded. Load one from Model Manager and I'll be myself properly."
            else ->
                "Heard you: \"${user.take(200)}\". I can't reason about that without a model — I'm in offline fallback mode. Install a local model (Model Manager) or enable the remote engine in Settings, then ask again."
        }
    }
}
