package com.bytechnum.reveil.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bytechnum.reveil.di.ApplicationScope
import com.bytechnum.reveil.domain.usecase.RescheduleAll
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Le système efface les alarmes au redémarrage, et une heure ou un fuseau modifiés
 * déplacent les échéances : tout est replanifié, y compris avant le premier déverrouillage.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var rescheduleAll: RescheduleAll

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
