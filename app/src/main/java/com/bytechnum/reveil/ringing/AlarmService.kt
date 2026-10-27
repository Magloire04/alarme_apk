package com.bytechnum.reveil.ringing

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bytechnum.reveil.data.audio.AlarmPlayer
import com.bytechnum.reveil.data.audio.AlarmVibrator
import com.bytechnum.reveil.data.audio.AlarmVolumeController
import com.bytechnum.reveil.data.audio.AudioManagerStreamVolume
import com.bytechnum.reveil.data.prefs.SettingsStore
import com.bytechnum.reveil.domain.alarm.EscalationPolicy
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.RingingStateStore
import com.bytechnum.reveil.domain.port.WakeEventStore
import com.bytechnum.reveil.domain.usecase.HandleAlarmFired
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Service au premier plan qui héberge la sonnerie. Les décisions vivent dans [RingingCoordinator] ;
 * le service se charge du premier plan, de la notification et du verrou de veille.
 */
@AndroidEntryPoint
class AlarmService :
    LifecycleService(),
    RingingHost {
    @Inject lateinit var handleAlarmFired: HandleAlarmFired

    @Inject lateinit var alarmStore: AlarmStore

    @Inject lateinit var wakeEventStore: WakeEventStore

    @Inject lateinit var ringingStateStore: RingingStateStore

    @Inject lateinit var settingsStore: SettingsStore

    @Inject lateinit var player: AlarmPlayer

    @Inject lateinit var vibrator: AlarmVibrator

    @Inject lateinit var audioManager: AudioManager

    @Inject lateinit var session: RingingSession

    @Inject lateinit var notifications: RingingNotifications

    private lateinit var coordinator: RingingCoordinator
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentLabel = ""
    private val errorHandler =
        CoroutineExceptionHandler { _, throwable -> Log.e(TAG, "Échec de la sonnerie", throwable) }

    override fun onCreate() {
        super.onCreate()
        coordinator = RingingCoordinator(
            handleAlarmFired = handleAlarmFired,
            alarmStore = alarmStore,
            wakeEventStore = wakeEventStore,
            stateStore = ringingStateStore,
            readMaxVolumePercent = { settingsStore.maxVolumePercent.first() },
            sound = player,
            vibration = vibrator,
            volume = AlarmVolumeController(AudioManagerStreamVolume(audioManager)),
            callState = AudioCallState(audioManager),
            host = this,
            session = session,
            scope = lifecycleScope,
            nowMillis = System::currentTimeMillis,
            reportError = { what, throwable -> Log.w(TAG, what, throwable) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Obligatoire dans les secondes qui suivent startForegroundService, avant tout travail.
        showRinging(currentLabel)
        val command = when (intent?.action) {
            ACTION_FIRE -> RingingCommand.Fire(intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1))
            ACTION_STOP_PROVISIONAL -> RingingCommand.StopProvisional
            null -> RingingCommand.Restart
            else -> null
        }
        lifecycleScope.launch(errorHandler) {
            try {
                if (command == null) stop(startId) else coordinator.handle(command, startId)
            } finally {
                StartupWakeLock.release()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseAwake()
        super.onDestroy()
    }

    override fun showRinging(label: String) {
        currentLabel = label
        val notification = notifications.createRingingNotification(label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                ServiceCompat.startForeground(
                    this,
                    RingingNotifications.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
                )
                return
            } catch (_: SecurityException) {
                // Type réservé refusé sur cet appareil : repli sur la lecture média.
            }
        }
        ServiceCompat.startForeground(
            this,
            RingingNotifications.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    override fun keepAwake() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { acquire(EscalationPolicy.MAX_RING_MS + WAKE_LOCK_MARGIN_MS) }
    }

    override fun releaseAwake() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun stop(startId: Int) {
        // Refusé si une commande plus récente est arrivée : elle sera traitée, puis arrêtera le service.
        stopSelfResult(startId)
    }

    companion object {
        const val ACTION_FIRE = "com.bytechnum.reveil.action.RING"
        const val ACTION_STOP_PROVISIONAL = "com.bytechnum.reveil.action.STOP_PROVISIONAL"
        private const val TAG = "AlarmService"
        private const val WAKE_LOCK_MARGIN_MS = 60_000L
        private const val WAKE_LOCK_TAG = "reveil:ringing"

        fun fire(context: Context, alarmId: Int): Intent = Intent(context, AlarmService::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)

        fun stopProvisional(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_STOP_PROVISIONAL))
        }
    }
}

/** Appel en cours ou sonnerie d'appel entrant, d'après le mode audio. */
class AudioCallState(private val audioManager: AudioManager) : CallState {
    override fun isInCall(): Boolean = audioManager.mode in setOf(
        AudioManager.MODE_IN_CALL,
        AudioManager.MODE_IN_COMMUNICATION,
        AudioManager.MODE_RINGTONE,
    )
}
