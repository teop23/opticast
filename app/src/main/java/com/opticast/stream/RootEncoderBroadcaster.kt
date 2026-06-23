package com.opticast.stream

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import com.opticast.model.Connection
import com.opticast.model.FocusMode
import com.opticast.model.StreamCodec
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
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
    private var previewView: TextureView? = null
    private var prepared = false
    private var focusMode = FocusMode.AUTO

    /** prepareVideo/prepareAudio require stream + preview stopped; sets [prepared]. */
    private fun prepare(width: Int, height: Int, videoBitrate: Int, fps: Int, audioBitrate: Int): Boolean {
        // StreamBase.prepareVideo(width, height, bitrate, fps, iFrameInterval, ...) — named to be
        // unambiguous. iFrameInterval = 1s keyframe interval (default is 2s) for faster join/recovery.
        val video = stream.prepareVideo(
            width = width, height = height, bitrate = videoBitrate, fps = fps, iFrameInterval = 1
        )
        val audio = stream.prepareAudio(44100, true, audioBitrate)
        prepared = video && audio
        return prepared
    }

    override fun start(connection: Connection) {
        if (stream.isStreaming) return
        // Re-prepare with this connection's settings. prepareVideo requires preview stopped,
        // so stop it, prepare, then restore the preview before streaming.
        val view = previewView
        if (stream.isOnPreview) stream.stopPreview()
        stream.setVideoCodec(if (connection.codec == StreamCodec.H265) VideoCodec.H265 else VideoCodec.H264)
        val ok = prepare(
            connection.width, connection.height,
            connection.videoBitrate, connection.fps, connection.audioBitrate
        )
        if (!ok) {
            view?.let { stream.startPreview(it) } // restore preview on failure
            state.value = StreamState.Error("Failed to prepare encoder")
            return
        }
        view?.let { stream.startPreview(it) }
        state.value = StreamState.Connecting
        startedAtMs = System.currentTimeMillis()
        stream.startStream(connection.toEndpoint())
        applyFocusMode()   // camera is live now; (re)assert the chosen focus mode
    }

    override fun stop() {
        if (stream.isStreaming) stream.stopStream()
        state.value = StreamState.Idle
    }

    /** Called by the service/UI to render preview; safe to skip for background streaming. */
    fun attachPreview(textureView: TextureView) {
        previewView = textureView
        // The encoder must be prepared before startPreview or the GL framebuffer is built
        // with no dimensions (FrameBuffer uncompleted). Prepare with sensible defaults;
        // start() re-prepares with the chosen connection's settings.
        if (!stream.isStreaming && !prepared) {
            prepare(1280, 720, 2_500_000, 30, 128_000)
        }
        if (!stream.isOnPreview) stream.startPreview(textureView)
        applyFocusMode()   // preview started the camera; assert the chosen focus mode
    }
    fun detachPreview() {
        if (stream.isOnPreview) stream.stopPreview()
        previewView = null
    }

    // In RootEncoder 2.7.4 camera/audio control lives on the sources, not StreamBase.
    private val camera get() = stream.videoSource as? Camera2Source
    private val mic get() = stream.audioSource as? MicrophoneSource

    /** One-shot tap-to-focus, only in AUTO mode (locked/infinity modes own the focus). Returns
     *  true if the camera applied it, so the UI can show its focus indicator only when real. */
    fun tapToFocus(view: View, event: MotionEvent): Boolean =
        if (focusMode == FocusMode.AUTO) camera?.tapToFocus(view, event) == true else false

    fun setZoom(event: MotionEvent) { camera?.setZoom(event) }

    override fun setFocusMode(mode: FocusMode) {
        focusMode = mode
        applyFocusMode()
    }

    /** Asserts [focusMode] on the camera. No-op if the camera isn't running yet; the start/preview
     *  paths call this again once it is. Best-effort: unsupported modes simply don't take. */
    private fun applyFocusMode() {
        val cam = camera ?: return
        when (focusMode) {
            FocusMode.AUTO -> cam.enableAutoFocus()
            FocusMode.LOCKED -> cam.disableAutoFocus()   // freeze at current distance
            FocusMode.INFINITY -> cam.setCustomRequest { b ->
                b.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                b.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)   // 0 diopters = infinity
            }
        }
    }

    override fun switchCamera() { camera?.switchCamera(); applyFocusMode() }
    override fun setVideoBitrate(bps: Int) { stream.setVideoBitrateOnFly(bps) }
    override fun setMuted(muted: Boolean) { mic?.let { if (muted) it.mute() else it.unMute() } }
    override fun setTorch(on: Boolean) { camera?.let { if (on) it.enableLantern() else it.disableLantern() } }
    override fun release() { if (stream.isStreaming) stream.stopStream(); stream.release() }

    // ConnectChecker
    override fun onConnectionStarted(url: String) { state.value = StreamState.Connecting }
    override fun onConnectionSuccess() { state.value = StreamState.Live }
    override fun onConnectionFailed(reason: String) {
        // Must stop the stream on failure or RootEncoder stays "streaming" and every retry
        // hits the isStreaming guard and no-ops.
        if (stream.isStreaming) stream.stopStream()
        state.value = StreamState.Error(reason)
    }
    override fun onNewBitrate(bitrate: Long) {
        reportedBitrate.value = bitrate
        val uptime = (System.currentTimeMillis() - startedAtMs) / 1000
        stats.value = stats.value.copy(bitrateBps = bitrate, uptimeSeconds = uptime)
    }
    override fun onDisconnect() { state.value = StreamState.Idle }
    override fun onAuthError() { state.value = StreamState.Error("Auth error") }
    override fun onAuthSuccess() { /* no-op */ }
}
