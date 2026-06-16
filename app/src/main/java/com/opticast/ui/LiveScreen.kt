package com.opticast.ui

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.StreamState
import com.opticast.stream.RootEncoderBroadcaster

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveScreen(vm: StreamViewModel, onConnections: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val streaming = ui.streamState is StreamState.Live ||
        ui.streamState is StreamState.Connecting ||
        ui.streamState is StreamState.Reconnecting

    Box(Modifier.fillMaxSize()) {
        // Preview is opt-in: rendering it costs ~1.3 extra CPU cores. When hidden, the
        // TextureView leaves composition -> detachPreview() stops the GL render; streaming
        // (if active) continues headless.
        if (ui.previewEnabled) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) {
                                (vm.broadcasterForPreview() as? RootEncoderBroadcaster)?.attachPreview(this@apply)
                            }
                            override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
                            override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean {
                                (vm.broadcasterForPreview() as? RootEncoderBroadcaster)?.detachPreview()
                                return true
                            }
                            override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                "Preview off (saves battery)\nTap Preview to show the camera",
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status overlay (top-left)
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            val label = when (val s = ui.streamState) {
                StreamState.Idle -> "Idle"
                StreamState.Connecting -> "Connecting…"
                StreamState.Live -> "LIVE"
                is StreamState.Reconnecting -> "Reconnecting (#${s.attempt})"
                is StreamState.Error -> "Error: ${s.reason}"
            }
            AssistChip(onClick = {}, label = { Text(label) })
            if (ui.streamState is StreamState.Live) {
                Text("${ui.stats.bitrateBps / 1000} kbps · ${ui.stats.uptimeSeconds}s")
            }
            ui.selected?.let { Text("Target: ${it.name}") }
        }

        // Controls (bottom): wrapping row of secondary actions over a full-width primary button.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onConnections) { Text("Targets") }
                TextButton(onClick = { vm.togglePreview() }) {
                    Text(if (ui.previewEnabled) "Hide" else "Preview")
                }
                TextButton(onClick = { vm.switchCamera() }) { Text("Flip") }
                TextButton(onClick = { vm.toggleMute() }) { Text(if (ui.muted) "Unmute" else "Mute") }
                TextButton(onClick = { vm.toggleTorch() }) { Text("Torch") }
            }
            Button(
                onClick = { if (streaming) vm.stop() else vm.goLive() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (streaming) "Stop" else "Go Live") }
        }
    }
}
