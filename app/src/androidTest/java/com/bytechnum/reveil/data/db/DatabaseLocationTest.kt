package com.bytechnum.reveil.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseLocationTest {
    @Test
    fun databaseLivesInDeviceProtectedStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.build(context)

        database.openHelper.writableDatabase
        database.close()

        val expected = context.createDeviceProtectedStorageContext().getDatabasePath(AppDatabase.FILE_NAME)
        assertTrue("Base absente de ${expected.parent}", expected.exists())
    }
}
