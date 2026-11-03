package com.bytechnum.reveil.data.prefs

import android.content.Context
import java.io.File

/**
 * Fichier DataStore dans le stockage protégé par l'appareil, lisible avant le premier déverrouillage.
 * preferencesDataStoreFile() repasse par le contexte applicatif, donc par le stockage chiffré :
 * il ne convient pas ici.
 */
fun deviceProtectedPreferencesFile(context: Context, name: String): File =
    File(context.createDeviceProtectedStorageContext().filesDir, "datastore/$name.preferences_pb")
