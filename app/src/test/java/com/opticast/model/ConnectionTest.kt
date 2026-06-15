package com.opticast.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionTest {

    private fun base() = Connection(
        id = "1", name = "Home relay", protocol = StreamProtocol.RTMP,
        host = "192.168.178.208", port = 1935, path = "live/ride",
        secret = null, width = 1280, height = 720, fps = 30,
        videoBitrate = 2_500_000, audioBitrate = 128_000
    )

    @Test fun `rtmp endpoint without secret`() {
        assertEquals("rtmp://192.168.178.208:1935/live/ride", base().toEndpoint())
    }

    @Test fun `rtmp endpoint embeds user-pass secret`() {
        val c = base().copy(secret = "user:pass")
        assertEquals("rtmp://user:pass@192.168.178.208:1935/live/ride", c.toEndpoint())
    }

    @Test fun `srt endpoint puts path in streamid and passphrase in query`() {
        val c = base().copy(protocol = StreamProtocol.SRT, port = 8890, secret = "myphrase")
        assertEquals(
            "srt://192.168.178.208:8890?streamid=publish/live/ride&passphrase=myphrase",
            c.toEndpoint()
        )
    }

    @Test fun `srt endpoint without passphrase omits query passphrase`() {
        val c = base().copy(protocol = StreamProtocol.SRT, port = 8890, secret = null)
        assertEquals(
            "srt://192.168.178.208:8890?streamid=publish/live/ride",
            c.toEndpoint()
        )
    }
}
