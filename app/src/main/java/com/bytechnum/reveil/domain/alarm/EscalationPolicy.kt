package com.bytechnum.reveil.domain.alarm

import kotlin.math.roundToInt

enum class VibrationLevel { NORMAL, STRONG, CONTINUOUS }

data class EscalationStage(
    val volumeFraction: Float,
    val vibration: VibrationLevel,
    val isFullBrightness: Boolean,
    val isStrident: Boolean,
    val isFallbackAvailable: Boolean,
    val shouldStop: Boolean,
)

/** Paliers de la sonnerie selon le temps écoulé depuis le déclenchement. */
object EscalationPolicy {
    const val MAX_RING_MS = 900_000L
    private const val STRONG_AT_MS = 30_000L
    private const val FULL_AT_MS = 90_000L
    private const val STRIDENT_AT_MS = 300_000L
    private const val FALLBACK_AT_MS = 600_000L

    fun getStageAt(elapsedMs: Long): EscalationStage = EscalationStage(
        volumeFraction = when {
            elapsedMs >= FULL_AT_MS -> 1.0f
            elapsedMs >= STRONG_AT_MS -> 0.8f
            else -> 0.6f
        },
        vibration = when {
            elapsedMs >= FULL_AT_MS -> VibrationLevel.CONTINUOUS
            elapsedMs >= STRONG_AT_MS -> VibrationLevel.STRONG
            else -> VibrationLevel.NORMAL
        },
        isFullBrightness = elapsedMs >= FULL_AT_MS,
        isStrident = elapsedMs >= STRIDENT_AT_MS,
        isFallbackAvailable = elapsedMs >= FALLBACK_AT_MS,
        shouldStop = elapsedMs >= MAX_RING_MS,
    )
}

/** Index de volume du flux d'alarme, jamais à zéro pour que la sonnerie reste audible. */
fun computeTargetVolumeIndex(maxIndex: Int, maxVolumePercent: Int, fraction: Float): Int =
    (maxIndex * maxVolumePercent / 100f * fraction).roundToInt().coerceIn(1, maxIndex)
