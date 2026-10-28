package com.muse.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muse.companion.security.SecurityPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "muse_settings")

/** All user-configurable settings, persisted with DataStore. Exposes a [SecurityPolicy] snapshot for the broker. */
class SettingsRepository(context: Context) {
    private val ctx = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val PERSONALITY = stringPreferencesKey("personality_json")
        val ONLINE_TOOLS = booleanPreferencesKey("online_tools_enabled")
        val REMOTE_ENGINE = booleanPreferencesKey("remote_engine_enabled")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val AUTO_ALLOW_IRREVERSIBLE = booleanPreferencesKey("auto_allow_irreversible")
        val KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val DISABLED_TOOLS = stringSetPreferencesKey("disabled_tools")
        val RETENTION_EPISODIC = intPreferencesKey("retention_episodic")
    }

    val personality: Flow<Personality> = ctx.dataStore.data.map { p ->
        p[Keys.PERSONALITY]?.let { runCatching { json.decodeFromString<Personality>(it) }.getOrNull() } ?: Personality()
    }

    val settings: Flow<MuseSettings> = ctx.dataStore.data.map { p ->
        MuseSettings(
            onlineToolsEnabled = p[Keys.ONLINE_TOOLS] ?: false,
            remoteEngineEnabled = p[Keys.REMOTE_ENGINE] ?: false,
            memoryEnabled = p[Keys.MEMORY_ENABLED] ?: true,
            autoAllowIrreversible = p[Keys.AUTO_ALLOW_IRREVERSIBLE] ?: false,
            killSwitchEngaged = p[Keys.KILL_SWITCH] ?: false,
            disabledToolIds = p[Keys.DISABLED_TOOLS] ?: emptySet(),
            episodicRetention = p[Keys.RETENTION_EPISODIC] ?: 500,
        )
    }

    val securityPolicy: Flow<SecurityPolicy> = settings.map { s ->
        SecurityPolicy(
            onlineToolsEnabled = s.onlineToolsEnabled,
            autoAllowIrreversible = s.autoAllowIrreversible,
            disabledToolIds = s.disabledToolIds,
            killSwitchEngaged = s.killSwitchEngaged,
        )
    }

    suspend fun setPersonality(p: Personality) = ctx.dataStore.edit { it[Keys.PERSONALITY] = json.encodeToString(p) }
    suspend fun setOnlineTools(v: Boolean) = ctx.dataStore.edit { it[Keys.ONLINE_TOOLS] = v }
    suspend fun setRemoteEngine(v: Boolean) = ctx.dataStore.edit { it[Keys.REMOTE_ENGINE] = v }
    suspend fun setMemoryEnabled(v: Boolean) = ctx.dataStore.edit { it[Keys.MEMORY_ENABLED] = v }
    suspend fun setAutoAllowIrreversible(v: Boolean) = ctx.dataStore.edit { it[Keys.AUTO_ALLOW_IRREVERSIBLE] = v }
    suspend fun setKillSwitch(v: Boolean) = ctx.dataStore.edit { it[Keys.KILL_SWITCH] = v }

    suspend fun setToolDisabled(id: String, disabled: Boolean) = ctx.dataStore.edit { p ->
        val cur = (p[Keys.DISABLED_TOOLS] ?: emptySet()).toMutableSet()
        if (disabled) cur.add(id) else cur.remove(id)
        p[Keys.DISABLED_TOOLS] = cur
    }
}

data class MuseSettings(
    val onlineToolsEnabled: Boolean,
    val remoteEngineEnabled: Boolean,
    val memoryEnabled: Boolean,
    val autoAllowIrreversible: Boolean,
    val killSwitchEngaged: Boolean,
    val disabledToolIds: Set<String>,
    val episodicRetention: Int,
)
