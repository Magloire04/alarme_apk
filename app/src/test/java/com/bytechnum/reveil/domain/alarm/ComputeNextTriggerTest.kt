package com.bytechnum.reveil.domain.alarm

import com.bytechnum.reveil.domain.model.Alarm
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeNextTriggerTest {
    private val computeNextTrigger = ComputeNextTrigger()
    private val portoNovo = ZoneId.of("Africa/Porto-Novo")
    private val paris = ZoneId.of("Europe/Paris")

    private fun alarm(hour: Int, minute: Int, days: Set<DayOfWeek> = emptySet()): Alarm =
        Alarm.createDefault(nowMillis = 0).copy(hour = hour, minute = minute, days = days)

    private fun at(text: String, zone: ZoneId = portoNovo): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.parse(text), zone)

    @Test
    fun oneShotLaterToday() {
        val next = computeNextTrigger(alarm(7, 30), at("2026-10-27T06:00"))

        assertEquals(at("2026-10-27T07:30"), next)
    }

    @Test
    fun oneShotEarlierGoesTomorrow() {
        val next = computeNextTrigger(alarm(5, 0), at("2026-10-27T06:00"))

        assertEquals(at("2026-10-28T05:00"), next)
    }

    @Test
    fun sameMinuteGoesTomorrow() {
        val next = computeNextTrigger(alarm(7, 30), at("2026-10-27T07:30:00"))

        assertEquals(at("2026-10-28T07:30"), next)
    }

    @Test
    fun weekdaysFromFridayGoesMonday() {
        val weekdays = (1..5).map { DayOfWeek.of(it) }.toSet()

        val next = computeNextTrigger(alarm(7, 0, weekdays), at("2026-10-30T08:00"))

        assertEquals(at("2026-11-02T07:00"), next)
    }

    @Test
    fun repeatingTodayLater() {
        val next = computeNextTrigger(alarm(7, 0, setOf(DayOfWeek.TUESDAY)), at("2026-10-27T06:00"))

        assertEquals(at("2026-10-27T07:00"), next)
    }

    @Test
    fun repeatingTodayPassedGoesNextWeek() {
        val next = computeNextTrigger(alarm(7, 0, setOf(DayOfWeek.TUESDAY)), at("2026-10-27T08:00"))

        assertEquals(at("2026-11-03T07:00"), next)
    }

    @Test
    fun midnight() {
        val next = computeNextTrigger(alarm(0, 0), at("2026-10-27T23:59"))

        assertEquals(at("2026-10-28T00:00"), next)
    }

    @Test
    fun dstGapShiftsToFirstValidMinute() {
        val next = computeNextTrigger(alarm(2, 30), at("2027-03-27T23:00", paris))

        assertEquals(ZonedDateTime.parse("2027-03-28T03:00+02:00[Europe/Paris]"), next)
    }

    @Test
    fun dstOverlapTakesFirstOccurrence() {
        val next = computeNextTrigger(alarm(2, 30), at("2026-10-25T00:00", paris))

        assertEquals(ZonedDateTime.parse("2026-10-25T02:30+02:00[Europe/Paris]"), next)
    }
}
