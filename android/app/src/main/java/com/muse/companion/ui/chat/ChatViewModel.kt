package com.muse.companion.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.companion.agent.AgentEvent
import com.muse.companion.ai.ChatMessage
import com.muse.companion.ai.Role
import com.muse.companion.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(val role: Role, val text: String, val system: Boolean = false)

data class PendingTool(val toolId: String, val args: Map<String, Any>, val reason: String)

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val online: Boolean = false,
    val engineName: String = "Offline (no model)",
    val pendingConfirmation: PendingTool? = null,
)

class ChatViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState(online = container.connectivity.isOnline()))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val history = mutableListOf<ChatMessage>()
    private var job: Job? = null

    fun updateInput(s: String) = _state.update { it.copy(input = s) }

    fun onVoiceResult(text: String) = _state.update { it.copy(input = text) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.isGenerating) return
        _state.update { it.copy(input = "", isGenerating = true, messages = it.messages + UiMessage(Role.USER, text)) }
        history.add(ChatMessage(Role.USER, text))

        job = viewModelScope.launch {
            val policy = container.settings.securityPolicy.first()
            val settings = container.settings.settings.first()
            val personality = container.settings.personality.first()
            val memories = if (settings.memoryEnabled) container.memory.search(text) else emptyList()

            var assistantIdx = -1
            val sb = StringBuilder()
            _state.update { st ->
                assistantIdx = st.messages.size
                st.copy(messages = st.messages + UiMessage(Role.ASSISTANT, ""))
            }

            container.orchestrator(policy).respond(history.toList(), personality, memories).collect { ev ->
                when (ev) {
                    is AgentEvent.Token -> {
                        sb.append(ev.text)
                        updateAssistant(assistantIdx, sb.toString())
                    }
                    is AgentEvent.ToolResultEvent ->
                        appendSystem("🔧 ${ev.toolId}: ${if (ev.success) "ok" else "failed"} — ${ev.output.take(200)}")
                    is AgentEvent.Denied -> appendSystem("⛔ ${ev.toolId} denied: ${ev.reason}")
                    is AgentEvent.ToolNeedsConfirmation ->
                        _state.update { it.copy(pendingConfirmation = PendingTool(ev.toolId, ev.args, ev.reason)) }
                    is AgentEvent.Error -> appendSystem("⚠️ ${ev.message}")
                    AgentEvent.Done -> {}
                }
            }
            history.add(ChatMessage(Role.ASSISTANT, sb.toString()))
            _state.update { it.copy(isGenerating = false) }
        }
    }

    fun confirmPending(approve: Boolean) {
        val p = _state.value.pendingConfirmation ?: return
        _state.update { it.copy(pendingConfirmation = null) }
        if (!approve) {
            appendSystem("⛔ ${p.toolId} cancelled by you")
            return
        }
        viewModelScope.launch {
            val policy = container.settings.securityPolicy.first()
            val ev = container.orchestrator(policy).executeConfirmed(p.toolId, p.args)
            if (ev is AgentEvent.ToolResultEvent) {
                appendSystem("🔧 ${ev.toolId}: ${if (ev.success) "ok" else "failed"} — ${ev.output.take(200)}")
            }
        }
    }

    fun emergencyStop() {
        job?.cancel()
        _state.update { it.copy(isGenerating = false, pendingConfirmation = null) }
        appendSystem("🛑 Stopped. Queued actions cancelled.")
    }

    private fun updateAssistant(idx: Int, text: String) = _state.update { st ->
        val msgs = st.messages.toMutableList()
        if (idx in msgs.indices) msgs[idx] = msgs[idx].copy(text = text)
        st.copy(messages = msgs)
    }

    private fun appendSystem(text: String) =
        _state.update { it.copy(messages = it.messages + UiMessage(Role.ASSISTANT, text, system = true)) }
}
