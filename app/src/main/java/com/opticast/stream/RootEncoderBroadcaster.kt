package com.opticast.stream

import android.content.Context
import android.view.SurfaceView
import com.opticast.model.Connection
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.flow.MutableStateFlow

class RootEncoderBroadcaster(context: Context) : Broadcaster, ConnectChecker {

    override val state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val stats = MutableStateFlow(StreamStats())
    override val reportedBitrate = MutableStateFlow(0L)

    private val stream = GenericStream(context, this).apply {
        getGlInterface().autoHandleOrientation = true
    }
    private var startedAtMs = 0L

    override fun start(connection: Connection) {
        if (stream.isStreaming) return
        val video = stream.prepareVideo(
            width = connection.width, height = connection.height,
            bitrate = connection.videoBitrate, fps = connection.fps, rotation = 0
        )
        val audio = stream.prepareAudio(
            sampleRate = 44100, isStereo = true, bitrate = connection.audioBitrate
        )
        if (!video || !audio) {
            state.value = StreamState.Error("Failed to prepare encoder")
            return
        }
        state.value = StreamState.Connecting
        startedAtMs = System.currentTimeMillis()
        stream.startStream(connection.toEndpoint())
    }

    override fun stop() {
        if (stream.isStreaming) stream.stopStream()
        state.value = StreamState.Idle
    }

    /** Called by the service/UI to render preview; safe to skip for background streaming. */
    fun attachPreview(surfaceView: SurfaceView) { stream.startPreview(surfaceView) }
    fun detachPreview() { if (stream.isOnPreview) stream.stopPreview() }

    // In RootEncoder 2.7.4 camera/audio control lives on the sources, not StreamBase.
    private val camera get() = stream.videoSource as? Camera2Source
    private val mic get() = stream.audioSource as? MicrophoneSource

    override fun switchCamera() { camera?.switchCamera() }
    override fun setVideoBitrate(bps: Int) { stream.setVideoBitrateOnFly(bps) }
    override fun setMuted(muted: Boolean) { mic?.let { if (muted) it.mute() else it.unMute() } }
    override fun setTorch(on: Boolean) { camera?.let { if (on) it.enableLantern() else it.disableLantern() } }
    override fun release() { if (stream.isStreaming) stream.stopStream(); stream.release() }

    // ConnectChecker
    override fun onConnectionStarted(url: String) { state.value = StreamState.Connecting }
    override fun onConnectionSuccess() { state.value = StreamState.Live }
    override fun onConnectionFailed(reason: String) { state.value = StreamState.Error(reason) }
    override fun onNewBitrate(bitrate: Long) {
        reportedBitrate.value = bitrate
        val uptime = (System.currentTimeMillis() - startedAtMs) / 1000
        stats.value = stats.value.copy(bitrateBps = bitrate, uptimeSeconds = uptime)
    }
    override fun onDisconnect() { state.value = StreamState.Idle }
    override fun onAuthError() { state.value = StreamState.Error("Auth error") }
    override fun onAuthSuccess() { /* no-op */ }
}
