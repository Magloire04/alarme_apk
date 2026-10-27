package com.bytechnum.reveil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bytechnum.reveil.ui.navigation.AppNavHost
import com.bytechnum.reveil.ui.theme.ReveilTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReveilTheme {
                AppNavHost()
            }
        }
    }
}
