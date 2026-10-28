package com.muse.companion.ui.chat

import android.Manifest
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muse.companion.ai.Role
import com.muse.companion.di.AppContainer
import com.muse.companion.voice.VoiceController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(container: AppContainer) {
    val vm: ChatViewModel = viewModel(factory = viewModelFactory { initializer { ChatViewModel(container) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val voice = remember { VoiceController(context) }
    DisposableEffect(Unit) {
        voice.init()
        onDispose { voice.shutdown() }
    }

    val sttLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) vm.onVoiceResult(spoken)
    }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sttLauncher.launch(voice.recognitionIntent())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Muse", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${if (state.online) "Online" else "Offline"} · ${state.engineName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (state.isGenerating || state.pendingConfirmation != null) {
                        IconButton(onClick = vm::emergencyStop) {
                            Icon(Icons.Filled.Stop, "Emergency stop", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            val listState = rememberLazyListState()
            LaunchedEffect(state.messages.size) {
                if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (state.messages.isEmpty()) item { EmptyHint() }
                items(state.messages) { m -> MessageBubble(m) { voice.speak(it) } }
            }
            InputBar(
                input = state.input,
                generating = state.isGenerating,
                onInput = vm::updateInput,
                onSend = vm::send,
                onMic = { micPerm.launch(Manifest.permission.RECORD_AUDIO) },
            )
        }
    }

    state.pendingConfirmation?.let { p ->
        AlertDialog(
            onDismissRequest = { vm.confirmPending(false) },
            title = { Text("Confirm: ${p.toolId}") },
            text = { Text("${p.reason}\n\nArguments: ${p.args}") },
            confirmButton = { TextButton(onClick = { vm.confirmPending(true) }) { Text("Allow once") } },
            dismissButton = { TextButton(onClick = { vm.confirmPending(false) }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyHint() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Muse is running offline with no model loaded yet. Say hi, or open Model Manager (coming next milestone) / Settings to enable an engine.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageBubble(m: UiMessage, onSpeak: (String) -> Unit) {
    val isUser = m.role == Role.USER && !m.system
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = when {
        m.system -> MaterialTheme.colorScheme.surface
        isUser -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(color = bg, shape = RoundedCornerShape(14.dp), modifier = Modifier.widthIn(max = 320.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                Text(
                    m.text.ifEmpty { "…" },
                    color = if (m.system) MaterialTheme.colorScheme.onSurfaceVariant else fg,
                    style = if (m.system) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!isUser && !m.system && m.text.isNotBlank()) {
                    IconButton(onClick = { onSpeak(m.text) }) {
                        Icon(Icons.Filled.VolumeUp, "Speak", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    generating: Boolean,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onMic) { Icon(Icons.Filled.Mic, "Push to talk") }
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Muse…") },
                maxLines = 4,
            )
            IconButton(onClick = onSend, enabled = !generating && input.isNotBlank()) {
                Icon(Icons.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
