package com.bytechnum.reveil.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.bytechnum.reveil.di.RingingPreferences
import com.bytechnum.reveil.domain.model.RingingState
import com.bytechnum.reveil.domain.port.RingingStateStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** État de sonnerie en DataStore, dans le stockage protégé par l'appareil. */
@Singleton
class DataStoreRingingStateStore @Inject constructor(
    @param:RingingPreferences private val dataStore: DataStore<Preferences>,
) : RingingStateStore {
    override suspend fun read(): RingingState? {
        val preferences = dataStore.data.first()
        val alarmId = preferences[ALARM_ID] ?: return null
        val firedAt = preferences[FIRED_AT] ?: return null
        return RingingState(alarmId, firedAt)
    }

    override suspend fun write(state: RingingState) {
        dataStore.edit {
            it[ALARM_ID] = state.alarmId
            it[FIRED_AT] = state.firedAt
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ALARM_ID = intPreferencesKey("alarm_id")
        val FIRED_AT = longPreferencesKey("fired_at")
    }
}
