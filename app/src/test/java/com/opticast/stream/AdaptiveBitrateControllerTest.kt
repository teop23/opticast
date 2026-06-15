package com.opticast.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveBitrateControllerTest {

    private fun controller() = AdaptiveBitrateController(
        minBps = 500_000, maxBps = 4_000_000, startBps = 2_000_000, stepBps = 250_000
    )

    @Test fun `starts at start bitrate`() {
        assertEquals(2_000_000, controller().currentBps)
    }

    @Test fun `healthy link steps up but not above max`() {
        val c = controller()
        // reported >= 90% of target => step up
        repeat(20) { c.onReportedBitrate((c.currentBps * 0.95).toLong()) }
        assertEquals(4_000_000, c.currentBps)
    }

    @Test fun `congested link steps down but not below min`() {
        val c = controller()
        // reported < 50% of target => step down
        repeat(20) { c.onReportedBitrate((c.currentBps * 0.2).toLong()) }
        assertEquals(500_000, c.currentBps)
    }

    @Test fun `step down emits new target to callback`() {
        var emitted = -1
        val c = AdaptiveBitrateController(500_000, 4_000_000, 2_000_000, 250_000) { emitted = it }
        c.onReportedBitrate(100_000) // very congested
        assertEquals(1_750_000, emitted)
        assertEquals(1_750_000, c.currentBps)
    }

    @Test fun `stable mid-range link does not change bitrate`() {
        val c = controller()
        // between 50% and 90% => hold
        c.onReportedBitrate((c.currentBps * 0.7).toLong())
        assertEquals(2_000_000, c.currentBps)
    }

    @Test fun `current bitrate always within bounds`() {
        val c = controller()
        repeat(100) { c.onReportedBitrate(if (it % 2 == 0) 10_000_000 else 1) }
        assertTrue(c.currentBps in 500_000..4_000_000)
    }
}
