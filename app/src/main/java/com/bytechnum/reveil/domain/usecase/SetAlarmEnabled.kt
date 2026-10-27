package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.port.AlarmScheduling
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.Clock
import javax.inject.Inject

class SetAlarmEnabled @Inject constructor(
    private val store: AlarmStore,
    private val scheduleNext: ScheduleNext,
    private val scheduling: AlarmScheduling,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: Int, isEnabled: Boolean) {
        val alarm = store.getById(id) ?: return
        val updated = alarm.copy(isEnabled = isEnabled, updatedAt = clock.now().toInstant().toEpochMilli())
        store.upsert(updated)
        if (isEnabled) scheduleNext(updated) else scheduling.cancel(id)
    }
}
