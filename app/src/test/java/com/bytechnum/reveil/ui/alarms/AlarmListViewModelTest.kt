package com.bytechnum.reveil.ui.alarms

import com.bytechnum.reveil.domain.alarm.ComputeNextTrigger
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.usecase.SaveAlarm
import com.bytechnum.reveil.domain.usecase.ScheduleNext
import com.bytechnum.reveil.domain.usecase.SetAlarmEnabled
import com.bytechnum.reveil.fakes.FakeAlarmStore
import com.bytechnum.reveil.fakes.FakeScheduling
import com.bytechnum.reveil.fakes.FixedClock
import com.bytechnum.reveil.fakes.MainDispatcherRule
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlarmListViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Africa/Porto-Novo")
    private val clock = FixedClock(ZonedDateTime.of(LocalDateTime.parse("2026-10-27T22:30:40"), zone))
    private val store = FakeAlarmStore()
    private val scheduling = FakeScheduling()
    private val scheduleNext = ScheduleNext(scheduling, ComputeNextTrigger(), clock)

    // Construit après la règle du répartiteur principal, sinon viewModelScope vise le vrai Main.
    private val viewModel by lazy {
        AlarmListViewModel(
            store,
            SaveAlarm(store, scheduleNext, scheduling, clock),
            SetAlarmEnabled(store, scheduleNext, scheduling, clock),
            ComputeNextTrigger(),
            clock,
        )
    }

    @Test
    fun testAlarmRingsAtLeastOneFullMinuteLater() = runTest {
        viewModel.createTestAlarm(label = "Test")

        val alarm = store.all.values.single()
        assertEquals(22, alarm.hour)
        assertEquals(32, alarm.minute)
        assertEquals("Test", alarm.label)
    }

    @Test
    fun nextAlarmIsTheEarliestEnabledOne() = runTest {
        store.upsert(Alarm.createDefault(0).copy(hour = 7, minute = 0))
        store.upsert(Alarm.createDefault(0).copy(hour = 23, minute = 0))
        store.upsert(Alarm.createDefault(0).copy(hour = 22, minute = 45, isEnabled = false))

        val next = viewModel.state.first { it.alarms.size == 3 }.next

        assertEquals(23, next?.alarm?.hour)
    }
}
