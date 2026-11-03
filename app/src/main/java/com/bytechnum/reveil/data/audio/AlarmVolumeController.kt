package com.bytechnum.reveil.data.audio

import android.media.AudioManager
import android.util.Log

interface AlarmStreamVolume {
    val maxIndex: Int

    fun current(): Int

    fun set(index: Int)
}

class AudioManagerStreamVolume(private val audioManager: AudioManager) : AlarmStreamVolume {
    override val maxIndex: Int get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

    override fun current(): Int = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

    override fun set(index: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, index, 0)
        } catch (exception: SecurityException) {
            Log.w(TAG, "Volume d'alarme non modifiable", exception)
        }
    }

    private companion object {
        const val TAG = "AlarmVolume"
    }
}

/** Impose le volume du palier pendant la sonnerie et rend le volume d'origine à la fin. */
class AlarmVolumeController(private val stream: AlarmStreamVolume) {
    private var originalIndex: Int? = null
    private var targetIndex: Int? = null

    val maxIndex: Int get() = stream.maxIndex

    fun begin() {
        originalIndex = stream.current()
    }

    fun apply(targetIndex: Int) {
        this.targetIndex = targetIndex
        stream.set(targetIndex)
    }

    fun enforce() {
        val target = targetIndex ?: return
        if (stream.current() != target) stream.set(target)
    }

    fun restore() {
        originalIndex?.let { stream.set(it) }
        originalIndex = null
        targetIndex = null
    }
}
