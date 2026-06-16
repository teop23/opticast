package com.opticast.ui

import android.graphics.SurfaceTexture
import android.view.MotionEvent
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.StreamState
import com.opticast.stream.RootEncoderBroadcaster
import com.opticast.ui.theme.Amber
import com.opticast.ui.theme.LiveRed
import com.opticast.ui.theme.MonoStat
import com.opticast.ui.theme.SurfaceElevated
import com.opticast.ui.theme.TextFaint
import com.opticast.ui.theme.TextMuted

@Composable
fun LiveScreen(vm: StreamViewModel, onConnections: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val streaming = ui.streamState is StreamState.Live ||
        ui.streamState is StreamState.Connecting ||
        ui.streamState is StreamState.Reconnecting

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

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
                        // Pinch to zoom, tap to focus (only meaningful while the preview is visible).
                        setOnTouchListener { v, event ->
                            val b = vm.broadcasterForPreview() as? RootEncoderBroadcaster
                            if (event.pointerCount >= 2) b?.setZoom(event)
                            else if (event.actionMasked == MotionEvent.ACTION_UP) b?.tapToFocus(v, event)
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.VisibilityOff, null, tint = TextFaint, modifier = Modifier.size(40.dp))
                Text("Preview off", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                Text("saves battery", color = TextFaint, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Top status row
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(ui.streamState, ui.stats.uptimeSeconds)
            Spacer(Modifier.weight(1f))
            if (ui.streamState is StreamState.Live) {
                Text(
                    "${ui.stats.bitrateBps / 1000} kbps",
                    style = MonoStat,
                    color = TextMuted
                )
            }
        }

        // Bottom controls over a scrim
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background)))
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ControlButton(if (ui.previewEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    "Preview", active = ui.previewEnabled) { vm.togglePreview() }
                ControlButton(Icons.Filled.Cameraswitch, "Flip") { vm.switchCamera() }
                ControlButton(if (ui.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    "Mic", active = ui.muted) { vm.toggleMute() }
                ControlButton(if (ui.torch) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    "Torch", active = ui.torch) { vm.toggleTorch() }
                ControlButton(Icons.Filled.Dns, "Targets") { onConnections() }
            }

            Button(
                onClick = { if (streaming) vm.stop() else vm.goLive() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = if (streaming)
                    ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = Color.White)
                else ButtonDefaults.buttonColors()
            ) {
                Text(
                    if (streaming) "STOP" else "GO LIVE",
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun StatusPill(state: StreamState, uptimeSeconds: Long) {
    val live = state is StreamState.Live
    val dotColor = when (state) {
        is StreamState.Live -> LiveRed
        is StreamState.Connecting, is StreamState.Reconnecting -> Amber
        is StreamState.Error -> LiveRed
        StreamState.Idle -> TextMuted
    }
    val label = when (state) {
        StreamState.Idle -> "READY"
        StreamState.Connecting -> "CONNECTING"
        StreamState.Live -> "LIVE"
        is StreamState.Reconnecting -> "RECONNECTING"
        is StreamState.Error -> "ERROR"
    }
    Surface(color = SurfaceElevated, shape = CircleShape) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Text(label, color = if (live) LiveRed else TextMuted,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            if (live) {
                Text(formatUptime(uptimeSeconds), style = MonoStat, color = TextMuted)
            }
            if (state is StreamState.Error) {
                Text(state.reason, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else TextMuted
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = SurfaceElevated, contentColor = tint
            ),
            modifier = Modifier.size(52.dp)
        ) { Icon(icon, label, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextFaint)
    }
}

private fun formatUptime(s: Long): String = "%02d:%02d".format(s / 60, s % 60)
