package com.bytechnum.reveil.fakes

import com.bytechnum.reveil.data.audio.AlarmStreamVolume
import com.bytechnum.reveil.domain.alarm.VibrationLevel
import com.bytechnum.reveil.domain.model.RingingState
import com.bytechnum.reveil.domain.model.SoundId
import com.bytechnum.reveil.domain.port.RingingStateStore
import com.bytechnum.reveil.ringing.CallState
import com.bytechnum.reveil.ringing.RingingHost
import com.bytechnum.reveil.ringing.SoundOutput
import com.bytechnum.reveil.ringing.VibrationOutput
import java.io.IOException

class FakeSound : SoundOutput {
    val started = mutableListOf<SoundId>()
    var current: SoundId? = null
    var isPaused = false
    val isPlaying: Boolean get() = current != null && !isPaused

    override fun start(sound: SoundId) {
        started += sound
        current = sound
        isPaused = false
    }

    override fun switchTo(sound: SoundId) {
        current = sound
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {
        isPaused = false
    }

    override fun stop() {
        current = null
    }
}

class FakeVibration : VibrationOutput {
    var level: VibrationLevel? = null

    override fun vibrate(level: VibrationLevel) {
        this.level = level
    }

    override fun stop() {
        level = null
    }
}

class FakeCallState(var isCallActive: Boolean = false) : CallState {
    override fun isInCall(): Boolean = isCallActive
}

class FakeHost : RingingHost {
    val stopRequests = mutableListOf<Int>()
    var isAwake = false
    var label: String? = null

    override fun showRinging(label: String) {
        this.label = label
    }

    override fun keepAwake() {
        isAwake = true
    }

    override fun releaseAwake() {
        isAwake = false
    }

    override fun stop(startId: Int) {
        stopRequests += startId
    }
}

class FakeRingingStateStore(var state: RingingState? = null, private val isWriteBroken: Boolean = false) :
    RingingStateStore {
    override suspend fun read(): RingingState? = state

    override suspend fun write(state: RingingState) {
        if (isWriteBroken) throw IOException("Stockage indisponible")
        this.state = state
    }

    override suspend fun clear() {
        state = null
    }
}

class FakeStreamVolume(override val maxIndex: Int, var index: Int) : AlarmStreamVolume {
    override fun current(): Int = index

    override fun set(index: Int) {
        this.index = index
    }
}
