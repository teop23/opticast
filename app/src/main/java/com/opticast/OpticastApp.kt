package com.opticast

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.opticast.data.ConnectionRepository
import com.opticast.data.DataStoreProfileStore
import com.opticast.data.EncryptedSecretStore
import com.opticast.stream.Broadcaster
import com.opticast.stream.RootEncoderBroadcaster
import com.opticast.ui.StreamViewModel

class OpticastApp : Application() {

    lateinit var broadcaster: Broadcaster
        private set
    private lateinit var repository: ConnectionRepository

    override fun onCreate() {
        super.onCreate()
        broadcaster = RootEncoderBroadcaster(this)
        repository = ConnectionRepository(
            DataStoreProfileStore(this),
            EncryptedSecretStore(this)
        )
    }

    fun viewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { StreamViewModel(broadcaster, repository) }
    }
}
