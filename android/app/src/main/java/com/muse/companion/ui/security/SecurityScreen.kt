package com.muse.companion.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muse.companion.data.audit.AuditEntity
import com.muse.companion.di.AppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(container: AppContainer) {
    val vm: SecurityViewModel = viewModel(factory = viewModelFactory { initializer { SecurityViewModel(container) } })
    val audit by vm.audit.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Security") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = if (settings.killSwitchEngaged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kill switch — disable all automation", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "When on, the broker denies every tool call. Chat still works.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = settings.killSwitchEngaged, onCheckedChange = vm::setKill)
                }
            }

            Text("Tool permissions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            vm.toolIds.forEach { id ->
                val enabled = id !in settings.disabledToolIds
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(id, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { checked -> vm.setToolDisabled(id, disabled = !checked) })
                }
            }

            Text("Audit log (append-only)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                if (audit.isEmpty()) {
                    item {
                        Text("No tool activity yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(audit) { e -> AuditRow(e) }
            }
        }
    }
}

private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

@Composable
private fun AuditRow(e: AuditEntity) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text("${fmt.format(Date(e.timeMillis))}  ·  ${e.toolId}  ·  ${e.decision}", style = MaterialTheme.typography.labelMedium)
            if (e.detail.isNotBlank()) {
                Text(e.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
