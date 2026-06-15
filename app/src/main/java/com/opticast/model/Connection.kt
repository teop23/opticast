package com.opticast.model

enum class StreamProtocol { RTMP, SRT }

data class Connection(
    val id: String,
    val name: String,
    val protocol: StreamProtocol,
    val host: String,
    val port: Int,
    val path: String,
    val secret: String?,          // RTMP "user:pass" or SRT passphrase
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrate: Int,        // bps
    val audioBitrate: Int         // bps
) {
    fun toEndpoint(): String = when (protocol) {
        StreamProtocol.RTMP -> {
            val auth = if (secret.isNullOrBlank()) "" else "$secret@"
            "rtmp://$auth$host:$port/$path"
        }
        StreamProtocol.SRT -> {
            val pass = if (secret.isNullOrBlank()) "" else "&passphrase=$secret"
            "srt://$host:$port?streamid=publish/$path$pass"
        }
    }
}
