package com.bytechnum.reveil.data.db

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class DaysMaskTest {
    @Test
    fun mondayIsFirstBit() {
        assertEquals(1, DaysMask.toMask(setOf(DayOfWeek.MONDAY)))
    }

    @Test
    fun sundayIsSeventhBit() {
        assertEquals(64, DaysMask.toMask(setOf(DayOfWeek.SUNDAY)))
    }

    @Test
    fun weekdaysGiveThirtyOne() {
        val weekdays = (1..5).map { DayOfWeek.of(it) }.toSet()

        assertEquals(31, DaysMask.toMask(weekdays))
    }

    @Test
    fun emptySetGivesZero() {
        assertEquals(0, DaysMask.toMask(emptySet()))
    }

    @Test
    fun everyMaskSurvivesRoundTrip() {
        for (mask in 0..127) {
            assertEquals(mask, DaysMask.toMask(DaysMask.toDays(mask)))
        }
    }
}
