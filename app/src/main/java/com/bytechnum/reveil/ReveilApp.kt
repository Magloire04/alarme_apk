package com.bytechnum.reveil

import android.app.Application
import com.bytechnum.reveil.di.ApplicationScope
import com.bytechnum.reveil.domain.usecase.RescheduleAll
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class ReveilApp : Application() {
    @Inject lateinit var rescheduleAll: RescheduleAll

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Filet de sécurité : chaque démarrage de l'appli replanifie les alarmes actives.
        applicationScope.launch { rescheduleAll() }
    }
}
