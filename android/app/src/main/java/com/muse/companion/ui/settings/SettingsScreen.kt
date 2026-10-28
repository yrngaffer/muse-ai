package com.muse.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muse.companion.data.settings.Personality
import com.muse.companion.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container) } })
    val persisted by vm.personality.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf(persisted) }
    LaunchedEffect(persisted) { draft = persisted }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("Personality")
            OutlinedTextField(draft.assistantName, { draft = draft.copy(assistantName = it) }, label = { Text("Assistant name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.ownerNickname, { draft = draft.copy(ownerNickname = it) }, label = { Text("What should it call you?") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.tone, { draft = draft.copy(tone = it) }, label = { Text("Tone") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Personality.Verbosity.entries.forEach { v ->
                    FilterChip(selected = draft.verbosity == v, onClick = { draft = draft.copy(verbosity = v) }, label = { Text(v.name.lowercase()) })
                }
            }
            Text("Humor: ${draft.humor}", style = MaterialTheme.typography.labelMedium)
            Slider(value = draft.humor.toFloat(), onValueChange = { draft = draft.copy(humor = it.toInt()) }, valueRange = 0f..100f)
            ToggleRow("Use emoji", draft.emojiUse) { draft = draft.copy(emojiUse = it) }
            Button(onClick = { vm.savePersonality(draft) }, modifier = Modifier.fillMaxWidth()) { Text("Save personality") }

            Section("Memory & privacy")
            ToggleRow("Collect memory", settings.memoryEnabled, vm::setMemoryEnabled)

            Section("Automation")
            ToggleRow(
                "Auto-allow irreversible actions",
                settings.autoAllowIrreversible,
                vm::setAutoAllowIrreversible,
                warn = "Skips confirmation for non-reversible Level-2 actions. Sensitive (Level-3) actions are ALWAYS confirmed regardless.",
            )

            Section("Network (off by default)")
            ToggleRow(
                "Online tools",
                settings.onlineToolsEnabled,
                vm::setOnlineTools,
                warn = "Lets tools like web fetch reach the internet. Private memory is never sent automatically.",
            )
            ToggleRow(
                "Remote model engine",
                settings.remoteEngineEnabled,
                vm::setRemoteEngine,
                warn = "Sends your prompts to a remote model provider. This means conversation text leaves the phone. Disabled by default.",
            )

            Section("This device")
            InfoCard(
                buildString {
                    appendLine("RAM: ${"%.1f".format(vm.deviceProfile.totalRamBytes / 1e9)} GB" + if (vm.deviceProfile.isLowRam) " (low-RAM)" else "")
                    appendLine("ABIs: ${vm.deviceProfile.abis.joinToString()}")
                    appendLine("Free storage: ${"%.1f".format(vm.deviceProfile.freeAppStorageBytes / 1e9)} GB")
                    appendLine("Android API: ${vm.deviceProfile.sdkInt}")
                    append("Recommended model: ${vm.recommendation.model?.name ?: "none"} — ${vm.recommendation.rationale}")
                },
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, warn: String? = null) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
        if (warn != null) {
            Text(warn, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
