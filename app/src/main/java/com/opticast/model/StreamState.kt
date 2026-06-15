package com.opticast.model

sealed interface StreamState {
    data object Idle : StreamState
    data object Connecting : StreamState
    data object Live : StreamState
    data class Reconnecting(val attempt: Int) : StreamState
    data class Error(val reason: String) : StreamState
}

data class StreamStats(
    val bitrateBps: Long = 0,
    val fps: Int = 0,
    val droppedFrames: Long = 0,
    val uptimeSeconds: Long = 0
)
