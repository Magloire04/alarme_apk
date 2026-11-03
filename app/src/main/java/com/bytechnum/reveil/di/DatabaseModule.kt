package com.bytechnum.reveil.di

import android.content.Context
import com.bytechnum.reveil.data.db.AlarmDao
import com.bytechnum.reveil.data.db.AppDatabase
import com.bytechnum.reveil.data.db.WakeEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.build(context)

    @Provides
    fun provideAlarmDao(database: AppDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun provideWakeEventDao(database: AppDatabase): WakeEventDao = database.wakeEventDao()
}
