package com.opticast.util

import com.opticast.model.Connection
import com.opticast.model.StreamProtocol

data class ValidationResult(val errors: List<String>, val warnings: List<String>)

object ConnectionValidator {

    private val LOCAL_HOSTS = setOf("127.0.0.1", "localhost", "::1")

    fun validate(c: Connection): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (c.name.isBlank()) errors += "Name is required"
        if (c.host.isBlank()) errors += "Host is required"
        if (c.port !in 1..65535) errors += "Port must be between 1 and 65535"
        if (c.width <= 0 || c.height <= 0) errors += "Resolution must be positive"
        if (c.fps !in 1..120) errors += "FPS must be between 1 and 120"
        if (c.videoBitrate <= 0) errors += "Video bitrate must be positive"

        val remote = c.host.trim().lowercase() !in LOCAL_HOSTS &&
            !c.host.startsWith("192.168.") && !c.host.startsWith("10.")
        if (remote) {
            val encrypted = when (c.protocol) {
                StreamProtocol.RTMP -> false // plain RTMP is never encrypted
                StreamProtocol.SRT -> !c.secret.isNullOrBlank() // passphrase => AES
            }
            if (!encrypted) {
                warnings += "Stream and credentials will be sent unencrypted to a remote host. " +
                    "Use SRT with a passphrase or an RTMPS server."
            }
        }
        return ValidationResult(errors, warnings)
    }
}
