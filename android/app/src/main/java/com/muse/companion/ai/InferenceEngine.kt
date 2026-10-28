package com.muse.companion.ai

import kotlinx.coroutines.flow.Flow

/**
 * Pluggable model backend. The rest of the app depends ONLY on this interface, so the engine can
 * be swapped (offline fallback ↔ on-device MediaPipe/llama.cpp ↔ opt-in remote) without changes.
 */
interface InferenceEngine {
    val id: String
    val displayName: String
    val runsOnDevice: Boolean
    val requiresNetwork: Boolean

    suspend fun isReady(): Boolean

    /** Streams response chunks. Cancelling the collecting coroutine cancels generation. */
    fun generate(messages: List<ChatMessage>, maxTokens: Int = 512): Flow<String>
}
