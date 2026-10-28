package com.bytechnum.reveil.ui.alarms

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bytechnum.reveil.data.audio.toRawResource
import com.bytechnum.reveil.domain.model.SoundId

/** Écoute de trois secondes d'une sonnerie, au volume d'alarme actuel. */
class SoundPreview(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    fun play(sound: SoundId) {
        stop()
        val descriptor = context.resources.openRawResourceFd(sound.toRawResource())
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            descriptor.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            isLooping = true
            prepare()
            start()
        }
        handler.postDelayed(::stop, PREVIEW_MS)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private companion object {
        const val PREVIEW_MS = 3_000L
    }
}

@Composable
fun rememberSoundPreview(): SoundPreview {
    val context = LocalContext.current
    val preview = remember { SoundPreview(context.applicationContext) }
    DisposableEffect(preview) { onDispose { preview.stop() } }
    return preview
}
