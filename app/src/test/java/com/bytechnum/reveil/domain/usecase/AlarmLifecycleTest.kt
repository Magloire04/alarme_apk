package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.alarm.ComputeNextTrigger
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.fakes.FakeAlarmStore
import com.bytechnum.reveil.fakes.FakeScheduling
import com.bytechnum.reveil.fakes.FixedClock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmLifecycleTest {
    private val clock =
        FixedClock(ZonedDateTime.of(LocalDateTime.parse("2026-10-27T06:00"), ZoneId.of("Africa/Porto-Novo")))
    private val store = FakeAlarmStore()
    private val scheduling = FakeScheduling()
    private val scheduleNext = ScheduleNext(scheduling, ComputeNextTrigger(), clock)

    @Test
    fun deleteRemovesAndCancels() = runTest {
        val id = SaveAlarm(store, scheduleNext, scheduling, clock)(Alarm.createDefault(0))

        DeleteAlarm(store, scheduling)(id)

        assertNull(store.getById(id))
        assertFalse(scheduling.scheduled.containsKey(id))
    }

    @Test
    fun disablingCancels() = runTest {
        val id = SaveAlarm(store, scheduleNext, scheduling, clock)(Alarm.createDefault(0))

        SetAlarmEnabled(store, scheduleNext, scheduling, clock)(id, isEnabled = false)

        assertFalse(store.getById(id)!!.isEnabled)
        assertFalse(scheduling.scheduled.containsKey(id))
    }

    @Test
    fun enablingSchedules() = runTest {
        val id = store.upsert(Alarm.createDefault(0).copy(isEnabled = false))

        SetAlarmEnabled(store, scheduleNext, scheduling, clock)(id, isEnabled = true)

        assertTrue(store.getById(id)!!.isEnabled)
        assertTrue(scheduling.scheduled.containsKey(id))
    }

    @Test
    fun rescheduleAllSchedulesOnlyEnabledAlarms() = runTest {
        val enabledId = store.upsert(Alarm.createDefault(0))
        val disabledId = store.upsert(Alarm.createDefault(0).copy(isEnabled = false))

        RescheduleAll(store, scheduleNext)()

        assertEquals(setOf(enabledId), scheduling.scheduled.keys)
        assertFalse(scheduling.scheduled.containsKey(disabledId))
    }
}
