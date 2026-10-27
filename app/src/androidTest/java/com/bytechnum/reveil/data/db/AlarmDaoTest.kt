package com.bytechnum.reveil.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bytechnum.reveil.domain.model.Alarm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var alarmDao: AlarmDao

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        alarmDao = database.alarmDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun entity(hour: Int, isEnabled: Boolean = true): AlarmEntity =
        Alarm.createDefault(0).copy(hour = hour, isEnabled = isEnabled).toEntity()

    @Test
    fun observeAllSortsByTime() = runTest {
        alarmDao.upsert(entity(hour = 7))
        alarmDao.upsert(entity(hour = 6))

        assertEquals(listOf(6, 7), alarmDao.observeAll().first().map { it.hour })
    }

    @Test
    fun upsertWithSameIdUpdates() = runTest {
        val id = alarmDao.upsert(entity(hour = 7)).toInt()

        alarmDao.upsert(entity(hour = 8).copy(id = id))

        assertEquals(8, alarmDao.getById(id)?.hour)
        assertEquals(1, alarmDao.observeAll().first().size)
    }

    @Test
    fun deleteRemoves() = runTest {
        val id = alarmDao.upsert(entity(hour = 7)).toInt()

        alarmDao.delete(id)

        assertNull(alarmDao.getById(id))
    }

    @Test
    fun getEnabledExcludesDisabledAlarms() = runTest {
        alarmDao.upsert(entity(hour = 7))
        alarmDao.upsert(entity(hour = 8, isEnabled = false))

        assertEquals(listOf(7), alarmDao.getEnabled().map { it.hour })
    }

    @Test
    fun alarmSurvivesDomainRoundTrip() = runTest {
        val alarm = Alarm.createDefault(42).copy(days = setOf(java.time.DayOfWeek.MONDAY), label = "Cours")
        val id = alarmDao.upsert(alarm.toEntity()).toInt()

        assertEquals(alarm.copy(id = id), alarmDao.getById(id)?.toDomain())
    }
}
