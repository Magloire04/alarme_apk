package com.bytechnum.reveil.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RawRes
import com.bytechnum.reveil.R
import com.bytechnum.reveil.domain.model.SoundId
import com.bytechnum.reveil.ringing.SoundOutput
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Joue la sonnerie en boucle sur le flux d'alarme. La pause pendant un appel est décidée
 * par le coordinateur, qui surveille l'état d'appel à chaque seconde.
 */
class AlarmPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val audioManager: AudioManager,
) : SoundOutput {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { }
    private var isPaused = false

    override fun start(sound: SoundId) {
        // Jamais deux lecteurs : un second appel remplace le premier au lieu de le laisser tourner.
        stop()
        requestFocus()
        mediaPlayer = createPlayer(sound)?.also { it.start() }
        isPaused = false
    }

    override fun switchTo(sound: SoundId) {
        mediaPlayer?.release()
        mediaPlayer = createPlayer(sound)?.also { if (!isPaused) it.start() }
    }

    override fun pause() {
        isPaused = true
        mediaPlayer?.pause()
    }

    override fun resume() {
        isPaused = false
        requestFocus()
        mediaPlayer?.start()
    }

    override fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        abandonFocus()
    }

    // Si la sonnerie embarquée ne se lit pas, la sonnerie d'alarme du téléphone prend le relais.
    private fun createPlayer(sound: SoundId): MediaPlayer? = try {
        val descriptor = context.resources.openRawResourceFd(sound.toRawResource())
        MediaPlayer().apply {
            setAudioAttributes(attributes)
            descriptor.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            isLooping = true
            prepare()
        }
    } catch (exception: Exception) {
        Log.w(TAG, "Sonnerie embarquée illisible, repli sur celle du téléphone", exception)
        createSystemAlarmPlayer()
    }

    private fun createSystemAlarmPlayer(): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(attributes)
            setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            isLooping = true
            prepare()
        }
    } catch (exception: Exception) {
        Log.e(TAG, "Aucune sonnerie lisible, seule la vibration reste", exception)
        null
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
                .also { focusRequest = it }
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        focusRequest = null
    }

    private companion object {
        const val TAG = "AlarmPlayer"
    }
}

@RawRes
fun SoundId.toRawResource(): Int = when (this) {
    SoundId.SOFT -> R.raw.alarm_soft
    SoundId.CLASSIC -> R.raw.alarm_classic
    SoundId.STRIDENT -> R.raw.alarm_strident
}
