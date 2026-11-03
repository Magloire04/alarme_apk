package com.bytechnum.reveil.ui.alarms

import androidx.lifecycle.SavedStateHandle
import com.bytechnum.reveil.domain.alarm.ComputeNextTrigger
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.usecase.DeleteAlarm
import com.bytechnum.reveil.domain.usecase.SaveAlarm
import com.bytechnum.reveil.domain.usecase.ScheduleNext
import com.bytechnum.reveil.fakes.FakeAlarmStore
import com.bytechnum.reveil.fakes.FakeScheduling
import com.bytechnum.reveil.fakes.FixedClock
import com.bytechnum.reveil.fakes.MainDispatcherRule
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmEditViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val clock =
        FixedClock(ZonedDateTime.of(LocalDateTime.parse("2026-10-27T06:00"), ZoneId.of("Africa/Porto-Novo")))
    private val store = FakeAlarmStore()
    private val scheduling = FakeScheduling()
    private val saveAlarm = SaveAlarm(store, ScheduleNext(scheduling, ComputeNextTrigger(), clock), scheduling, clock)

    private fun createViewModel(alarmId: Int = AlarmEditRoute.NEW_ALARM_ID) = AlarmEditViewModel(
        SavedStateHandle(mapOf("alarmId" to alarmId)),
        store,
        saveAlarm,
        DeleteAlarm(store, scheduling),
        clock,
    )

    @Test
    fun newAlarmStartsWithDefaults() {
        val nowMillis = clock.now().toInstant().toEpochMilli()

        assertEquals(Alarm.createDefault(nowMillis), createViewModel().state.value)
    }

    @Test
    fun targetIsClampedBetweenFiveAndFifty() {
        val viewModel = createViewModel()

        viewModel.setTarget(3)
        assertEquals(5, viewModel.state.value.challengeTarget)

        viewModel.setTarget(80)
        assertEquals(50, viewModel.state.value.challengeTarget)
    }

    @Test
    fun saveStoresAndSchedulesAlarm() = runTest {
        val viewModel = createViewModel()
        var isDone = false
        viewModel.setTime(hour = 6, minute = 45)
        viewModel.toggleDay(DayOfWeek.MONDAY)

        viewModel.save { isDone = true }

        val saved = store.all.values.single()
        assertEquals(6, saved.hour)
        assertEquals(45, saved.minute)
        assertEquals(setOf(DayOfWeek.MONDAY), saved.days)
        assertTrue(scheduling.scheduled.containsKey(saved.id))
        assertTrue(isDone)
    }

    @Test
    fun existingAlarmIsLoaded() = runTest {
        val id = store.upsert(Alarm.createDefault(0).copy(hour = 5, minute = 15, label = "Footing"))

        val viewModel = createViewModel(alarmId = id)

        assertEquals("Footing", viewModel.state.value.label)
        assertEquals(5, viewModel.state.value.hour)
    }
}
