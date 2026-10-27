package com.bytechnum.reveil.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.bytechnum.reveil.di.RingingPreferences
import com.bytechnum.reveil.domain.model.RingingState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Sonnerie en cours, relue par le service s'il est relancé par le système. */
@Singleton
class RingingStateStore @Inject constructor(@param:RingingPreferences private val dataStore: DataStore<Preferences>) {
    suspend fun read(): RingingState? {
        val preferences = dataStore.data.first()
        val alarmId = preferences[ALARM_ID] ?: return null
        val firedAt = preferences[FIRED_AT] ?: return null
        return RingingState(alarmId, firedAt)
    }

    suspend fun write(state: RingingState) {
        dataStore.edit {
            it[ALARM_ID] = state.alarmId
            it[FIRED_AT] = state.firedAt
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ALARM_ID = intPreferencesKey("alarm_id")
        val FIRED_AT = longPreferencesKey("fired_at")
    }
}
