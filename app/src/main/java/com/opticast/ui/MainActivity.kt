package com.opticast.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.opticast.OpticastApp
import com.opticast.model.StreamState
import com.opticast.service.StreamingService
import com.opticast.ui.theme.OpticastTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: StreamViewModel by viewModels {
        (application as OpticastApp).viewModelFactory()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled by UI re-query; mic denial => video-only path */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // draw under the system bars; screens pad for insets themselves
        // Share the singleton broadcaster with the service; do NOT start the service yet.
        // A camera|microphone foreground service may only be started once streaming begins
        // (and after CAMERA permission is granted), so it is driven off stream state below.
        StreamingService.broadcaster = (application as OpticastApp).broadcaster

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.map { it.streamState }.distinctUntilChanged().collect { state ->
                    val intent = Intent(this@MainActivity, StreamingService::class.java)
                    when (state) {
                        is StreamState.Connecting, is StreamState.Live, is StreamState.Reconnecting ->
                            ContextCompat.startForegroundService(this@MainActivity, intent)
                        is StreamState.Idle, is StreamState.Error ->
                            stopService(intent)
                    }
                }
            }
        }

        maybeAskBatteryExemption()
        setContent { OpticastTheme { OpticastNavHost(vm) } }
    }

    /**
     * One-time prompt to exempt the app from battery optimization, so aggressive OEMs
     * (HyperOS/MIUI) don't kill the foreground service mid-stream. Asked once; user can decline.
     */
    private fun maybeAskBatteryExemption() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val prefs = getSharedPreferences("opticast_prefs", Context.MODE_PRIVATE)
        if (!pm.isIgnoringBatteryOptimizations(packageName) && !prefs.getBoolean("asked_batt", false)) {
            prefs.edit().putBoolean("asked_batt", true).apply()
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }
}
