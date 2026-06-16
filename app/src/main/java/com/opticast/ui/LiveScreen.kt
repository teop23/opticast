package com.opticast.ui

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.StreamState
import com.opticast.stream.RootEncoderBroadcaster

@Composable
fun LiveScreen(vm: StreamViewModel, onConnections: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        // Preview surface. attachPreview must wait until the Surface is actually valid
        // (surfaceCreated) — calling it in the factory crashes RootEncoder with
        // "Make sure the Surface is valid".
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            (vm.broadcasterForPreview() as? RootEncoderBroadcaster)?.attachPreview(this@apply)
                        }
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {}
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            (vm.broadcasterForPreview() as? RootEncoderBroadcaster)?.detachPreview()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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

        Row(
            Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onConnections) { Text("Targets") }
            Button(onClick = { vm.switchCamera() }) { Text("Flip") }
            Button(onClick = { vm.toggleMute() }) { Text(if (ui.muted) "Unmute" else "Mute") }
            Button(onClick = { vm.toggleTorch() }) { Text("Torch") }
            if (ui.streamState is StreamState.Live || ui.streamState is StreamState.Connecting) {
                Button(onClick = { vm.stop() }) { Text("Stop") }
            } else {
                Button(onClick = { vm.goLive() }) { Text("Go Live") }
            }
        }
    }
}
