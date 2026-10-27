package com.bytechnum.reveil.data.db

import java.time.DayOfWeek

/** Jours de répétition en masque de bits : bit 0 = lundi, bit 6 = dimanche. */
object DaysMask {
    fun toMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun toDays(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
}
