package com.bytechnum.reveil.di

import android.content.Context
import androidx.room.Room
import com.bytechnum.reveil.data.db.AlarmDao
import com.bytechnum.reveil.data.db.AppDatabase
import com.bytechnum.reveil.data.db.WakeEventDao
import com.bytechnum.reveil.domain.port.AlarmScheduling
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Base en mémoire : les tests d'interface ne touchent pas aux vraies alarmes du téléphone. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object InMemoryDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides
    fun provideAlarmDao(database: AppDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun provideWakeEventDao(database: AppDatabase): WakeEventDao = database.wakeEventDao()
}

/** Aucune vraie alarme n'est posée pendant les tests d'interface. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SchedulingModule::class])
object NoOpSchedulingModule {
    @Provides
    fun provideAlarmScheduling(): AlarmScheduling = object : AlarmScheduling {
        override fun schedule(alarmId: Int, triggerAtMillis: Long) = Unit

        override fun cancel(alarmId: Int) = Unit
    }
}
