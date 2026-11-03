package com.bytechnum.reveil.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Autorisations sans lesquelles une alarme risque de ne pas sonner ou de ne pas s'afficher. */
enum class MissingPermission { NOTIFICATIONS, FULL_SCREEN, EXACT_ALARM }

object Permissions {
    fun findMissingPermissions(context: Context): Set<MissingPermission> = buildSet {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            add(MissingPermission.NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            add(MissingPermission.FULL_SCREEN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        ) {
            add(MissingPermission.EXACT_ALARM)
        }
    }

    /** Écran des réglages où l'utilisateur accorde l'autorisation. */
    fun createSettingsIntent(context: Context, permission: MissingPermission): Intent {
        val packageUri = Uri.fromParts("package", context.packageName, null)
        val sdk = Build.VERSION.SDK_INT
        return when {
            permission == MissingPermission.FULL_SCREEN && sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)

            permission == MissingPermission.EXACT_ALARM && sdk >= Build.VERSION_CODES.S ->
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)

            permission == MissingPermission.NOTIFICATIONS && sdk >= Build.VERSION_CODES.O ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        }
    }
}
