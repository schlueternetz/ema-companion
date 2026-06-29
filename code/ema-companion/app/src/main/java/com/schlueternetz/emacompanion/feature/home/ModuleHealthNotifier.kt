package com.schlueternetz.emacompanion.feature.home

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthState
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus

/**
 * Posts or clears the module health notification based on the current [ModuleHealthState].
 * - YELLOW or RED: replaces any previous notification (does not stack).
 * - GREEN or UNKNOWN: cancels the previous notification.
 * Silent no-op if POST_NOTIFICATIONS permission is not granted on API 33+.
 */
object ModuleHealthNotifier {

    private const val CHANNEL_ID = "module_health"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_module_health_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(
                        R.string.notification_channel_module_health_description,
                    )
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun notify(context: Context, state: ModuleHealthState) {
        val manager = context.getSystemService(NotificationManager::class.java)
        when (state.status) {
            ModuleHealthStatus.YELLOW, ModuleHealthStatus.RED -> postNotification(context, manager, state)
            else -> manager.cancel(NOTIFICATION_ID)
        }
    }

    private fun postNotification(
        context: Context,
        manager: NotificationManager,
        state: ModuleHealthState,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val (title, text) = if (state.status == ModuleHealthStatus.RED) {
            context.getString(R.string.notification_module_health_red_title) to
                context.getString(R.string.notification_module_health_red_text)
        } else {
            context.getString(R.string.notification_module_health_yellow_title) to
                context.getString(R.string.notification_module_health_yellow_text)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
