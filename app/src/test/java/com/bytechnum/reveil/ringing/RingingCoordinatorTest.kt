package com.bytechnum.reveil.ringing

import com.bytechnum.reveil.data.audio.AlarmVolumeController
import com.bytechnum.reveil.domain.alarm.ComputeNextTrigger
import com.bytechnum.reveil.domain.alarm.VibrationLevel
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.RingingState
import com.bytechnum.reveil.domain.model.SoundId
import com.bytechnum.reveil.domain.model.WakeMethod
import com.bytechnum.reveil.domain.usecase.HandleAlarmFired
import com.bytechnum.reveil.domain.usecase.ScheduleNext
import com.bytechnum.reveil.fakes.FakeAlarmStore
import com.bytechnum.reveil.fakes.FakeCallState
import com.bytechnum.reveil.fakes.FakeHost
import com.bytechnum.reveil.fakes.FakeRingingStateStore
import com.bytechnum.reveil.fakes.FakeScheduling
import com.bytechnum.reveil.fakes.FakeSound
import com.bytechnum.reveil.fakes.FakeStreamVolume
import com.bytechnum.reveil.fakes.FakeVibration
import com.bytechnum.reveil.fakes.FakeWakeEventStore
import com.bytechnum.reveil.fakes.FixedClock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RingingCoordinatorTest {
    private val store = FakeAlarmStore()
    private val wakeEvents = FakeWakeEventStore()
    private val sound = FakeSound()
    private val vibration = FakeVibration()
    private val callState = FakeCallState()
    private val host = FakeHost()
    private val stream = FakeStreamVolume(maxIndex = 10, index = 3)
    private val session = RingingSession()

    private fun TestScope.nowMillis(): Long = BASE_MILLIS + testScheduler.currentTime

    private fun TestScope.createCoordinator(
        stateStore: FakeRingingStateStore = FakeRingingStateStore(),
    ): RingingCoordinator {
        val clock =
            FixedClock(ZonedDateTime.ofInstant(Instant.ofEpochMilli(BASE_MILLIS), ZoneId.of("Africa/Porto-Novo")))
        val scheduling = FakeScheduling()
        return RingingCoordinator(
            handleAlarmFired = HandleAlarmFired(
                store,
                ScheduleNext(scheduling, ComputeNextTrigger(), clock),
                scheduling,
                clock,
            ),
            alarmStore = store,
            wakeEventStore = wakeEvents,
            stateStore = stateStore,
            readMaxVolumePercent = { 100 },
            sound = sound,
            vibration = vibration,
            volume = AlarmVolumeController(stream),
            callState = callState,
            host = host,
            session = session,
            scope = backgroundScope,
            nowMillis = { nowMillis() },
        )
    }

    private suspend fun storeAlarm(hour: Int = 7): Int = store.upsert(Alarm.createDefault(0).copy(hour = hour))

    @Test
    fun fireStartsSoundAndRemembersRinging() = runTest {
        val stateStore = FakeRingingStateStore()
        val coordinator = createCoordinator(stateStore)
        val id = storeAlarm()

        coordinator.handle(RingingCommand.Fire(id), startId = 1)

        assertEquals(listOf(SoundId.CLASSIC), sound.started)
        assertEquals(RingingState(id, nowMillis()), stateStore.state)
        assertTrue(host.isAwake)
    }

    @Test
    fun twoAlarmsInSameMinuteStartOnlyOneSound() = runTest {
        val coordinator = createCoordinator()
        val firstId = storeAlarm()
        val secondId = storeAlarm()

        launch { coordinator.handle(RingingCommand.Fire(firstId), startId = 1) }
        launch { coordinator.handle(RingingCommand.Fire(secondId), startId = 2) }
        runCurrent()

        assertEquals(1, sound.started.size)
        assertEquals(WakeMethod.SKIPPED, wakeEvents.events.single { it.alarmId == secondId }.method)
    }

    @Test
    fun stopReportsItsOwnStartIdSoQueuedFireStillRings() = runTest {
        val coordinator = createCoordinator()
        val firstId = storeAlarm()
        val secondId = storeAlarm()

        coordinator.handle(RingingCommand.Fire(firstId), startId = 1)
        coordinator.handle(RingingCommand.StopProvisional, startId = 2)
        coordinator.handle(RingingCommand.Fire(secondId), startId = 3)

        assertEquals(listOf(2), host.stopRequests)
        assertEquals(2, sound.started.size)
        assertTrue(sound.isPlaying)
    }

    @Test
    fun soundStartsEvenWhenStateCannotBeSaved() = runTest {
        val coordinator = createCoordinator(FakeRingingStateStore(isWriteBroken = true))

        coordinator.handle(RingingCommand.Fire(storeAlarm()), startId = 1)

        assertTrue(sound.isPlaying)
    }

    @Test
    fun callPausesRingingThenItResumes() = runTest {
        val coordinator = createCoordinator()
        coordinator.handle(RingingCommand.Fire(storeAlarm()), startId = 1)

        callState.isCallActive = true
        advanceTimeBy(1_001)
        assertTrue(sound.isPaused)
        assertNull(vibration.level)

        callState.isCallActive = false
        advanceTimeBy(1_000)
        assertFalse(sound.isPaused)
        assertEquals(VibrationLevel.NORMAL, vibration.level)
    }

    @Test
    fun loweredVolumeIsRaisedBackWithinOneSecond() = runTest {
        val coordinator = createCoordinator()
        coordinator.handle(RingingCommand.Fire(storeAlarm()), startId = 1)
        val target = stream.index

        stream.index = 0
        advanceTimeBy(1_001)

        assertEquals(target, stream.index)
    }

    @Test
    fun fifteenMinutesStopsAndRecordsMissed() = runTest {
        val stateStore = FakeRingingStateStore()
        val coordinator = createCoordinator(stateStore)
        val id = storeAlarm()
        coordinator.handle(RingingCommand.Fire(id), startId = 1)

        advanceTimeBy(900_001)

        assertNull(sound.current)
        assertEquals(WakeMethod.MISSED, wakeEvents.events.single { it.alarmId == id }.method)
        assertEquals(listOf(1), host.stopRequests)
        assertNull(stateStore.state)
        assertNull(session.state.value)
        assertEquals(3, stream.index)
    }

    @Test
    fun restartResumesAtTheStageReachedBeforeTheServiceDied() = runTest {
        val id = storeAlarm()
        val coordinator = createCoordinator(FakeRingingStateStore(RingingState(id, nowMillis() - 100_000)))

        coordinator.handle(RingingCommand.Restart, startId = 1)

        assertTrue(sound.isPlaying)
        assertEquals(1.0f, session.state.value?.stage?.volumeFraction)
    }

    @Test
    fun restartAfterFifteenMinutesRecordsMissedWithoutSound() = runTest {
        val id = storeAlarm()
        val coordinator = createCoordinator(FakeRingingStateStore(RingingState(id, nowMillis() - 900_000)))

        coordinator.handle(RingingCommand.Restart, startId = 1)

        assertTrue(sound.started.isEmpty())
        assertEquals(WakeMethod.MISSED, wakeEvents.events.single().method)
        assertEquals(listOf(1), host.stopRequests)
    }

    @Test
    fun staleStateIsRecordedMissedAndNewAlarmRings() = runTest {
        val staleId = storeAlarm(hour = 6)
        val coordinator = createCoordinator(FakeRingingStateStore(RingingState(staleId, nowMillis() - 60_000)))

        coordinator.handle(RingingCommand.Fire(storeAlarm()), startId = 1)

        assertEquals(WakeMethod.MISSED, wakeEvents.events.single { it.alarmId == staleId }.method)
        assertTrue(sound.isPlaying)
    }

    private companion object {
        const val BASE_MILLIS = 1_793_080_800_000L
    }
}
