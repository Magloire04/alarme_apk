package com.bytechnum.reveil.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bytechnum.reveil.ui.alarms.AlarmEditRoute
import com.bytechnum.reveil.ui.alarms.AlarmEditScreen
import com.bytechnum.reveil.ui.alarms.AlarmListRoute
import com.bytechnum.reveil.ui.alarms.AlarmListScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AlarmListRoute) {
        composable<AlarmListRoute> {
            AlarmListScreen(
                onAddAlarm = { navController.navigate(AlarmEditRoute()) },
                onEditAlarm = { alarmId -> navController.navigate(AlarmEditRoute(alarmId)) },
            )
        }
        composable<AlarmEditRoute> {
            AlarmEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
