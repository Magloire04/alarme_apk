package com.bytechnum.reveil.ringing

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bytechnum.reveil.data.audio.AlarmPlayer
import com.bytechnum.reveil.data.audio.AlarmVibrator
import com.bytechnum.reveil.data.audio.AlarmVolumeController
import com.bytechnum.reveil.data.audio.AudioManagerStreamVolume
import com.bytechnum.reveil.data.prefs.RingingStateStore
import com.bytechnum.reveil.data.prefs.SettingsStore
import com.bytechnum.reveil.domain.alarm.EscalationPolicy
import com.bytechnum.reveil.domain.alarm.EscalationStage
import com.bytechnum.reveil.domain.alarm.FireDecision
import com.bytechnum.reveil.domain.alarm.RestartDecision
import com.bytechnum.reveil.domain.alarm.RingingRules
import com.bytechnum.reveil.domain.alarm.computeTargetVolumeIndex
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.RingingState
import com.bytechnum.reveil.domain.model.SoundId
import com.bytechnum.reveil.domain.model.WakeEvent
import com.bytechnum.reveil.domain.model.WakeMethod
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.WakeEventStore
import com.bytechnum.reveil.domain.usecase.HandleAlarmFired
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Fait sonner une alarme au premier plan : son, vibration et volume montent par paliers
 * jusqu'à l'arrêt. Relancé par le système s'il est tué, il reprend au bon palier.
 */
@AndroidEntryPoint
class AlarmService : LifecycleService() {
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

    private val volumeController by lazy { AlarmVolumeController(AudioManagerStreamVolume(audioManager)) }
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringingJob: Job? = null
    private var ringingAlarm: Alarm? = null
    private var currentStage: EscalationStage? = null
    private var isPausedByCall = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Obligatoire dans les secondes qui suivent startForegroundService, avant tout travail.
        startInForeground(ringingAlarm?.label.orEmpty())
        when (intent?.action) {
            ACTION_FIRE -> lifecycleScope.launch { onFire(intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)) }
            ACTION_STOP_PROVISIONAL -> lifecycleScope.launch { finish(method = null) }
            null -> lifecycleScope.launch { onRestart() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private suspend fun onFire(alarmId: Int) {
        val nowMillis = System.currentTimeMillis()
        val alarm = handleAlarmFired(alarmId)
        val current = readCurrentRinging()
        if (alarm == null) {
            if (ringingJob?.isActive != true) stopRinging()
            return
        }
        when (val decision = RingingRules.decideOnFire(current, alarm.id, nowMillis)) {
            is FireDecision.Start -> {
                ringingStateStore.write(decision.state)
                startRinging(alarm, decision.state)
            }

            FireDecision.Skip -> wakeEventStore.record(
                WakeEvent(
                    alarmId = alarm.id,
                    firedAt = nowMillis,
                    dismissedAt = null,
                    method = WakeMethod.SKIPPED,
                    repsDone = 0,
                    durationSec = 0,
                ),
            )
        }
    }

    // Un état enregistré alors que ce service ne sonne pas vient d'une sonnerie interrompue :
    // il est consigné comme manqué et effacé, sinon il ferait sauter toutes les alarmes suivantes.
    private suspend fun readCurrentRinging(): RingingState? {
        val saved = ringingStateStore.read() ?: return null
        if (ringingJob?.isActive == true) return saved
        recordWake(saved, WakeMethod.MISSED)
        ringingStateStore.clear()
        return null
    }

    private suspend fun onRestart() {
        when (val decision = RingingRules.decideOnRestart(ringingStateStore.read(), System.currentTimeMillis())) {
            is RestartDecision.Resume -> {
                val alarm = alarmStore.getById(decision.state.alarmId) ?: Alarm.createDefault(decision.state.firedAt)
                startRinging(alarm.copy(id = decision.state.alarmId), decision.state)
            }

            is RestartDecision.ExpireAsMissed -> finish(WakeMethod.MISSED, decision.state)

            RestartDecision.Nothing -> stopRinging()
        }
    }

    private suspend fun startRinging(alarm: Alarm, state: RingingState) {
        ringingAlarm = alarm
        acquireWakeLock()
        startInForeground(alarm.label)
        val maxVolumePercent = settingsStore.maxVolumePercent.first()
        volumeController.begin()
        player.start(alarm.soundId, onFocusLost = ::pauseForCall, onFocusGained = ::resumeAfterCall)
        ringingJob = lifecycleScope.launch {
            while (isActive) {
                val stage = EscalationPolicy.getStageAt(System.currentTimeMillis() - state.firedAt)
                if (stage != currentStage) applyStage(stage, alarm, maxVolumePercent)
                if (!isPausedByCall) volumeController.enforce()
                session.update(RingingUiState(alarm.id, alarm.label, state.firedAt, stage))
                if (stage.shouldStop) {
                    // Hors de cette boucle : finish annule ringingJob et ne doit pas s'annuler lui-même.
                    lifecycleScope.launch { finish(WakeMethod.MISSED, state) }
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private fun applyStage(stage: EscalationStage, alarm: Alarm, maxVolumePercent: Int) {
        val previous = currentStage
        currentStage = stage
        volumeController.apply(
            computeTargetVolumeIndex(volumeController.maxIndex, maxVolumePercent, stage.volumeFraction),
        )
        if (alarm.hasVibration && !isPausedByCall && stage.vibration != previous?.vibration) {
            vibrator.vibrate(stage.vibration)
        }
        if (stage.isStrident && previous?.isStrident != true) player.switchTo(SoundId.STRIDENT)
    }

    private fun pauseForCall() {
        isPausedByCall = true
        player.pause()
        vibrator.stop()
    }

    private fun resumeAfterCall() {
        if (!isPausedByCall) return
        isPausedByCall = false
        player.resume()
        val stage = currentStage
        if (ringingAlarm?.hasVibration == true && stage != null) vibrator.vibrate(stage.vibration)
    }

    /** Termine la sonnerie ; [method] nul pour l'arrêt provisoire, qui ne laisse pas de trace. */
    private suspend fun finish(method: WakeMethod?, state: RingingState? = null) {
        val ringingState = state ?: ringingStateStore.read()
        if (method != null && ringingState != null) recordWake(ringingState, method)
        stopRinging()
    }

    private suspend fun recordWake(state: RingingState, method: WakeMethod) {
        val nowMillis = System.currentTimeMillis()
        wakeEventStore.record(
            WakeEvent(
                alarmId = state.alarmId,
                firedAt = state.firedAt,
                dismissedAt = nowMillis,
                method = method,
                repsDone = 0,
                durationSec = ((nowMillis - state.firedAt) / 1000).toInt(),
            ),
        )
    }

    private suspend fun stopRinging() {
        ringingJob?.cancel()
        ringingJob = null
        player.stop()
        vibrator.stop()
        volumeController.restore()
        ringingStateStore.clear()
        session.update(null)
        ringingAlarm = null
        currentStage = null
        isPausedByCall = false
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground(label: String) {
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

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { acquire(EscalationPolicy.MAX_RING_MS + WAKE_LOCK_MARGIN_MS) }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        const val ACTION_FIRE = "com.bytechnum.reveil.action.RING"
        const val ACTION_STOP_PROVISIONAL = "com.bytechnum.reveil.action.STOP_PROVISIONAL"
        private const val TICK_MS = 1_000L
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
