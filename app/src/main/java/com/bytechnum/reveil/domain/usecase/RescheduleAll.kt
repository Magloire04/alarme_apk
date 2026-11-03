package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.port.AlarmStore
import javax.inject.Inject

/** Replanifie toutes les alarmes actives : démarrage, changement d'heure, mise à jour de l'appli. */
class RescheduleAll @Inject constructor(private val store: AlarmStore, private val scheduleNext: ScheduleNext) {
    suspend operator fun invoke() {
        store.getEnabled().forEach { scheduleNext(it) }
    }
}
