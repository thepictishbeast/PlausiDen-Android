package com.plausiden.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * Creates the foreground service notification channel and notification.
 *
 * IMPORTANT: All notification text must be INNOCUOUS.
 * Title: "Device Optimization"
 * Text: "Device optimization running"
 * This must NOT reveal the true purpose of the app.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "plausiden_foreground"
    private const val CHANNEL_NAME = "Device Optimization"
    private const val CHANNEL_DESCRIPTION = "Background optimization service"

    /**
     * Create the notification channel. Must be called before posting any notification.
     * Safe to call multiple times (Android ignores duplicate channels).
     */
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW  // Low importance = no sound/vibration
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET  // Hide on lockscreen
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Build the foreground service notification.
     *
     * Uses innocuous text so the notification does not draw attention.
     * Tapping the notification opens MainActivity.
     */
    fun buildForegroundNotification(context: Context): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_manage)  // TODO: custom icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
