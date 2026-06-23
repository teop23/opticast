package com.opticast.stream

import com.opticast.model.Connection
import com.opticast.model.FocusMode
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the streaming engine. The only thing that touches RootEncoder is the
 * production implementation; everything else (UI, controllers) depends on this interface.
 */
interface Broadcaster {
    val state: StateFlow<StreamState>
    val stats: StateFlow<StreamStats>

    /** Reported bitrate from the network layer (bps), for adaptive bitrate. */
    val reportedBitrate: StateFlow<Long>

    fun start(connection: Connection)
    fun stop()
    fun switchCamera()
    fun setVideoBitrate(bps: Int)
    fun setMuted(muted: Boolean)
    fun setTorch(on: Boolean)
    fun setFocusMode(mode: FocusMode)
    fun release()
}
