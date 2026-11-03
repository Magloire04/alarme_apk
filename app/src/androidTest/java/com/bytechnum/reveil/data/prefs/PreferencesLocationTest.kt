package com.bytechnum.reveil.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesLocationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val protectedDir = context.createDeviceProtectedStorageContext().dataDir.canonicalPath

    @Test
    fun preferencesFileIsInDeviceProtectedStorage() {
        val file = deviceProtectedPreferencesFile(context, "settings")

        assertTrue("${file.canonicalPath} hors de $protectedDir", file.canonicalPath.startsWith(protectedDir))
    }

    @Test
    fun dataStoreWritesItsFileInDeviceProtectedStorage() = runTest {
        val file = deviceProtectedPreferencesFile(context, "location_test")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }

        dataStore.edit { it[intPreferencesKey("value")] = 1 }

        assertTrue(file.exists())
        scope.cancel()
        file.delete()
    }
}
