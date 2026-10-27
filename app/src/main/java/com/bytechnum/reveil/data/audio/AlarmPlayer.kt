package com.bytechnum.reveil.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RawRes
import com.bytechnum.reveil.R
import com.bytechnum.reveil.domain.model.SoundId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Joue la sonnerie en boucle sur le flux d'alarme et se met en pause pendant un appel. */
class AlarmPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val audioManager: AudioManager,
) {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var isPaused = false

    fun start(sound: SoundId, onFocusLost: () -> Unit, onFocusGained: () -> Unit) {
        requestFocus(onFocusLost, onFocusGained)
        mediaPlayer = createPlayer(sound).also { it.start() }
        isPaused = false
    }

    fun switchTo(sound: SoundId) {
        mediaPlayer?.release()
        mediaPlayer = createPlayer(sound).also { if (!isPaused) it.start() }
    }

    fun pause() {
        isPaused = true
        mediaPlayer?.pause()
    }

    fun resume() {
        isPaused = false
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        abandonFocus()
    }

    private fun createPlayer(sound: SoundId): MediaPlayer {
        val descriptor = context.resources.openRawResourceFd(sound.toRawResource())
        return MediaPlayer().apply {
            setAudioAttributes(attributes)
            descriptor.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            isLooping = true
            prepare()
        }
    }

    // Seul un appel met la sonnerie en pause : une autre appli qui prend la main ne la coupe pas.
    private fun requestFocus(onFocusLost: () -> Unit, onFocusGained: () -> Unit) {
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            when {
                change == AudioManager.AUDIOFOCUS_GAIN -> onFocusGained()
                change < 0 && isInCall() -> onFocusLost()
            }
        }
        focusListener = listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(listener)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(listener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            focusListener?.let { audioManager.abandonAudioFocus(it) }
        }
        focusRequest = null
        focusListener = null
    }

    private fun isInCall(): Boolean = audioManager.mode in setOf(
        AudioManager.MODE_IN_CALL,
        AudioManager.MODE_IN_COMMUNICATION,
        AudioManager.MODE_RINGTONE,
    )
}

@RawRes
fun SoundId.toRawResource(): Int = when (this) {
    SoundId.SOFT -> R.raw.alarm_soft
    SoundId.CLASSIC -> R.raw.alarm_classic
    SoundId.STRIDENT -> R.raw.alarm_strident
}
