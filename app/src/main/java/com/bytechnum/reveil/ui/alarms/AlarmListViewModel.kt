package com.bytechnum.reveil.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytechnum.reveil.domain.alarm.ComputeNextTrigger
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.Clock
import com.bytechnum.reveil.domain.usecase.SaveAlarm
import com.bytechnum.reveil.domain.usecase.SetAlarmEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NextAlarm(val alarm: Alarm, val triggerAt: ZonedDateTime)

data class AlarmListState(val alarms: List<Alarm> = emptyList(), val next: NextAlarm? = null)

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    store: AlarmStore,
    private val saveAlarm: SaveAlarm,
    private val setAlarmEnabled: SetAlarmEnabled,
    private val computeNextTrigger: ComputeNextTrigger,
    private val clock: Clock,
) : ViewModel() {
    val state: StateFlow<AlarmListState> = store.observeAll()
        .map { alarms -> AlarmListState(alarms, findNextAlarm(alarms)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlarmListState())

    fun setEnabled(id: Int, isEnabled: Boolean) {
        viewModelScope.launch { setAlarmEnabled(id, isEnabled) }
    }

    /** Alarme ponctuelle au moins une minute pleine plus tard, pour avoir le temps de verrouiller. */
    fun createTestAlarm(label: String) {
        val now = clock.now()
        val triggerAt = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(TEST_ALARM_DELAY_MINUTES)
        val alarm = Alarm.createDefault(now.toInstant().toEpochMilli())
            .copy(hour = triggerAt.hour, minute = triggerAt.minute, label = label)
        viewModelScope.launch { saveAlarm(alarm) }
    }

    private fun findNextAlarm(alarms: List<Alarm>): NextAlarm? {
        val now = clock.now()
        return alarms.filter { it.isEnabled }
            .map { NextAlarm(it, computeNextTrigger(it, now)) }
            .minByOrNull { it.triggerAt }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TEST_ALARM_DELAY_MINUTES = 2L
    }
}
