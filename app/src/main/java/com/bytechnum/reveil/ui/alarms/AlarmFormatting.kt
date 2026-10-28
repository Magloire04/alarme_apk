package com.bytechnum.reveil.ui.alarms

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bytechnum.reveil.R
import java.time.DayOfWeek
import java.time.Duration
import java.time.format.TextStyle
import java.util.Locale

private val weekdays = (1..5).map { DayOfWeek.of(it) }.toSet()
private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

fun DayOfWeek.letter(): String = getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase()

fun DayOfWeek.fullName(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())

@Composable
fun describeDays(days: Set<DayOfWeek>): String = when (days) {
    emptySet<DayOfWeek>() -> stringResource(R.string.repeat_once)
    DayOfWeek.entries.toSet() -> stringResource(R.string.repeat_every_day)
    weekdays -> stringResource(R.string.repeat_weekdays)
    weekend -> stringResource(R.string.repeat_weekend)
    else -> days.sorted().joinToString(", ") { it.shortName() }
}

@Composable
fun describeDelay(delay: Duration): String {
    val totalMinutes = delay.toMinutes() + 1
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> stringResource(R.string.delay_hours_minutes, hours, minutes)
        else -> stringResource(R.string.delay_minutes, minutes)
    }
}
