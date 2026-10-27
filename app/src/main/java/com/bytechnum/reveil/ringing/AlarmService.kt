package com.bytechnum.reveil.ringing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** Service de sonnerie. Version provisoire, remplacée par la tâche #7. */
class AlarmService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_FIRE = "com.bytechnum.reveil.action.RING"

        fun fire(context: Context, alarmId: Int): Intent = Intent(context, AlarmService::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
    }
}
