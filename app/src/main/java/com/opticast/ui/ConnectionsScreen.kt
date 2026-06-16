package com.opticast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.Connection
import com.opticast.model.QualityPresets
import com.opticast.model.StreamCodec
import com.opticast.model.StreamProtocol
import com.opticast.ui.theme.Amber
import com.opticast.ui.theme.MonoStat
import com.opticast.ui.theme.SurfaceElevated
import com.opticast.ui.theme.TextFaint
import com.opticast.ui.theme.TextMuted
import com.opticast.util.ConnectionValidator
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ConnectionsScreen(vm: StreamViewModel, onBack: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Connection?>(null) }
    var about by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = TextMuted) }
            Text("Targets", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { about = true }) { Icon(Icons.Filled.Info, "About", tint = TextMuted) }
            FilledTonalButton(onClick = { editing = blankConnection() }) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("New")
            }
        }
        Spacer(Modifier.height(12.dp))

        if (ui.connections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No targets yet — tap New", color = TextFaint)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ui.connections, key = { it.id }) { c ->
                    ConnectionCard(
                        c,
                        selected = ui.selected?.id == c.id,
                        onSelect = { vm.selectConnection(c); onBack() },
                        onEdit = { editing = c },
                        onDelete = { scope.launch { vm.delete(c.id) } }
                    )
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

    if (about) {
        val url = "https://github.com/teop23/opticast"
        AlertDialog(
            onDismissRequest = { about = false },
            confirmButton = { TextButton(onClick = { about = false }) { Text("Close") } },
            title = { Text("Opticast ${com.opticast.BuildConfig.VERSION_NAME}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Open-source camera broadcaster — streams your phone camera to any RTMP/SRT server. No accounts, no telemetry.")
                    Text("License: GPL-3.0", color = TextMuted)
                    TextButton(contentPadding = PaddingValues(0.dp), onClick = {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    }) { Text(url) }
                }
            }
        )
    }
}

@Composable
private fun ConnectionCard(
    c: Connection, selected: Boolean,
    onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    Surface(
        color = SurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(c.name.ifBlank { "(unnamed)" }, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("${c.protocol} ${c.host}:${c.port}/${c.path}", style = MonoStat, color = TextMuted)
            Text("${c.width}×${c.height} · ${c.fps}fps · ${c.videoBitrate / 1000}k · ${c.codec}",
                style = MonoStat, color = TextFaint)
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun blankConnection() = Connection(
    id = UUID.randomUUID().toString(), name = "", protocol = StreamProtocol.SRT,
    host = "", port = 8890, path = "live/stream", secret = null,
    width = 1280, height = 720, fps = 30, videoBitrate = 2_500_000, audioBitrate = 128_000,
    codec = StreamCodec.H264
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionEditorDialog(initial: Connection, onDismiss: () -> Unit, onSave: (Connection) -> Unit) {
    var c by remember { mutableStateOf(initial) }
    var advanced by remember { mutableStateOf(false) }
    val result = ConnectionValidator.validate(c)
    val activePreset = QualityPresets.match(c)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Text(if (initial.name.isBlank()) "New target" else "Edit target",
                    style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)

                // Protocol
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(c.protocol == StreamProtocol.RTMP,
                        { c = c.copy(protocol = StreamProtocol.RTMP) }, { Text("RTMP") })
                    FilterChip(c.protocol == StreamProtocol.SRT,
                        { c = c.copy(protocol = StreamProtocol.SRT) }, { Text("SRT") })
                }

                OutlinedTextField(c.name, { c = c.copy(name = it) },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(c.host, { c = c.copy(host = it) },
                    label = { Text("Host") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(c.port.toString(), { c = c.copy(port = it.toIntOrNull() ?: 0) },
                        label = { Text("Port") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(c.path, { c = c.copy(path = it) },
                        label = { Text("App / path") }, singleLine = true, modifier = Modifier.weight(2f))
                }
                OutlinedTextField(c.secret ?: "", { c = c.copy(secret = it.ifBlank { null }) },
                    label = { Text(if (c.protocol == StreamProtocol.SRT) "Passphrase" else "user:pass") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())

                // Quality presets
                Text("QUALITY", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityPresets.ALL.forEach { p ->
                        FilterChip(
                            selected = activePreset == p,
                            onClick = { c = QualityPresets.apply(c, p) },
                            label = { Text(p.label) }
                        )
                    }
                }

                // Advanced expander
                Row(Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Advanced", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    Icon(if (advanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextMuted)
                }
                AnimatedVisibility(advanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberField("Width", c.width, Modifier.weight(1f)) { c = c.copy(width = it) }
                            NumberField("Height", c.height, Modifier.weight(1f)) { c = c.copy(height = it) }
                            NumberField("FPS", c.fps, Modifier.weight(1f)) { c = c.copy(fps = it) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberField("Video kbps", c.videoBitrate / 1000, Modifier.weight(1f)) {
                                c = c.copy(videoBitrate = it * 1000)
                            }
                            NumberField("Audio kbps", c.audioBitrate / 1000, Modifier.weight(1f)) {
                                c = c.copy(audioBitrate = it * 1000)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Codec", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                            FilterChip(c.codec == StreamCodec.H264, { c = c.copy(codec = StreamCodec.H264) }, { Text("H.264") })
                            FilterChip(c.codec == StreamCodec.H265, { c = c.copy(codec = StreamCodec.H265) }, { Text("H.265") })
                        }
                    }
                }

                // Validation messages
                result.errors.forEach {
                    Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                result.warnings.forEach {
                    Text("⚠  $it", color = Amber, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(c) },
                        enabled = result.canSave,
                        modifier = Modifier.weight(1f),
                        colors = if (result.needsConfirm)
                            ButtonDefaults.buttonColors(containerColor = Amber, contentColor = androidx.compose.ui.graphics.Color(0xFF231A00))
                        else ButtonDefaults.buttonColors()
                    ) { Text(if (result.needsConfirm) "Save anyway" else "Save") }
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: 0) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
