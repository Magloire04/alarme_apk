package com.bytechnum.reveil.ringing

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytechnum.reveil.ui.ringing.RingingScreen
import com.bytechnum.reveil.ui.theme.ReveilTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

/** Écran de sonnerie, affiché par-dessus l'écran verrouillé. Le retour arrière ne le ferme pas. */
@AndroidEntryPoint
class RingingActivity : ComponentActivity() {
    @Inject lateinit var session: RingingSession

    private var hasRung = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        onBackPressedDispatcher.addCallback(this) { }
        setContent {
            val state by session.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                // Ouvert depuis une notification périmée : rien ne sonne, on referme.
                delay(STARTUP_GRACE_MS)
                if (session.state.value == null) finish()
            }
            LaunchedEffect(state == null) {
                if (state == null && hasRung) finish()
                if (state != null) hasRung = true
            }
            LaunchedEffect(state?.stage?.isFullBrightness) {
                setFullBrightness(state?.stage?.isFullBrightness == true)
            }
            ReveilTheme {
                state?.let { RingingScreen(state = it, onStopProvisional = { AlarmService.stopProvisional(this) }) }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setFullBrightness(isFull: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (isFull) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    private companion object {
        const val STARTUP_GRACE_MS = 5_000L
    }
}
