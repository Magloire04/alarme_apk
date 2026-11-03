package com.bytechnum.reveil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.bytechnum.reveil.domain.usecase.RescheduleAll
import com.bytechnum.reveil.ui.navigation.AppNavHost
import com.bytechnum.reveil.ui.theme.ReveilTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var rescheduleAll: RescheduleAll

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Filet de sécurité à chaque ouverture de l'appli. Pas au démarrage du processus :
        // un réveil à froid replanifierait une alarme ponctuelle qui est en train de sonner.
        if (savedInstanceState == null) lifecycleScope.launch { rescheduleAll() }
        enableEdgeToEdge()
        setContent {
            ReveilTheme {
                AppNavHost()
            }
        }
    }
}
