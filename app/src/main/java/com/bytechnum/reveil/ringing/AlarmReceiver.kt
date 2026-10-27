package com.bytechnum.reveil.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Reçoit l'alarme à l'heure dite et lance le service de sonnerie au premier plan. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, NO_ALARM_ID)
        if (alarmId == NO_ALARM_ID) return
        ContextCompat.startForegroundService(context, AlarmService.fire(context, alarmId))
    }

    companion object {
        const val ACTION_FIRE = "com.bytechnum.reveil.action.FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val NO_ALARM_ID = -1
    }
}
