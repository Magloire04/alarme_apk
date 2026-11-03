package com.bytechnum.reveil.ui.alarms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.ChallengeType
import com.bytechnum.reveil.domain.model.Difficulty
import com.bytechnum.reveil.domain.model.SoundId
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.Clock
import com.bytechnum.reveil.domain.usecase.DeleteAlarm
import com.bytechnum.reveil.domain.usecase.SaveAlarm
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    store: AlarmStore,
    private val saveAlarm: SaveAlarm,
    private val deleteAlarm: DeleteAlarm,
    clock: Clock,
) : ViewModel() {
    private val alarmId: Int = savedStateHandle.get<Int>(ARG_ALARM_ID) ?: AlarmEditRoute.NEW_ALARM_ID
    val isNew: Boolean = alarmId == AlarmEditRoute.NEW_ALARM_ID

    private val mutableState = MutableStateFlow(Alarm.createDefault(clock.now().toInstant().toEpochMilli()))
    val state: StateFlow<Alarm> = mutableState.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch { store.getById(alarmId)?.let { mutableState.value = it } }
        }
    }

    fun setTime(hour: Int, minute: Int) = mutableState.update { it.copy(hour = hour, minute = minute) }

    fun toggleDay(day: DayOfWeek) = mutableState.update {
        val days = if (day in it.days) it.days - day else it.days + day
        it.copy(days = days)
    }

    fun setLabel(label: String) = mutableState.update { it.copy(label = label.take(MAX_LABEL_LENGTH)) }

    fun setSound(soundId: SoundId) = mutableState.update { it.copy(soundId = soundId) }

    fun setVibration(hasVibration: Boolean) = mutableState.update { it.copy(hasVibration = hasVibration) }

    fun setChallenge(challengeType: ChallengeType) = mutableState.update { it.copy(challengeType = challengeType) }

    fun setTarget(target: Int) = mutableState.update {
        it.copy(challengeTarget = target.coerceIn(Alarm.MIN_CHALLENGE_TARGET, Alarm.MAX_CHALLENGE_TARGET))
    }

    fun setDifficulty(difficulty: Difficulty) = mutableState.update { it.copy(difficulty = difficulty) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            saveAlarm(mutableState.value.copy(isEnabled = true))
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            deleteAlarm(alarmId)
            onDone()
        }
    }

    private companion object {
        const val ARG_ALARM_ID = "alarmId"
        const val MAX_LABEL_LENGTH = 40
    }
}
