package com.bytechnum.reveil.domain.model

enum class WakeMethod {
    /** Défi réussi. */
    CHALLENGE,

    /** Sortie de secours utilisée. */
    FALLBACK,

    /** Sonnerie arrêtée seule après la durée maximale. */
    MISSED,

    /** Alarme ignorée parce qu'une autre sonnait déjà. */
    SKIPPED,
}

data class WakeEvent(
    val id: Long = 0,
    val alarmId: Int,
    val firedAt: Long,
    val dismissedAt: Long?,
    val method: WakeMethod,
    val repsDone: Int,
    val durationSec: Int,
)
