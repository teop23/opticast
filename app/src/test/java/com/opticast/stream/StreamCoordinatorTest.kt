package com.opticast.stream

import app.cash.turbine.test
import com.opticast.fakes.FakeBroadcaster
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import com.opticast.model.StreamState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamCoordinatorTest {

    private fun conn(bitrate: Int = 2_000_000) = Connection(
        "1", "n", StreamProtocol.RTMP, "h", 1935, "p", null,
        1280, 720, 30, bitrate, 128_000
    )

    @Test fun `adaptive bitrate steps down on congestion`() = runTest {
        val fake = FakeBroadcaster()
        val coord = StreamCoordinator(fake, backgroundScope)
        coord.start(conn(bitrate = 2_000_000)) // abr: max 2M, start 2M, step 200k
        runCurrent()
        fake.emitReportedBitrate(100_000)       // ~5% of target -> congested -> step down
        advanceUntilIdle()
        assertEquals(1_800_000, fake.lastBitrate)
    }

    @Test fun `enters reconnecting on unexpected drop`() = runTest {
        val fake = FakeBroadcaster()
        val coord = StreamCoordinator(fake, backgroundScope, ReconnectController(baseMs = 10, capMs = 50, maxAttempts = 3))
        coord.state.test {
            assertEquals(StreamState.Idle, awaitItem())
            coord.start(conn())
            assertEquals(StreamState.Connecting, awaitItem())
            fake.emitState(StreamState.Live)
            assertEquals(StreamState.Live, awaitItem())
            fake.emitState(StreamState.Error("drop")) // unexpected drop while wanting to be live
            assertEquals(StreamState.Reconnecting(1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `stop returns to idle`() = runTest {
        val fake = FakeBroadcaster()
        val coord = StreamCoordinator(fake, backgroundScope)
        coord.start(conn()); runCurrent()
        fake.emitState(StreamState.Live); runCurrent()
        coord.stop(); advanceUntilIdle()
        assertEquals(StreamState.Idle, coord.state.value)
    }
}
