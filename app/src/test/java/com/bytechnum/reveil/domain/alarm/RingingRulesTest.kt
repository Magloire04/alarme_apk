package com.bytechnum.reveil.domain.alarm

import com.bytechnum.reveil.domain.model.RingingState
import org.junit.Assert.assertEquals
import org.junit.Test

class RingingRulesTest {
    @Test
    fun fireWithNothingRingingStarts() {
        assertEquals(
            FireDecision.Start(RingingState(alarmId = 3, firedAt = 1000)),
            RingingRules.decideOnFire(current = null, alarmId = 3, nowMillis = 1000),
        )
    }

    @Test
    fun fireWhileAnotherRingsIsSkipped() {
        assertEquals(
            FireDecision.Skip,
            RingingRules.decideOnFire(current = RingingState(3, 1000), alarmId = 4, nowMillis = 2000),
        )
    }

    @Test
    fun restartWithoutSavedStateDoesNothing() {
        assertEquals(RestartDecision.Nothing, RingingRules.decideOnRestart(saved = null, nowMillis = 5))
    }

    @Test
    fun restartBeforeFifteenMinutesResumes() {
        val saved = RingingState(alarmId = 3, firedAt = 0)

        assertEquals(RestartDecision.Resume(saved), RingingRules.decideOnRestart(saved, nowMillis = 899_999))
    }

    @Test
    fun restartAfterFifteenMinutesExpiresAsMissed() {
        val saved = RingingState(alarmId = 3, firedAt = 0)

        assertEquals(RestartDecision.ExpireAsMissed(saved), RingingRules.decideOnRestart(saved, nowMillis = 900_000))
    }
}
