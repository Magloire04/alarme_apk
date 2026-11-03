package com.bytechnum.reveil.data.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmVolumeControllerTest {
    private class FakeStreamVolume(override val maxIndex: Int, var index: Int) : AlarmStreamVolume {
        override fun current(): Int = index

        override fun set(index: Int) {
            this.index = index
        }
    }

    private val stream = FakeStreamVolume(maxIndex = 7, index = 3)
    private val controller = AlarmVolumeController(stream)

    @Test
    fun applySetsTargetVolume() {
        controller.begin()

        controller.apply(4)

        assertEquals(4, stream.index)
    }

    @Test
    fun enforceRestoresTargetAfterUserLowersVolume() {
        controller.begin()
        controller.apply(4)
        stream.index = 0

        controller.enforce()

        assertEquals(4, stream.index)
    }

    @Test
    fun restorePutsBackOriginalVolume() {
        controller.begin()
        controller.apply(4)

        controller.restore()

        assertEquals(3, stream.index)
    }

    @Test
    fun enforceBeforeApplyLeavesVolumeUntouched() {
        controller.begin()
        stream.index = 2

        controller.enforce()

        assertEquals(2, stream.index)
    }
}
