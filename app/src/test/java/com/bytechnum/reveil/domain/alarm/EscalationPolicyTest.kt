package com.bytechnum.reveil.domain.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EscalationPolicyTest {
    @Test
    fun startStageIsSixtyPercentWithNormalVibration() {
        val expected = EscalationStage(
            volumeFraction = 0.6f,
            vibration = VibrationLevel.NORMAL,
            isFullBrightness = false,
            isStrident = false,
            isFallbackAvailable = false,
            shouldStop = false,
        )

        assertEquals(expected, EscalationPolicy.getStageAt(0))
    }

    @Test
    fun startStageLastsUntilThirtySeconds() {
        assertEquals(EscalationPolicy.getStageAt(0), EscalationPolicy.getStageAt(29_999))
    }

    @Test
    fun thirtySecondsRaisesToEightyPercentAndStrongVibration() {
        val stage = EscalationPolicy.getStageAt(30_000)

        assertEquals(0.8f, stage.volumeFraction)
        assertEquals(VibrationLevel.STRONG, stage.vibration)
    }

    @Test
    fun ninetySecondsGivesFullVolumeContinuousVibrationAndFullBrightness() {
        val stage = EscalationPolicy.getStageAt(90_000)

        assertEquals(1.0f, stage.volumeFraction)
        assertEquals(VibrationLevel.CONTINUOUS, stage.vibration)
        assertTrue(stage.isFullBrightness)
    }

    @Test
    fun fiveMinutesSwitchesToStridentSound() {
        assertFalse(EscalationPolicy.getStageAt(299_999).isStrident)
        assertTrue(EscalationPolicy.getStageAt(300_000).isStrident)
    }

    @Test
    fun tenMinutesOpensFallback() {
        assertFalse(EscalationPolicy.getStageAt(599_999).isFallbackAvailable)
        assertTrue(EscalationPolicy.getStageAt(600_000).isFallbackAvailable)
    }

    @Test
    fun fifteenMinutesStopsRinging() {
        assertFalse(EscalationPolicy.getStageAt(899_999).shouldStop)
        assertTrue(EscalationPolicy.getStageAt(900_000).shouldStop)
    }

    @Test
    fun targetVolumeAtSixtyPercentOfSeven() {
        assertEquals(4, computeTargetVolumeIndex(maxIndex = 7, maxVolumePercent = 100, fraction = 0.6f))
    }

    @Test
    fun targetVolumeAtFullIsMaxIndex() {
        assertEquals(7, computeTargetVolumeIndex(maxIndex = 7, maxVolumePercent = 100, fraction = 1f))
    }

    @Test
    fun targetVolumeRespectsMaxVolumeSetting() {
        assertEquals(2, computeTargetVolumeIndex(maxIndex = 7, maxVolumePercent = 50, fraction = 0.6f))
    }

    @Test
    fun targetVolumeNeverDropsToSilence() {
        assertEquals(1, computeTargetVolumeIndex(maxIndex = 7, maxVolumePercent = 10, fraction = 0.6f))
    }
}
