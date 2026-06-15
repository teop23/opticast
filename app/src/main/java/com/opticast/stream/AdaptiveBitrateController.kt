package com.opticast.stream

/**
 * Pure-logic adaptive bitrate. Fed reported bitrate samples (bps); emits a new target bitrate
 * via [onChange] when it steps up or down. Caller wires onChange to Broadcaster.setVideoBitrate.
 */
class AdaptiveBitrateController(
    private val minBps: Int,
    private val maxBps: Int,
    startBps: Int,
    private val stepBps: Int,
    private val onChange: (Int) -> Unit = {}
) {
    var currentBps: Int = startBps.coerceIn(minBps, maxBps)
        private set

    fun onReportedBitrate(reportedBps: Long) {
        val ratio = if (currentBps == 0) 0.0 else reportedBps.toDouble() / currentBps
        val next = when {
            ratio < 0.5 -> (currentBps - stepBps)
            ratio >= 0.9 -> (currentBps + stepBps)
            else -> currentBps
        }.coerceIn(minBps, maxBps)

        if (next != currentBps) {
            currentBps = next
            onChange(next)
        }
    }
}
