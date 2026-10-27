package com.bytechnum.reveil.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.bytechnum.reveil.di.SettingsPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsStore @Inject constructor(@param:SettingsPreferences private val dataStore: DataStore<Preferences>) {
    val maxVolumePercent: Flow<Int> = dataStore.data.map { it[MAX_VOLUME_PERCENT] ?: DEFAULT_MAX_VOLUME_PERCENT }

    suspend fun setMaxVolumePercent(percent: Int) {
        dataStore.edit { it[MAX_VOLUME_PERCENT] = percent.coerceIn(MIN_MAX_VOLUME_PERCENT, DEFAULT_MAX_VOLUME_PERCENT) }
    }

    private companion object {
        val MAX_VOLUME_PERCENT = intPreferencesKey("max_volume_percent")
        const val DEFAULT_MAX_VOLUME_PERCENT = 100
        const val MIN_MAX_VOLUME_PERCENT = 10
    }
}
