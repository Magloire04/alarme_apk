package com.bytechnum.reveil.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bytechnum.reveil.ringing.AlarmReceiver
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scheduler = AlarmScheduler(context, context.getSystemService(AlarmManager::class.java))

    private fun findFireIntent(alarmId: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        alarmId,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    @After
    fun cancelTestAlarm() {
        scheduler.cancel(TEST_ALARM_ID)
    }

    @Test
    fun scheduleRegistersFireIntent() {
        scheduler.schedule(TEST_ALARM_ID, System.currentTimeMillis() + ONE_HOUR_MS)

        assertNotNull(findFireIntent(TEST_ALARM_ID))
    }

    @Test
    fun cancelRemovesFireIntent() {
        scheduler.schedule(TEST_ALARM_ID, System.currentTimeMillis() + ONE_HOUR_MS)

        scheduler.cancel(TEST_ALARM_ID)

        assertNull(findFireIntent(TEST_ALARM_ID))
    }

    private companion object {
        const val TEST_ALARM_ID = 42_000
        const val ONE_HOUR_MS = 3_600_000L
    }
}
