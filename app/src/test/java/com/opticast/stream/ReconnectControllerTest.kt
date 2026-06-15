package com.opticast.stream

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectControllerTest {

    @Test fun `backoff schedule grows and caps`() {
        val c = ReconnectController(baseMs = 1000, capMs = 5000, maxAttempts = 6)
        assertEquals(1000, c.delayForAttempt(1))
        assertEquals(2000, c.delayForAttempt(2))
        assertEquals(4000, c.delayForAttempt(3))
        assertEquals(5000, c.delayForAttempt(4)) // capped
        assertEquals(5000, c.delayForAttempt(5)) // capped
    }

    @Test fun `runs reconnect attempts until success`() = runTest {
        val c = ReconnectController(baseMs = 1000, capMs = 5000, maxAttempts = 6)
        var attempts = 0
        val delays = mutableListOf<Long>()
        val ok = c.run(
            delayFn = { delays += it },
            attempt = { attempts++; attempts >= 3 } // succeeds on 3rd
        )
        assertEquals(true, ok)
        assertEquals(3, attempts)
        assertEquals(listOf(1000L, 2000L), delays) // delay before attempts 2 and 3
    }

    @Test fun `gives up after max attempts`() = runTest {
        val c = ReconnectController(baseMs = 1000, capMs = 5000, maxAttempts = 3)
        var attempts = 0
        val ok = c.run(delayFn = {}, attempt = { attempts++; false })
        assertEquals(false, ok)
        assertEquals(3, attempts)
    }
}
