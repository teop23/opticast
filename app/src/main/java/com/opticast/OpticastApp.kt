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
import com.opticast.stream.StreamCoordinator
import com.opticast.ui.StreamViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OpticastApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var broadcaster: Broadcaster
        private set
    private lateinit var repository: ConnectionRepository

    override fun onCreate() {
        super.onCreate()
        broadcaster = StreamCoordinator(RootEncoderBroadcaster(this), appScope)
        repository = ConnectionRepository(
            DataStoreProfileStore(this),
            EncryptedSecretStore(this)
        )
    }

    fun viewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { StreamViewModel(broadcaster, repository) }
    }
}
