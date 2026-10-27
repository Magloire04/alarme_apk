package com.bytechnum.reveil.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.bytechnum.reveil.domain.alarm.VibrationLevel
import com.bytechnum.reveil.ringing.VibrationOutput
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AlarmVibrator @Inject constructor(@param:ApplicationContext private val context: Context) : VibrationOutput {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    override fun vibrate(level: VibrationLevel) {
        // Motif répété : attente, vibration, pause (ms).
        val timings = when (level) {
            VibrationLevel.NORMAL -> longArrayOf(0, 500, 1000)
            VibrationLevel.STRONG -> longArrayOf(0, 800, 400)
            VibrationLevel.CONTINUOUS -> longArrayOf(0, 1000, 100)
        }
        vibrator.cancel()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> vibrator.vibrate(
                createEffect(timings),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(createEffect(timings), alarmAudioAttributes())
            }

            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, 0, alarmAudioAttributes())
            }
        }
    }

    override fun stop() {
        vibrator.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createEffect(timings: LongArray): VibrationEffect {
        val amplitudes = if (vibrator.hasAmplitudeControl()) {
            intArrayOf(
                0,
                255,
                0,
            )
        } else {
            intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
        }
        return VibrationEffect.createWaveform(timings, amplitudes, 0)
    }

    private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
