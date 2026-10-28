package com.muse.companion.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.companion.data.memory.MemoryLayer
import com.muse.companion.data.memory.VerificationStatus
import com.muse.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(private val container: AppContainer) : ViewModel() {
    val memories = container.memory.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(text: String, layer: MemoryLayer) = viewModelScope.launch {
        if (text.isNotBlank()) {
            container.memory.remember(
                content = text,
                layer = layer,
                source = "user",
                verification = VerificationStatus.USER_CONFIRMED,
            )
        }
    }

    fun delete(id: Long) = viewModelScope.launch { container.memory.deleteById(id) }
    fun clearAll() = viewModelScope.launch { container.memory.clearAll() }
}
