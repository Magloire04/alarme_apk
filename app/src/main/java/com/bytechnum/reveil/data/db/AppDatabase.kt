package com.bytechnum.reveil.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class, WakeEventEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun wakeEventDao(): WakeEventDao

    companion object {
        const val FILE_NAME = "reveil.db"

        // Stockage protégé par l'appareil : lisible avant le premier déverrouillage,
        // pour qu'une alarme sonne après un redémarrage nocturne.
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.createDeviceProtectedStorageContext(),
            AppDatabase::class.java,
            FILE_NAME,
        ).build()
    }
}
