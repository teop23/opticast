package com.opticast.data

import com.opticast.model.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConnectionRepository(
    private val profileStore: ProfileStore,
    private val secretStore: SecretStore
) {
    /** Profiles with their secrets rehydrated from the encrypted store. */
    fun connections(): Flow<List<Connection>> =
        profileStore.profiles().map { list ->
            list.map { it.copy(secret = secretStore.get(it.id)) }
        }

    suspend fun save(connection: Connection) {
        secretStore.put(connection.id, connection.secret)
        profileStore.upsert(connection)           // store strips the secret itself
    }

    suspend fun delete(id: String) {
        profileStore.delete(id)
        secretStore.remove(id)
    }

    fun selectedId(): Flow<String?> = profileStore.selectedId()
    suspend fun setSelected(id: String?) = profileStore.setSelectedId(id)

    fun focusMode(): Flow<String?> = profileStore.focusMode()
    suspend fun setFocusMode(mode: String) = profileStore.setFocusMode(mode)
}
