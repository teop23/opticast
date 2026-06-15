package com.opticast.util

import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionValidatorTest {

    private fun c() = Connection("1", "n", StreamProtocol.RTMP, "relay.example.com",
        1935, "live/x", null, 1280, 720, 30, 2_500_000, 128_000)

    @Test fun `valid connection has no errors`() {
        assertTrue(ConnectionValidator.validate(c()).errors.isEmpty())
    }

    @Test fun `blank host is an error`() {
        assertTrue(ConnectionValidator.validate(c().copy(host = " ")).errors.isNotEmpty())
    }

    @Test fun `port out of range is an error`() {
        assertTrue(ConnectionValidator.validate(c().copy(port = 70000)).errors.isNotEmpty())
    }

    @Test fun `plain rtmp to remote host warns about encryption`() {
        val r = ConnectionValidator.validate(c())
        assertTrue(r.warnings.any { it.contains("unencrypted", ignoreCase = true) })
    }

    @Test fun `plain rtmp to localhost does not warn`() {
        val r = ConnectionValidator.validate(c().copy(host = "127.0.0.1"))
        assertEquals(emptyList<String>(), r.warnings)
    }

    @Test fun `srt with passphrase to remote does not warn`() {
        val r = ConnectionValidator.validate(
            c().copy(protocol = StreamProtocol.SRT, port = 8890, secret = "phrase")
        )
        assertEquals(emptyList<String>(), r.warnings)
    }
}
