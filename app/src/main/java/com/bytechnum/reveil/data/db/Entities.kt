package com.bytechnum.reveil.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val daysMask: Int,
    val isEnabled: Boolean,
    val label: String,
    val soundId: String,
    val hasVibration: Boolean,
    val challengeType: String,
    val challengeTarget: Int,
    val difficulty: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "wake_events", indices = [Index("firedAt")])
data class WakeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Int,
    val firedAt: Long,
    val dismissedAt: Long?,
    val method: String,
    val repsDone: Int,
    val durationSec: Int,
)
