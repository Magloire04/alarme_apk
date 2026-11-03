package com.bytechnum.reveil.ringing

import com.bytechnum.reveil.domain.alarm.VibrationLevel
import com.bytechnum.reveil.domain.model.SoundId

interface SoundOutput {
    fun start(sound: SoundId)

    fun switchTo(sound: SoundId)

    fun pause()

    fun resume()

    fun stop()
}

interface VibrationOutput {
    fun vibrate(level: VibrationLevel)

    fun stop()
}

fun interface CallState {
    fun isInCall(): Boolean
}

/** Ce que le coordinateur demande au service qui l'héberge. */
interface RingingHost {
    fun showRinging(label: String)

    fun keepAwake()

    fun releaseAwake()

    /** Arrête le service, sauf si une commande plus récente que [startId] est arrivée entre-temps. */
    fun stop(startId: Int)
}
