package com.bytechnum.reveil.data.db

import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.ChallengeType
import com.bytechnum.reveil.domain.model.Difficulty
import com.bytechnum.reveil.domain.model.SoundId
import com.bytechnum.reveil.domain.model.WakeEvent
import com.bytechnum.reveil.domain.model.WakeMethod

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    daysMask = DaysMask.toMask(days),
    isEnabled = isEnabled,
    label = label,
    soundId = soundId.name,
    hasVibration = hasVibration,
    challengeType = challengeType.name,
    challengeTarget = challengeTarget,
    difficulty = difficulty.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    days = DaysMask.toDays(daysMask),
    isEnabled = isEnabled,
    label = label,
    soundId = SoundId.valueOf(soundId),
    hasVibration = hasVibration,
    challengeType = ChallengeType.valueOf(challengeType),
    challengeTarget = challengeTarget,
    difficulty = Difficulty.valueOf(difficulty),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun WakeEvent.toEntity(): WakeEventEntity = WakeEventEntity(
    id = id,
    alarmId = alarmId,
    firedAt = firedAt,
    dismissedAt = dismissedAt,
    method = method.name,
    repsDone = repsDone,
    durationSec = durationSec,
)

fun WakeEventEntity.toDomain(): WakeEvent = WakeEvent(
    id = id,
    alarmId = alarmId,
    firedAt = firedAt,
    dismissedAt = dismissedAt,
    method = WakeMethod.valueOf(method),
    repsDone = repsDone,
    durationSec = durationSec,
)
