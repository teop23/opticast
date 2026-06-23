package com.opticast.ui

import android.graphics.SurfaceTexture
import android.view.MotionEvent
import android.view.TextureView
import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.FocusMode
import com.opticast.model.StreamState
import com.opticast.stream.RootEncoderBroadcaster
import com.opticast.ui.theme.Amber
import com.opticast.ui.theme.Lime
import kotlinx.coroutines.delay
import kotlin.math.hypot
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

    // Tap-to-focus indicator: where the last successful focus tap landed, and a tick that
    // re-fires the animation on each new tap.
    val focusPoint = remember { mutableStateOf<Offset?>(null) }
    var focusTick by remember { mutableStateOf(0) }

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
                        // Pinch to zoom (2+ pointers); tap to focus (a single, stationary tap).
                        // Tracking down position/time and the multi-touch flag avoids the old bug
                        // where any ACTION_UP — including the end of a pinch or a drag — moved focus.
                        val slop = ViewConfiguration.get(ctx).scaledTouchSlop
                        var downX = 0f
                        var downY = 0f
                        var downT = 0L
                        var multiTouch = false
                        var moved = false
                        setOnTouchListener { v, event ->
                            val b = vm.broadcasterForPreview() as? RootEncoderBroadcaster
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    downX = event.x; downY = event.y
                                    downT = System.currentTimeMillis()
                                    multiTouch = false; moved = false
                                }
                                MotionEvent.ACTION_POINTER_DOWN -> {
                                    multiTouch = true; b?.setZoom(event)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    if (event.pointerCount >= 2) { multiTouch = true; b?.setZoom(event) }
                                    else if (hypot(event.x - downX, event.y - downY) > slop) moved = true
                                }
                                MotionEvent.ACTION_UP -> {
                                    val dt = System.currentTimeMillis() - downT
                                    if (!multiTouch && !moved && dt < 300L &&
                                        b?.tapToFocus(v, event) == true
                                    ) {
                                        focusPoint.value = Offset(event.x, event.y)
                                        focusTick++
                                    }
                                }
                            }
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Animated focus square at the tapped point: snaps in slightly larger, settles,
            // holds briefly, then fades. Cleared by the LaunchedEffect when done.
            focusPoint.value?.let { pt ->
                val scale = remember { Animatable(1.35f) }
                val alpha = remember { Animatable(1f) }
                LaunchedEffect(focusTick) {
                    scale.snapTo(1.35f); alpha.snapTo(1f)
                    scale.animateTo(1f, tween(200))
                    delay(650)
                    alpha.animateTo(0f, tween(220))
                    focusPoint.value = null
                }
                Canvas(Modifier.fillMaxSize()) {
                    val s = 34.dp.toPx() * scale.value
                    drawRect(
                        color = Lime.copy(alpha = alpha.value),
                        topLeft = Offset(pt.x - s, pt.y - s),
                        size = Size(s * 2, s * 2),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
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
                FocusControl(ui.focusMode) { vm.setFocusMode(it) }
                ControlButton(if (ui.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    "Mic", active = ui.muted) { vm.toggleMute() }
                ControlButton(if (ui.torch) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    "Torch", active = ui.torch) { vm.toggleTorch() }
                ControlButton(
                    Icons.Filled.Dns,
                    ui.selected?.name?.takeIf { it.isNotBlank() } ?: "Targets"
                ) { onConnections() }
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
            shape = RoundedCornerShape(13.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = SurfaceElevated, contentColor = tint
            ),
            modifier = Modifier.size(46.dp)
        ) { Icon(icon, label, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 54.dp)
        )
    }
}

/** Focus-mode control: a single button (icon reflects the active mode) that opens an explicit
 *  menu of all modes — nothing is hidden behind a blind cycle. */
@Composable
private fun FocusControl(mode: FocusMode, onSelect: (FocusMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val icon = when (mode) {
        FocusMode.AUTO -> Icons.Filled.CenterFocusStrong
        FocusMode.LOCKED -> Icons.Filled.Lock
        FocusMode.INFINITY -> Icons.Filled.AllInclusive
    }
    Box {
        ControlButton(icon, "Focus", active = mode != FocusMode.AUTO) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FocusMenuItem("Auto", "Continuous autofocus", Icons.Filled.CenterFocusStrong,
                mode == FocusMode.AUTO) { onSelect(FocusMode.AUTO); open = false }
            FocusMenuItem("Lock", "Freeze focus where it is", Icons.Filled.Lock,
                mode == FocusMode.LOCKED) { onSelect(FocusMode.LOCKED); open = false }
            FocusMenuItem("Infinity", "Far scene; ignore near objects", Icons.Filled.AllInclusive,
                mode == FocusMode.INFINITY) { onSelect(FocusMode.INFINITY); open = false }
        }
    }
}

@Composable
private fun FocusMenuItem(
    title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    DropdownMenuItem(
        text = {
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextFaint)
            }
        },
        leadingIcon = { Icon(icon, null, tint = if (selected) accent else TextMuted) },
        trailingIcon = if (selected) {
            { Icon(Icons.Filled.Check, null, tint = accent) }
        } else null,
        onClick = onClick
    )
}

private fun formatUptime(s: Long): String = "%02d:%02d".format(s / 60, s % 60)
