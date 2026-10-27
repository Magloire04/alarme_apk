package com.bytechnum.reveil.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bytechnum.reveil.MainActivity
import com.bytechnum.reveil.domain.port.AlarmScheduling
import com.bytechnum.reveil.ringing.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Alarmes exactes par setAlarmClock : exemptées du mode Doze et visibles dans la barre d'état. */
class AlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
) : AlarmScheduling {
    override fun schedule(alarmId: Int, triggerAtMillis: Long) {
        val fireIntent = createFireIntent(alarmId)
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, createShowIntent(alarmId)),
                fireIntent,
            )
        } catch (exception: SecurityException) {
            // Android 12 sans l'autorisation d'alarme exacte : mieux vaut une alarme approximative que rien.
            Log.w(TAG, "Alarme exacte refusée, repli approximatif", exception)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
        }
    }

    override fun cancel(alarmId: Int) {
        val fireIntent = createFireIntent(alarmId)
        alarmManager.cancel(fireIntent)
        fireIntent.cancel()
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun createFireIntent(alarmId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        alarmId,
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createShowIntent(alarmId: Int): PendingIntent = PendingIntent.getActivity(
        context,
        alarmId,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
