package com.opticast.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opticast.model.Connection
import com.opticast.model.StreamCodec
import com.opticast.model.StreamProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "opticast_profiles")
private val PROFILES_KEY = stringPreferencesKey("profiles_json")

class DataStoreProfileStore(private val context: Context) : ProfileStore {

    override fun profiles(): Flow<List<Connection>> =
        context.dataStore.data.map { prefs -> decode(prefs[PROFILES_KEY] ?: "[]") }

    override suspend fun upsert(connection: Connection) {
        val stripped = connection.copy(secret = null)
        context.dataStore.edit { prefs ->
            val current = decode(prefs[PROFILES_KEY] ?: "[]")
                .filter { it.id != stripped.id } + stripped
            prefs[PROFILES_KEY] = encode(current)
        }
    }

    override suspend fun delete(id: String) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs[PROFILES_KEY] ?: "[]").filter { it.id != id }
            prefs[PROFILES_KEY] = encode(current)
        }
    }

    private fun encode(list: List<Connection>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("protocol", c.protocol.name)
                put("host", c.host); put("port", c.port); put("path", c.path)
                put("width", c.width); put("height", c.height); put("fps", c.fps)
                put("videoBitrate", c.videoBitrate); put("audioBitrate", c.audioBitrate)
                put("codec", c.codec.name)
            })
        }
        return arr.toString()
    }

    private fun decode(json: String): List<Connection> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Connection(
                id = o.getString("id"), name = o.getString("name"),
                protocol = StreamProtocol.valueOf(o.getString("protocol")),
                host = o.getString("host"), port = o.getInt("port"), path = o.getString("path"),
                secret = null,
                width = o.getInt("width"), height = o.getInt("height"), fps = o.getInt("fps"),
                videoBitrate = o.getInt("videoBitrate"), audioBitrate = o.getInt("audioBitrate"),
                codec = StreamCodec.valueOf(o.optString("codec", "H264"))
            )
        }
    }
}
