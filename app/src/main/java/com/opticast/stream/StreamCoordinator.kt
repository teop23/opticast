package com.opticast.stream

import com.opticast.model.Connection
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Decorates a [Broadcaster] with auto-reconnect (capped backoff) and adaptive bitrate.
 * It IS a Broadcaster, so it drops in transparently — the ViewModel/UI are unaffected.
 *
 * - Reconnect: if the stream drops while the user wants to be live, retries start() with
 *   exponential backoff, surfacing Reconnecting(attempt) until it recovers or gives up.
 * - Adaptive bitrate: feeds the reported send-rate into AdaptiveBitrateController, which
 *   nudges the encoder bitrate down on congestion / back up when the link recovers, bounded
 *   by the connection's configured bitrate.
 */
class StreamCoordinator(
    private val inner: Broadcaster,
    private val scope: CoroutineScope,
    private val reconnect: ReconnectController = ReconnectController(baseMs = 1000, capMs = 8000, maxAttempts = 6),
) : Broadcaster {

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val state: StateFlow<StreamState> = _state.asStateFlow()
    override val stats: StateFlow<StreamStats> = inner.stats
    override val reportedBitrate: StateFlow<Long> = inner.reportedBitrate

    private var current: Connection? = null
    private var wantLive = false
    private var reconnectJob: Job? = null
    private var abr: AdaptiveBitrateController? = null

    init {
        scope.launch { inner.state.collect { onInnerState(it) } }
        scope.launch { inner.reportedBitrate.collect { abr?.onReportedBitrate(it) } }
    }

    override fun start(connection: Connection) {
        current = connection
        wantLive = true
        abr = AdaptiveBitrateController(
            minBps = (connection.videoBitrate * 0.3).toInt().coerceAtLeast(300_000),
            maxBps = connection.videoBitrate,
            startBps = connection.videoBitrate,
            stepBps = (connection.videoBitrate / 10).coerceAtLeast(100_000),
            onChange = { inner.setVideoBitrate(it) }
        )
        inner.start(connection)
    }

    override fun stop() {
        wantLive = false
        reconnectJob?.cancel(); reconnectJob = null
        inner.stop()
        _state.value = StreamState.Idle
    }

    override fun switchCamera() = inner.switchCamera()
    override fun setVideoBitrate(bps: Int) = inner.setVideoBitrate(bps)
    override fun setMuted(muted: Boolean) = inner.setMuted(muted)
    override fun setTorch(on: Boolean) = inner.setTorch(on)
    override fun release() { reconnectJob?.cancel(); inner.release() }

    /** The underlying engine, so the UI can attach a camera preview to it. */
    fun engine(): Broadcaster = inner

    private fun onInnerState(s: StreamState) {
        if (reconnectJob != null) {
            // The reconnect loop owns the visible state; only surface a recovered Live.
            if (s is StreamState.Live) _state.value = s
            return
        }
        when (s) {
            is StreamState.Idle, is StreamState.Error -> if (wantLive) startReconnect() else _state.value = s
            else -> _state.value = s
        }
    }

    private fun startReconnect() {
        val c = current ?: return
        reconnectJob = scope.launch {
            for (n in 1..reconnect.maxAttempts) {
                if (!wantLive) break
                _state.value = StreamState.Reconnecting(n)
                delay(reconnect.delayForAttempt(n))
                if (!wantLive) break
                inner.stop()          // reset any half-open state
                inner.start(c)
                withTimeoutOrNull(8000) {
                    inner.state.first { it is StreamState.Live || it is StreamState.Error }
                }
                if (inner.state.value is StreamState.Live) { reconnectJob = null; return@launch }
            }
            if (wantLive) { _state.value = StreamState.Error("Reconnect failed"); wantLive = false }
            reconnectJob = null
        }
    }
}
