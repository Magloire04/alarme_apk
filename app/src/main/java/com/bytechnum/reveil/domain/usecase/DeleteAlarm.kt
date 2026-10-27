package com.bytechnum.reveil.domain.usecase

import com.bytechnum.reveil.domain.port.AlarmScheduling
import com.bytechnum.reveil.domain.port.AlarmStore
import javax.inject.Inject

class DeleteAlarm @Inject constructor(private val store: AlarmStore, private val scheduling: AlarmScheduling) {
    suspend operator fun invoke(id: Int) {
        scheduling.cancel(id)
        store.delete(id)
    }
}
