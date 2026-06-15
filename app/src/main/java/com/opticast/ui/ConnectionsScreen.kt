package com.opticast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import com.opticast.util.ConnectionValidator
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ConnectionsScreen(vm: StreamViewModel, onBack: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Connection?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row {
            Button(onClick = onBack) { Text("Back") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { editing = blankConnection() }) { Text("New") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(ui.connections) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    supportingContent = { Text("${c.protocol} ${c.host}:${c.port}/${c.path}") },
                    modifier = Modifier.clickable { vm.selectConnection(c); onBack() }
                )
                Row {
                    TextButton(onClick = { editing = c }) { Text("Edit") }
                    TextButton(onClick = { scope.launch { vm.delete(c.id) } }) { Text("Delete") }
                }
            }
        }
    }

    editing?.let { conn ->
        ConnectionEditorDialog(
            initial = conn,
            onDismiss = { editing = null },
            onSave = { scope.launch { vm.save(it); editing = null } }
        )
    }
}

private fun blankConnection() = Connection(
    id = UUID.randomUUID().toString(), name = "", protocol = StreamProtocol.SRT,
    host = "", port = 8890, path = "live/stream", secret = null,
    width = 1280, height = 720, fps = 30, videoBitrate = 2_500_000, audioBitrate = 128_000
)

@Composable
fun ConnectionEditorDialog(initial: Connection, onDismiss: () -> Unit, onSave: (Connection) -> Unit) {
    var c by remember { mutableStateOf(initial) }
    val result = ConnectionValidator.validate(c)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = result.errors.isEmpty(), onClick = { onSave(c) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Connection") },
        text = {
            Column {
                OutlinedTextField(c.name, { c = c.copy(name = it) }, label = { Text("Name") })
                Row {
                    FilterChip(
                        selected = c.protocol == StreamProtocol.RTMP,
                        onClick = { c = c.copy(protocol = StreamProtocol.RTMP) },
                        label = { Text("RTMP") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = c.protocol == StreamProtocol.SRT,
                        onClick = { c = c.copy(protocol = StreamProtocol.SRT) },
                        label = { Text("SRT") }
                    )
                }
                OutlinedTextField(c.host, { c = c.copy(host = it) }, label = { Text("Host") })
                OutlinedTextField(
                    c.port.toString(),
                    { c = c.copy(port = it.toIntOrNull() ?: 0) },
                    label = { Text("Port") }
                )
                OutlinedTextField(c.path, { c = c.copy(path = it) }, label = { Text("App/Path") })
                OutlinedTextField(
                    c.secret ?: "",
                    { c = c.copy(secret = it.ifBlank { null }) },
                    label = { Text(if (c.protocol == StreamProtocol.SRT) "Passphrase" else "user:pass") }
                )
                result.errors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                result.warnings.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}
