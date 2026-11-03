package com.bytechnum.reveil.fakes

import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.WakeEvent
import com.bytechnum.reveil.domain.port.AlarmScheduling
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.Clock
import com.bytechnum.reveil.domain.port.WakeEventStore
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAlarmStore : AlarmStore {
    private val alarms = MutableStateFlow<Map<Int, Alarm>>(emptyMap())
    private var nextId = 1

    val all: Map<Int, Alarm> get() = alarms.value

    override fun observeAll(): Flow<List<Alarm>> = alarms.map {
        it.values.sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    override suspend fun getById(id: Int): Alarm? = alarms.value[id]

    override suspend fun upsert(alarm: Alarm): Int {
        val id = if (alarm.id == 0) nextId++ else alarm.id
        alarms.value = alarms.value + (id to alarm.copy(id = id))
        return id
    }

    override suspend fun delete(id: Int) {
        alarms.value = alarms.value - id
    }

    override suspend fun getEnabled(): List<Alarm> = alarms.value.values.filter { it.isEnabled }
}

class FakeScheduling : AlarmScheduling {
    val scheduled = mutableMapOf<Int, Long>()

    override fun schedule(alarmId: Int, triggerAtMillis: Long) {
        scheduled[alarmId] = triggerAtMillis
    }

    override fun cancel(alarmId: Int) {
        scheduled.remove(alarmId)
    }
}

class FakeWakeEventStore : WakeEventStore {
    val events = mutableListOf<WakeEvent>()

    override suspend fun record(event: WakeEvent) {
        events += event
    }
}

class FixedClock(var current: ZonedDateTime) : Clock {
    override fun now(): ZonedDateTime = current
}
