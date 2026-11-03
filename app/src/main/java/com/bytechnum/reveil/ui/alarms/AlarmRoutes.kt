package com.bytechnum.reveil.ui.alarms

import kotlinx.serialization.Serializable

@Serializable
data object AlarmListRoute

@Serializable
data class AlarmEditRoute(val alarmId: Int = NEW_ALARM_ID) {
    companion object {
        const val NEW_ALARM_ID = 0
    }
}
