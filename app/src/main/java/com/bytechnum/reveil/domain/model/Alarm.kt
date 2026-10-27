package com.bytechnum.reveil.domain.model

import java.time.DayOfWeek

enum class ChallengeType { JUMP, SQUAT, MARCH, JACK, RANDOM }

enum class Difficulty { EASY, NORMAL, HARD }

enum class SoundId { SOFT, CLASSIC, STRIDENT }

/** Une alarme sans jour sonne une seule fois, puis se désactive. */
data class Alarm(
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val days: Set<DayOfWeek>,
    val isEnabled: Boolean,
    val label: String,
    val soundId: SoundId,
    val hasVibration: Boolean,
    val challengeType: ChallengeType,
    val challengeTarget: Int,
    val difficulty: Difficulty,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isRepeating: Boolean get() = days.isNotEmpty()

    companion object {
        const val DEFAULT_CHALLENGE_TARGET = 15
        const val MIN_CHALLENGE_TARGET = 5
        const val MAX_CHALLENGE_TARGET = 50

        fun createDefault(nowMillis: Long): Alarm = Alarm(
            hour = 7,
            minute = 0,
            days = emptySet(),
            isEnabled = true,
            label = "",
            soundId = SoundId.CLASSIC,
            hasVibration = true,
            challengeType = ChallengeType.JUMP,
            challengeTarget = DEFAULT_CHALLENGE_TARGET,
            difficulty = Difficulty.NORMAL,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )
    }
}
