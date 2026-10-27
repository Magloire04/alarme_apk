package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.alarm.ComputeNextTrigger
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.fakes.FakeAlarmStore
import com.bytechnum.reveil.fakes.FakeScheduling
import com.bytechnum.reveil.fakes.FixedClock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HandleAlarmFiredTest {
    private val zone = ZoneId.of("Africa/Porto-Novo")
    private val clock = FixedClock(ZonedDateTime.of(LocalDateTime.parse("2026-10-27T07:00:05"), zone))
    private val store = FakeAlarmStore()
    private val scheduling = FakeScheduling()
    private val handleAlarmFired =
        HandleAlarmFired(store, ScheduleNext(scheduling, ComputeNextTrigger(), clock), scheduling, clock)

    private fun millisAt(text: String): Long =
        ZonedDateTime.of(LocalDateTime.parse(text), zone).toInstant().toEpochMilli()

    @Test
    fun oneShotAlarmIsReturnedAndDisabled() = runTest {
        val id = store.upsert(Alarm.createDefault(0).copy(hour = 7, minute = 0))

        val fired = handleAlarmFired(id)

        assertEquals(id, fired?.id)
        assertFalse(store.getById(id)!!.isEnabled)
        assertFalse(scheduling.scheduled.containsKey(id))
    }

    @Test
    fun oneShotAlarmScheduledByAnotherPathIsCancelled() = runTest {
        // Au démarrage à froid, la replanification a pu la reposer pour le lendemain.
        val id = store.upsert(Alarm.createDefault(0).copy(hour = 7, minute = 0))
        scheduling.schedule(id, millisAt("2026-10-28T07:00"))

        handleAlarmFired(id)

        assertFalse(scheduling.scheduled.containsKey(id))
    }

    @Test
    fun repeatingAlarmIsScheduledForNextWeek() = runTest {
        val id = store.upsert(Alarm.createDefault(0).copy(hour = 7, minute = 0, days = setOf(DayOfWeek.TUESDAY)))

        handleAlarmFired(id)

        assertEquals(millisAt("2026-11-03T07:00"), scheduling.scheduled[id])
    }

    @Test
    fun unknownAlarmReturnsNull() = runTest {
        assertNull(handleAlarmFired(42))
    }

    @Test
    fun disabledAlarmReturnsNull() = runTest {
        val id = store.upsert(Alarm.createDefault(0).copy(isEnabled = false))

        assertNull(handleAlarmFired(id))
    }
}
