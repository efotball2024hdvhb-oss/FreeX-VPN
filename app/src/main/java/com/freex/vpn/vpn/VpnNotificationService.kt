package com.freex.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.freex.vpn.R

class VpnNotificationService : Service() {
    companion object {
        const val ACTION_START = "com.freex.vpn.START_NOTIFICATION"
        const val ACTION_STOP = "com.freex.vpn.STOP_NOTIFICATION"
        private const val CHANNEL_ID = "freex_vpn"
        private const val NOTIFICATION_ID = 4101

        fun start(context: android.content.Context, serverName: String) {
            val intent = Intent(context, VpnNotificationService::class.java)
                .setAction(ACTION_START)
                .putExtra("server", serverName)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, VpnNotificationService::class.java).setAction(ACTION_STOP))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val server = intent?.getStringExtra("server") ?: "VPN"
        startForeground(NOTIFICATION_ID, notification(server))
        return START_STICKY
    }

    private fun notification(server: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_freex)
            .setContentTitle(getString(R.string.notification_connected))
            .setContentText(server)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
