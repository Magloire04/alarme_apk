package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.port.AlarmScheduling
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.Clock
import javax.inject.Inject

/**
 * À appeler dès le déclenchement, avant la sonnerie : la suite est réglée même si la sonnerie
 * est sautée ou si le service meurt ensuite. Renvoie null pour une intention périmée.
 */
class HandleAlarmFired @Inject constructor(
    private val store: AlarmStore,
    private val scheduleNext: ScheduleNext,
    private val scheduling: AlarmScheduling,
    private val clock: Clock,
) {
    suspend operator fun invoke(alarmId: Int): Alarm? {
        val alarm = store.getById(alarmId)?.takeIf { it.isEnabled } ?: return null
        if (alarm.isRepeating) {
            scheduleNext(alarm)
        } else {
            store.upsert(alarm.copy(isEnabled = false, updatedAt = clock.now().toInstant().toEpochMilli()))
            // Une replanification lancée au démarrage du processus a pu la reposer pour le lendemain.
            scheduling.cancel(alarm.id)
        }
        return alarm
    }
}
