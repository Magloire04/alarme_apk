package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.port.AlarmScheduling
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.Clock
import javax.inject.Inject

class SaveAlarm @Inject constructor(
    private val store: AlarmStore,
    private val scheduleNext: ScheduleNext,
    private val scheduling: AlarmScheduling,
    private val clock: Clock,
) {
    suspend operator fun invoke(alarm: Alarm): Int {
        val nowMillis = clock.now().toInstant().toEpochMilli()
        val id = store.upsert(alarm.copy(updatedAt = nowMillis))
        val saved = alarm.copy(id = id)
        if (saved.isEnabled) scheduleNext(saved) else scheduling.cancel(id)
        return id
    }
}
