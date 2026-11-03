package com.bytechnum.reveil.domain.alarm

import com.bytechnum.reveil.domain.model.RingingState

sealed interface FireDecision {
    data class Start(val state: RingingState) : FireDecision

    data object Skip : FireDecision
}

sealed interface RestartDecision {
    data class Resume(val state: RingingState) : RestartDecision

    data class ExpireAsMissed(val state: RingingState) : RestartDecision

    data object Nothing : RestartDecision
}

object RingingRules {
    /** Une seule sonnerie à la fois : une alarme qui arrive pendant une autre est sautée. */
    fun decideOnFire(current: RingingState?, alarmId: Int, nowMillis: Long): FireDecision =
        if (current == null) FireDecision.Start(RingingState(alarmId, nowMillis)) else FireDecision.Skip

    /** Après un arrêt du service par le système, reprendre ou constater le réveil manqué. */
    fun decideOnRestart(saved: RingingState?, nowMillis: Long): RestartDecision = when {
        saved == null -> RestartDecision.Nothing
        nowMillis - saved.firedAt >= EscalationPolicy.MAX_RING_MS -> RestartDecision.ExpireAsMissed(saved)
        else -> RestartDecision.Resume(saved)
    }
}
