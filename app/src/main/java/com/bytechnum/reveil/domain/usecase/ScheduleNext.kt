package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.alarm.ComputeNextTrigger
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.port.AlarmScheduling
import com.bytechnum.reveil.domain.port.Clock
import javax.inject.Inject

class ScheduleNext @Inject constructor(
    private val scheduling: AlarmScheduling,
    private val computeNextTrigger: ComputeNextTrigger,
    private val clock: Clock,
) {
    operator fun invoke(alarm: Alarm) {
        val triggerAt = computeNextTrigger(alarm, clock.now())
        scheduling.schedule(alarm.id, triggerAt.toInstant().toEpochMilli())
    }
}
