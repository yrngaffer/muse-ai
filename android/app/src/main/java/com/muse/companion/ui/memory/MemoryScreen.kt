package com.muse.companion.ui.memory

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.muse.companion.data.memory.MemoryLayer
import com.muse.companion.data.memory.MemoryUi
import com.muse.companion.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(container: AppContainer) {
    val vm: MemoryViewModel = viewModel(factory = viewModelFactory { initializer { MemoryViewModel(container) } })
    val memories by vm.memories.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    var layer by remember { mutableStateOf(MemoryLayer.PERMANENT) }
    var showClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory (${memories.size})") },
                actions = {
                    IconButton(onClick = { showClear = true }) {
                        Icon(Icons.Filled.DeleteSweep, "Clear all", tint = MaterialTheme.colorScheme.error)
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(horizontal = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                MemoryLayer.entries.forEach { l ->
                    FilterChip(selected = layer == l, onClick = { layer = l }, label = { Text(l.name.lowercase()) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a memory…") },
                    maxLines = 3,
                )
                IconButton(onClick = {
                    vm.add(draft, layer)
                    draft = ""
                }) { Icon(Icons.Filled.Add, "Add memory", tint = MaterialTheme.colorScheme.primary) }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                items(memories) { m -> MemoryRow(m) { vm.delete(m.id) } }
            }
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Clear all memory?") },
            text = { Text("This permanently deletes every stored memory. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); showClear = false }) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MemoryRow(m: MemoryUi, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${m.layer.name.lowercase()} · ${m.verification.name.lowercase()} · ${m.source}" +
                        if (m.sensitivity.name == "SENSITIVE") " · 🔒 encrypted" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
