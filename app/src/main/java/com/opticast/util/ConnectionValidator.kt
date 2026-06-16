package com.opticast.util

import com.opticast.model.Connection
import com.opticast.model.QualityPresets
import com.opticast.model.StreamProtocol

data class ValidationResult(val errors: List<String>, val warnings: List<String>) {
    val canSave get() = errors.isEmpty()
    val needsConfirm get() = errors.isEmpty() && warnings.isNotEmpty()
}

object ConnectionValidator {

    private val LOCAL_HOSTS = setOf("127.0.0.1", "localhost", "::1")

    fun validate(c: Connection): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Hard errors — block save.
        if (c.name.isBlank()) errors += "Name is required"
        if (c.host.isBlank()) errors += "Host is required"
        if (c.port !in 1..65535) errors += "Port must be between 1 and 65535"
        if (c.width <= 0 || c.height <= 0) errors += "Resolution must be positive"
        if (c.fps <= 0) errors += "FPS must be positive"
        if (c.videoBitrate <= 0) errors += "Video bitrate must be positive"

        // Soft warnings — allow with confirmation.
        addTransportWarning(c, warnings)
        if (errors.isEmpty()) addQualityWarnings(c, warnings)

        return ValidationResult(errors, warnings)
    }

    private fun addTransportWarning(c: Connection, warnings: MutableList<String>) {
        val remote = c.host.trim().lowercase() !in LOCAL_HOSTS &&
            !c.host.startsWith("192.168.") && !c.host.startsWith("10.")
        if (!remote) return
        val encrypted = when (c.protocol) {
            StreamProtocol.RTMP -> false
            StreamProtocol.SRT -> !c.secret.isNullOrBlank()
        }
        if (!encrypted) {
            warnings += "Stream and credentials will be sent unencrypted to a remote host. " +
                "Use SRT with a passphrase or an RTMPS server."
        }
    }

    private fun addQualityWarnings(c: Connection, warnings: MutableList<String>) {
        // A known-good preset never warns.
        if (QualityPresets.match(c) != null) return

        if (c.fps !in setOf(24, 30, 60)) {
            warnings += "${c.fps} fps is unusual — 24, 30 or 60 are safest across devices."
        }
        if (c.width % 16 != 0 || c.height % 16 != 0) {
            warnings += "Resolution ${c.width}×${c.height} isn't a multiple of 16; some encoders reject it."
        }
        if (c.height >= 1080 && c.fps >= 60) {
            warnings += "1080p60 is heavy for mid-range phones — expect dropped frames or overheating."
        }

        val recommended = c.width.toLong() * c.height * c.fps * 0.1 // ~0.1 bits/pixel/frame, H.264
        val mbps = c.videoBitrate / 1_000_000.0
        when {
            c.videoBitrate < 300_000 ->
                warnings += "%.1f Mbps is very low — video may look blocky.".format(mbps)
            c.videoBitrate > 12_000_000 ->
                warnings += "%.1f Mbps is very high — most links can't sustain it.".format(mbps)
            c.videoBitrate > recommended * 2 ->
                warnings += "%.1f Mbps is high for ${c.width}×${c.height}@${c.fps} — wastes bandwidth.".format(mbps)
            c.videoBitrate < recommended * 0.35 ->
                warnings += "%.1f Mbps is low for ${c.width}×${c.height}@${c.fps} — quality may suffer.".format(mbps)
        }
    }
}
