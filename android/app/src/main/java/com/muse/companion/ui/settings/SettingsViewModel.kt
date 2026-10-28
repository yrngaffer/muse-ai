package com.muse.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.companion.ai.model.ModelCatalog
import com.muse.companion.data.settings.MuseSettings
import com.muse.companion.data.settings.Personality
import com.muse.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val personality = container.settings.personality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Personality())

    val settings = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_SETTINGS)

    val deviceProfile = container.device.profile()
    val recommendation = ModelCatalog.recommend(deviceProfile)

    fun savePersonality(p: Personality) = viewModelScope.launch { container.settings.setPersonality(p) }
    fun setMemoryEnabled(v: Boolean) = viewModelScope.launch { container.settings.setMemoryEnabled(v) }
    fun setOnlineTools(v: Boolean) = viewModelScope.launch { container.settings.setOnlineTools(v) }
    fun setRemoteEngine(v: Boolean) = viewModelScope.launch { container.settings.setRemoteEngine(v) }
    fun setAutoAllowIrreversible(v: Boolean) = viewModelScope.launch { container.settings.setAutoAllowIrreversible(v) }

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
