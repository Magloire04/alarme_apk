package com.bytechnum.reveil.ringing

import com.bytechnum.reveil.domain.alarm.EscalationStage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RingingUiState(val alarmId: Int, val label: String, val firedAt: Long, val stage: EscalationStage)

/** Pont entre le service qui sonne et l'écran de sonnerie, dans le même processus. */
@Singleton
class RingingSession @Inject constructor() {
    private val mutableState = MutableStateFlow<RingingUiState?>(null)
    val state: StateFlow<RingingUiState?> = mutableState.asStateFlow()

    fun update(state: RingingUiState?) {
        mutableState.value = state
    }
}
