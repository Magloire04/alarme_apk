package com.bytechnum.reveil.ringing

import com.bytechnum.reveil.data.audio.AlarmVolumeController
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
import com.bytechnum.reveil.domain.port.RingingStateStore
import com.bytechnum.reveil.domain.port.WakeEventStore
import com.bytechnum.reveil.domain.usecase.HandleAlarmFired
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface RingingCommand {
    data class Fire(val alarmId: Int) : RingingCommand

    data object Restart : RingingCommand

    data object StopProvisional : RingingCommand
}

/**
 * Orchestration de la sonnerie, hors du service pour être testée sur la JVM.
 * Les commandes passent une à une : deux alarmes simultanées ne peuvent pas démarrer deux sons.
 * Le son démarre avant toute écriture : un stockage en panne ne doit jamais rendre l'alarme muette.
 */
class RingingCoordinator(
    private val handleAlarmFired: HandleAlarmFired,
    private val alarmStore: AlarmStore,
    private val wakeEventStore: WakeEventStore,
    private val stateStore: RingingStateStore,
    private val readMaxVolumePercent: suspend () -> Int,
    private val sound: SoundOutput,
    private val vibration: VibrationOutput,
    private val volume: AlarmVolumeController,
    private val callState: CallState,
    private val host: RingingHost,
    private val session: RingingSession,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long,
    private val reportError: (String, Throwable) -> Unit = { _, _ -> },
) {
    private class ActiveRinging(val alarm: Alarm, val state: RingingState, val maxVolumePercent: Int) {
        var stage: EscalationStage? = null
        var isPausedByCall = false
    }

    private val mutex = Mutex()
    private var active: ActiveRinging? = null
    private var loopJob: Job? = null
    private var lastStartId = 0

    suspend fun handle(command: RingingCommand, startId: Int) = mutex.withLock {
        lastStartId = startId
        when (command) {
            is RingingCommand.Fire -> onFire(command.alarmId)
            RingingCommand.Restart -> onRestart()
            RingingCommand.StopProvisional -> finish(method = null)
        }
    }

    private suspend fun onFire(alarmId: Int) {
        val now = nowMillis()
        val alarm = readFiredAlarm(alarmId, now)
        if (active == null) discardStaleState()
        if (alarm == null) {
            if (active == null) host.stop(lastStartId)
            return
        }
        when (val decision = RingingRules.decideOnFire(active?.state, alarm.id, now)) {
            is FireDecision.Start -> {
                startRinging(alarm, decision.state)
                attempt("Enregistrement de la sonnerie") { stateStore.write(decision.state) }
            }

            FireDecision.Skip -> attempt("Enregistrement de l'alarme sautée") {
                wakeEventStore.record(
                    WakeEvent(
                        alarmId = alarm.id,
                        firedAt = now,
                        dismissedAt = null,
                        method = WakeMethod.SKIPPED,
                        repsDone = 0,
                        durationSec = 0,
                    ),
                )
            }
        }
    }

    /** Null pour une intention périmée. Si la base ne répond pas, sonner avec les réglages par défaut. */
    private suspend fun readFiredAlarm(alarmId: Int, now: Long): Alarm? = try {
        handleAlarmFired(alarmId)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        reportError("Lecture de l'alarme", exception)
        Alarm.createDefault(now).copy(id = alarmId)
    }

    private suspend fun onRestart() {
        if (active != null) return
        val saved = attempt("Lecture de la sonnerie") { stateStore.read() }
        when (val decision = RingingRules.decideOnRestart(saved, nowMillis())) {
            is RestartDecision.Resume -> {
                val alarmId = decision.state.alarmId
                val alarm = attempt("Lecture de l'alarme") { alarmStore.getById(alarmId) }
                    ?: Alarm.createDefault(decision.state.firedAt).copy(id = alarmId)
                startRinging(alarm, decision.state)
            }

            is RestartDecision.ExpireAsMissed -> finish(WakeMethod.MISSED, decision.state)

            RestartDecision.Nothing -> host.stop(lastStartId)
        }
    }

    // Un état enregistré alors que rien ne sonne vient d'une sonnerie interrompue :
    // consigné comme manqué et effacé, sinon il ferait sauter toutes les alarmes suivantes.
    private suspend fun discardStaleState() {
        val stale = attempt("Lecture de la sonnerie") { stateStore.read() } ?: return
        recordWake(stale, WakeMethod.MISSED)
        attempt("Effacement de la sonnerie") { stateStore.clear() }
    }

    private suspend fun startRinging(alarm: Alarm, state: RingingState) {
        host.keepAwake()
        host.showRinging(alarm.label)
        volume.begin()
        sound.start(alarm.soundId)
        val maxVolumePercent = attempt("Lecture du volume maximal") { readMaxVolumePercent() } ?: DEFAULT_MAX_PERCENT
        val ringing = ActiveRinging(alarm, state, maxVolumePercent)
        active = ringing
        tick(ringing)
        loopJob = scope.launch {
            while (true) {
                delay(TICK_MS)
                mutex.withLock { active?.let { tick(it) } }
            }
        }
    }

    private suspend fun tick(ringing: ActiveRinging) {
        val stage = EscalationPolicy.getStageAt(nowMillis() - ringing.state.firedAt)
        followCallState(ringing, stage)
        if (stage != ringing.stage) applyStage(ringing, stage)
        if (!ringing.isPausedByCall) volume.enforce()
        session.update(RingingUiState(ringing.alarm.id, ringing.alarm.label, ringing.state.firedAt, stage))
        if (stage.shouldStop) finish(WakeMethod.MISSED)
    }

    // Interrogé à chaque seconde : le mode d'appel arrive parfois après la perte de focus audio,
    // et une perte de focus définitive ne doit pas laisser l'alarme muette.
    private fun followCallState(ringing: ActiveRinging, stage: EscalationStage) {
        val isInCall = callState.isInCall()
        if (isInCall && !ringing.isPausedByCall) {
            ringing.isPausedByCall = true
            sound.pause()
            vibration.stop()
        } else if (!isInCall && ringing.isPausedByCall) {
            ringing.isPausedByCall = false
            sound.resume()
            if (ringing.alarm.hasVibration) vibration.vibrate((ringing.stage ?: stage).vibration)
        }
    }

    private fun applyStage(ringing: ActiveRinging, stage: EscalationStage) {
        val previous = ringing.stage
        ringing.stage = stage
        volume.apply(computeTargetVolumeIndex(volume.maxIndex, ringing.maxVolumePercent, stage.volumeFraction))
        if (ringing.alarm.hasVibration && !ringing.isPausedByCall && stage.vibration != previous?.vibration) {
            vibration.vibrate(stage.vibration)
        }
        if (stage.isStrident && previous?.isStrident != true) sound.switchTo(SoundId.STRIDENT)
    }

    /** [method] nul pour l'arrêt provisoire, qui ne laisse pas de trace. */
    private suspend fun finish(method: WakeMethod?, state: RingingState? = active?.state) {
        if (method != null && state != null) recordWake(state, method)
        if (active != null) {
            sound.stop()
            vibration.stop()
            volume.restore()
        }
        active = null
        attempt("Effacement de la sonnerie") { stateStore.clear() }
        session.update(null)
        host.releaseAwake()
        host.stop(lastStartId)
        // En dernier : finish peut tourner dans la boucle qu'il annule.
        loopJob?.cancel()
        loopJob = null
    }

    private suspend fun recordWake(state: RingingState, method: WakeMethod) {
        val now = nowMillis()
        val event = WakeEvent(
            alarmId = state.alarmId,
            firedAt = state.firedAt,
            dismissedAt = now,
            method = method,
            repsDone = 0,
            durationSec = ((now - state.firedAt) / 1000).toInt(),
        )
        attempt("Enregistrement du réveil") { wakeEventStore.record(event) }
    }

    private suspend fun <T> attempt(what: String, block: suspend () -> T): T? = try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        reportError(what, exception)
        null
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val DEFAULT_MAX_PERCENT = 100
    }
}
