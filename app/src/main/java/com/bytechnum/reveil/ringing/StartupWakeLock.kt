package com.bytechnum.reveil.ringing

import android.content.Context
import android.os.PowerManager

/**
 * Garde le processeur éveillé entre la réception de l'alarme et le début du son : le verrou du
 * système s'arrête à la fin de onReceive, et le téléphone pourrait se rendormir avant le service.
 */
object StartupWakeLock {
    private const val TAG = "reveil:startup"
    private const val TIMEOUT_MS = 60_000L
    private var wakeLock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquire(context: Context) {
        val lock = wakeLock ?: context.applicationContext.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(TIMEOUT_MS)
    }

    @Synchronized
    fun release() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }
}
