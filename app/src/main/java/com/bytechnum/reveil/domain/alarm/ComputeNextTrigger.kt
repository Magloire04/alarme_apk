package com.bytechnum.reveil.domain.alarm

import com.bytechnum.reveil.domain.model.Alarm
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/** Prochaine sonnerie strictement après [now], dans le fuseau de [now]. */
class ComputeNextTrigger @Inject constructor() {
    operator fun invoke(alarm: Alarm, now: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(alarm.hour, alarm.minute)
        for (offset in 0L..DAYS_TO_SCAN) {
            val date = now.toLocalDate().plusDays(offset)
            if (alarm.isRepeating && date.dayOfWeek !in alarm.days) continue
            val candidate = resolve(date, time, now)
            if (candidate.isAfter(now)) return candidate
        }
        error("Aucune échéance trouvée pour l'alarme ${alarm.id}")
    }

    // Heure sautée au passage à l'heure d'été : première minute valide qui suit.
    // Heure en double au retour à l'heure d'hiver : ZonedDateTime.of garde la première.
    private fun resolve(date: LocalDate, time: LocalTime, now: ZonedDateTime): ZonedDateTime {
        val zone = now.zone
        val localDateTime = LocalDateTime.of(date, time)
        if (zone.rules.getValidOffsets(localDateTime).isEmpty()) {
            return ZonedDateTime.of(zone.rules.getTransition(localDateTime).dateTimeAfter, zone)
        }
        return ZonedDateTime.of(localDateTime, zone)
    }

    private companion object {
        const val DAYS_TO_SCAN = 8L
    }
}
