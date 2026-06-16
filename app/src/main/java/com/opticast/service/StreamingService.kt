package com.opticast.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.opticast.stream.Broadcaster

class StreamingService : Service() {

    companion object {
        const val CHANNEL_ID = "opticast_stream"
        const val NOTIF_ID = 1
        const val ACTION_STOP = "com.opticast.action.STOP"
        // Set by the app graph before binding/starting (see OpticastApp / MainActivity).
        @Volatile var broadcaster: Broadcaster? = null
    }

    private val binder = LocalBinder()
    inner class LocalBinder : android.os.Binder() {
        fun broadcaster(): Broadcaster? = StreamingService.broadcaster
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Notification "Stop" button: stop the stream and tear the service down.
            // Works regardless of whether the Activity is in the foreground.
            broadcaster?.stop()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        acquireWakeLock()
        return START_STICKY
    }

    private fun startForegroundCompat() {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // 34
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        // Tapping the notification body opens the app (existing task brought to front).
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPi = PendingIntent.getActivity(this, 0, openIntent, flags)

        // The "Stop streaming" action routes back to this service with ACTION_STOP.
        val stopIntent = Intent(this, StreamingService::class.java).setAction(ACTION_STOP)
        val stopPi = PendingIntent.getService(this, 1, stopIntent, flags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Opticast")
            .setContentText("Streaming — tap to open")
            .setSmallIcon(com.opticast.R.drawable.ic_stat_opticast)
            .setColor(0xFFC6F000.toInt())          // lime accent tints the small-icon badge
            .setOngoing(true)
            .setContentIntent(contentPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop streaming", stopPi)
            .build()
    }

    private fun createChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Streaming", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Opticast::stream").also {
            it.acquire(4 * 60 * 60 * 1000L) // 4h safety cap
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }
}
