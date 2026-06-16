package com.opticast.data

import com.opticast.model.Connection
import kotlinx.coroutines.flow.Flow

/** Persists everything about a Connection EXCEPT its secret. */
interface ProfileStore {
    fun profiles(): Flow<List<Connection>>
    suspend fun upsert(connection: Connection)      // secret field ignored here
    suspend fun delete(id: String)
    fun selectedId(): Flow<String?>
    suspend fun setSelectedId(id: String?)
}
