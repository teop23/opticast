package com.opticast.data

/** Persists secrets (passwords / passphrases) in encrypted storage, keyed by connection id. */
interface SecretStore {
    fun get(id: String): String?
    fun put(id: String, secret: String?)
    fun remove(id: String)
}
