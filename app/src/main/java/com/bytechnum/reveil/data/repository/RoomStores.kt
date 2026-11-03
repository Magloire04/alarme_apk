package com.bytechnum.reveil.data.repository

import com.bytechnum.reveil.data.db.AlarmDao
import com.bytechnum.reveil.data.db.WakeEventDao
import com.bytechnum.reveil.data.db.toDomain
import com.bytechnum.reveil.data.db.toEntity
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.WakeEvent
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.WakeEventStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAlarmStore @Inject constructor(private val alarmDao: AlarmDao) : AlarmStore {
    override fun observeAll(): Flow<List<Alarm>> = alarmDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Int): Alarm? = alarmDao.getById(id)?.toDomain()

    override suspend fun upsert(alarm: Alarm): Int {
        val rowId = alarmDao.upsert(alarm.toEntity())
        return if (alarm.id == 0) rowId.toInt() else alarm.id
    }

    override suspend fun delete(id: Int) = alarmDao.delete(id)

    override suspend fun getEnabled(): List<Alarm> = alarmDao.getEnabled().map { it.toDomain() }
}

class RoomWakeEventStore @Inject constructor(private val wakeEventDao: WakeEventDao) : WakeEventStore {
    override suspend fun record(event: WakeEvent) = wakeEventDao.insert(event.toEntity())
}
