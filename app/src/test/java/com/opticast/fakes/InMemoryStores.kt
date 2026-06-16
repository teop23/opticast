package com.opticast.fakes

import com.opticast.data.ProfileStore
import com.opticast.data.SecretStore
import com.opticast.model.Connection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryProfileStore : ProfileStore {
    private val state = MutableStateFlow<List<Connection>>(emptyList())
    private val selected = MutableStateFlow<String?>(null)
    override fun profiles() = state.asStateFlow()
    override suspend fun upsert(connection: Connection) {
        val stripped = connection.copy(secret = null)
        state.value = state.value.filter { it.id != connection.id } + stripped
    }
    override suspend fun delete(id: String) {
        state.value = state.value.filter { it.id != id }
    }
    override fun selectedId() = selected.asStateFlow()
    override suspend fun setSelectedId(id: String?) { selected.value = id }
}

class InMemorySecretStore : SecretStore {
    val map = mutableMapOf<String, String>()
    override fun get(id: String) = map[id]
    override fun put(id: String, secret: String?) {
        if (secret == null) map.remove(id) else map[id] = secret
    }
    override fun remove(id: String) { map.remove(id) }
}
