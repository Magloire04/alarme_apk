package com.bytechnum.reveil.di

import com.bytechnum.reveil.data.repository.RoomAlarmStore
import com.bytechnum.reveil.data.repository.RoomWakeEventStore
import com.bytechnum.reveil.domain.port.AlarmStore
import com.bytechnum.reveil.domain.port.Clock
import com.bytechnum.reveil.domain.port.WakeEventStore
import com.bytechnum.reveil.util.DeviceClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun bindAlarmStore(store: RoomAlarmStore): AlarmStore

    @Binds
    abstract fun bindWakeEventStore(store: RoomWakeEventStore): WakeEventStore

    @Binds
    abstract fun bindClock(clock: DeviceClock): Clock
}
