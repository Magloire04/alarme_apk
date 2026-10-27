package com.bytechnum.reveil.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Int): AlarmEntity?

    /** Renvoie l'identifiant de ligne pour une insertion, -1 pour une mise à jour. */
    @Upsert
    suspend fun upsert(entity: AlarmEntity): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabled(): List<AlarmEntity>
}

@Dao
interface WakeEventDao {
    @Insert
    suspend fun insert(entity: WakeEventEntity)

    @Query("SELECT * FROM wake_events WHERE firedAt >= :fromMillis AND firedAt < :toMillis ORDER BY firedAt")
    suspend fun getBetween(fromMillis: Long, toMillis: Long): List<WakeEventEntity>
}
