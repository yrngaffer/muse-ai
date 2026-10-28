package com.muse.companion.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.companion.data.settings.MuseSettings
import com.muse.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityViewModel(private val container: AppContainer) : ViewModel() {
    val audit = container.auditDao.observeRecent(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_SETTINGS)

    val toolIds: List<String> = container.toolRegistry.ids()

    fun setKill(v: Boolean) = viewModelScope.launch { container.settings.setKillSwitch(v) }
    fun setToolDisabled(id: String, disabled: Boolean) = viewModelScope.launch { container.settings.setToolDisabled(id, disabled) }

    companion object {
        private val DEFAULT_SETTINGS = MuseSettings(
            onlineToolsEnabled = false,
            remoteEngineEnabled = false,
            memoryEnabled = true,
            autoAllowIrreversible = false,
            killSwitchEngaged = false,
            disabledToolIds = emptySet(),
            episodicRetention = 500,
        )
    }
}
