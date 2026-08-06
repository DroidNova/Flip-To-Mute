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

interface PausedNotificationController {
    fun showPausedNotification()
    fun cancelPausedNotification()
}

class MonitoringNotificationManager(context: Context) : PausedNotificationController {
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
        val pauseIntent = PendingIntent.getService(
            context,
            PAUSE_REQUEST_CODE,
            FlipMonitoringService.createPauseIntent(context),
            flags,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_flip)
            .setContentTitle(context.getString(R.string.monitoring_notification_title))
            .setContentText(context.getString(R.string.monitoring_notification_text))
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.pause_monitoring), pauseIntent)
            .addAction(0, context.getString(R.string.turn_off), stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    override fun showPausedNotification() {
        createChannel()
        try {
            notificationManager?.notify(PAUSED_NOTIFICATION_ID, buildPausedNotification())
        } catch (_: SecurityException) {
            // Notification access may have been revoked while monitoring was paused.
        }
    }

    override fun cancelPausedNotification() {
        notificationManager?.cancel(PAUSED_NOTIFICATION_ID)
    }

    internal fun buildPausedNotification(): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            context, PAUSED_CONTENT_REQUEST_CODE, Intent(context, MainActivity::class.java), flags,
        )
        val resumeIntent = PendingIntent.getForegroundService(
            context, RESUME_REQUEST_CODE, FlipMonitoringService.createResumeIntent(context), flags,
        )
        val stopIntent = PendingIntent.getService(
            context, PAUSED_STOP_REQUEST_CODE, FlipMonitoringService.createStopIntent(context), flags,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_flip)
            .setContentTitle(context.getString(R.string.monitoring_paused_title))
            .setContentText(context.getString(R.string.monitoring_paused_notification_text))
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.resume_monitoring), resumeIntent)
            .addAction(0, context.getString(R.string.turn_off), stopIntent)
            .setOngoing(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "flip_monitoring"
        const val NOTIFICATION_ID = 1001
        const val PAUSED_NOTIFICATION_ID = 1002
        private const val PAUSE_REQUEST_CODE = 2
        private const val RESUME_REQUEST_CODE = 3
        private const val PAUSED_STOP_REQUEST_CODE = 4
        private const val PAUSED_CONTENT_REQUEST_CODE = 5
    }
}
