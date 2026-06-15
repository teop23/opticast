package com.opticast.ui

import app.cash.turbine.test
import com.opticast.data.ConnectionRepository
import com.opticast.fakes.FakeBroadcaster
import com.opticast.fakes.InMemoryProfileStore
import com.opticast.fakes.InMemorySecretStore
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import com.opticast.model.StreamState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreamViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val broadcaster = FakeBroadcaster()
    private val repo = ConnectionRepository(InMemoryProfileStore(), InMemorySecretStore())
    private lateinit var vm: StreamViewModel

    private fun conn(id: String) = Connection(id, "n", StreamProtocol.RTMP, "127.0.0.1",
        1935, "live/x", null, 1280, 720, 30, 2_500_000, 128_000)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = StreamViewModel(broadcaster, repo)
    }

    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `goLive with a selected connection starts the broadcaster`() = runTest {
        vm.selectConnection(conn("a"))
        vm.goLive()
        assertEquals("a", broadcaster.startedWith?.id)
    }

    @Test fun `goLive with no selection sets an error and does not start`() = runTest {
        vm.goLive()
        assertEquals(null, broadcaster.startedWith)
        assertEquals(StreamState.Error("No connection selected"), vm.uiState.value.streamState)
    }

    @Test fun `broadcaster state propagates to ui state`() = runTest {
        vm.uiState.test {
            assertEquals(StreamState.Idle, awaitItem().streamState)
            broadcaster.emitState(StreamState.Live)
            assertEquals(StreamState.Live, awaitItem().streamState)
        }
    }

    @Test fun `toggleMute forwards to broadcaster and updates ui`() = runTest {
        vm.toggleMute()
        assertEquals(true, broadcaster.mutedFlag)
        assertEquals(true, vm.uiState.value.muted)
    }

    @Test fun `stop stops the broadcaster`() = runTest {
        vm.selectConnection(conn("a")); vm.goLive()
        vm.stop()
        assertEquals(StreamState.Idle, broadcaster.state.value)
    }
}
