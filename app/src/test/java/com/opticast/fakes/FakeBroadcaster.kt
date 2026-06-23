package com.opticast.fakes

import com.opticast.model.Connection
import com.opticast.model.FocusMode
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import com.opticast.stream.Broadcaster
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBroadcaster : Broadcaster {
    override val state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val stats = MutableStateFlow(StreamStats())
    override val reportedBitrate = MutableStateFlow(0L)

    var startedWith: Connection? = null
    var lastBitrate: Int? = null
    var mutedFlag = false
    var torchFlag = false
    var focusModeFlag = FocusMode.AUTO
    var switchCount = 0
    var released = false

    override fun start(connection: Connection) {
        startedWith = connection
        state.value = StreamState.Connecting
    }
    override fun stop() { state.value = StreamState.Idle }
    override fun switchCamera() { switchCount++ }
    override fun setVideoBitrate(bps: Int) { lastBitrate = bps }
    override fun setMuted(muted: Boolean) { mutedFlag = muted }
    override fun setTorch(on: Boolean) { torchFlag = on }
    override fun setFocusMode(mode: FocusMode) { focusModeFlag = mode }
    override fun release() { released = true }

    // Test helpers
    fun emitState(s: StreamState) { state.value = s }
    fun emitReportedBitrate(bps: Long) { reportedBitrate.value = bps }
}
