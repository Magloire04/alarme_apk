package com.bytechnum.reveil.domain.port

import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.WakeEvent
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow

interface Clock {
    fun now(): ZonedDateTime
}

interface AlarmStore {
    fun observeAll(): Flow<List<Alarm>>

    suspend fun getById(id: Int): Alarm?

    /** Renvoie l'identifiant de l'alarme enregistrée. */
    suspend fun upsert(alarm: Alarm): Int

    suspend fun delete(id: Int)

    suspend fun getEnabled(): List<Alarm>
}

interface WakeEventStore {
    suspend fun record(event: WakeEvent)
}

interface AlarmScheduling {
    fun schedule(alarmId: Int, triggerAtMillis: Long)

    fun cancel(alarmId: Int)
}
