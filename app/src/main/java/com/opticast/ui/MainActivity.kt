package com.opticast.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.opticast.OpticastApp
import com.opticast.service.StreamingService

class MainActivity : ComponentActivity() {

    private val vm: StreamViewModel by viewModels {
        (application as OpticastApp).viewModelFactory()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled by UI re-query; mic denial => video-only path */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StreamingService.broadcaster = (application as OpticastApp).broadcaster
        startService(Intent(this, StreamingService::class.java))
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
        setContent { OpticastNavHost(vm) }
    }
}
