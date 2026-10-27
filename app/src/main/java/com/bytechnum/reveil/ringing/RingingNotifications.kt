package com.bytechnum.reveil.ringing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bytechnum.reveil.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RingingNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
) {
    fun createRingingNotification(label: String): Notification {
        ensureChannel()
        val openRinging = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, RingingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label.ifBlank { context.getString(R.string.ringing_notification_title) })
            .setContentText(context.getString(R.string.ringing_notification_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(openRinging, true)
            .setContentIntent(openRinging)
            .build()
    }

    // Le canal reste muet : le son et la vibration viennent du service, pour pouvoir les faire monter.
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ringing_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "alarm_ringing"
        private const val REQUEST_CODE = 0
    }
}
