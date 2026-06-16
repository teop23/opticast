package com.opticast.model

/** A known-good resolution + fps + video-bitrate combination. */
data class QualityPreset(
    val label: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrate: Int, // bps
)

object QualityPresets {
    val ALL = listOf(
        QualityPreset("480p30", 854, 480, 30, 1_000_000),
        QualityPreset("540p30", 960, 540, 30, 1_500_000),
        QualityPreset("720p30", 1280, 720, 30, 2_500_000),
        QualityPreset("720p60", 1280, 720, 60, 4_000_000),
        QualityPreset("1080p30", 1920, 1080, 30, 4_500_000),
        QualityPreset("1080p60", 1920, 1080, 60, 6_500_000),
    )

    /** The preset matching a connection's exact res/fps/bitrate, or null if it's a custom combo. */
    fun match(c: Connection): QualityPreset? = ALL.firstOrNull {
        it.width == c.width && it.height == c.height &&
            it.fps == c.fps && it.videoBitrate == c.videoBitrate
    }

    fun apply(c: Connection, p: QualityPreset): Connection =
        c.copy(width = p.width, height = p.height, fps = p.fps, videoBitrate = p.videoBitrate)
}
