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
import org.junit.Test

class SaveAlarmTest {
    private val zone = ZoneId.of("Africa/Porto-Novo")
    private val clock = FixedClock(ZonedDateTime.of(LocalDateTime.parse("2026-10-27T06:00"), zone))
    private val store = FakeAlarmStore()
    private val scheduling = FakeScheduling()
    private val scheduleNext = ScheduleNext(scheduling, ComputeNextTrigger(), clock)
    private val saveAlarm = SaveAlarm(store, scheduleNext, scheduling, clock)

    private fun millisAt(text: String): Long =
        ZonedDateTime.of(LocalDateTime.parse(text), zone).toInstant().toEpochMilli()

    @Test
    fun enabledAlarmIsStoredAndScheduled() = runTest {
        val id = saveAlarm(Alarm.createDefault(0).copy(hour = 7, minute = 30))

        assertEquals(7, store.getById(id)?.hour)
        assertEquals(millisAt("2026-10-27T07:30"), scheduling.scheduled[id])
    }

    @Test
    fun savingStampsUpdatedAt() = runTest {
        val id = saveAlarm(Alarm.createDefault(0))

        assertEquals(clock.now().toInstant().toEpochMilli(), store.getById(id)?.updatedAt)
    }

    @Test
    fun disabledAlarmIsNotScheduled() = runTest {
        val id = saveAlarm(Alarm.createDefault(0).copy(isEnabled = false))

        assertFalse(scheduling.scheduled.containsKey(id))
    }

    @Test
    fun disablingCancelsPreviousSchedule() = runTest {
        val id = saveAlarm(Alarm.createDefault(0).copy(hour = 7, minute = 30))

        saveAlarm(store.getById(id)!!.copy(isEnabled = false))

        assertFalse(scheduling.scheduled.containsKey(id))
    }
}
