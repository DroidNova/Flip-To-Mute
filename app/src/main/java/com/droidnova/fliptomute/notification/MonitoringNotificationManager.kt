package com.droidnova.fliptomute.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.droidnova.fliptomute.MainActivity
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.service.FlipMonitoringService

class MonitoringNotificationManager(context: Context) {
    private val context = context.applicationContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.monitoring_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager?.createNotificationChannel(channel)
    }

    fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            flags,
        )
        val stopIntent = PendingIntent.getService(
            context,
            1,
            FlipMonitoringService.createStopIntent(context),
            flags,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_flip)
            .setContentTitle(context.getString(R.string.monitoring_notification_title))
            .setContentText(context.getString(R.string.monitoring_notification_text))
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.turn_off), stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "flip_monitoring"
        const val NOTIFICATION_ID = 1001
    }
}
