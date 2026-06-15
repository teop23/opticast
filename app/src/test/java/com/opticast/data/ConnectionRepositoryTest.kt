package com.opticast.data

import app.cash.turbine.test
import com.opticast.fakes.InMemoryProfileStore
import com.opticast.fakes.InMemorySecretStore
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionRepositoryTest {

    private val profiles = InMemoryProfileStore()
    private val secrets = InMemorySecretStore()
    private val repo = ConnectionRepository(profiles, secrets)

    private fun conn(id: String, secret: String?) = Connection(
        id, "n", StreamProtocol.RTMP, "h", 1935, "p", secret,
        1280, 720, 30, 2_500_000, 128_000
    )

    @Test fun `save then read rehydrates secret from secret store`() = runTest {
        repo.save(conn("a", "user:pass"))
        repo.connections().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("user:pass", list.first().secret)
        }
    }

    @Test fun `secret is stored in secret store, not profile store`() = runTest {
        repo.save(conn("a", "topsecret"))
        assertNull(profiles.profiles().value.first().secret) // profile store stripped
        assertEquals("topsecret", secrets.get("a"))           // secret store has it
    }

    @Test fun `delete removes profile and secret`() = runTest {
        repo.save(conn("a", "s"))
        repo.delete("a")
        repo.connections().test { assertEquals(0, awaitItem().size) }
        assertNull(secrets.get("a"))
    }
}
