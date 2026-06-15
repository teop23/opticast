package com.opticast.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedSecretStore(context: Context) : SecretStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "opticast_secrets",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun get(id: String): String? = prefs.getString(id, null)
    override fun put(id: String, secret: String?) {
        prefs.edit().apply { if (secret == null) remove(id) else putString(id, secret) }.apply()
    }
    override fun remove(id: String) { prefs.edit().remove(id).apply() }
}
